package iudx.data.marketplace.apiserver;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.handlers.ExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.apiserver.util.Constants.*;

public class ConsumerApis {
  public static final Logger LOGGER = LogManager.getLogger(ConsumerApis.class);

  private final Vertx vertx;
  private final Router router;

  ConsumerApis(Vertx vertx, Router router) {
    this.vertx = vertx;
    this.router = router;
  }

  Router init() {


    ExceptionHandler exceptionHandler = new ExceptionHandler();

    router
        .get(CONSUMER_BASE_PATH + LIST_PROVIDERS_PATH)
        .handler(this::listProviders)
        .failureHandler(exceptionHandler);

    router
        .get(CONSUMER_BASE_PATH + LIST_DATASETS_PATH)
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
  }

  private void listProducts(RoutingContext routingContext) {
  }

  private void listPurchases(RoutingContext routingContext) {
  }
}
