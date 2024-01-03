package iudx.data.marketplace.policies;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import iudx.data.marketplace.apiserver.ApiServerVerticle;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;

import java.util.Map;

import static iudx.data.marketplace.common.Constants.POLICY_SERVICE_ADDRESS;
import static iudx.data.marketplace.policies.util.Constants.DB_RECONNECT_ATTEMPTS;

public class PolicyVerticle extends AbstractVerticle {

  private PostgresServiceImpl postgresServiceImpl;
  private PolicyServiceImpl policyService;
  private DeletePolicy deletePolicy;
  private CreatePolicy createPolicy;
  private VerifyPolicy verifyPolicy;
  private GetPolicy getPolicy;
  private CatalogueService catalogueService;

  @Override
  public void start() {
    catalogueService = new CatalogueService(vertx, config());
    postgresServiceImpl = new PostgresServiceImpl(config(), vertx);
    deletePolicy = new DeletePolicy(postgresServiceImpl);
    getPolicy = new GetPolicy(postgresServiceImpl);
    createPolicy = new CreatePolicy(postgresServiceImpl, catalogueService);
    verifyPolicy = new VerifyPolicy(postgresServiceImpl);
    policyService =
        new PolicyServiceImpl(deletePolicy, createPolicy, getPolicy, verifyPolicy, config());

    new ServiceBinder(vertx)
        .setAddress(POLICY_SERVICE_ADDRESS)
        .register(PolicyService.class, policyService);
  }
}
