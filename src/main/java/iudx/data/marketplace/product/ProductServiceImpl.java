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
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.product.util.QueryBuilder;
import iudx.data.marketplace.product.util.Status;
import iudx.data.marketplace.razorpay.RazorPayService;
import java.util.ArrayList;
import java.util.List;
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
  private String apdUrl;

  public ProductServiceImpl(
      JsonObject config,
      PostgresService pgService,
      CatalogueService catService,
      RazorPayService razorPayService,
      boolean isAccountActivationCheckBeingDone) {
    this.pgService = pgService;
    this.catService = catService;
    this.queryBuilder = new QueryBuilder(config.getJsonArray(TABLES));
    this.productTableName = config.getJsonArray(TABLES).getString(0);
    this.razorPayService = razorPayService;
    this.isAccountActivationCheckBeingDone = isAccountActivationCheckBeingDone;
    this.apdUrl = config.getString(APD_URL);
  }

  private static boolean isSameProviderForAll(JsonArray resourceDetails) {
    LOGGER.debug("resourceDetails : " + resourceDetails);
    String providerOfFirstResource = resourceDetails.getJsonObject(0).getString(PROVIDER);
    boolean sameProviderForAll = true;
    for (int i = 1; i < resourceDetails.size() && sameProviderForAll; i++) {
      sameProviderForAll =
          resourceDetails
              .getJsonObject(i)
              .getString(PROVIDER)
              .equalsIgnoreCase(providerOfFirstResource);
    }
    return sameProviderForAll;
  }

    private boolean isSameApdUrlForAll(JsonArray resourceDetails) {
        LOGGER.debug("current APD URL is : {}",  this.apdUrl);
        String currentApdUrl = this.apdUrl;
        boolean sameApdUrl = true;
        for (int i = 0; i < resourceDetails.size() && sameApdUrl; i++) {
            String apdUrlOfResource = resourceDetails.getJsonObject(i).getString(APD_URL);
            LOGGER.debug("APD URL of the current resource is : {}", apdUrlOfResource);
            sameApdUrl =
                    apdUrlOfResource.equals(currentApdUrl);
        }
        return sameApdUrl;
    }

  @Override
  public ProductService createProduct(
      User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    JsonArray resourceDetails = new JsonArray();
    String providerID = user.getUserId();
    String productID =
        URN_PREFIX.concat(providerID).concat(":").concat(request.getString(PRODUCT_ID));

    /* Check Merchant Account Existence on RazorPay */
    checkMerchantAccountStatus(user)
        .compose(
            merchantAccountStatus -> {
              List<Future> itemFutures =
                  fetchItemDetailsFromCat(request, providerID, productID, resourceDetails);

              return CompositeFuture.all(itemFutures);
            })
        .compose(
            isApdUrlValid -> {
              boolean isApdUrlSame = isSameApdUrlForAll(resourceDetails);
              if (isApdUrlSame) {
                return isApdUrlValid;
              }
              return Future.failedFuture(
                  "The resource is forbidden to access as the resource belongs to a different APD");
            })
        /* Check if all resources belong to the same provider */
        .compose(
            ar -> {
              boolean sameProviderForAll = isSameProviderForAll(resourceDetails);
              if (!sameProviderForAll) {
                return Future.failedFuture("The resources listed belong to different providers");
              } else {
                return checkIfProductExists(providerID, productID);
              }
            })
        /* Check if product already exists */
        .compose(
            existenceHandler -> {
              if (existenceHandler) {
                return Future.failedFuture(ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getMessage());
              } else {
                String providerItemId = resourceDetails.getJsonObject(0).getString(PROVIDER);
                return catService.getItemDetails(providerItemId);
              }
            })
        /* Check if provider of resources matches the provider user */
        .onComplete(
            completeHandler -> {
              if (completeHandler.succeeded()) {
                if (!completeHandler
                    .result()
                    .getString("ownerUserId")
                    .equalsIgnoreCase(providerID)) {

                  handler.handle(
                      Future.failedFuture(
                          new RespBuilder()
                              .withType(ResponseUrn.FORBIDDEN_URN.getUrn())
                              .withTitle(ResponseUrn.FORBIDDEN_URN.getMessage())
                              .withDetail(
                                  "The user with given token does not own the resource listed")
                              .getResponse()));
                } else {
                  request
                      .put(PROVIDER_NAME, completeHandler.result().getString(PROVIDER_NAME, ""))
                      .put(RESOURCE_SERVER, user.getResourceServerUrl());

                  List<String> queries =
                      queryBuilder.buildCreateProductQueries(request, resourceDetails);

                  /* Finally Create the Product */
                  pgService.executeTransaction(
                      queries,
                      pgHandler -> {
                        if (pgHandler.succeeded()) {
                          JsonObject result =
                              new RespBuilder()
                                  .withType(ResponseUrn.SUCCESS_URN.getUrn())
                                  .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                                  .withResult(new JsonObject().put(PRODUCT_ID, productID))
                                  .withDetail("Product created successfully")
                                  .getJsonResponse();
                          handler.handle(Future.succeededFuture(result));
                        } else {
                          LOGGER.error(pgHandler.cause());
                          handler.handle(Future.failedFuture(pgHandler.cause()));
                        }
                      });
                }
              } else {
                String failureMessage =
                    new RespBuilder()
                        .withType(ResponseUrn.FORBIDDEN_PRODUCT_CREATION.getUrn())
                        .withTitle(ResponseUrn.FORBIDDEN_PRODUCT_CREATION.getMessage())
                        .withDetail(completeHandler.cause().getLocalizedMessage())
                        .getResponse();

                if (completeHandler
                    .cause()
                    .getMessage()
                    .contains(ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getMessage())) {

                  failureMessage =
                      new RespBuilder()
                          .withType(ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getUrn())
                          .withTitle(ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getMessage())
                          .withDetail("Product already exists")
                          .getResponse();
                }
                handler.handle(Future.failedFuture(failureMessage));
              }
            });

    return this;
  }

  private List<Future> fetchItemDetailsFromCat(
      JsonObject request, String providerID, String productID, JsonArray resourceDetails) {
    request.put(PROVIDER_ID, providerID).put(PRODUCT_ID, productID);

    JsonArray resourceIds = request.getJsonArray(RESOURCE_IDS);

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
    return itemFutures;
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
            if (handler.result().getInteger("totalHits") != 0) {
              promise.complete(true);
            } else {
              promise.complete(false);
            }
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

    JsonObject deleteProductVariantParams =
        new JsonObject().put(PRODUCT_ID, productID).put(PROVIDER_ID, providerID);

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
                        if (!pgHandler.result().getJsonArray(RESULTS).isEmpty()) {
                          LOGGER.debug(
                              "Successfully deleted product : {}",
                              pgHandler.result().encodePrettily());

                          pgService.executePreparedQuery(
                              DELETE_PV_QUERY,
                              deleteProductVariantParams,
                              deleteProductVariantHandler -> {
                                if (deleteProductVariantHandler.succeeded()) {
                                  LOGGER.info(
                                      "Product variants deleted successfully : {}",
                                      deleteProductVariantHandler.result());
                                  RespBuilder respBuilder =
                                      new RespBuilder()
                                          .withType(ResponseUrn.SUCCESS_URN.getUrn())
                                          .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                                          .withDetail("Successfully deleted");
                                  handler.handle(
                                      Future.succeededFuture(respBuilder.getJsonResponse()));
                                } else {
                                  LOGGER.error(
                                      "Failed to delete product variants : "
                                          + deleteProductVariantHandler.cause());
                                  RespBuilder respBuilder =
                                      new RespBuilder()
                                          .withType(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn())
                                          .withTitle(
                                              ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                                          .withDetail(
                                              "Something went wrong while deleting the product variants");
                                  handler.handle(Future.failedFuture(respBuilder.getResponse()));
                                }
                              });

                        } else {
                          /* product has been previously deleted */
                          RespBuilder respBuilder =
                              new RespBuilder()
                                  .withType(ResponseUrn.BAD_REQUEST_URN.getUrn())
                                  .withTitle(ResponseUrn.BAD_REQUEST_URN.getMessage())
                                  .withDetail(
                                      "Product cannot be deleted, as it was deleted previously");
                          handler.handle(Future.failedFuture(respBuilder.getResponse()));
                        }

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
    String resourceServerUrl = user.getResourceServerUrl();
    JsonObject params =
        new JsonObject()
            .put(STATUS, Status.ACTIVE.toString())
            .put(PROVIDER_ID, providerID)
            .put("resourceServerUrl", resourceServerUrl);

    if (request.containsKey("resourceId")) {
      params.put("resourceId", request.getString("resourceId"));
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

  /**
   * Checks if the provider / merchant have filled their account details in Razorpay and completed
   * their KYC before creating a product on Data Market place server
   *
   * @param user Provider user object
   * @return ProductService object
   */
  Future<Void> checkMerchantAccountStatus(User user) {
    Promise<Void> promise = Promise.promise();

    if (isAccountActivationCheckBeingDone) {
      /* ideal flow to simulate synchronisation */
      String providerId = user.getUserId();
      Future<JsonObject> providerDetailsFuture =
          fetchRazorpayDetailsOfProvider(FETCH_MERCHANT_INFO_QUERY, providerId);

      ResultContainer resultContainer = new ResultContainer();

      /* check if account status is activated in database */
      providerDetailsFuture
          .compose(
              json -> {
                resultContainer.resultJson = json;
                boolean isStatusActivated = json.getString("status").equalsIgnoreCase("ACTIVATED");
                return Future.succeededFuture(isStatusActivated);
              })
          .compose(
              isAccountStatusActive -> {
                if (!isAccountStatusActive) {
                  /* check if account is activated in Razorpay */
                  return razorPayService.fetchProductConfiguration(
                      resultContainer.resultJson); /* update status in merchant_table */
                }
                /* account is activated */
                return Future.succeededFuture(true);
              })
          .compose(
              isLinkedAccountActivated ->
                  updateStatusOfLinkedAccount(UPDATE_LINKED_ACCOUNT_STATUS_QUERY, providerId))
          .onComplete(
              updateStatusHandler -> {
                if (updateStatusHandler.succeeded()) {
                  promise.complete();
                } else {
                  promise.fail(updateStatusHandler.cause().getMessage());
                }
              });

    } else {
      promise.complete();
    }
    return promise.future();
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
                  ResponseUrn.FORBIDDEN_PRODUCT_CREATION.getMessage()
                      + " as, linked account is not created");
            }
          } else {
            LOGGER.info(
                "Failed to fetch razorpay details of provider : " + handler.cause().getMessage());
            promise.fail(FAILURE_MESSAGE + "Internal Server Error");
          }
        });
    return promise.future();
  }

  public Future<Boolean> updateStatusOfLinkedAccount(String query, String providerId) {
    Promise<Boolean> promise = Promise.promise();
    String finalQuery = query.replace("$1", providerId);
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
              promise.fail(FAILURE_MESSAGE + "Internal Server Error");
            }
          } else {
            LOGGER.info(
                "Failed to fetch razorpay details of provider : " + handler.cause().getMessage());
            promise.fail(FAILURE_MESSAGE + "Internal Server Error");
          }
        });
    return promise.future();
  }

  private final class ResultContainer {
    JsonObject resultJson;
  }
}
