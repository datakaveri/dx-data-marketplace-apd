package iudx.data.marketplace.razorpay;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.ServiceHelper;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.common.Constants.POSTGRES_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.Constants.RAZORPAY_SERVICE_ADDRESS;
import static iudx.data.marketplace.razorpay.Constants.RAZORPAY_KEY;
import static iudx.data.marketplace.razorpay.Constants.RAZORPAY_SECRET;

public class RazorPayVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(AbstractVerticle.class);
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;
  private PostgresService postgresService;
  private RazorPayService razorPayService;

  private RazorpayClient razorpayClient;

  @Override
  public void start() throws RazorpayException {
    postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);

    String razorPayKey = config().getString(RAZORPAY_KEY);
    String razorPaySecret = config().getString(RAZORPAY_SECRET);

    Boolean enableLogging = config().getBoolean("enableLogging", false);
    if (enableLogging) {
      LOGGER.warn("RazorPay enable logging set to true, do not set in production!!");
      razorpayClient = new RazorpayClient(razorPayKey, razorPaySecret, true);
    } else {
      razorpayClient = new RazorpayClient(razorPayKey, razorPaySecret);
    }

    razorPayService = new RazorPayServiceImpl(razorpayClient, postgresService, config());

    binder = new ServiceBinder(vertx);
    consumer =
        binder
            .setAddress(RAZORPAY_SERVICE_ADDRESS)
            .register(RazorPayService.class, razorPayService);
    LOGGER.info("RazorPay Service started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
