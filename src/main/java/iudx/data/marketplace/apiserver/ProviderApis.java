package iudx.data.marketplace.apiserver;

import static iudx.data.marketplace.apiserver.response.ResponseUtil.generateResponse;
import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.Constants.AUTH_INFO;
import static iudx.data.marketplace.common.Constants.PRODUCT_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.Constants.PRODUCT_VARIANT_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.HttpStatusCode.BAD_REQUEST;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.handlers.AuthHandler;
import iudx.data.marketplace.apiserver.handlers.ExceptionHandler;
import iudx.data.marketplace.apiserver.handlers.ValidationHandler;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.authenticator.AuthClient;
import iudx.data.marketplace.authenticator.AuthenticationService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.product.ProductService;
import iudx.data.marketplace.product.variant.ProductVariantService;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProviderApis {
  public static final Logger LOGGER = LogManager.getLogger(ProviderApis.class);

  private final Vertx vertx;
  private final Router router;
  private ProductService productService;
  private ProductVariantService variantService;
  private Api api;
  private PostgresService postgresService;
  private String detail;
  private AuthClient authClient;
  private AuthenticationService authenticationService;

  ProviderApis(
      Vertx vertx,
      Router router,
      Api apis,
      PostgresService postgresService,
      AuthClient authClient,
      AuthenticationService authenticationService) {
    this.vertx = vertx;
    this.router = router;
    this.api = apis;
    this.postgresService = postgresService;
    this.authClient = authClient;
    this.authenticationService = authenticationService;
  }

  Router init() {

    ValidationHandler productValidationHandler = new ValidationHandler(vertx, RequestType.PRODUCT);
    ValidationHandler variantValidationHandler =
        new ValidationHandler(vertx, RequestType.PRODUCT_VARIANT);
    ValidationHandler resourceValidationHandler =
        new ValidationHandler(vertx, RequestType.RESOURCE);
    ExceptionHandler exceptionHandler = new ExceptionHandler();

    productService = ProductService.createProxy(vertx, PRODUCT_SERVICE_ADDRESS);
    variantService = ProductVariantService.createProxy(vertx, PRODUCT_VARIANT_SERVICE_ADDRESS);

    router
        .post(PROVIDER_PATH + PRODUCT_PATH)
        .consumes(APPLICATION_JSON)
        .handler(productValidationHandler)
        .handler(AuthHandler.create(authenticationService, vertx, api, postgresService, authClient))
        .handler(this::handleCreateProduct)
        .failureHandler(exceptionHandler);

    router
        .delete(PROVIDER_PATH + PRODUCT_PATH)
        .handler(productValidationHandler)
        .handler(AuthHandler.create(authenticationService, vertx, api, postgresService, authClient))
        .handler(this::handleDeleteProduct)
        .failureHandler(exceptionHandler);

    router
        .get(PROVIDER_PATH + LIST_PRODUCTS_PATH)
        .handler(resourceValidationHandler)
        .handler(AuthHandler.create(authenticationService, vertx, api, postgresService, authClient))
        .handler(this::listProducts)
        .failureHandler(exceptionHandler);

    router
        .get(PROVIDER_PATH + LIST_PURCHASES_PATH)
        .handler(AuthHandler.create(authenticationService, vertx, api, postgresService, authClient))
        .handler(this::listPurchases)
        .failureHandler(exceptionHandler);

    router
        .post(PROVIDER_PATH + PRODUCT_VARIANT_PATH)
        .handler(variantValidationHandler)
        .handler(AuthHandler.create(authenticationService, vertx, api, postgresService, authClient))
        .handler(this::handleCreateProductVariant)
        .failureHandler(exceptionHandler);

    router
        .put(PROVIDER_PATH + PRODUCT_VARIANT_PATH)
        .handler(variantValidationHandler)
        .handler(AuthHandler.create(authenticationService, vertx, api, postgresService, authClient))
        .handler(this::handleUpdateProductVariant)
        .failureHandler(exceptionHandler);

    router
        .get(PROVIDER_PATH + PRODUCT_VARIANT_PATH)
        .handler(variantValidationHandler)
        .handler(AuthHandler.create(authenticationService, vertx, api, postgresService, authClient))
        .handler(this::handleGetProductVariants)
        .failureHandler(exceptionHandler);

    router
        .delete(PROVIDER_PATH + PRODUCT_VARIANT_PATH)
        .handler(variantValidationHandler)
        .handler(AuthHandler.create(authenticationService, vertx, api, postgresService, authClient))
        .handler(this::handleDeleteProductVariant)
        .failureHandler(exceptionHandler);
    return this.router;
  }

  private void handleCreateProduct(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);
    User user = routingContext.get("user");

    productService.createProduct(
        user,
        requestBody,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(routingContext, 201, handler.result());
          } else {
            String errorMessage = handler.cause().getMessage();
            if (errorMessage.contains(ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getUrn())) {
              routingContext.fail(
                  new DxRuntimeException(
                      409,
                      ResponseUrn.RESOURCE_ALREADY_EXISTS_URN,
                      ResponseUrn.RESOURCE_ALREADY_EXISTS_URN.getMessage()));
            } else if (errorMessage.contains(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())) {
              routingContext.fail(
                  new DxRuntimeException(500, ResponseUrn.INTERNAL_SERVER_ERR_URN, errorMessage));
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
    User user = routingContext.get("user");
    productService.deleteProduct(
        user,
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
    User user = routingContext.get("user");
    for (Map.Entry<String, String> param : request.params()) {
      requestBody.put(param.getKey(), param.getValue());
    }
    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);

    productService.listProducts(
        user,
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
    User user = routingContext.get("user");

    variantService.createProductVariant(
        user,
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
    User user = routingContext.get("user");

    variantService.updateProductVariant(
        user,
        requestBody,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(routingContext, 200, handler.result());
          } else {
            handleFailureResponse(routingContext, handler.cause());
          }
        });
  }

  private void handleGetProductVariants(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    JsonObject requestBody = new JsonObject();
    User user = routingContext.get("user");
    for (Map.Entry<String, String> param : request.params()) {
      requestBody.put(param.getKey(), param.getValue());
    }

    requestBody.put(AUTH_INFO, authInfo);

    variantService.listProductVariants(
        user,
        requestBody,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(routingContext, 200, handler.result());
          } else {
            handleFailureResponse(routingContext, handler.cause());
          }
        });
  }

  private void handleDeleteProductVariant(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    JsonObject requestBody =
        new JsonObject()
            .put(AUTH_INFO, authInfo)
            .put(PRODUCT_ID, request.getParam(PRODUCT_ID))
            .put(PRODUCT_VARIANT_NAME, request.getParam(PRODUCT_VARIANT_NAME));
    User user = routingContext.get("user");

    variantService.deleteProductVariant(
        user,
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

  private void handleFailure(RoutingContext routingContext, String failureMessage) {
    HttpServerResponse response = routingContext.response();
    LOGGER.debug("Failure Message : {} ", failureMessage);

    try {
      JsonObject jsonObject = new JsonObject(failureMessage);
      int type = jsonObject.getInteger(TYPE);
      String title = jsonObject.getString(TITLE);

      HttpStatusCode status = HttpStatusCode.getByValue(type);

      ResponseUrn urn;

      // get the urn by either type or title
      if (title != null) {
        urn = ResponseUrn.fromCode(title);
      } else {

        urn = ResponseUrn.fromCode(String.valueOf(type));
      }
      if (jsonObject.getString(DETAIL) != null) {
        detail = jsonObject.getString(DETAIL);
        response
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(type)
            .end(generateResponse(status, urn, detail).toString());
      } else {
        response
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(type)
            .end(generateResponse(status, urn).toString());
      }

    } catch (DecodeException exception) {
      LOGGER.error("Error : Expecting JSON from backend service [ jsonFormattingException ] ");
      handleResponse(response, BAD_REQUEST, ResponseUrn.BACKING_SERVICE_FORMAT_URN);
    }
  }

  private void handleResponse(
      HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn) {
    handleResponse(response, statusCode, urn, statusCode.getDescription());
  }

  private void handleResponse(
      HttpServerResponse response,
      HttpStatusCode statusCode,
      ResponseUrn urn,
      String failureMessage) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, failureMessage).toString());
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
