package iudx.data.marketplace.policies;

import com.google.common.base.Predicates;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Tuple;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.util.Status;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.auditing.util.Constants.*;
import static iudx.data.marketplace.product.util.Constants.TYPE;

public class CreatePolicy {
    private static final Logger LOGGER = LogManager.getLogger(CreatePolicy.class);
    private final PostgresService postgresService;
    private final CatalogueService catalogueService;
    private AuditingService auditingService;
    private Api api;


    public CreatePolicy(PostgresService postgresService, CatalogueService catalogueService, AuditingService auditingService, Api api) {
        this.postgresService = postgresService;
        this.catalogueService = catalogueService;
        this.auditingService = auditingService;
        this.api = api;
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

        JsonObject userJson = new JsonObject()
                .put("userId", providerId)
                .put(USER_ROLE, "provider")
                .put(EMAIL_ID, providerEmailId)
                .put(FIRST_NAME, providerFirstName)
                .put(LAST_NAME, providerLastName)
                .put(RS_SERVER_URL, "rs.iudx.io");
        User user1 = new User(userJson);

        String userInsertion2 = "INSERT INTO user_table (_id, email_id, first_name, last_name) VALUES ($1, $2, $3, $4)";

        JsonObject consumerParams = new JsonObject()
                .put("$1", consumerId.toString())
                .put("$2", consumerEmailId)
                .put("$3", consumerFirstName)
                .put("$4", consumerLastName);

        JsonObject providerParams = new JsonObject()
                .put("$1", providerId.toString())
                .put("$2", providerEmailId)
                .put("$3", providerFirstName)
                .put("$4", providerLastName);

        /* resource related information */
        UUID resourceId = UUID.randomUUID();
        String resourceName = UUID.randomUUID().toString().substring(0, 6);
        String resourceServerUrl = "rs.iudx.io";
        String accessPolicy = "SECURE";

        Tuple resourceEntityTuple = Tuple.of(resourceId, resourceName, providerId, resourceServerUrl, accessPolicy);
        String resourceEntityInsertion = "INSERT INTO resource_entity (_id, resource_name, provider_id, resource_server_url, accesspolicy)" +
                " VALUES ($1, $2, $3, $4, $5)";

        JsonObject resourceInsertionParam = new JsonObject()
                .put("$1", resourceId.toString())
                .put("$2", resourceName)
                .put("$3", providerId.toString())
                .put("$4", resourceServerUrl)
                .put("$5", accessPolicy);

        /* product related info */
        UUID productId = UUID.randomUUID();
        String status = "ACTIVE";

        Tuple productTuple = Tuple.of(productId, resourceId, status);
//        String insertProduct = "INSERT INTO PRODUCT (product_id, resource_id, status) VALUES ('$1', '$2', '$3')";

        String insertProduct2 = "INSERT INTO PRODUCT (product_id, resource_id, status) VALUES ($1, $2, $3)";
        JsonObject productParam = new JsonObject()
                .put("$1", productId.toString())
                .put("$2", resourceId.toString())
                .put("$3", status);

        /* product variant insertion */
        UUID pvId = UUID.randomUUID();
        String productVariantName = UUID.randomUUID().toString();
        String resourceNames = "ARRAY['" +resourceName + "']";
        String resourceIds = "ARRAY['" +resourceId + "']";
        String constraint = "ARRAY['" +new JsonObject().put("access", "file").encode() + "']";
        String price = "10$";
        int validity = 10;
        String productVariantStatus = "ACTIVE";
//        Tuple productVariantTuple = Tuple.of(pvId, productVariantName, productId, providerId,
//                resourceNames, resourceIds, constraint, price, validity, productVariantStatus);

        String productVariantInsertion = "INSERT INTO public.product_variant(" +
                "_id, product_variant_name, product_id, provider_id, resource_name, resource_ids, " +
                "resource_capabilities, price, validity, status)" +
                " VALUES ('$1', '$2', '$3', '$4', $5, $6::uuid[], $7, '$8', '$9', '$10');";

    String finalProductVariantInsertion =
        productVariantInsertion
            .replace("'$1'", "'" + pvId.toString() + "'")
            .replace("$2", productVariantName)
            .replace("$3", productId.toString())
            .replace("$4", providerId.toString())
            .replace("$5", resourceNames)
            .replace("$6", resourceIds)
            .replace("$7", constraint)
            .replace("$8", price)
            .replace("$9", String.valueOf(validity))
           .replace("'$10'", "'" + productVariantStatus +"'" );

        /* purchase related info */
        UUID purchaseId = UUID.randomUUID();
        String paymentStatus = "SUCCEEDED";
        LocalDateTime paymentTime = LocalDateTime.now();
        int expiry = 4;
        JsonObject productVariant = new JsonObject().put("someKey", "someValue");
        Tuple purchaseTuple = Tuple.of(purchaseId, consumerId, pvId, paymentStatus, paymentTime, expiry,productVariant);
        String insertPurchase = "INSERT INTO PURCHASE (_id, consumer_id, product_variant_id, payment_status, " +
                "payment_time, expiry, " +
                "product_variant) VALUES ('$1', '$2', '$3', '$4','$5', '$6', '$7');";

        String finalPurchaseInsertion = insertPurchase
                .replace("$1", purchaseId.toString())
                .replace("$2", consumerId.toString())
                .replace("$3", pvId.toString())
                .replace("$4", paymentStatus)
                .replace("$5", paymentTime.toString())
                .replace("$6", String.valueOf(expiry))
                .replace("$7", productVariant.encode());

    /* policy related information */
    UUID policyId = UUID.randomUUID();
    JsonObject constraints = new JsonObject().put("access", "file");
    LocalDateTime expiry_at = LocalDateTime.of(2025,4, 4, 4, 5,6);
    String policyStatus = "ACTIVE";
    Tuple policyTuple = Tuple.of(policyId, resourceId, purchaseId, constraints, providerId,consumerEmailId, expiry_at, policyStatus, pvId);

    String insertPolicy = "INSERT INTO POLICY (_id, resource_id, purchase_id,  constraints, provider_id, consumer_email_id, expiry_at, status, product_variant_id)" +
            " VALUES ('$1', '$2', '$3', '$4', '$5', '$6', '$7', '$8', '$9')";

    String finalInsertPolicy = insertPolicy
            .replace("$1", policyId.toString())
            .replace("$2", resourceId.toString())
            .replace("$3", purchaseId.toString())
            .replace("$4", constraints.encode())
            .replace("$5", providerId.toString())
            .replace("$6", consumerEmailId)
            .replace("$7", expiry_at.toString())
            .replace("$8", policyStatus)
            .replace("$9", pvId.toString());




    var consumerInsertion =
        insertQueries(userInsertion2,consumerParams, "consumer insertion ");


    var providerInsertion =
            consumerInsertion.compose(consumerInsertedSuccessfully -> {
                return insertQueries(userInsertion2, providerParams, "provider insertion ");
            });

    var resourceInsertion =
        providerInsertion.compose(
            providerInsertedSuccessfully -> {
              return insertQueries(
                  resourceEntityInsertion, resourceInsertionParam, "resource insertion");
            });

    var productInsertion =
        resourceInsertion.compose(
            resourceInsertedSuccessfully -> {
              return insertQueries(insertProduct2, productParam, "product insertion ");
            });

    var pvInsertion = productInsertion.compose(productInsertedSuccessfully -> {
        return insertQueries(finalProductVariantInsertion,"product variant insertion ");
    });

    var purchaseInsertion =
        pvInsertion.compose(
            productVariantInsertedSuccessfully -> {
              return insertQueries(finalPurchaseInsertion, "purchase insertion");
            });

    var policyInsertion =
        purchaseInsertion.compose(
            purchaseDoneSuccessfully -> {
              return insertQueries(finalInsertPolicy, "policy insertion ");
            });
        policyInsertion.onComplete(
        handler -> {
          if (handler.succeeded()) {
            promise.complete(
                new JsonObject()
                    .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                    .put(TITLE, "Insertion successful"));

              /* sending information for auditing */

              /* audit info = Request body + response + extra information if any*/
              JsonObject auditInfo = new JsonObject()
                      .put("policyId", policyId.toString())
                      .put("resourceId", resourceId.toString())
                      .put("purchaseId", purchaseId.toString())
                      .put("constraints",constraints.encode())
                      .put("providerId", providerId.toString())
                      .put("consumerEmailId", consumerEmailId)
                      .put("expiryAt", expiry_at.toString())
                      .put("productVariantId", pvId.toString())
                      .put("resourceServerUrl", resourceServerUrl)
                      .put("accessPolicy",accessPolicy)
                      .put("policyStatus", Status.ACTIVE.toString());
              auditingService.handleAuditLogs(user1, auditInfo, api.getPoliciesUrl(), HttpMethod.POST.toString());
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

  public Future<JsonObject> insertQueries(String query, String tag) {
    Promise<JsonObject> promise = Promise.promise();
    postgresService.executeQuery(
        query,
        pgHandler -> {
          if (pgHandler.succeeded()) {
            LOGGER.info("Query insertion successfully done : {}", tag);
            promise.complete(
                new JsonObject().put(TYPE, ResponseUrn.SUCCESS_URN.getUrn()).put(TITLE, "Success"));
          } else {
            LOGGER.error("Failure while executing query : {} ", tag);
            LOGGER.error("Error : {}", pgHandler.cause().getMessage());
            promise.fail(
                new JsonObject()
                    .put(TYPE, ResponseUrn.DB_ERROR_URN.getUrn())
                    .put(TITLE, "Failure")
                    .encode());
          }
        });
    return promise.future();
    }

    public Future<JsonObject> insertQueries(String query,JsonObject param, String tag) {
        Promise<JsonObject> promise = Promise.promise();
        postgresService.executePreparedQuery(
                query,
                param,
                pgHandler -> {
                    if (pgHandler.succeeded()) {
                        LOGGER.info("Query insertion successfully done : {}", tag);
                        promise.complete(
                                new JsonObject().put(TYPE, ResponseUrn.SUCCESS_URN.getUrn()).put(TITLE, "Success"));
                    } else {
                        LOGGER.error("Failure while executing query : {} ", tag);
                        LOGGER.error("Error : {}", pgHandler.cause().getMessage());
                        promise.fail(
                                new JsonObject()
                                        .put(TYPE, ResponseUrn.DB_ERROR_URN.getUrn())
                                        .put(TITLE, "Failure")
                                        .encode());
                    }
                });
        return promise.future();
    }
}
