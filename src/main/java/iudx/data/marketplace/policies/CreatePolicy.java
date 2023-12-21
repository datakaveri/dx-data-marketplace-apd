package iudx.data.marketplace.policies;

import com.google.common.base.Predicates;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static iudx.data.marketplace.apiserver.util.Constants.TITLE;
import static iudx.data.marketplace.product.util.Constants.TYPE;

public class CreatePolicy {
    private static final Logger LOGGER = LogManager.getLogger(CreatePolicy.class);
    private final PostgresServiceImpl postgresService;
    private final CatalogueService catalogueService;

    public CreatePolicy(PostgresServiceImpl postgresService, CatalogueService catalogueService) {
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

        Tuple consumerInsertionTuple = Tuple.of(consumerId, consumerEmailId, consumerFirstName, consumerLastName);

        /* provider details initialization */
        UUID providerId = UUID.randomUUID();
        String providerEmailId = "provider@gmail.com";
        String providerFirstName = "Bob";
        String providerLastName = "Provider";

        Tuple providerInsertionTuple = Tuple.of(providerId, providerEmailId, providerFirstName, providerLastName);

        String userInsertion = "INSERT INTO user_table (_id, email_id, first_name, last_name) VALUES ($1, $2, $3, $4)";


        /* resource related information */
        UUID resourceId = UUID.randomUUID();
        String resourceName = UUID.randomUUID().toString().substring(0, 6);
        String resourceServerUrl = "rs.iudx.io";
        String accessPolicy = "streaming";

        Tuple resourceEntityTuple = Tuple.of(resourceId, resourceName, providerId, resourceServerUrl, accessPolicy);
        String resourceEntityInsertion = "INSERT INTO resource_entity (_id, resource_name, provider_id, resource_server_url, accesspolicy)" +
                " VALUES ($1, $2, $3, $4, $5)";



        /* product related info */
        UUID productId = UUID.randomUUID();
        String status = "ACTIVE";

        Tuple productTuple = Tuple.of(productId, resourceId, status);
        String insertProduct = "INSERT INTO PRODUCT (product_id, resource_id, status) VALUES ($1, $2, $3)";

    /* policy related information */
    UUID policyId = UUID.randomUUID();
    JsonObject constraints = new JsonObject().put("access", "file");
    LocalDateTime expiry_at = LocalDateTime.of(2025,4, 4, 4, 5,6);
    String policyStatus = "ACTIVE";
    Tuple policyTuple = Tuple.of(policyId, resourceId, constraints, providerId,consumerEmailId, expiry_at, policyStatus, productId);
    String insertPolicy = "INSERT INTO POLICY (_id, resource_id, constraints, provider_id, consumer_email_id, expiry_at, status, product_id)" +
            " VALUES ($1, $2, $3, $4, $5, $6, $7, $8)";

    /* purchase related info */
        UUID purchaseId = UUID.randomUUID();
        String paymentStatus = "SUCCEEDED";
        LocalDateTime paymentTime = LocalDateTime.now();
        int expiry = 4;
        JsonObject productVariant = new JsonObject().put("someKey", "someValue");
        Tuple purchaseTuple = Tuple.of(purchaseId, consumerId, productId, paymentStatus, paymentTime, expiry,productVariant);
        String insertPurchase = "INSERT INTO PURCHASE (_id, consumer_id, product_id, payment_status, " +
                "payment_time, expiry, " +
                "product_variant) VALUES ($1, $2, $3, $4,$5, $6, $7);";




    var consumerInsertion =
        insertQueries(userInsertion, consumerInsertionTuple, "consumer insertion ");


    var providerInsertion =
            consumerInsertion.compose(consumerInsertedSuccessfully -> {
                return insertQueries(userInsertion, providerInsertionTuple, "provider insertion ");
            });

    var resourceInsertion =
        providerInsertion.compose(
            providerInsertedSuccessfully -> {
              return insertQueries(
                  resourceEntityInsertion, resourceEntityTuple, "resource insertion");
            });

    var productInsertion =
        resourceInsertion.compose(
            resourceInsertedSuccessfully -> {
              return insertQueries(insertProduct, productTuple, "product insertion ");
            });

    var policyInsertion =
        productInsertion.compose(
            productInsertedSuccessfully -> {
              return insertQueries(insertPolicy, policyTuple, "policy insertion ");
            });

        var purchaseInsertion =
                policyInsertion.compose(
                        policyInsertedSuccessfully -> {
                            return insertQueries(insertPurchase, purchaseTuple, "purchase insertion");
                        }
                );

        policyInsertion.onComplete(
        handler -> {
          if (handler.succeeded()) {
            promise.complete(
                new JsonObject()
                    .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                    .put(TITLE, "Insertion successful"));
          } else {
              handler.cause().printStackTrace();
            promise.fail(
                new JsonObject()
                    .put(TYPE, ResponseUrn.DB_ERROR_URN.getUrn())
                    .put(TITLE, "Insertion Failed")
                    .encode());
          }
        });

//        RetryPolicy<Object> retryPolicy = RetryPolicy.builder()
//                .handle(Exception.class)
//                .onRetry(event -> {
//                    LOGGER.info("retrying ----->");
//                })
//                .onFailure(event -> {
//                    LOGGER.error("Policy creation failed");
//                    // send the failure response with URN from here
//                // trigger some script that will tell us this has failed
//                }).onSuccess(successEvent -> {
//                    // Success response with URN
//                })
//                .withDelay(Duration.ofSeconds(5))
//                .withBackoff(1, 3, ChronoUnit.SECONDS) // look at the documentation to include decimals
//                .withMaxRetries(4)
//                .build();
//
//    Failsafe.with(retryPolicy)
//        .getAsyncExecution(
//            handler -> {
//              /* inserting same consumer information again which will lead to postgres throwing -- error */
//              insertQueries(userInsertion, consumerInsertionTuple, "consumer insertion ")
//                  .onComplete(
//                      insertQueryHandler -> {
//                        if (insertQueryHandler.succeeded()) {
//                          handler.recordResult(insertQueryHandler.result());
//                        } else {
//                          handler.recordException(
//                              new DxRuntimeException(
//                                  500,
//                                  ResponseUrn.DB_ERROR_URN,
//                                  insertQueryHandler.cause().getMessage()));
//                        }
//                      });
//            });

    return promise.future();
  }

  public Future<JsonObject> insertQueries(String query, Tuple params, String tag) {
        Promise<JsonObject> promise = Promise.promise();
    postgresService.executePreparedQuery(
        query,
        params).onSuccess(handler -> {
        LOGGER.info("Query insertion successfully done : {}", tag);
        promise.complete(new JsonObject().put(TYPE, ResponseUrn.SUCCESS_URN.getUrn()).put(TITLE, "Success"));

    }).onFailure(failureHandler -> {
        LOGGER.error("Failure while executing query : {} ", tag);
        LOGGER.error("Error : {}", failureHandler.getCause().getMessage());
        promise.fail(new JsonObject().put(TYPE, ResponseUrn.DB_ERROR_URN.getUrn()).put(TITLE, "Failure").encode());
    });
    return promise.future();
    }

}
