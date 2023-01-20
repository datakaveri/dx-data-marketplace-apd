package iudx.data.marketplace.product;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.common.Constants.POSTGRES_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.Constants.PRODUCT_SERVICE_ADDRESS;

public class ProductVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(ProductVerticle.class);
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;

  private PostgresService postgresService;
  private ProductService productService;
  private CatalogueService catService;

  @Override
  public void start() throws Exception {
    postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    catService = new CatalogueService(vertx, config());

    productService = new ProductServiceImpl(config(), postgresService, catService);

    binder = new ServiceBinder(vertx);
    consumer =
        binder.setAddress(PRODUCT_SERVICE_ADDRESS).register(ProductService.class, productService);
    LOGGER.info("Product Service started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
