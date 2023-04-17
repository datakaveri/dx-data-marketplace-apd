package iudx.data.marketplace.apiserver;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.handlers.AuthHandler;
import iudx.data.marketplace.apiserver.handlers.ExceptionHandler;
import iudx.data.marketplace.apiserver.handlers.ValidationHandler;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.consumer.ConsumerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.Constants.AUTH_INFO;
import static iudx.data.marketplace.common.Constants.CONSUMER_SERVICE_ADDRESS;

public class ConsumerApis {
  public static final Logger LOGGER = LogManager.getLogger(ConsumerApis.class);

  private final Vertx vertx;
  private final Router router;

  private ConsumerService consumerService;

  ConsumerApis(Vertx vertx, Router router) {
    this.vertx = vertx;
    this.router = router;
  }

  Router init() {

    ValidationHandler datasetValidationHandler = new ValidationHandler(vertx, RequestType.DATASET);
    ExceptionHandler exceptionHandler = new ExceptionHandler();

    consumerService = ConsumerService.createProxy(vertx, CONSUMER_SERVICE_ADDRESS);

    router
        .get(CONSUMER_BASE_PATH + LIST_PROVIDERS_PATH)
        .handler(this::listProviders)
        .failureHandler(exceptionHandler);

    router
        .get(CONSUMER_BASE_PATH + LIST_DATASETS_PATH)
        .handler(datasetValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::listDatasets)
        .failureHandler(exceptionHandler);

    router
        .get(CONSUMER_BASE_PATH + LIST_PRODUCTS_PATH)
        .handler(this::listProducts)
        .failureHandler(exceptionHandler);

    router
        .get(CONSUMER_BASE_PATH + LIST_PURCHASES_PATH)
        .handler(this::listPurchases)
        .failureHandler(exceptionHandler);

    return this.router;
  }

  private void listProviders(RoutingContext routingContext) {
  }

  private void listDatasets(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = new JsonObject();
    for(Map.Entry<String, String> param : request.params()) {
      requestBody.put(param.getKey(), param.getValue());
    }

    JsonObject authInfo = (JsonObject) routingContext.data().get(AUTH_INFO);
    requestBody.put(AUTH_INFO, authInfo);

    consumerService.listDatasets(requestBody, handler -> {
      if(handler.succeeded()) {
        handleSuccessResponse(routingContext, handler.result());
      } else {
        handleFailureResponse(routingContext, handler.cause());
      }
    });
  }

  private void listProducts(RoutingContext routingContext) {
  }

  private void listPurchases(RoutingContext routingContext) {
  }

  private void handleFailureResponse(RoutingContext routingContext, Throwable cause) {
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(400)
        .end(cause.getMessage());
  }

  private void handleSuccessResponse(RoutingContext routingContext, JsonObject result) {
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(200)
        .end(result.toString());
  }
}
