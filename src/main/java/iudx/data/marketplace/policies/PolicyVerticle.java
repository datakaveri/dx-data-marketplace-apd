package iudx.data.marketplace.policies;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;

import java.util.Map;

import static iudx.data.marketplace.common.Constants.POLICY_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.Constants.POSTGRES_SERVICE_ADDRESS;
import static iudx.data.marketplace.policies.util.Constants.DB_RECONNECT_ATTEMPTS;
import static iudx.data.marketplace.policies.util.Constants.DB_RECONNECT_INTERVAL_MS;

public class PolicyVerticle extends AbstractVerticle {

    private PostgresService postgresService;
    private PolicyServiceImpl policyService;
    private DeletePolicy deletePolicy;
    private CreatePolicy createPolicy;
    private VerifyPolicy verifyPolicy;
    private GetPolicy getPolicy;
    private CatalogueService catalogueService;
    private Vertx vertx;

    @Override
    public void start() throws Exception {
        vertx = Vertx.vertx();
        catalogueService = new CatalogueService(vertx, config());
        postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
        deletePolicy = new DeletePolicy(postgresService);
        getPolicy = new GetPolicy(postgresService);
        createPolicy = new CreatePolicy(postgresService, catalogueService);
        verifyPolicy = new VerifyPolicy(postgresService, catalogueService);
        policyService = new PolicyServiceImpl(deletePolicy, createPolicy, getPolicy, verifyPolicy, config());

         new ServiceBinder(vertx)
                .setAddress(POLICY_SERVICE_ADDRESS)
                .register(PolicyService.class, policyService);
    }
}
