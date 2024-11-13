package iudx.data.marketplace.policies;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static iudx.data.marketplace.common.HttpStatusCode.VERIFY_FORBIDDEN;
import static iudx.data.marketplace.policies.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.util.Status;
import iudx.data.marketplace.postgres.PostgresService;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VerifyPolicy {
  private static final Logger LOGGER = LogManager.getLogger(VerifyPolicy.class);
  private final PostgresService postgresService;
  private String orderId;

  public VerifyPolicy(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  public Future<JsonObject> initiateVerifyPolicy(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    UUID ownerId = UUID.fromString(request.getJsonObject("owner").getString("id"));
    String userEmail = request.getJsonObject("user").getString("email");
    UUID itemId = UUID.fromString(request.getJsonObject("item").getString("itemId"));

    /*check if the orderId is present in the context object*/
    if (request.containsKey("context")
        && StringUtils.isNotBlank(request.getJsonObject("context").getString("orderId"))) {
      String orderId = request.getJsonObject("context").getString("orderId");
      setOrderId(orderId);
    }

    Future<JsonObject> checkForExistingPolicy =
        checkExistingPoliciesForId(itemId, ownerId, userEmail);

    Future<JsonObject> getPolicyDetail =
        checkForExistingPolicy.compose(
            isPolicyExist -> {
              if (isPolicyExist.containsKey("id")) {
                return Future.succeededFuture(isPolicyExist);
              } else {
                return Future.failedFuture(
                    generateErrorResponse(
                        VERIFY_FORBIDDEN, "No policy exists for the given resource"));
              }
            });

    getPolicyDetail
        .onSuccess(
            successHandler -> {
              JsonObject responseJson =
                  new JsonObject()
                      .put(TYPE, ResponseUrn.VERIFY_SUCCESS_URN.getUrn())
                      .put("apdConstraints", successHandler.getJsonObject("constraints"));
              promise.complete(responseJson);
            })
        .onFailure(promise::fail);
    return promise.future();
  }

  private Future<JsonObject> checkExistingPoliciesForId(
      UUID itemId, UUID ownerId, String userEmailId) {
//    Tuple selectTuples = Tuple.of(itemId, ownerId, Status.ACTIVE, userEmailId);
    String query;

    JsonObject params =
        new JsonObject()
            .put("itemId", itemId.toString())
            .put("ownerId", ownerId.toString())
            .put("status", Status.ACTIVE)
            .put("userEmailId", userEmailId);

    if (StringUtils.isNotBlank(getOrderId())) {
      query = CHECK_POLICY_FROM_ORDER_ID;
      params.put("orderId", getOrderId());
    } else {
      query = CHECK_EXISTING_POLICY;
    }

    Promise<JsonObject> promise = Promise.promise();
    postgresService.executePreparedQuery(
        query,
        params,
        handler -> {
          if (handler.failed()) {
            LOGGER.error(
                "isPolicyForIdExist fail, DB execution failed :: {}", handler.cause().getMessage());
            promise.fail(
                generateErrorResponse(
                    INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getDescription()));
          } else {
            JsonArray policy = handler.result().getJsonArray(RESULT);
            boolean isPolicyNotPresent = policy.isEmpty();
            if (isPolicyNotPresent) {
              LOGGER.error("No matching policy");
              promise.complete(new JsonObject());
            } else {
              LOGGER.debug("policy exists : {} ", handler.result().encode());
              if (handler.result().getJsonArray(RESULT).size() > 1
                  && query.equals(CHECK_POLICY_FROM_ORDER_ID)) {
                LOGGER.fatal(
                    "Fetched more than 1 policy for a single"
                        + " resource with a single orderId : {}",
                    orderId);
                promise.fail(
                    generateErrorResponse(
                        INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getDescription()));
                return;
              }
              JsonObject result = handler.result().getJsonArray(RESULT).getJsonObject(0);
              JsonObject constraints = result.getJsonObject("constraints");
              String policyId = result.getString("_id");
              JsonObject response =
                  new JsonObject().put("constraints", constraints).put("id", policyId);
              promise.complete(response);
            }
          }
        });

    return promise.future();
  }

  public String generateErrorResponse(HttpStatusCode httpStatusCode, String errorMessage) {
    return new JsonObject()
        .put(TYPE, httpStatusCode.getValue())
        .put(TITLE, httpStatusCode.getUrn())
        .put(DETAIL, errorMessage)
        .encode();
  }

  private String getOrderId() {
    return this.orderId;
  }

  private void setOrderId(String orderId) {
    this.orderId = orderId;
  }
}
