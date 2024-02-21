package iudx.data.marketplace.product.variant;

import static iudx.data.marketplace.product.util.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.product.util.QueryBuilder;
import iudx.data.marketplace.product.util.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProductVariantServiceImpl implements ProductVariantService {

  public static final Logger LOGGER = LogManager.getLogger(ProductVariantServiceImpl.class);
  private final PostgresService pgService;
  private QueryBuilder queryBuilder;

  public ProductVariantServiceImpl(JsonObject config, PostgresService postgresService) {
    this.pgService = postgresService;
    this.queryBuilder = new QueryBuilder(config.getJsonArray(TABLES));
  }

  @Override
  public ProductVariantService createProductVariant(User user,
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String productID = request.getString(PRODUCT_ID);
    String variantName = request.getString(VARIANT);
    Future<Boolean> checkForExistence = checkIfProductVariantExists(productID, variantName);
    Future<JsonObject> productDetailsFuture = getProductDetails(productID);
    JsonArray resources = request.getJsonArray(RESOURCES_ARRAY);

    checkForExistence
        .compose(
            existenceHandler -> {
              if (existenceHandler) {
                return Future.failedFuture(ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getUrn());
              } else {
                return productDetailsFuture;
              }
            })
        .onComplete(
            pdfHandler -> {
              if (pdfHandler.succeeded()) {
                //                  check if the length is 0
                //                  if it is return with forbidden response
                boolean isResultEmpty = pdfHandler.result().getJsonArray(RESULTS).isEmpty();
                if (isResultEmpty) {
                   handler.handle(
                      Future.failedFuture(
                              new RespBuilder()
                                      .withType(ResponseUrn.BAD_REQUEST_URN.getUrn())
                                      .withTitle(ResponseUrn.BAD_REQUEST_URN.getMessage())
                                      .withDetail("Product Variant is only created after product is created").getResponse()
                          ));
                   return;
                }
                JsonObject res = pdfHandler.result().getJsonArray(RESULTS).getJsonObject(0);
                JsonArray resResources = res.getJsonArray(RESOURCES_ARRAY);
                if (resources.size() != resResources.size()) {
                  handler.handle(
                      Future.failedFuture(
                          "Number of resources is incorrect, required : " + resResources.size()));
                }
                int i, j;
                for (i = 0; i < resources.size(); i++) {
                  for (j = 0; j < resResources.size(); j++) {
                    String reqID = resources.getJsonObject(i).getString(ID);
                    String resID = resResources.getJsonObject(j).getString(ID);
                    if (reqID.equalsIgnoreCase(resID)) {
                      resResources.getJsonObject(j).mergeIn(resources.getJsonObject(i));
                      break;
                    }
                  }
                }
                request.mergeIn(res);
                String query = queryBuilder.buildCreateProductVariantQuery(request);
                pgService.executeQuery(
                    query,
                    pgHandler -> {
                      if (pgHandler.succeeded()) {
                        RespBuilder respBuilder =
                            new RespBuilder()
                                .withType(ResponseUrn.SUCCESS_URN.getUrn())
                                .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                                .withResult(
                                    new JsonArray()
                                        .add(
                                            new JsonObject()
                                                .put(PRODUCT_ID, productID)
                                                .put(VARIANT, variantName)));
                        handler.handle(Future.succeededFuture(respBuilder.getJsonResponse()));
                      } else {
                        handler.handle(Future.failedFuture(pgHandler.cause()));
                      }
                    });
              } else {
                handler.handle(Future.failedFuture(pdfHandler.cause()));
              }
            });
    return this;
  }

  private Future<Boolean> checkIfProductVariantExists(String productID, String variantName) {
    Promise<Boolean> promise = Promise.promise();
    String query = queryBuilder.selectProductVariant(productID, variantName);
    pgService.executeCountQuery(
        query,
        handler -> {
          if (handler.succeeded()) {
            if (handler.result().getInteger("totalHits") != 0) promise.complete(true);
            else promise.complete(false);
          } else {
            promise.fail(handler.cause());
          }
        });
    return promise.future();
  }

  Future<JsonObject> getProductDetails(String productID) {
    Promise<JsonObject> promise = Promise.promise();

    String query = queryBuilder.buildProductDetailsQuery(productID);
    LOGGER.debug(query);
    pgService.executeQuery(
        query,
        pgHandler -> {
          if (pgHandler.succeeded()) {
            promise.complete(pgHandler.result());
          } else {
            promise.fail(pgHandler.cause());
          }
        });
    return promise.future();
  }

  @Override
  public ProductVariantService updateProductVariant(User user,
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String productID = request.getString(PRODUCT_ID);
    String variant = request.getString(VARIANT);

    Future<Boolean> updateProductVariantFuture = updateProductVariantStatus(productID, variant);
    updateProductVariantFuture.onComplete(
        updateHandler -> {
          if (updateHandler.result()) {
            createProductVariant(
                    user,
                request,
                insertHandler -> {
                  if (insertHandler.succeeded()) {
                    handler.handle(Future.succeededFuture(insertHandler.result()));
                  } else {
                    handler.handle(Future.failedFuture(insertHandler.cause()));
                  }
                });
          } else {
            throw new DxRuntimeException(
                500, ResponseUrn.DB_ERROR_URN, ResponseUrn.DB_ERROR_URN.getMessage());
          }
        });

    return this;
  }

  Future<Boolean> updateProductVariantStatus(String productID, String variant) {
    Promise<Boolean> promise = Promise.promise();
    String query = queryBuilder.updateProductVariantStatusQuery(productID, variant);

    pgService.executeQuery(
        query,
        pgHandler -> {
          if (pgHandler.succeeded()) {
            LOGGER.debug(pgHandler.result());
            promise.complete(true);
          } else {
            promise.fail(pgHandler.cause());
          }
        });

    return promise.future();
  }

  @Override
  public ProductVariantService deleteProductVariant(User user,
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String productID = request.getString(PRODUCT_ID);
    String variant = request.getString(VARIANT);

    Future<Boolean> updateProductVariantFuture = updateProductVariantStatus(productID, variant);
    updateProductVariantFuture.onComplete(
        updateHandler -> {
          if (updateHandler.result()) {
            RespBuilder respBuilder =
                new RespBuilder()
                    .withType(ResponseUrn.SUCCESS_URN.getUrn())
                    .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                    .withDetail("Successfully deleted");
            handler.handle(Future.succeededFuture(respBuilder.getJsonResponse()));
          } else {
            throw new DxRuntimeException(
                500, ResponseUrn.DB_ERROR_URN, ResponseUrn.DB_ERROR_URN.getMessage());
          }
        });
    return this;
  }

  @Override
  public ProductVariantService listProductVariants(User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug(request);
    String query = queryBuilder.listProductVariants(request);

    JsonObject params = new JsonObject()
        .put(PRODUCT_ID, request.getString(PRODUCT_ID))
        .put(STATUS, Status.ACTIVE.toString());

    if(request.containsKey(VARIANT)) {
      params.put(VARIANT, request.getString(VARIANT));
    }

    pgService.executePreparedQuery(query, params, pgHandler -> {

      if (pgHandler.succeeded()) {
        handler.handle(Future.succeededFuture(pgHandler.result()));
      } else {
        handler.handle(Future.failedFuture(pgHandler.cause()));
      }
    });

    return this;
  }
}
