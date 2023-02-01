package iudx.data.marketplace.product;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.product.util.QueryBuilder;
import iudx.data.marketplace.product.util.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.product.util.Constants.*;

public class ProductServiceImpl implements ProductService {
  private static final Logger LOGGER = LogManager.getLogger(ProductServiceImpl.class);
  private final PostgresService pgService;
  private CatalogueService catService;
  private final String productTableName;
  private QueryBuilder queryBuilder;

  public ProductServiceImpl(
      JsonObject config, PostgresService pgService, CatalogueService catService) {
    this.pgService = pgService;
    this.catService = catService;
    this.queryBuilder = new QueryBuilder(config.getJsonArray(TABLES));
    this.productTableName = config.getJsonArray(TABLES).getString(0);
  }

  @Override
  public ProductService createProduct(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String providerID = request.getJsonObject(AUTH_INFO).getString(IID);
    // TODO: get provider ID from token
    providerID = "datakaveri.org/b8bd3e3f39615c8ec96722131ae95056b5938f2f";
    String productID =
        URN_PREFIX.concat(providerID).concat(":").concat(request.getString(PRODUCT_ID));
    request.put(PROVIDER_ID, providerID).put(PRODUCT_ID, productID);

    Future<Boolean> checkForExistence = checkIfProductExists(providerID, productID);
    Future<JsonObject> getProviderDetails = catService.getItemDetails(providerID);
    JsonArray datasetIDs = request.getJsonArray(DATASETS);
    JsonArray datasetDetails = new JsonArray();

    checkForExistence
        .compose(
            existenceHandler -> {
              if (existenceHandler) {
                throw new DxRuntimeException(
                    409,
                    ResponseUrn.RESOURCE_ALREADY_EXISTS_URN,
                    ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getMessage());
              } else {
                return getProviderDetails;
              }
            })
        .onComplete(
            completeHandler -> {
              if (completeHandler.succeeded()) {
                request.put(PROVIDER_NAME, completeHandler.result().getString(PROVIDER_NAME));
                List<Future> itemFutures = new ArrayList<>();
                List<Future> relFutures = new ArrayList<>();
                datasetIDs.forEach(
                    datasetID -> {
                      Future<JsonObject> getDatasetDetails =
                          catService.getItemDetails((String) datasetID);
                      Future<JsonObject> getResourceCount =
                          catService.getResourceCount((String) datasetID);
                      itemFutures.add(getDatasetDetails);
                      relFutures.add(getResourceCount);
                    });

                itemFutures.forEach(
                    fr -> {
                      fr.onSuccess(
                          h -> {
                            JsonObject res = (JsonObject) h;
                            relFutures.forEach(
                                rf -> {
                                  rf.onSuccess(
                                      j -> {
                                        JsonObject jres = (JsonObject) j;
                                        if (jres.getString(DATASET_ID)
                                            .equalsIgnoreCase(res.getString(DATASET_ID))) {
                                          res.put(TOTAL_RESOURCES, jres.getInteger("totalHits"));
                                          datasetDetails.add(res);
                                        }
                                      });
                                });
                          });
                    });

                CompositeFuture.all(itemFutures)
                    .onComplete(
                        ar -> {
                          CompositeFuture.all(relFutures)
                              .onComplete(
                                  at -> {
                                    List<String> queries =
                                        queryBuilder.buildCreateProductQueries(
                                            request, datasetDetails);

                                    pgService.executeTransaction(
                                        queries,
                                        pgHandler -> {
                                          if (pgHandler.succeeded()) {
                                            LOGGER.debug(pgHandler.result());
                                            handler.handle(
                                                Future.succeededFuture(
                                                    pgHandler.result().put(PRODUCT_ID, productID)));
                                          } else {
                                            LOGGER.error(pgHandler.cause());
                                            handler.handle(Future.failedFuture(pgHandler.cause()));
                                          }
                                        });
                                  });
                        });
              } else {
                LOGGER.debug("error here");
              }
            });
    return this;
  }

  private Future<Boolean> checkIfProductExists(String providerID, String productID) {
    Promise<Boolean> promise = Promise.promise();
    StringBuilder query =
        new StringBuilder(
            SELECT_PRODUCT_QUERY
                .replace("$0", productTableName)
                .replace("$1", providerID)
                .replace("$2", productID));
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
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    JsonObject params =
        new JsonObject()
            .put(STATUS, Status.INACTIVE.toString())
            .put(PRODUCT_ID, request.getString(PRODUCT_ID));

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
    return this;
  }

  @Override
  public ProductService listProducts(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String providerID = request.getString(IID);
    providerID = "datakaveri.org/b8bd3e3f39615c8ec96722131ae95056b5938f2f";
    JsonObject params =
        new JsonObject().put(STATUS, Status.ACTIVE.toString()).put(PROVIDER_ID, providerID);

    if (request.containsKey(DATASET_ID)) {
      params.put(DATASET_ID, request.getString(DATASET_ID));
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
