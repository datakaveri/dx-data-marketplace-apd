package iudx.data.marketplace.webhook;

import static iudx.data.marketplace.webhook.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.consumer.util.PaymentStatus;
import iudx.data.marketplace.policies.PolicyService;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebhookServiceImpl implements WebhookService {

  private static final Logger LOGGER = LogManager.getLogger(WebhookServiceImpl.class);
  private final PostgresService postgresService;
  private final PolicyService policyService;
  private final String invoiceTable;

  public WebhookServiceImpl(
      PostgresService postgresService, PolicyService policyService, String invoiceTable) {
    this.postgresService = postgresService;
    this.policyService = policyService;
    this.invoiceTable = invoiceTable;
  }

  @Override
  public Future<JsonObject> recordOrderPaid(String orderId) {
    Promise<JsonObject> promise = Promise.promise();

    updatePaymentStatusForInvoice(orderId, PaymentStatus.SUCCESSFUL)
        .compose(ar -> policyService.createPolicy(orderId))
        .onComplete(
            completeHandler -> {
              if (completeHandler.succeeded()) {
                promise.complete(completeHandler.result());
              } else {
                promise.fail(completeHandler.cause());
              }
            });

    return promise.future();
  }

  @Override
  public Future<Void> recordPaymentFailure(String orderId) {
    return updatePaymentStatusForInvoice(orderId, PaymentStatus.FAILED);
  }

  Future<Void> updatePaymentStatusForInvoice(String orderId, PaymentStatus paymentStatus) {
    Promise<Void> promise = Promise.promise();
    StringBuilder query =
        new StringBuilder(UPDATE_PAYMENT_STATUS_QUERY.replace("$0", invoiceTable));

    JsonObject params =
        new JsonObject().put(PAYMENT_STATUS, paymentStatus.getPaymentStatus()).put(ORDER_ID, orderId);

    postgresService.executePreparedQuery(
        query.toString(),
        params,
        pgHandler -> {
          if (pgHandler.succeeded()) {
              LOGGER.debug("Result after updating : {}", pgHandler.result().encodePrettily());
            promise.complete();
          } else {
            promise.fail(pgHandler.cause());
          }
        });

    return promise.future();
  }
}
