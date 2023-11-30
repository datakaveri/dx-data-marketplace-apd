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
    private PgPool pool;
    private Vertx vertx;

    @Override
    public void start() throws Exception {
        vertx = Vertx.vertx();
        /* Database properties */
        /* Database Properties */
        String databaseIp = config().getString("databaseIP");
        int databasePort = config().getInteger("databasePort");
        String databaseSchema = config().getString("databaseSchema");
        String databaseName = config().getString("databaseName");
        String databaseUserName = config().getString("databaseUserName");
        String databasePassword = config().getString("databasePassword");
        int poolSize = config().getInteger("poolSize");
        Map<String, String> schemaProp = Map.of("search_path", databaseSchema);

        /* Set Connection Object and schema */
        PgConnectOptions connectOptions =
                new PgConnectOptions()
                        .setPort(databasePort)
                        .setHost(databaseIp)
                        .setProperties(schemaProp)
                        .setDatabase(databaseName)
                        .setUser(databaseUserName)
                        .setPassword(databasePassword)
                        .setReconnectAttempts(DB_RECONNECT_ATTEMPTS)
                        .setReconnectInterval(DB_RECONNECT_INTERVAL_MS);

        /* Pool options */
        PoolOptions poolOptions = new PoolOptions().setMaxSize(poolSize);

        /* Create the client pool */
        this.pool = PgPool.pool(vertx, connectOptions, poolOptions);

        catalogueService = new CatalogueService(vertx, config());
        postgresService = new PostgresServiceImpl(pool);
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
