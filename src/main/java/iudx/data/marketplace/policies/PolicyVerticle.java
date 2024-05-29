package iudx.data.marketplace.policies;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import iudx.data.marketplace.apiserver.ApiServerVerticle;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;

import java.util.Map;

import static iudx.data.marketplace.common.Constants.*;

public class PolicyVerticle extends AbstractVerticle {

  private PostgresService postgresServiceImpl;
  private PolicyServiceImpl policyService;
  private DeletePolicy deletePolicy;
  private CreatePolicy createPolicy;
  private VerifyPolicy verifyPolicy;
  private GetPolicy getPolicy;
  private CatalogueService catalogueService;
  private AuditingService auditingService;
  private Api api;
  private FetchPolicyUsingPvId fetchPolicyUsingPvId;

  @Override
  public void start() {
    catalogueService = new CatalogueService(vertx, config());
    postgresServiceImpl = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    auditingService = AuditingService.createProxy(vertx, AUDITING_SERVICE_ADDRESS);
    api = Api.getInstance(config().getString("dxApiBasePath"));
    deletePolicy = new DeletePolicy(postgresServiceImpl, auditingService, api);
    getPolicy = new GetPolicy(postgresServiceImpl);
    createPolicy = new CreatePolicy(postgresServiceImpl, auditingService, api);
    verifyPolicy = new VerifyPolicy(postgresServiceImpl);
    fetchPolicyUsingPvId = new FetchPolicyUsingPvId(postgresServiceImpl);
    policyService =
            new PolicyServiceImpl(deletePolicy, createPolicy, getPolicy, verifyPolicy,fetchPolicyUsingPvId);

    new ServiceBinder(vertx)
        .setAddress(POLICY_SERVICE_ADDRESS)
        .register(PolicyService.class, policyService);
  }
}
