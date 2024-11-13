package iudx.data.marketplace.product.variant;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.common.Util;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.common.Constants.POSTGRES_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.Constants.PRODUCT_VARIANT_SERVICE_ADDRESS;

public class ProductVariantVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LogManager.getLogger(ProductVariantVerticle.class);
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;
  private PostgresService postgresService;
  private ProductVariantService variantService;
  private Util util;

  @Override
  public void start() throws Exception {
    postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    util = new Util();
    variantService = new ProductVariantServiceImpl(config(), postgresService, util);

    binder = new ServiceBinder(vertx);
    consumer =
        binder
            .setAddress(PRODUCT_VARIANT_SERVICE_ADDRESS)
            .register(ProductVariantService.class, variantService);
    LOGGER.info("Product Variant Service started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
