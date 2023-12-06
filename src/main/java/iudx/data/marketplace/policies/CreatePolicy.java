package iudx.data.marketplace.policies;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static iudx.data.marketplace.apiserver.util.Constants.TITLE;
import static iudx.data.marketplace.product.util.Constants.TYPE;

public class CreatePolicy {
    private static final Logger LOGGER = LogManager.getLogger(CreatePolicy.class);
    private final PostgresService postgresService;
    private final CatalogueService catalogueService;

    public CreatePolicy(PostgresService postgresService, CatalogueService catalogueService) {
        this.postgresService = postgresService;
        this.catalogueService = catalogueService;
    }
    public Future<JsonObject> initiateCreatePolicy(JsonObject request, User user) {
        Promise<JsonObject> promise = Promise.promise();
        /* TODO : Create a policy and return the response saying policy is created */
        /* 200 Success response */
        /* 500 Failure response */
        /* creating a dummy policy, resource, provider, consumer, product*/

        /* consumer details initialization */
        UUID consumerId = UUID.randomUUID();
        String consumerEmailId = "consumer@gmail.com";
        String consumerFirstName = "Alice";
        String consumerLastName = "Consumer";

        JsonObject consumerInsertionJson = new JsonObject()
                .put("consumerId", consumerId.toString())
                .put("consumerEmailId", consumerEmailId)
                .put("consumerFirstName", consumerFirstName)
                .put("consumerLastName", consumerLastName);

        /* provider details initialization */
        UUID providerId = UUID.randomUUID();
        String providerEmailId = "provider@gmail.com";
        String providerFirstName = "Bob";
        String providerLastName = "Provider";

        JsonObject providerInsertionJson = new JsonObject()
                .put("providerId", providerId.toString())
                .put("providerEmailId", providerEmailId)
                .put("providerFirstName", providerFirstName)
                .put("providerLastName", providerLastName);
        String userInsertion = "INSERT INTO user_table (_id, email_id, first_name, last_name) VALUES ($1, $2, $3, $4)";


        /* resource related information */
        UUID resourceId = UUID.randomUUID();
        String resourceName = "Dummy-resource";
        String resourceServerUrl = "rs.iudx.io";
        String accessPolicy = "streaming";

        JsonObject resourceEntityJson = new JsonObject()
                .put("resourceId", resourceId.toString())
                .put("resourceName", resourceName)
                .put("providerId", providerId.toString())
                .put("resourceServerUrl", resourceServerUrl)
                .put("accessPolicy", accessPolicy);
        String resourceEntityInsertion = "INSERT INTO resource_entity (_id, resource_name, provider_id, resource_server_url, accesspolicy)" +
                " VALUES ($1, $2, $3, $4, $5)";



        /* product related info */
        UUID productId = UUID.randomUUID();
        String status = "ACTIVE";
        JsonObject productJson = new JsonObject()
                .put("productId", productId.toString())
              .put("resourceId", resourceId.toString())
                .put("status", status);
        String insertProduct = "INSERT INTO PRODUCT (product_id, resource_id, status) VALUES ($1, $2, $3)";

    /* policy related information */
    UUID policyId = UUID.randomUUID();
    JsonObject constraints = new JsonObject().put("access", "file");
    String expiry_amount = "40 MB";
    String policyStatus = "ACTIVE";
    JsonObject policyJson =
        new JsonObject()
           .put("policyId", policyId.toString())
            .put("resourceId", resourceId.toString())
            .put("constraints", constraints)
           .put("providerId", providerId.toString())
            .put("consumerEmailId", consumerEmailId)
            .put("expiry_amount", expiry_amount)
            .put("policyStatus", policyStatus)
               .put("productId", productId.toString());

    String insertPolicy = "INSERT INTO POLICY (_id, resource_id, constraints, provider_id, consumer_email_id, expiry_amount, status, product_id)" +
            " VALUES ($1, $2, $3, $4, $5, $6, $7, $8)";

    var consumerInsertion =
        insertQueries(userInsertion, consumerInsertionJson, "consumer insertion ");


    var providerInsertion =
            consumerInsertion.compose(consumerInsertedSuccessfully -> {
                return insertQueries(userInsertion, providerInsertionJson, "provider insertion ");
            });

    var resourceInsertion =
        providerInsertion.compose(
            providerInsertedSuccessfully -> {
              return insertQueries(
                  resourceEntityInsertion, resourceEntityJson, "resource insertion");
            });

    var productInsertion =
        resourceInsertion.compose(
            resourceInsertedSuccessfully -> {
              return insertQueries(insertProduct, productJson, "product insertion ");
            });

    var policyInsertion =
        productInsertion.compose(
            productInsertedSuccessfully -> {
              return insertQueries(insertPolicy, policyJson, "policy insertion ");
            });

    policyInsertion.onComplete(
        handler -> {
          if (handler.succeeded()) {
            promise.complete(
                new JsonObject()
                    .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                    .put(TITLE, "Insertion successful"));
          } else {
            promise.fail(
                new JsonObject()
                    .put(TYPE, ResponseUrn.DB_ERROR_URN.getUrn())
                    .put(TITLE, "Insertion Failed")
                    .encode());
          }
        });

    return promise.future();
  }

  public Future<JsonObject> insertQueries(String query, JsonObject params, String tag) {
        Promise<JsonObject> promise = Promise.promise();
    postgresService.executePreparedQuery(
        query,
        params,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Query insertion successfully done : {}", tag);
            promise.complete(new JsonObject().put(TYPE, ResponseUrn.SUCCESS_URN.getUrn()).put(TITLE, "Success"));
          } else {
            LOGGER.error("Failure while executing query : {} ", tag);
            LOGGER.error("Error : {}", handler.cause().getMessage());
            promise.fail(new JsonObject().put(TYPE, ResponseUrn.DB_ERROR_URN.getUrn()).put(TITLE, "Failure").encode());
          }
        });
    return promise.future();
    }

}
