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
  private RazorpayClient razorpayClient;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    api = Api.getInstance(config().getString("dxApiBasePath"));
    String razorPayKey = config().getString("razorPayKey");
    String razorPaySecret = config().getString("razorPaySecret");


    Boolean enableLogging = config().getBoolean("enableLogging", false);
    if (enableLogging) {
      LOGGER.warn("RazorPay enable logging set to true, do not set in production!!");
      razorpayClient = new RazorpayClient(razorPayKey, razorPaySecret, true);
    } else {
      razorpayClient = new RazorpayClient(razorPayKey, razorPaySecret);
    }
    auditingService = AuditingService.createProxy(vertx, AUDITING_SERVICE_ADDRESS);

    createLinkedAccount =
        new CreateLinkedAccount.CreateLinkedAccountBuilder(postgresService, api, auditingService)
            .setRazorpayClient(razorpayClient)
            .build();

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
