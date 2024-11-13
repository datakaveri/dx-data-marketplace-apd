package iudx.data.marketplace.apiserver;

import static iudx.data.marketplace.apiserver.response.ResponseUtil.generateResponse;
import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.Constants.AUTH_INFO;
import static iudx.data.marketplace.common.Constants.CONSUMER_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.HttpStatusCode.BAD_REQUEST;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.handlers.AuthHandler;
import iudx.data.marketplace.apiserver.handlers.ExceptionHandler;
import iudx.data.marketplace.apiserver.handlers.ValidationHandler;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.authenticator.AuthClient;
import iudx.data.marketplace.authenticator.AuthenticationService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.consumer.ConsumerService;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsumerApis {
  public static final Logger LOGGER = LogManager.getLogger(ConsumerApis.class);

  private final Vertx vertx;
  private final Router router;

  private ConsumerService consumerService;
  private Api api;
  private PostgresService postgresService;
  private AuthClient authClient;
  private AuthenticationService authenticationService;

  ConsumerApis(
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

    ValidationHandler resourceValidationHandler =
        new ValidationHandler(vertx, RequestType.RESOURCE);
    ValidationHandler providerValidationHandler =
        new ValidationHandler(vertx, RequestType.PROVIDER);
    ValidationHandler orderValidationHandler = new ValidationHandler(vertx, RequestType.ORDER);
    ValidationHandler purchaseValidationHandler =
        new ValidationHandler(vertx, RequestType.PURCHASE);
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    ValidationHandler productVariantHandler = new ValidationHandler(vertx, RequestType.PRODUCT);

    consumerService = ConsumerService.createProxy(vertx, CONSUMER_SERVICE_ADDRESS);

    router
        .get(api.getConsumerListProviders())
        .handler(providerValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::listProviders)
        .failureHandler(exceptionHandler);

    router
        .get(api.getConsumerListResourcePath())
        .handler(resourceValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::listResources)
        .failureHandler(exceptionHandler);

    router
        .get(api.getConsumerListProducts())
        .handler(resourceValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::listProducts)
        .failureHandler(exceptionHandler);

    router
        .get(api.getConsumerListPurchases())
        .handler(purchaseValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::listPurchases)
        .failureHandler(exceptionHandler);

    router
        .get(api.getConsumerProductVariantPath())
        .handler(productVariantHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::listProductVariants)
        .failureHandler(exceptionHandler);

    router
        .post(CONSUMER_PATH + ORDERS_PATH + "/:productVariantId")
        .handler(orderValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::createOrder)
        .failureHandler(exceptionHandler);
    return this.router;
  }

  private void createOrder(RoutingContext routingContext) {
    LOGGER.info("inside create order method");
    Map<String, String> pathParams = routingContext.pathParams();
    String variantId = pathParams.get(PRODUCT_VARIANT_ID);

    LOGGER.debug(variantId);

    JsonObject requestBody =
        new JsonObject()
            .put(AUTH_INFO, routingContext.data().get(AUTH_INFO))
            .put(PRODUCT_VARIANT_ID, variantId);
    User user = routingContext.get("user");

    consumerService.createOrder(
        requestBody,
        user,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(routingContext, 201, handler.result());
          } else {
            handleFailureResponse(routingContext, handler.cause());
          }
        });
  }

  private void listProviders(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = new JsonObject();
    for (Map.Entry<String, String> param : request.params()) {
      requestBody.put(param.getKey(), param.getValue());
    }

    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);
    User consumer = routingContext.get("user");

    consumerService.listProviders(
        consumer,
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

  private void listResources(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = new JsonObject();
    for (Map.Entry<String, String> param : request.params()) {
      requestBody.put(param.getKey(), param.getValue());
    }

    User consumer = routingContext.get("user");
    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);

    consumerService.listResources(
        consumer,
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

  private void listProductVariants(RoutingContext routingContext) {

    User consumer = routingContext.get("user");
    MultiMap requestParams = routingContext.request().params();
    String productId = requestParams.get("productId");
    JsonObject requestJson = new JsonObject().put("productId", productId);

    consumerService.listProductVariants(
        consumer,
        requestJson,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(
                routingContext, HttpStatusCode.SUCCESS.getValue(), handler.result());
          } else {
            handleFailureResponse(routingContext, handler.cause().getMessage());
          }
        });
  }

  private void listProducts(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = new JsonObject();
    for (Map.Entry<String, String> param : request.params()) {
      requestBody.put(param.getKey(), param.getValue());
    }

    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);
    User consumer = routingContext.get("user");

    consumerService.listProducts(
        consumer,
        requestBody,
        handler -> {
          if (handler.succeeded()) {
            if (handler.result().getJsonArray(RESULTS).isEmpty()) {
              handleSuccessResponse(routingContext, 204, handler.result());
            } else {
              handleSuccessResponse(routingContext, 200, handler.result());
            }
          } else {
            handleFailureResponse(routingContext, handler.cause().getMessage());
          }
        });
  }

  private void listPurchases(RoutingContext routingContext) {
    User consumer = routingContext.get("user");
    MultiMap requestParams = routingContext.request().params();
    String resourceId = requestParams.get("resourceId");
    String productId = requestParams.get("productId");
    String paymentStatus = requestParams.get("paymentStatus");
    JsonObject requestJson =
        new JsonObject()
            .put("resourceId", resourceId)
            .put("productId", productId)
            .put("paymentStatus", paymentStatus);

    consumerService.listPurchase(
        consumer,
        requestJson,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(
                routingContext, HttpStatusCode.SUCCESS.getValue(), handler.result());
          } else {
            handleFailureResponse(routingContext, handler.cause().getMessage());
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

  /**
   * Handles Failed HTTP Response
   *
   * @param routingContext Routing context object
   * @param failureMessage Failure message for response
   */
  private void handleFailureResponse(RoutingContext routingContext, String failureMessage) {
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
        String detail = jsonObject.getString(DETAIL);
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
}
