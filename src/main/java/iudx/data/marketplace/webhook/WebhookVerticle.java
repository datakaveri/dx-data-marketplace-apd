package iudx.data.marketplace.webhook;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.policies.PolicyService;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.product.util.Constants.TABLES;

public class WebhookVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(WebhookVerticle.class);

  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;
  private PostgresService postgresService;
  private PolicyService policyService;
  private WebhookService webhookService;

  @Override
  public void start() {
    postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    policyService = PolicyService.createProxy(vertx, POLICY_SERVICE_ADDRESS);

    String invoiceTable = config().getJsonArray(TABLES).getString(6);

    webhookService = new WebhookServiceImpl(postgresService, policyService, invoiceTable);

    binder = new ServiceBinder(vertx);
    consumer =
        binder.setAddress(WEBHOOK_SERVICE_ADDRESS).register(WebhookService.class, webhookService);
    LOGGER.info("webhook Service started");
  }
}
