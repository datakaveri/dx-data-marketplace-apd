package iudx.data.marketplace;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


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

        if (container.isRunning()) {
            LOG.info("container is running....");
            postgresService = new PostgresServiceImpl(postgresConfig,vertx);

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
        var consumerInsertion = postgresService.executePreparedQuery(INSERT_INTO_USER_TABLE, consumerTuple);

        var providerInsertion = consumerInsertion.
                compose(handler -> postgresService.executePreparedQuery(INSERT_INTO_USER_TABLE, ownerTuple));
        var resourceInsertion = providerInsertion.
                compose(handler -> postgresService.executePreparedQuery(INSERT_INTO_RESOURCE_ENTITY_TABLE, resourceInsertionTuple));
        var productInsertion = resourceInsertion.
                compose(handler -> postgresService.executePreparedQuery(INSERT_INTO_PRODUCT_TABLE, productTuple));
        var policyInsertion = productInsertion
                .compose(handler -> postgresService.executePreparedQuery(INSERT_INTO_POLICY_TABLE, policyInsertionTuple));
        var purchaseInsertion = policyInsertion
                .compose(handler -> postgresService.executePreparedQuery(INSERT_INTO_PURCHASE_TABLE, purchaseTuple));

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


}
