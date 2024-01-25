package iudx.data.marketplace.apiserver.provider;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.postgres.PostgresService;

import static iudx.data.marketplace.common.Constants.*;

public class LinkedAccountVerticle extends AbstractVerticle {

  private PostgresService postgresService;
  private LinkedAccountServiceImpl linkedAccountService;
  private CreateLinkedAccount createLinkedAccount;
  private FetchLinkedAccount fetchLinkedAccount;
  private UpdateLinkedAccount updateLinkedAccount;
  private AuditingService auditingService;

  private Api api;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    api = Api.getInstance(config().getString("dxApiBasePath"));
    auditingService = AuditingService.createProxy(vertx, AUDITING_SERVICE_ADDRESS);
    createLinkedAccount = new CreateLinkedAccount(postgresService, api, auditingService);
    fetchLinkedAccount = new FetchLinkedAccount(postgresService, api);
    updateLinkedAccount = new UpdateLinkedAccount(postgresService, api, auditingService);
    linkedAccountService =
        new LinkedAccountServiceImpl(createLinkedAccount, fetchLinkedAccount, updateLinkedAccount, config());

    new ServiceBinder(vertx)
        .setAddress(LINKED_ACCOUNT_ADDRESS)
        .register(LinkedAccountService.class, linkedAccountService);
    startPromise.complete();
  }
}
