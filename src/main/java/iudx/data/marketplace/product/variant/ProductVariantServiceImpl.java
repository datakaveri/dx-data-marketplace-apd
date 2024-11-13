package iudx.data.marketplace.product.variant;

import static iudx.data.marketplace.apiserver.util.Constants.DETAIL;
import static iudx.data.marketplace.apiserver.util.Constants.TITLE;
import static iudx.data.marketplace.product.util.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.common.Util;
import iudx.data.marketplace.consumer.util.PaymentStatus;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.product.util.QueryBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProductVariantServiceImpl implements ProductVariantService {

  public static final Logger LOGGER = LogManager.getLogger(ProductVariantServiceImpl.class);
  private final PostgresService pgService;
  private final Util util;
  private final QueryBuilder queryBuilder;

  public ProductVariantServiceImpl(JsonObject config, PostgresService postgresService, Util util) {
    this.pgService = postgresService;
    this.queryBuilder = new QueryBuilder(config.getJsonArray(TABLES));
    this.util = util;
  }

  @Override
  public ProductVariantService createProductVariant(
      User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String productId = request.getString(PRODUCT_ID);
    String variantName = request.getString(PRODUCT_VARIANT_NAME);
    String providerId = user.getUserId();
    Future<JsonObject> productDetailsFuture = getProductDetails(productId, providerId);
    JsonArray resources = request.getJsonArray(RESOURCES_ARRAY);
    Future<Boolean> checkIfProductExists = checkForExistenceOfProduct(productId, providerId);

    Future<Boolean> checkForExistence =
        checkIfProductExists.compose(
            isProductFound -> {
              if (isProductFound) {
                return checkIfProductVariantExists(productId, variantName);
              } else {
                String failureMessage =
                    new RespBuilder()
                        .withType(ResponseUrn.BAD_REQUEST_URN.getUrn())
                        .withTitle(ResponseUrn.BAD_REQUEST_URN.getMessage())
                        .withDetail(
                            "Product Variant cannot be created as product is in INACTIVE state or is not found")
                        .getResponse();
                return Future.failedFuture(failureMessage);
              }
            });
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
                              .withType(ResponseUrn.FORBIDDEN_URN.getUrn())
                              .withTitle(ResponseUrn.FORBIDDEN_URN.getMessage())
                              .withDetail(
                                  "Product Variant is only created after product is created")
                              .getResponse()));
                  return;
                }
                JsonObject res = pdfHandler.result().getJsonArray(RESULTS).getJsonObject(0);
                JsonArray resResources = res.getJsonArray(RESOURCES_ARRAY);
                if (resources.size() != resResources.size()) {
                  /* if the number of resources listed while creating product variant !=
                   * number of resources listed while creating product
                   * then bad request is thrown and returned without creating product variant */
                  String detail =
                      "Number of resources is incorrect, required : " + resResources.size();
                  String failureMessage =
                      new RespBuilder()
                          .withType(ResponseUrn.BAD_REQUEST_URN.getUrn())
                          .withTitle(ResponseUrn.BAD_REQUEST_URN.getMessage())
                          .withDetail(detail)
                          .getResponse();
                  handler.handle(Future.failedFuture(failureMessage));
                  return;
                }
                int i, j;
                for (i = 0; i < resources.size(); i++) {
                  for (j = 0; j < resResources.size(); j++) {
                    String reqId = resources.getJsonObject(i).getString(ID);
                    String resId = resResources.getJsonObject(j).getString(ID);
                    if (reqId.equalsIgnoreCase(resId)) {
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
                        /* get the product variant ID */
                        String productVariantId =
                            pgHandler
                                .result()
                                .getJsonArray(RESULTS)
                                .getJsonObject(0)
                                .getString("_id");
                        RespBuilder respBuilder =
                            new RespBuilder()
                                .withType(ResponseUrn.SUCCESS_URN.getUrn())
                                .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                                .withResult(
                                    new JsonArray()
                                        .add(
                                            new JsonObject()
                                                .put("productVariantId", productVariantId)
                                                .put("productId", productId)
                                                .put(PRODUCT_VARIANT_NAME, variantName)))
                                .withDetail("Product Variant created successfully");
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

  private Future<Boolean> checkIfProductVariantExists(String productId, String variantName) {
    Promise<Boolean> promise = Promise.promise();
    String query = queryBuilder.selectProductVariant(productId, variantName);
    pgService.executeCountQuery(
        query,
        handler -> {
          if (handler.succeeded()) {
            if (handler.result().getInteger("totalHits") != 0) {
              promise.complete(true);
            }
            else {
              promise.complete(false);
            }
          } else {
            promise.fail(handler.cause());
          }
        });
    return promise.future();
  }

  Future<JsonObject> getProductDetails(String productId, String providerId) {
    Promise<JsonObject> promise = Promise.promise();

    String query = queryBuilder.buildProductDetailsQuery(productId, providerId);
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

  Future<Boolean> checkForExistenceOfProduct(String productId, String providerId) {
    Promise<Boolean> promise = Promise.promise();
    String query = queryBuilder.checkIfProductExists(productId, providerId);
    pgService.executeQuery(
        query,
        handler -> {
          if (handler.succeeded()) {
            /* check if product is not found */
            boolean isEmpty = handler.result().getJsonArray(RESULTS).isEmpty();
            if (!isEmpty) {
              LOGGER.debug("product is found");
              JsonObject result = handler.result().getJsonArray(RESULTS).getJsonObject(0);
              boolean isProductInactive = result.getString("status").equalsIgnoreCase("inactive");
              boolean isProductBelongingToProvider =
                  result.getString("provider_id").equals(providerId);
              if (isProductInactive) {
                LOGGER.error("product is already deleted");
                promise.complete(false);
              } else if (!isProductBelongingToProvider) {
                LOGGER.error("ownership check failed");
                String failureMessage =
                    new RespBuilder()
                        .withType(ResponseUrn.FORBIDDEN_URN.getUrn())
                        .withTitle(ResponseUrn.FORBIDDEN_URN.getMessage())
                        .withDetail(
                            "Product variant cannot be created, as the provider does not own the product")
                        .getResponse();
                promise.fail(failureMessage);
              } else {
                LOGGER.debug("Product is found for creating product variant");
                promise.complete(true);
              }
            } else {
              LOGGER.error("Product is not found");
              promise.complete(false);
            }
          } else {
            LOGGER.error("Failure while checking the existence of product : " + handler.cause());
            String failureMessage =
                new RespBuilder()
                    .withType(ResponseUrn.DB_ERROR_URN.getUrn())
                    .withTitle(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                    .withDetail("Internal Server Error")
                    .getResponse();
            promise.fail(failureMessage);
          }
        });

    return promise.future();
  }

  @Override
  public ProductVariantService updateProductVariant(
      User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String productId = request.getString(PRODUCT_ID);
    String variant = request.getString(PRODUCT_VARIANT_NAME);
    /* check if the product variant exists */
    String query = queryBuilder.checkProductVariantExistence(productId, variant);
    pgService.executeQuery(
        query,
        existenceHandler -> {
          if (existenceHandler.succeeded()) {
            boolean isEmpty = existenceHandler.result().getJsonArray(RESULTS).isEmpty();
            if (!isEmpty) {

              Future<Boolean> updateProductVariantFuture =
                  updateProductVariantStatus(productId, variant);
              updateProductVariantFuture.onComplete(
                  updateHandler -> {
                    if (updateHandler.result()) {
                      createProductVariant(
                          user,
                          request,
                          insertHandler -> {
                            if (insertHandler.succeeded()) {
                              handler.handle(
                                  Future.succeededFuture(
                                      insertHandler
                                          .result()
                                          .put(DETAIL, "Product Variant updated successfully")));
                            } else {
                              handler.handle(Future.failedFuture(insertHandler.cause()));
                            }
                          });
                    } else {
                      throw new DxRuntimeException(
                          500, ResponseUrn.DB_ERROR_URN, ResponseUrn.DB_ERROR_URN.getMessage());
                    }
                  });
            } else {
              handler.handle(
                  Future.failedFuture(
                      new RespBuilder()
                          .withType(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                          .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getMessage())
                          .withDetail(
                              "Product Variant cannot be updated as the product is in INACTIVE state or "
                                  +
                                  "product is not found")
                          .getResponse()));
            }

          } else {
            throw new DxRuntimeException(
                500, ResponseUrn.DB_ERROR_URN, ResponseUrn.DB_ERROR_URN.getMessage());
          }
        });

    return this;
  }

  Future<Boolean> updateProductVariantStatus(String productId, String variant) {
    Promise<Boolean> promise = Promise.promise();
    String query = queryBuilder.updateProductVariantStatusQuery(productId, variant);

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

  Future<JsonObject> updateProductVariantStatus(String productVariantId) {
    Promise<JsonObject> promise = Promise.promise();
    String query = queryBuilder.updateProductVariantStatusQuery(productVariantId);

    pgService.executeQuery(
        query,
        pgHandler -> {
          if (pgHandler.succeeded()) {
            LOGGER.debug(pgHandler.result());
            boolean isResultsEmpty = pgHandler.result().getJsonArray(RESULTS).isEmpty();
            if (isResultsEmpty) {
              /* the product variant id that is to be deleted, does not exist or is already in inactive status*/
              RespBuilder respBuilder =
                  new RespBuilder()
                      .withType(HttpStatusCode.NOT_FOUND.getValue())
                      .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                      .withDetail("Product variant not found");
              promise.fail(respBuilder.getResponse());
            } else {
              RespBuilder respBuilder =
                  new RespBuilder()
                      .withType(ResponseUrn.SUCCESS_URN.getUrn())
                      .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                      .withDetail("Successfully deleted");
              promise.complete(respBuilder.getJsonResponse());
            }
          } else {
            promise.fail(pgHandler.cause());
            throw new DxRuntimeException(
                500, ResponseUrn.DB_ERROR_URN, ResponseUrn.DB_ERROR_URN.getMessage());
          }
        });

    return promise.future();
  }

  @Override
  public ProductVariantService deleteProductVariant(
      User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String productVariantId = request.getString("productVariantId");

    updateProductVariantStatus(productVariantId)
        .onComplete(
            updateHandler -> {
              if (updateHandler.succeeded()) {
                handler.handle(Future.succeededFuture(updateHandler.result()));
              } else {
                handler.handle(Future.failedFuture(updateHandler.cause().getMessage()));
              }
            });
    return this;
  }

  @Override
  public ProductVariantService listProductVariants(
      User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug(request);
    String resourceServerUrl = user.getResourceServerUrl();
    String providerId = user.getUserId();
    String query = queryBuilder.listProductVariants(request, providerId);

    JsonObject params =
        new JsonObject()
            .put(PRODUCT_ID, request.getString(PRODUCT_ID))
            .put("resourceServerUrl", resourceServerUrl)
            .put("providerId", providerId);

    if (request.containsKey(PRODUCT_VARIANT_NAME)) {
      params.put(PRODUCT_VARIANT_NAME, request.getString(PRODUCT_VARIANT_NAME));
    }

    pgService.executePreparedQuery(
        query,
        params,
        pgHandler -> {
          if (pgHandler.succeeded()) {
            if (pgHandler.result().getJsonArray(RESULTS).isEmpty()) {
              String failureMessage =
                  new RespBuilder()
                      .withType(HttpStatusCode.NOT_FOUND.getValue())
                      .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                      .withDetail("Product variants not found")
                      .getResponse();
              handler.handle(Future.failedFuture(failureMessage));
            } else {
              handler.handle(Future.succeededFuture(pgHandler.result()));
            }
          } else {
            LOGGER.error("Failure : " + pgHandler.cause());

            String failureMessage =
                new RespBuilder()
                    .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                    .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                    .withDetail(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                    .getResponse();
            handler.handle(Future.failedFuture(failureMessage));
          }
        });

    return this;
  }

  @Override
  public ProductVariantService listPurchase(
      User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String resourceId = request.getString("resourceId");
    String productId = request.getString("productId");
    String resourceServerUrl = user.getResourceServerUrl();
    try {
      PaymentStatus paymentStatus = PaymentStatus.fromString(request.getString("paymentStatus"));

      String query;

      if (paymentStatus.equals(PaymentStatus.SUCCESSFUL)) {
        query =
            queryBuilder.listSuccessfulPurchaseForProvider(
                user.getUserId(), resourceId, productId, resourceServerUrl);
      } else if (paymentStatus.equals(PaymentStatus.FAILED)) {
        query =
            queryBuilder.listPurchaseForProviderDuringFailedPayment(
                user.getUserId(), resourceId, productId, resourceServerUrl);
      } else {
        query =
            queryBuilder.listPurchaseForProviderDuringPendingStatus(
                user.getUserId(), resourceId, productId, resourceServerUrl);
      }

      Future<JsonArray> paymentFuture = executePurchaseQuery(query, resourceId, productId, user);
      //      Future<JsonArray> userResponseFuture =
      paymentFuture.onComplete(
          pgHandler -> {
            if (pgHandler.succeeded()) {
              JsonObject response =
                  new JsonObject()
                      .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                      .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                      .put(RESULTS, pgHandler.result());
              handler.handle(Future.succeededFuture(response));

            } else {
              handler.handle(Future.failedFuture(pgHandler.cause().getMessage()));
            }
          });

    } catch (DxRuntimeException exception) {
      LOGGER.debug("Exception : " + exception.getMessage());
      String failureMessage =
          new RespBuilder()
              .withType(HttpStatusCode.BAD_REQUEST.getValue())
              .withTitle(ResponseUrn.BAD_REQUEST_URN.getUrn())
              .withDetail("Invalid payment status")
              .getResponse();
      handler.handle(Future.failedFuture(failureMessage));
    }

    return this;
  }

  public Future<JsonArray> executePurchaseQuery(
      String query, String resourceId, String productId, User user) {
    Promise<JsonArray> promise = Promise.promise();
    pgService.executeQuery(
        query,
        queryHandler -> {
          if (queryHandler.succeeded()) {
            LOGGER.debug("Fetched invoice related information from postgres successfully");
            JsonArray result = queryHandler.result().getJsonArray(RESULTS);
            if (!result.isEmpty()) {
              JsonArray userResponse = new JsonArray();
              for (Object row : result) {
                JsonObject rowEntry = JsonObject.mapFrom(row);

                // gets provider info, consumer info, product info
                rowEntry
                    .mergeIn(util.getUserJsonFromRowEntry(rowEntry, Role.CONSUMER))
                    .mergeIn(util.generateUserJson(user))
                    .mergeIn(util.getProductInfo(rowEntry));
                userResponse.add(rowEntry);
              }
              promise.complete(userResponse);
            } else {
              LOGGER.debug(
                  "No invoice present for the given resource "
                      + ": {} or product : {}, for the provider : {}",
                  resourceId,
                  productId,
                  user.getUserId());

              boolean isAnyQueryParamSent =
                  StringUtils.isNotBlank(resourceId) || StringUtils.isNotBlank(productId);

              String failureMessage =
                  new RespBuilder()
                      .withType(HttpStatusCode.NO_CONTENT.getValue())
                      .withTitle(HttpStatusCode.NO_CONTENT.getUrn())
                      .getResponse();
              if (isAnyQueryParamSent) {
                failureMessage =
                    new RespBuilder()
                        .withType(HttpStatusCode.NOT_FOUND.getValue())
                        .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                        .withDetail("Purchase info not found")
                        .getResponse();
              }
              promise.fail(failureMessage);
            }
          } else {
            String failureMessage =
                new RespBuilder()
                    .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                    .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                    .withDetail(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                    .getResponse();
            promise.fail(failureMessage);
          }
        });
    return promise.future();
  }
}
