package iudx.data.marketplace.consumer;

import static iudx.data.marketplace.common.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.common.Util;
import iudx.data.marketplace.consumer.service.ConsumerService;
import iudx.data.marketplace.consumer.service.ConsumerServiceImpl;
import iudx.data.marketplace.postgres.service.PostgresService;
import iudx.data.marketplace.razorpay.service.RazorPayService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsumerVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LogManager.getLogger(ConsumerVerticle.class);
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;

  @Override
  public void start() throws Exception {
    PostgresService postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    RazorPayService razorPayService = RazorPayService.createProxy(vertx, RAZORPAY_SERVICE_ADDRESS);
    Util util = new Util();

    ConsumerService consumerService = new ConsumerServiceImpl(config(), postgresService, razorPayService, util);
    binder = new ServiceBinder(vertx);
    consumer =
        binder
            .setAddress(CONSUMER_SERVICE_ADDRESS)
            .register(ConsumerService.class, consumerService);
    LOGGER.info("Consumer Service started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
