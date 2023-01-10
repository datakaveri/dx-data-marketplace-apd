package iudx.data.marketplace.apiserver;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.handlers.ExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.apiserver.util.Constants.*;

public class ProviderApis {
  public static final Logger LOGGER = LogManager.getLogger(ProviderApis.class);


  private final Vertx vertx;
  private final Router router;

  ProviderApis(Vertx vertx, Router router) {
    this.vertx = vertx;
    this.router = router;
  }

  Router init() {

    ExceptionHandler exceptionHandler = new ExceptionHandler();
    router
        .post(PROVIDER_BASE_PATH + PRODUCT_PATH)
        .consumes(APPLICATION_JSON)
        .handler(this::handleCreateProduct)
        .failureHandler(exceptionHandler);

    router
        .delete(PROVIDER_BASE_PATH + PRODUCT_PATH)
        .handler(this::handleDeleteProduct)
        .failureHandler(exceptionHandler);

    router
        .get(PROVIDER_BASE_PATH + LIST_PRODUCTS_PATH)
        .handler(this::listProducts)
        .failureHandler(exceptionHandler);

    router
        .get(PROVIDER_BASE_PATH + LIST_PURCHASES_PATH)
        .handler(this::listPurchases)
        .failureHandler(exceptionHandler);

    router
        .post(PROVIDER_BASE_PATH + PRODUCT_VARIANT_PATH)
        .handler(this::handleCreateProductVariant)
        .failureHandler(exceptionHandler);

    router
        .put(PROVIDER_BASE_PATH + PRODUCT_VARIANT_PATH)
        .handler(this::handleUpdateProductVariant)
        .failureHandler(exceptionHandler);

    router
        .get(PROVIDER_BASE_PATH + PRODUCT_VARIANT_PATH)
        .handler(this::handleGetProductVariants)
        .failureHandler(exceptionHandler);

    router
        .delete(PROVIDER_BASE_PATH + PRODUCT_VARIANT_PATH)
        .handler(this::handleDeleteProductVariant)
        .failureHandler(exceptionHandler);
    return this.router;
  }

  private void handleCreateProduct(RoutingContext routingContext) {

  }

  private void handleDeleteProduct(RoutingContext routingContext) {
  }

  private void listProducts(RoutingContext routingContext) {
  }

  private void listPurchases(RoutingContext routingContext) {
  }

  private void handleCreateProductVariant(RoutingContext routingContext) {

  }

  private void handleUpdateProductVariant(RoutingContext routingContext) {

  }

  private void handleGetProductVariants(RoutingContext routingContext) {

  }

  private void handleDeleteProductVariant(RoutingContext routingContext) {

  }
}
