package iudx.data.marketplace;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static iudx.data.marketplace.apiserver.util.Constants.RESULT;


public class Util {
    public static final String INSERT_INTO_RESOURCE_ENTITY_TABLE =
            "INSERT INTO resource_entity (_id, resource_name, provider_id, resource_server_url, accesspolicy) VALUES ($1, $2, $3, $4, $5) RETURNING _id;";
    public static final String INSERT_INTO_POLICY_TABLE =
            "INSERT INTO POLICY (_id, resource_id, constraints, provider_id, consumer_email_id, expiry_amount, status, product_id)  VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING _id;";
    public static final String INSERT_INTO_USER_TABLE =
            "INSERT INTO user_table (_id, email_id, first_name, last_name) VALUES ($1, $2, $3, $4) RETURNING _id;";
    public static final String INSERT_INTO_PRODUCT_TABLE =
            "INSERT INTO PRODUCT (product_id, resource_id, status) VALUES ($1, $2, $3) RETURNING _id;";
    public static final String INSERT_INTO_PURCHASE_TABLE =
            "INSERT INTO PURCHASE (_id, consumer_id, product_id, payment_status, payment_time, expiry, product_variant) VALUES ($1, $2, $3, $4,$5, $6, $7) RETURNING _id";
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);
    private String status;
    private JsonObject constraints;
    private UUID resourceId;
    private UUID policyId;
    private UUID consumerId;
    private String consumerEmailId;
    private String consumerFirstName;
    private String consumerLastName;
    private UUID ownerId;
    private String ownerEmailId;
    private String ownerFirstName;
    private String ownerLastName;
    private Tuple resourceInsertionTuple;
    private Tuple consumerTuple;
    private Tuple ownerTuple;
    private Tuple policyInsertionTuple;
    private UUID productId;
    private Tuple productTuple;
    private UUID purchaseId;
    private String paymentStatus;
    private LocalDateTime paymentTime;
    private int expiry;
    private JsonObject productVariant;
    private Tuple purchaseTuple;
    private PgConnectOptions connectOptions;
    private PoolOptions poolOptions;
    private PgPool pool;

    private PostgresServiceImpl postgresService;
    public static UUID generateRandomUuid() {
        return UUID.randomUUID();
    }

    public static String generateRandomString() {
        return UUID.randomUUID().toString();
    }

    public static String generateRandomEmailId() {
        return generateRandomString().substring(0, 6)
                + "@"
                + generateRandomString().substring(0, 3)
                + ".com";
    }

    public static String generateRandomUrl() {
        return generateRandomString().substring(0, 2)
                + "."
                + generateRandomString().substring(0, 4)
                + ".io";
    }

    public PostgresService setUp(PostgreSQLContainer container) {
        Vertx vertx = Vertx.vertx();
        Integer port = container.getFirstMappedPort();
        String host = container.getHost();
        String db = container.getDatabaseName();
        String user = container.getUsername();
        String password = container.getPassword();
        String jdbcUrl = container.getJdbcUrl();

        JsonObject postgresConfig =
                new JsonObject()
                        .put("databaseIP", host)
                        .put("databasePort", port)
                        .put("databaseName", db)
                        .put("databaseUserName", user)
                        .put("databaseSchema", "public")
                        .put("databasePassword", password)
                        .put("poolSize", 25);
        this.connectOptions =
                new PgConnectOptions()
                        .setPort(port)
                        .setHost(host)
                        .setDatabase(db)
                        .setUser(user)
                        .setPassword(password)
                        .setReconnectAttempts(2)
                        .setReconnectInterval(1000L);

        this.poolOptions = new PoolOptions().setMaxSize(25);
        if (container.isRunning()) {
            LOG.info("container is running....");
            this.pool = PgPool.pool(vertx, connectOptions, poolOptions);
            postgresService = new PostgresServiceImpl(pool);

            Flyway flyway =
                    Flyway.configure()
                            .dataSource(
                                    jdbcUrl,
                                    postgresConfig.getString("databaseUserName"),
                                    postgresConfig.getString("databasePassword"))
                            .placeholders(Map.of("user", user, "dmp_user", user))
                            .locations("db/migration/")
                            .load();
            flyway.repair();
            var migrationResult = flyway.migrate();
            LOG.info("Migration result {}", migrationResult.migrationsExecuted);
            LOG.info("Migration details {}", migrationResult.getTotalMigrationTime());

            initialize();
            return postgresService;
        }
        return null;
    }

    private void initialize() {
        status = "ACTIVE";
        LocalDateTime expiryTime = LocalDateTime.of(2025, 12, 10, 3, 20, 20, 29);
        constraints = new JsonObject();
        resourceId = generateRandomUuid();
        ownerId = generateRandomUuid();

        String resourceName = generateRandomString();
        String accessPolicy = generateRandomString();

        resourceInsertionTuple =
                Tuple.of(resourceId, resourceName, ownerId, "rs.iudx.io", accessPolicy);

        consumerId = generateRandomUuid();
        consumerEmailId = generateRandomEmailId();
        consumerFirstName = generateRandomString();
        consumerLastName = generateRandomString();
        consumerTuple =
                Tuple.of(
                        consumerId, consumerEmailId, consumerFirstName, consumerLastName);

        ownerEmailId = generateRandomEmailId();
        ownerFirstName = generateRandomString();
        ownerLastName = generateRandomString();
        ownerTuple =
                Tuple.of(ownerId, ownerEmailId, ownerFirstName, ownerLastName);

        productId = generateRandomUuid();
        productTuple = Tuple.of(productId, resourceId, status);



        policyId = generateRandomUuid();
        String expiryAmount = "40 MB";

        policyInsertionTuple =
                Tuple.of(policyId, resourceId, constraints, ownerId, consumerEmailId, expiryAmount, status, productId);

        purchaseId = generateRandomUuid();
        paymentStatus = "SUCCEEDED";
        paymentTime = LocalDateTime.of(2023, 12, 10, 3, 20, 20, 29);
        expiry = 4;
        productVariant = new JsonObject().put("someKey", "someValue");
        purchaseTuple = Tuple.of(purchaseId, consumerId, productId, paymentStatus, paymentTime, expiry, productVariant);
    }

    // compose
    public Future<Boolean> testInsert() {
        LOG.info("inside test insert method");

        Promise<Boolean> promise = Promise.promise();
        var consumerInsertion = this.executeQuery(consumerTuple, INSERT_INTO_USER_TABLE);

        var providerInsertion = consumerInsertion.
                compose(handler -> this.executeQuery(ownerTuple, INSERT_INTO_USER_TABLE));
        var resourceInsertion = providerInsertion.
                compose(handler -> this.executeQuery(resourceInsertionTuple, INSERT_INTO_RESOURCE_ENTITY_TABLE));
        var productInsertion = resourceInsertion.
                compose(handler -> this.executeQuery(productTuple, INSERT_INTO_PRODUCT_TABLE));
        var policyInsertion = productInsertion
                .compose(handler -> this.executeQuery(policyInsertionTuple,INSERT_INTO_POLICY_TABLE));
        var purchaseInsertion = policyInsertion
                .compose(handler -> this.executeQuery(purchaseTuple, INSERT_INTO_PURCHASE_TABLE));

        purchaseInsertion.onComplete(handler -> {
            if(handler.succeeded())
            {
                LOG.info("Insertion successful");
                promise.complete(true);
            }
            else
            {
                handler.cause().printStackTrace();
                LOG.error("Failed : " + handler.cause());
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }

    public String getStatus() {
        return status;
    }

    public JsonObject getConstraints() {
        return constraints;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public UUID getConsumerId() {
        return consumerId;
    }

    public String getConsumerEmailId() {
        return consumerEmailId;
    }

    public String getConsumerFirstName() {
        return consumerFirstName;
    }

    public String getConsumerLastName() {
        return consumerLastName;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerEmailId() {
        return ownerEmailId;
    }

    public String getOwnerFirstName() {
        return ownerFirstName;
    }

    public String getOwnerLastName() {
        return ownerLastName;
    }

    public Tuple getResourceInsertionTuple() {
        return resourceInsertionTuple;
    }

    public Tuple getConsumerTuple() {
        return consumerTuple;
    }

    public Tuple getOwnerTuple() {
        return ownerTuple;
    }

    public Tuple getPolicyInsertionTuple() {
        return policyInsertionTuple;
    }

    public UUID getProductId() {
        return productId;
    }

    public Tuple getProductTuple() {
        return productTuple;
    }

    public UUID getPurchaseId() {
        return purchaseId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public int getExpiry() {
        return expiry;
    }

    public JsonObject getProductVariant() {
        return productVariant;
    }

    public Tuple getPurchaseTuple() {
        return purchaseTuple;
    }

    public PostgresServiceImpl getPostgresService() {
        return postgresService;
    }

    public Future<JsonObject> executeQuery(Tuple tuple, String query) {
        Promise<JsonObject> promise = Promise.promise();
        Collector<Row, ?, List<JsonObject>> rowListCollectors =
                Collectors.mapping(row -> row.toJson(), Collectors.toList());

        pool.withConnection(
                        sqlConnection ->
                                sqlConnection
                                        .preparedQuery(query)
                                        .collecting(rowListCollectors)
                                        .execute(tuple)
                                        .map(rows -> rows.value()))
                .onSuccess(
                        successHandler -> {
                            promise.complete(
                                    new JsonObject().put(RESULT, "Success").put("response", successHandler));
                        })
                .onFailure(
                        failureHandler -> {
                            failureHandler.printStackTrace();
                            promise.fail("Failure due to: " + failureHandler.getCause().getMessage());
                        });
        return promise.future();
    }

    public Future<JsonObject> executeBatchQuery(List<Tuple> tuple, String query) {
        Promise<JsonObject> promise = Promise.promise();
        Collector<Row, ?, List<JsonObject>> rowListCollectors =
                Collectors.mapping(row -> row.toJson(), Collectors.toList());

        pool.withConnection(
                        sqlConnection ->
                                sqlConnection
                                        .preparedQuery(query)
                                        .collecting(rowListCollectors)
                                        .executeBatch(tuple)
                                        .map(rows -> rows.value()))
                .onSuccess(
                        successHandler -> {
                            promise.complete(
                                    new JsonObject().put(RESULT, "Success").put("response", successHandler));
                        })
                .onFailure(
                        failureHandler -> {
                            failureHandler.printStackTrace();
                            promise.fail("Failure due to: " + failureHandler.getCause().getMessage());
                        });
        return promise.future();
    }
}
