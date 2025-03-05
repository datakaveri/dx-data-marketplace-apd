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

  private PostgresService postgresService;
  private LinkedAccountServiceImpl linkedAccountService;
  private CreateLinkedAccount createLinkedAccount;
  private FetchLinkedAccount fetchLinkedAccount;
  private UpdateLinkedAccount updateLinkedAccount;
  private AuditingService auditingService;

  private Api api;
  private RazorPayService razorPayService;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    api = Api.getInstance(config().getString("dxApiBasePath"));
    auditingService = AuditingService.createProxy(vertx, AUDITING_SERVICE_ADDRESS);

    razorPayService = RazorPayService.createProxy(vertx, RAZORPAY_SERVICE_ADDRESS);

    createLinkedAccount =
        new CreateLinkedAccount(postgresService, api, auditingService, razorPayService);

    fetchLinkedAccount = new FetchLinkedAccount(postgresService, api, razorPayService);
    updateLinkedAccount =
        new UpdateLinkedAccount(postgresService, api, auditingService, razorPayService);
    linkedAccountService =
        new LinkedAccountServiceImpl(createLinkedAccount, fetchLinkedAccount, updateLinkedAccount);

    new ServiceBinder(vertx)
        .setAddress(LINKED_ACCOUNT_ADDRESS)
        .register(LinkedAccountService.class, linkedAccountService);
    startPromise.complete();
  }
}
