package iudx.data.marketplace.policies;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.policies.util.Constants.*;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.postgres.PostgresService;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreatePolicy {
  private static final Logger LOGGER = LogManager.getLogger(CreatePolicy.class);
  private static final String POLICY_STATUS = "ACTIVE";
  private final PostgresService postgresService;
  private AuditingService auditingService;
  private Api api;
  private String providerId;
  private String invoiceId;
  private String productVariantId;
  private int expiryInMonths;
  private String consumerEmailId;
  private String expiryAt;
  private User provider;

  public CreatePolicy(PostgresService postgresService, AuditingService auditingService, Api api) {
    this.postgresService = postgresService;
    this.auditingService = auditingService;
    this.api = api;
  }

  /* A policy is created only when the payment status = SUCCEEDED */
  public Future<Boolean> createPolicy(String orderId) {
    /*policyId, resource_id, purchase_id, constraints, provider_id, */
    /* consumer_email_id, status, product_variant_id*/

    /* policyId : UUID : created here */
    /* resource_id : UUID : From product_variant_id : From invoice table */
    /* invoice_id : UUID : invoice table */
    /* constraints : capabilities : product_variant table */
    /* provider_id : product_variant_id [invoice] : provider_id [product_variant_table] */
    /* consumer_email_id : consumer_id [order_table] : email_id [user_table] */
    /* status : ACTIVE */
    /* product_variant_id : invoice table */

    /*WHERE payment_status = SUCCEEDED */
    /* get order ID */
    /* get invoice ID*/
    /* get the product_variant_id based on the order_id */
    /* get the provider ID from pv table based on the invoice ID*/
    /* get the consumer_emailId based on the consumer_id in invoice from user_table */
    /* get the expiry numeric */
    /* calculate the expiry time of the policy based on the numeric */

    /* get the resource ID */
    /* create policy ID */
    /* get each constraint for each resource */
    /* set status = ACTIVE */
    String getResourceInfoQuery = GET_REQUIRED_INFO_QUERY.replace("$1", orderId);
    Future<JsonObject> fetchResourceInfoFuture =
        executePostgresQuery(getResourceInfoQuery, "get resource info");
    Future<Boolean> future =
        fetchResourceInfoFuture.compose(
            jsonObject -> {
              LOGGER.debug("Json object : {}", jsonObject.encodePrettily());
              invoiceId = jsonObject.getString("invoiceId");
              productVariantId = jsonObject.getString("productVariantId");
              expiryInMonths = jsonObject.getInteger("expiry");
              providerId = jsonObject.getString("providerId");

              JsonObject providerUser =
                  new JsonObject().put(USERID, providerId).put(USER_ROLE, Role.PROVIDER.getRole());
              providerUser.mergeIn(jsonObject);
              User provider = new User(providerUser);

              consumerEmailId = jsonObject.getString("consumerEmailId");
              JsonArray resourceIdsAndCapabilities = jsonObject.getJsonArray("resourceInfo");

                List<String> resourceIds =
                  resourceIdsAndCapabilities.stream()
                      .map(e -> JsonObject.mapFrom(e).getString("id"))
                      .collect(Collectors.toList());

                List<JsonArray> listOfConstraints =
                        resourceIdsAndCapabilities.stream()
                                .map(e -> JsonObject.mapFrom(e).getJsonArray("capabilities"))
                                .collect(Collectors.toList());


              LocalDateTime presentTime = LocalDateTime.now();
              expiryAt = presentTime.plusMonths(expiryInMonths).toString();
              String createPolicyFinalQuery =
                  CREATE_POLICY_QUERY
                      .replace("$3", invoiceId)
                      .replace("$5", providerId)
                      .replace("$6", consumerEmailId)
                      .replace("$7", expiryAt)
                      .replace("$8", POLICY_STATUS)
                      .replace("$9", productVariantId);

              List<Future> futureList = new ArrayList<>();
              int index = 0;
              JsonObject constraint = new JsonObject();
              List<String> listOfQueries = new ArrayList<>();
              for (String resourceItem : resourceIds) {
                String policyId = UUID.randomUUID().toString();
                JsonArray list = listOfConstraints.get(index++);
                constraint.put("access", list);
                createPolicyFinalQuery =
                    createPolicyFinalQuery
                        .replace("$1", policyId)
                        .replace("$2", resourceItem)
                        .replace("$4", constraint + "");
                listOfQueries.add(createPolicyFinalQuery);
                futureList.add(
                    initiateAuditing(provider, orderId, policyId, resourceItem, constraint));
              }
              Future<Boolean> policyCreationFuture = executeTransaction(listOfQueries, orderId);

              /* send data for auditing after policy is created*/
              CompositeFuture.all(futureList)
                  .onComplete(
                      map -> {
                        LOGGER.info("audit completed");
                      });
              return policyCreationFuture;
            });

    return future;
  }

  private Future<Void> initiateAuditing(
      User provider, String orderId, String policyId, String resourceId, JsonObject constraint) {
    /* auditing */
    JsonObject information =
        new JsonObject()
            .put("orderId", orderId)
            .put("policyId", policyId)
            .put("invoiceId", this.invoiceId)
            .put("productVariantId", this.productVariantId)
            .put("expiryInMonths", this.expiryInMonths)
            .put("consumerEmailId", this.consumerEmailId)
            .put("resourceId", resourceId)
            .put("constraint", constraint.encode())
            .put("expiryAt", this.expiryAt)
            .put("policyStatus", POLICY_STATUS);

    return auditingService.handleAuditLogs(
        provider, information, api.getPoliciesUrl(), String.valueOf(HttpMethod.POST));
  }

  public Future<JsonObject> executePostgresQuery(String query, String detail) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.debug("Query : {}", query);
    postgresService.executeQuery(
        query,
        pgHandler -> {
          if (pgHandler.succeeded()) {
            LOGGER.debug("success response : " + pgHandler.result().encodePrettily());
            LOGGER.info("Query execution successfully done : {}", detail);
            if (pgHandler.result().getJsonArray(RESULTS).size() == 1) {
              JsonObject result = pgHandler.result().getJsonArray(RESULTS).getJsonObject(0);
              promise.complete(result);
            } else {
              /* there are multiple invoices for the same order Id */
              /* OR multiple policies are created at the same time and not one after the other */
              LOGGER.error("Failure while fetching the results : {} ", detail);
              LOGGER.error("Error : {}", pgHandler.result().getJsonArray(RESULTS));
              promise.fail("Error : No payment found for the given order");
            }

          } else {
            LOGGER.error("Failure while executing query : {} ", detail);
            LOGGER.error("Error : {}", pgHandler.cause().getMessage());
            promise.fail("Error : {}" + pgHandler.cause().getMessage());
          }
        });
    return promise.future();
  }

  public Future<Boolean> executeTransaction(List<String> queries, String orderId) {
    Promise<Boolean> promise = Promise.promise();
    postgresService.executeTransaction(
        queries,
        pgHandler -> {
          if (pgHandler.succeeded()) {
            LOGGER.info("Policies inserted successfully for order : {}", orderId);
            promise.complete(true);
          } else {
            LOGGER.error("Failure while inserting policy for order : {}", orderId);
            LOGGER.error("Error : {}", pgHandler.cause().getMessage());
            promise.fail("Error : {}" + pgHandler.cause().getMessage());
          }
        });
    return promise.future();
  }
}
