package iudx.data.marketplace.consumer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.policies.util.Util;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.razorpay.RazorPayService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.common.Constants.*;

public class ConsumerVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LogManager.getLogger(ConsumerVerticle.class);
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;

  private PostgresService postgresService;
  private ConsumerService consumerService;
  private RazorPayService razorPayService;
  private Util util;

  @Override
  public void start() throws Exception {
    postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    razorPayService = RazorPayService.createProxy(vertx, RAZORPAY_SERVICE_ADDRESS);
    util = new Util();

    consumerService = new ConsumerServiceImpl(config(), postgresService, razorPayService, util);
    binder = new ServiceBinder(vertx);
    consumer = binder.setAddress(CONSUMER_SERVICE_ADDRESS).register(ConsumerService.class, consumerService);
    LOGGER.info("Consumer Service started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
