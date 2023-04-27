package iudx.data.marketplace.apiserver;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.Constants.AUTH_INFO;
import static iudx.data.marketplace.common.Constants.PRODUCT_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.Constants.PRODUCT_VARIANT_SERVICE_ADDRESS;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.handlers.AuthHandler;
import iudx.data.marketplace.apiserver.handlers.ExceptionHandler;
import iudx.data.marketplace.apiserver.handlers.ValidationHandler;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.product.ProductService;
import iudx.data.marketplace.product.variant.ProductVariantService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProviderApis {
  public static final Logger LOGGER = LogManager.getLogger(ProviderApis.class);

  private final Vertx vertx;
  private final Router router;
  private ProductService productService;
  private ProductVariantService variantService;

  ProviderApis(Vertx vertx, Router router) {
    this.vertx = vertx;
    this.router = router;
  }

  Router init() {

    ValidationHandler productValidationHandler = new ValidationHandler(vertx, RequestType.PRODUCT);
    ValidationHandler variantValidationHandler =
        new ValidationHandler(vertx, RequestType.PRODUCT_VARIANT);
    ValidationHandler datasetValidationHandler = new ValidationHandler(vertx, RequestType.DATASET);
    ExceptionHandler exceptionHandler = new ExceptionHandler();

    productService = ProductService.createProxy(vertx, PRODUCT_SERVICE_ADDRESS);
    variantService = ProductVariantService.createProxy(vertx, PRODUCT_VARIANT_SERVICE_ADDRESS);

    router
        .post(PROVIDER_BASE_PATH + PRODUCT_PATH)
        .consumes(APPLICATION_JSON)
        .handler(productValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleCreateProduct)
        .failureHandler(exceptionHandler);

    router
        .delete(PROVIDER_BASE_PATH + PRODUCT_PATH)
        .handler(productValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleDeleteProduct)
        .failureHandler(exceptionHandler);

    router
        .get(PROVIDER_BASE_PATH + LIST_PRODUCTS_PATH)
        .handler(datasetValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::listProducts)
        .failureHandler(exceptionHandler);

    router
        .get(PROVIDER_BASE_PATH + LIST_PURCHASES_PATH)
        .handler(AuthHandler.create(vertx))
        .handler(this::listPurchases)
        .failureHandler(exceptionHandler);

    router
        .post(PROVIDER_BASE_PATH + PRODUCT_VARIANT_PATH)
        .handler(variantValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleCreateProductVariant)
        .failureHandler(exceptionHandler);

    router
        .put(PROVIDER_BASE_PATH + PRODUCT_VARIANT_PATH)
        .handler(variantValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleUpdateProductVariant)
        .failureHandler(exceptionHandler);

    router
        .get(PROVIDER_BASE_PATH + PRODUCT_VARIANT_PATH)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleGetProductVariants)
        .failureHandler(exceptionHandler);

    router
        .delete(PROVIDER_BASE_PATH + PRODUCT_VARIANT_PATH)
        .handler(variantValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleDeleteProductVariant)
        .failureHandler(exceptionHandler);
    return this.router;
  }

  private void handleCreateProduct(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);

    productService.createProduct(
        requestBody,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(routingContext, 201, handler.result());
          } else {
            String errorMessage = handler.cause().getMessage();
            if (errorMessage.equalsIgnoreCase(ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getUrn())) {
              routingContext.fail(
                  new DxRuntimeException(
                      409,
                      ResponseUrn.RESOURCE_ALREADY_EXISTS_URN,
                      ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getMessage()));
            } else {
              handleFailureResponse(routingContext, handler.cause());
            }
          }
        });
  }

  private void handleDeleteProduct(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = new JsonObject().put(PRODUCT_ID, request.getParam(PRODUCT_ID));
    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);

    productService.deleteProduct(
        requestBody,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(routingContext, 200, handler.result());
          } else {
            String errorMessage = handler.cause().getMessage();
            if (errorMessage.equalsIgnoreCase(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())) {
              routingContext.fail(
                  new DxRuntimeException(
                      404,
                      ResponseUrn.RESOURCE_NOT_FOUND_URN,
                      ResponseUrn.RESOURCE_NOT_FOUND_URN.getMessage()));
            } else {
              handleFailureResponse(routingContext, handler.cause());
            }
          }
        });
  }

  private void listProducts(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = new JsonObject();
    if (request.getParam(DATASET_ID) != null) {
      requestBody.put(DATASET_ID, request.getParam(DATASET_ID));
    }
    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);

    productService.listProducts(
        requestBody,
        handler -> {
          if (handler.succeeded()) {
            if (handler.result().getJsonArray(RESULTS).isEmpty()) {
              handleSuccessResponse(routingContext, 204, handler.result());
            } else {
              handleSuccessResponse(routingContext, 200, handler.result());
            }
          } else {
            handleFailureResponse(routingContext, handler.cause());
          }
        });
  }

  private void listPurchases(RoutingContext routingContext) {
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(404)
        .end(
            new RespBuilder()
                .withType(ResponseUrn.YET_NOT_IMPLEMENTED_URN.getUrn())
                .withTitle(ResponseUrn.YET_NOT_IMPLEMENTED_URN.getMessage())
                .getResponse());
  }

  private void handleCreateProductVariant(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);

    variantService.createProductVariant(
        requestBody,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(routingContext, 201, handler.result());
          } else {
            String errMessage = handler.cause().getMessage();
            if (errMessage.equalsIgnoreCase(ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getUrn())) {
              routingContext.fail(
                  new DxRuntimeException(
                      409,
                      ResponseUrn.RESOURCE_ALREADY_EXISTS_URN,
                      ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getMessage()));

            } else {
              handleFailureResponse(routingContext, handler.cause());
            }
          }
        });
  }

  private void handleUpdateProductVariant(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);

    variantService.updateProductVariant(
        requestBody,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(routingContext, 200, handler.result());
          } else {
            handleFailureResponse(routingContext, handler.cause());
          }
        });
  }

  private void handleGetProductVariants(RoutingContext routingContext) {}

  private void handleDeleteProductVariant(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    JsonObject requestBody =
        new JsonObject()
            .put(AUTH_INFO, authInfo)
            .put(PRODUCT_ID, request.getParam(PRODUCT_ID))
            .put(PRODUCT_VARIANT_NAME, request.getParam(PRODUCT_VARIANT_NAME));

    variantService.deleteProductVariant(
        requestBody,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(routingContext, 200, handler.result());
          } else {
            handleFailureResponse(routingContext, handler.cause());
          }
        });
  }

  private void handleFailureResponse(RoutingContext routingContext, Throwable cause) {
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(400)
        .end(cause.getMessage());
  }

  private void handleSuccessResponse(
      RoutingContext routingContext, int statusCode, JsonObject result) {

    switch (statusCode) {
      case 200:
      case 201:
        routingContext
            .response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(statusCode)
            .end(result.toString());
        break;
      case 204:
        routingContext
            .response()
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(statusCode)
            .end();
        break;
    }
  }
}
