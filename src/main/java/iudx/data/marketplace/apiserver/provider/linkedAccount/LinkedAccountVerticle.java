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

public class LinkedAccountVerticle extends AbstractVerticle {

  private PostgresService postgresService;
  private LinkedAccountServiceImpl linkedAccountService;
  private CreateLinkedAccount createLinkedAccount;
  private FetchLinkedAccount fetchLinkedAccount;
  private UpdateLinkedAccount updateLinkedAccount;
  private AuditingService auditingService;

  private Api api;
  private WebClient webClient;
  private WebClientOptions webClientOptions;
  private RazorpayClient razorpayClient;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    api = Api.getInstance(config().getString("dxApiBasePath"));
    String razorpayId = config().getString("razorpayTestId");
    String razorpaySecret = config().getString("razorpayTestSecret");
    webClientOptions = new WebClientOptions();
    webClientOptions.setTrustAll(false).setVerifyHost(true).setSsl(true);
    webClient = WebClient.create(vertx, webClientOptions);

    razorpayClient = new RazorpayClient(razorpayId, razorpaySecret, true);
    auditingService = AuditingService.createProxy(vertx, AUDITING_SERVICE_ADDRESS);

    createLinkedAccount =
        new CreateLinkedAccount.CreateLinkedAccountBuilder(postgresService, api, auditingService)
            .setKey(razorpayId)
            .setSecret(razorpaySecret)
            .setWebClient(webClient)
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
