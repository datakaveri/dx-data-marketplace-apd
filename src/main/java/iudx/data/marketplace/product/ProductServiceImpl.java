package iudx.data.marketplace.product;

import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.product.util.Constants.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.product.util.QueryBuilder;
import iudx.data.marketplace.product.util.Status;
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

  public ProductServiceImpl(
      JsonObject config, PostgresService pgService, CatalogueService catService) {
    this.pgService = pgService;
    this.catService = catService;
    this.queryBuilder = new QueryBuilder(config.getJsonArray(TABLES));
    this.productTableName = config.getJsonArray(TABLES).getString(0);
  }

  @Override
  public ProductService createProduct(User user,
                                      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
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

    LOGGER.debug("checkQuery: {}",query);
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
  public ProductService deleteProduct( User user,
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

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
  public ProductService listProducts(User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String providerID = user.getUserId();
    JsonObject params =
        new JsonObject().put(STATUS, Status.ACTIVE.toString()).put(PROVIDER_ID, providerID);

    if (request.containsKey(RESOURCE_ID)) {
      params.put(RESOURCE_ID, request.getString(RESOURCE_ID));
    }

    String query = queryBuilder.buildListProductsQuery(request);

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
}
