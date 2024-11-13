package iudx.data.marketplace.policies;

import static iudx.data.marketplace.common.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.postgres.PostgresService;

public class PolicyVerticle extends AbstractVerticle {

  private PostgresService postgresServiceImpl;
  private PolicyServiceImpl policyService;
  private DeletePolicy deletePolicy;
  private CreatePolicy createPolicy;
  private VerifyPolicy verifyPolicy;
  private GetPolicy getPolicy;
  private AuditingService auditingService;
  private Api api;
  private FetchPolicyUsingPvId fetchPolicyUsingPvId;

  @Override
  public void start() {
    postgresServiceImpl = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    auditingService = AuditingService.createProxy(vertx, AUDITING_SERVICE_ADDRESS);
    api = Api.getInstance(config().getString("dxApiBasePath"));
    deletePolicy = new DeletePolicy(postgresServiceImpl, auditingService, api);
    getPolicy = new GetPolicy(postgresServiceImpl);
    createPolicy = new CreatePolicy(postgresServiceImpl, auditingService, api);
    verifyPolicy = new VerifyPolicy(postgresServiceImpl);
    fetchPolicyUsingPvId = new FetchPolicyUsingPvId(postgresServiceImpl);
    policyService =
        new PolicyServiceImpl(
            deletePolicy, createPolicy, getPolicy, verifyPolicy, fetchPolicyUsingPvId);

    new ServiceBinder(vertx)
        .setAddress(POLICY_SERVICE_ADDRESS)
        .register(PolicyService.class, policyService);
  }
}
