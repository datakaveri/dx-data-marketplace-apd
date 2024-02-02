package iudx.data.marketplace.apiserver.provider.linkedAccount;

import static iudx.data.marketplace.common.Constants.*;

import com.razorpay.RazorpayClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.razorpay.RazorPayService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkedAccountVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(LinkedAccountVerticle.class);

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
        new CreateLinkedAccount(postgresService, api, auditingService,razorPayService);

    fetchLinkedAccount = new FetchLinkedAccount(postgresService, api);
    updateLinkedAccount = new UpdateLinkedAccount(postgresService, api, auditingService);
    linkedAccountService =
        new LinkedAccountServiceImpl(createLinkedAccount, fetchLinkedAccount, updateLinkedAccount);

    new ServiceBinder(vertx)
        .setAddress(LINKED_ACCOUNT_ADDRESS)
        .register(LinkedAccountService.class, linkedAccountService);
    startPromise.complete();
  }
}
