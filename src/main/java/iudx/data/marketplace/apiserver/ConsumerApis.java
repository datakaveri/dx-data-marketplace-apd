package iudx.data.marketplace.apiserver;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.Constants.AUTH_INFO;
import static iudx.data.marketplace.common.Constants.CONSUMER_SERVICE_ADDRESS;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.handlers.AuthHandler;
import iudx.data.marketplace.apiserver.handlers.ExceptionHandler;
import iudx.data.marketplace.apiserver.handlers.ValidationHandler;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.consumer.ConsumerService;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsumerApis {
  public static final Logger LOGGER = LogManager.getLogger(ConsumerApis.class);

  private final Vertx vertx;
  private final Router router;

  private ConsumerService consumerService;
  private Api api;

  ConsumerApis(Vertx vertx, Router router, Api apis) {
    this.vertx = vertx;
    this.router = router;
    this.api = apis;
  }

  Router init() {

    ValidationHandler resourceValidationHandler = new ValidationHandler(vertx, RequestType.RESOURCE);
    ValidationHandler providerValidationHandler =
        new ValidationHandler(vertx, RequestType.PROVIDER);
    ExceptionHandler exceptionHandler = new ExceptionHandler();

    consumerService = ConsumerService.createProxy(vertx, CONSUMER_SERVICE_ADDRESS);

    router
        .get(CONSUMER_PATH + LIST_PROVIDERS_PATH)
        .handler(providerValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::listProviders)
        .failureHandler(exceptionHandler);

    router
        .get(CONSUMER_PATH + LIST_RESOURCES_PATH)
        .handler(resourceValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::listResources)
        .failureHandler(exceptionHandler);

    router
        .get(CONSUMER_PATH + LIST_PRODUCTS_PATH)
        .handler(resourceValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::listProducts)
        .failureHandler(exceptionHandler);

    router
        .get(CONSUMER_PATH + LIST_PURCHASES_PATH)
        .handler(this::listPurchases)
        .failureHandler(exceptionHandler);

    return this.router;
  }

  private void listProviders(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = new JsonObject();
    for (Map.Entry<String, String> param : request.params()) {
      requestBody.put(param.getKey(), param.getValue());
    }

    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);

    consumerService.listProviders(
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

    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);

    consumerService.listResources(
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

  private void listProducts(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = new JsonObject();
    for (Map.Entry<String, String> param : request.params()) {
      requestBody.put(param.getKey(), param.getValue());
    }

    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);

    consumerService.listProducts(
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
