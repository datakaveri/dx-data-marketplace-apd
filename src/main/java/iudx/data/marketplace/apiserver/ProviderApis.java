package iudx.data.marketplace.apiserver;

import static iudx.data.marketplace.apiserver.response.ResponseUtil.generateResponse;
import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.Constants.AUTH_INFO;
import static iudx.data.marketplace.common.Constants.PRODUCT_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.Constants.PRODUCT_VARIANT_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.HttpStatusCode.BAD_REQUEST;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.handlers.*;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.aaaService.AuthClient;
import iudx.data.marketplace.authenticator.AuthenticationService;
import iudx.data.marketplace.authenticator.model.DxRole;
import iudx.data.marketplace.authenticator.model.UserInfo;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.HttpStatusCode;
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
  private AccessHandler accessHandler;
  private UserInfo userInfo;
  private UserInfoFromAuthHandler userInfoFromAuthHandler;

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

    ValidationHandler productValidationHandler = new ValidationHandler(RequestType.PRODUCT);
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    accessHandler = new AccessHandler();
    userInfo = new UserInfo();
    userInfoFromAuthHandler = new UserInfoFromAuthHandler(authClient, userInfo);

    productService = ProductService.createProxy(vertx, PRODUCT_SERVICE_ADDRESS);
    variantService = ProductVariantService.createProxy(vertx, PRODUCT_VARIANT_SERVICE_ADDRESS);

    router
        .post(api.getProviderProductPath())
        .consumes(APPLICATION_JSON)
        .handler(productValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(accessHandler.setUserRolesForEndpoint(DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(userInfoFromAuthHandler)
        .handler(this::handleCreateProduct)
        .failureHandler(exceptionHandler);

    router
        .delete(api.getProviderProductPath())
        .handler(productValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(accessHandler.setUserRolesForEndpoint(DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(userInfoFromAuthHandler)
        .handler(this::handleDeleteProduct)
        .failureHandler(exceptionHandler);
    ValidationHandler resourceValidationHandler = new ValidationHandler(RequestType.RESOURCE);

    router
        .get(api.getProviderListProductsPath())
        .handler(resourceValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(accessHandler.setUserRolesForEndpoint(DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(userInfoFromAuthHandler)
        .handler(this::listProducts)
        .failureHandler(exceptionHandler);
    ValidationHandler purchaseValidationHandler = new ValidationHandler(RequestType.PURCHASE);

    router
        .get(api.getProviderListPurchasesPath())
        .handler(purchaseValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(accessHandler.setUserRolesForEndpoint(DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(userInfoFromAuthHandler)
        .handler(this::listPurchases)
        .failureHandler(exceptionHandler);
    ValidationHandler variantValidationHandler = new ValidationHandler(RequestType.PRODUCT_VARIANT);

    router
        .post(api.getProviderProductVariantPath())
        .handler(variantValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(accessHandler.setUserRolesForEndpoint(DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(userInfoFromAuthHandler)
        .handler(this::handleCreateProductVariant)
        .failureHandler(exceptionHandler);

    router
        .put(api.getProviderProductVariantPath())
        .handler(variantValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(accessHandler.setUserRolesForEndpoint(DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(userInfoFromAuthHandler)
        .handler(this::handleUpdateProductVariant)
        .failureHandler(exceptionHandler);
    ValidationHandler listVariantValidationHandler =
        new ValidationHandler(RequestType.LIST_PRODUCT_VARIANT);
    router
        .get(api.getProviderProductVariantPath())
        .handler(listVariantValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(accessHandler.setUserRolesForEndpoint(DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(userInfoFromAuthHandler)
        .handler(this::handleGetProductVariants)
        .failureHandler(exceptionHandler);
    ValidationHandler deleteVariantValidationHandler =
        new ValidationHandler(RequestType.DELETE_PRODUCT_VARIANT);

    router
        .delete(api.getProviderProductVariantPath())
        .handler(deleteVariantValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(accessHandler.setUserRolesForEndpoint(DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(userInfoFromAuthHandler)
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
            } else if (errorMessage.contains(ResponseUrn.FORBIDDEN_URN.getUrn())) {
              handleFailureResponse(
                  routingContext, errorMessage, HttpStatusCode.FORBIDDEN.getValue());
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
    User provider = routingContext.get("user");
    MultiMap requestParams = routingContext.request().params();
    String resourceId = requestParams.get("resourceId");
    String productId = requestParams.get("productId");
    String paymentStatus = requestParams.get("paymentStatus");
    JsonObject requestJson =
        new JsonObject()
            .put("resourceId", resourceId)
            .put("productId", productId)
            .put("paymentStatus", paymentStatus);
    variantService.listPurchase(
        provider,
        requestJson,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(
                routingContext, HttpStatusCode.SUCCESS.getValue(), handler.result());
          } else {
            handleFailure(routingContext, handler.cause().getMessage());
          }
        });
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

            } else if (errMessage.contains(ResponseUrn.FORBIDDEN_URN.getUrn())) {
              handleFailureResponse(
                  routingContext, errMessage, HttpStatusCode.FORBIDDEN.getValue());
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
            handleFailure(routingContext, handler.cause().getMessage());
          }
        });
  }

  private void handleDeleteProductVariant(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    JsonObject requestBody =
        new JsonObject()
            .put(AUTH_INFO, authInfo)
            .put(PRODUCT_VARIANT_ID, request.getParam(PRODUCT_VARIANT_ID));
    User user = routingContext.get("user");

    variantService.deleteProductVariant(
        user,
        requestBody,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(routingContext, 200, handler.result());
          } else {
            handleFailure(routingContext, handler.cause().getMessage());
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

  private void handleFailureResponse(
      RoutingContext routingContext, String failureMessage, int statusCode) {
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode)
        .end(failureMessage);
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
      default:
        break;
    }
  }
}
