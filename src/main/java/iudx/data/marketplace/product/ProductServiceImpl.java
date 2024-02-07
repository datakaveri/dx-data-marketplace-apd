package iudx.data.marketplace.product;

import static iudx.data.marketplace.apiserver.provider.linkedAccount.util.Constants.*;
import static iudx.data.marketplace.apiserver.provider.linkedAccount.util.Constants.FAILURE_MESSAGE;
import static iudx.data.marketplace.apiserver.util.Constants.RESULTS;
import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.product.util.Constants.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.product.util.QueryBuilder;
import iudx.data.marketplace.product.util.Status;
import java.util.ArrayList;
import java.util.List;

import iudx.data.marketplace.razorpay.RazorPayService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProductServiceImpl implements ProductService {
  private static final Logger LOGGER = LogManager.getLogger(ProductServiceImpl.class);
  private final PostgresService pgService;
  private final String productTableName;
  private CatalogueService catService;
  private QueryBuilder queryBuilder;
  private RazorPayService razorPayService;
  private boolean isAccountActivationCheckBeingDone;

  public ProductServiceImpl(
          JsonObject config, PostgresService pgService, CatalogueService catService, RazorPayService razorPayService, boolean isAccountActivationCheckBeingDone) {
    this.pgService = pgService;
    this.catService = catService;
    this.queryBuilder = new QueryBuilder(config.getJsonArray(TABLES));
    this.productTableName = config.getJsonArray(TABLES).getString(0);
    this.razorPayService = razorPayService;
    this.isAccountActivationCheckBeingDone = isAccountActivationCheckBeingDone;
  }


  @Override
  public ProductService createProduct(
      User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String providerID = user.getUserId();
    String productID =
        URN_PREFIX.concat(providerID).concat(":").concat(request.getString(PRODUCT_ID));
    request.put(PROVIDER_ID, providerID).put(PRODUCT_ID, productID);

    Future<Boolean> checkForExistence = checkIfProductExists(providerID, productID);
    JsonArray resourceIds = request.getJsonArray(RESOURCE_IDS);
    JsonArray resourceDetails = new JsonArray();

    List<Future> itemFutures = new ArrayList<>();
    resourceIds.forEach(
        resourceID -> {
          Future<JsonObject> getResourceDetails = catService.getItemDetails((String) resourceID);
          itemFutures.add(getResourceDetails);
        });

    itemFutures.forEach(
        fr -> {
          fr.onSuccess(
              h -> {
                JsonObject res = (JsonObject) h;
                resourceDetails.add(res);
              });
        });

    CompositeFuture.all(itemFutures)
        .onComplete(
            ar -> {
              LOGGER.debug(resourceDetails);
              String providerOfFirstResource = resourceDetails.getJsonObject(0).getString(PROVIDER);
              boolean sameProviderForAll = true;
              for (int i = 1; i < resourceDetails.size() && sameProviderForAll; i++) {
                sameProviderForAll =
                    resourceDetails
                        .getJsonObject(i)
                        .getString(PROVIDER)
                        .equalsIgnoreCase(providerOfFirstResource);
              }
              if (!sameProviderForAll) {
                handler.handle(
                    Future.failedFuture("The resources listed belong to different providers"));
              } else {
                String providerItemId = providerOfFirstResource;
                Future<JsonObject> getProviderDetails = catService.getItemDetails(providerItemId);
                checkForExistence
                    .compose(
                        existenceHandler -> {
                          if (existenceHandler) {
                            return Future.failedFuture(
                                ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getUrn());
                          } else {
                            return getProviderDetails;
                          }
                        })
                    .onComplete(
                        completeHandler -> {
                          if (completeHandler.succeeded()) {
                            if (!completeHandler
                                .result()
                                .getString("ownerUserId")
                                .equalsIgnoreCase(providerID)) {
                              handler.handle(
                                  Future.failedFuture(
                                      "The user with given token does not own the resource listed"));
                            } else {
                              request.put(
                                  PROVIDER_NAME,
                                  completeHandler.result().getString(PROVIDER_NAME, ""));

                              List<String> queries =
                                  queryBuilder.buildCreateProductQueries(request, resourceDetails);

                              pgService.executeTransaction(
                                  queries,
                                  pgHandler -> {
                                    if (pgHandler.succeeded()) {
                                      handler.handle(
                                          Future.succeededFuture(
                                              pgHandler.result().put(PRODUCT_ID, productID)));
                                    } else {
                                      LOGGER.error(pgHandler.cause());
                                      handler.handle(Future.failedFuture(pgHandler.cause()));
                                    }
                                  });
                            }
                          } else {
                            handler.handle(Future.failedFuture(completeHandler.cause()));
                          }
                        });
              }
            });

    return this;
  }

  Future<Boolean> checkIfProductExists(String providerID, String productID) {
    Promise<Boolean> promise = Promise.promise();
    StringBuilder query =
        new StringBuilder(
            SELECT_PRODUCT_QUERY
                .replace("$0", productTableName)
                .replace("$1", providerID)
                .replace("$2", productID));

    LOGGER.debug("checkQuery: {}", query);
    pgService.executeCountQuery(
        query.toString(),
        handler -> {
          if (handler.succeeded()) {
            if (handler.result().getInteger("totalHits") != 0) promise.complete(true);
            else promise.complete(false);
          } else {
            throw new DxRuntimeException(
                500, ResponseUrn.DB_ERROR_URN, handler.cause().getLocalizedMessage());
          }
        });
    return promise.future();
  }

  @Override
  public ProductService deleteProduct(
      User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String providerID = user.getUserId();
    String productID = request.getString(PRODUCT_ID);
    JsonObject params =
        new JsonObject().put(STATUS, Status.INACTIVE.toString()).put(PRODUCT_ID, productID);

    checkIfProductExists(providerID, productID)
        .onComplete(
            existsHandler -> {
              LOGGER.error(existsHandler.result());
              if (!existsHandler.result()) {
                LOGGER.error("deletion failed");
                handler.handle(Future.failedFuture(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn()));
              } else {
                pgService.executePreparedQuery(
                    DELETE_PRODUCT_QUERY.replace("$0", productTableName),
                    params,
                    pgHandler -> {
                      if (pgHandler.succeeded()) {
                        handler.handle(Future.succeededFuture(pgHandler.result()));
                      } else {
                        LOGGER.error("deletion failed");
                        handler.handle(Future.failedFuture(pgHandler.cause()));
                      }
                    });
              }
            });
    return this;
  }

  @Override
  public ProductService listProducts(
      User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String providerID = user.getUserId();
    JsonObject params =
        new JsonObject().put(STATUS, Status.ACTIVE.toString()).put(PROVIDER_ID, providerID);

    if (request.containsKey(RESOURCE_ID)) {
      params.put(RESOURCE_ID, request.getString(RESOURCE_ID));
    }

    String query = queryBuilder.buildListProductsQuery(request);

    LOGGER.debug(query);
    pgService.executePreparedQuery(
        query,
        params,
        pgHandler -> {
          if (pgHandler.succeeded()) {
            handler.handle(Future.succeededFuture(pgHandler.result()));
          } else {
            LOGGER.error("list failed");
            handler.handle(Future.failedFuture(pgHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public ProductService checkMerchantAccountStatus(
      User user, Handler<AsyncResult<Boolean>> handler) {
    if (isAccountActivationCheckBeingDone) {
      /* ideal flow to simulate synchronisation */
      //      TODO: Replace this with the providerId from the token
      //        String providerId = user.getUserId();
      String dummyProviderId = "8afb4269-bee4-4a88-9947-128315479eb6";
      Future<JsonObject> providerDetailsFuture =
          fetchRazorpayDetailsOfProvider(FETCH_MERCHANT_INFO_QUERY, dummyProviderId);
      /* checkIfAccountIsActivated */
      Future<Boolean> linkedAccountActivationFuture =
          providerDetailsFuture.compose(
              providerDetailsJson ->
                  razorPayService.fetchProductConfiguration(providerDetailsJson));
      /* update status in merchant_table */
      Future<Boolean> updateStatusFuture =
          linkedAccountActivationFuture.compose(
              isLinkedAccountActivated -> {
                return updateStatusOfLinkedAccount(UPDATE_LINKED_ACCOUNT_STATUS_QUERY, dummyProviderId);
              });
      updateStatusFuture.onComplete(
          updateStatusHandler -> {
            if (updateStatusHandler.succeeded()) {
              handler.handle(Future.succeededFuture(true));
            } else {
              handler.handle(Future.failedFuture(updateStatusFuture.cause().getMessage()));
            }
          });
    } else {
      handler.handle(Future.succeededFuture(true));
    }
    return this;
  }

    public Future<JsonObject> fetchRazorpayDetailsOfProvider(String query, String providerId) {
        Promise<JsonObject> promise = Promise.promise();

        String finalQuery = query.replace("$1", providerId);
        pgService.executeQuery(
                finalQuery,
                handler -> {
                    if (handler.succeeded()) {
                        /*  check if response is empty*/
                        if (!handler.result().getJsonArray(RESULTS).isEmpty()) {
                            JsonObject result = handler.result().getJsonArray(RESULTS).getJsonObject(0);
                            String accountId = result.getString("account_id");
                            String rzp_account_product_id = result.getString("rzp_account_product_id");
                            String status = result.getString("status");
                            LOGGER.info(
                                    "Provider with _id : {} , with accountId {}, accountProductId {} has status : {}",
                                    providerId,
                                    accountId,
                                    rzp_account_product_id,
                                    status);
                            promise.complete(result);
                        } else {
                            LOGGER.fatal(
                                    "The provider information is not inserted in the table after a linked account is "
                                            + "created given that "
                                            + "linked account creation,accepting tnc, insertion of information in table and fetching that "
                                            + "provider info"
                                            + " before product creation is done serially");
                            promise.fail(
                                    new RespBuilder()
                                            .withType(HttpStatusCode.FORBIDDEN.getValue())
                                            .withTitle(ResponseUrn.FORBIDDEN_PRODUCT_CREATION.getUrn())
                                            .withDetail(ResponseUrn.FORBIDDEN_PRODUCT_CREATION.getMessage() + " as, linked account is not created")
                                            .getResponse());
                        }
                    } else {
                        LOGGER.info(
                                "Failed to fetch razorpay details of provider : " + handler.cause().getMessage());
                        promise.fail(
                                new RespBuilder()
                                        .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                                        .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                                        .withDetail(FAILURE_MESSAGE + "Internal Server Error")
                                        .getResponse());
                    }
                });
        return promise.future();
    }

    public Future<Boolean> updateStatusOfLinkedAccount(String query, String providerId)
    {
        Promise<Boolean> promise = Promise.promise();
        String finalQuery =
                query.replace("$1", providerId);
        pgService.executeQuery(
                finalQuery,
                handler -> {
                    if (handler.succeeded()) {
                        /*  check if response is empty*/
                        if (!handler.result().getJsonArray(RESULTS).isEmpty()) {
                            JsonObject result = handler.result().getJsonArray(RESULTS).getJsonObject(0);
                            String referenceId = result.getString("reference_id");
                            LOGGER.info(
                                    "Provider with _id : {} , with referenceId {}, has status of the linked account : {}",
                                    providerId,
                                    referenceId,
                                    "activated");
                            promise.complete(true);
                        } else {
                            LOGGER.fatal(
                                    "The provider linked account status is not updated in the table after a linked account is "
                                            + "created given that "
                                            + "linked account creation, accepting tnc, insertion of information in table and fetching that "
                                            + "provider info"
                                            + " before product creation is done serially");
                            promise.fail(
                                    new RespBuilder()
                                            .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                                            .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                                            .withDetail(FAILURE_MESSAGE + "Internal Server Error")
                                            .getResponse());
                        }
                    } else {
                        LOGGER.info(
                                "Failed to fetch razorpay details of provider : " + handler.cause().getMessage());
                        promise.fail(
                                new RespBuilder()
                                        .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                                        .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                                        .withDetail(FAILURE_MESSAGE + "Internal Server Error")
                                        .getResponse());
                    }
                });
        return promise.future();
    }

}
