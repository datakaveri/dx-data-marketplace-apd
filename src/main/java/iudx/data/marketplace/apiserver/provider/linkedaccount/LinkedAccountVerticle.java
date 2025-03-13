package iudx.data.marketplace.apiserver.provider.linkedaccount;

import iudx.data.marketplace.apiserver.provider.linkedaccount.service.*;
import static iudx.data.marketplace.common.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.auditing.service.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.postgres.service.PostgresService;
import iudx.data.marketplace.razorpay.service.RazorPayService;

public class LinkedAccountVerticle extends AbstractVerticle {
  //  private static final Logger LOGGER = LogManager.getLogger(LinkedAccountVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    PostgresService postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    Api api = Api.getInstance(config().getString("dxApiBasePath"));
    AuditingService auditingService = AuditingService.createProxy(vertx, AUDITING_SERVICE_ADDRESS);

    RazorPayService razorPayService = RazorPayService.createProxy(vertx, RAZORPAY_SERVICE_ADDRESS);

    CreateLinkedAccount createLinkedAccount =
        new CreateLinkedAccount(postgresService, api, auditingService, razorPayService);

    FetchLinkedAccount fetchLinkedAccount = new FetchLinkedAccount(postgresService, api, razorPayService);
    UpdateLinkedAccount updateLinkedAccount =
        new UpdateLinkedAccount(postgresService, api, auditingService, razorPayService);
    LinkedAccountServiceImpl linkedAccountService =
        new LinkedAccountServiceImpl(createLinkedAccount, fetchLinkedAccount, updateLinkedAccount);

    new ServiceBinder(vertx)
        .setAddress(LINKED_ACCOUNT_ADDRESS)
        .register(LinkedAccountService.class, linkedAccountService);
    startPromise.complete();
  }
}
