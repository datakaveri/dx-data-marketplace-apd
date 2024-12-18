package iudx.data.marketplace.product;

import static iudx.data.marketplace.common.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.catalogueService.CatalogueService;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.razorpay.RazorPayService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProductVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(ProductVerticle.class);
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;

  private PostgresService postgresService;
  private ProductService productService;
  private CatalogueService catService;
  private RazorPayService razorPayService;
  private boolean isAccountActivationCheckBeingDone;

  @Override
  public void start() throws Exception {
    postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    catService = new CatalogueService(vertx, config());
    razorPayService = RazorPayService.createProxy(vertx, RAZORPAY_SERVICE_ADDRESS);

    isAccountActivationCheckBeingDone = config().getBoolean("isAccountActivationCheckBeingDone");
    if (!isAccountActivationCheckBeingDone) {
      LOGGER.warn(
          "\n\n" + "account activation check is set to false. Enable it in production" + "\n\n");
    }
    productService =
        new ProductServiceImpl(
            config(),
            postgresService,
            catService,
            razorPayService,
            isAccountActivationCheckBeingDone);

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
