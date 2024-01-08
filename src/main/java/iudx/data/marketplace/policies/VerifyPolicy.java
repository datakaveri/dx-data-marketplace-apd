package iudx.data.marketplace.policies;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.util.Status;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static iudx.data.marketplace.common.HttpStatusCode.VERIFY_FORBIDDEN;
import static iudx.data.marketplace.policies.util.Constants.CHECK_EXISTING_POLICY;
import static iudx.data.marketplace.policies.util.Constants.CHECK_IF_POLICY_PRESENT_QUERY;

public class VerifyPolicy {
  private static final Logger LOGGER = LogManager.getLogger(VerifyPolicy.class);
  private final PostgresService postgresService;

  public VerifyPolicy(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  public Future<JsonObject> initiateVerifyPolicy(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    UUID ownerId = UUID.fromString(request.getJsonObject("owner").getString("id"));
    String userEmail = request.getJsonObject("user").getString("email");
    UUID itemId = UUID.fromString(request.getJsonObject("item").getString("itemId"));
    //        TODO: Item type resource group from the payload is removed and item type in the table
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
    Tuple selectTuples = Tuple.of(itemId, ownerId, Status.ACTIVE, userEmailId);
    JsonObject params = new JsonObject()
            .put("itemId", itemId.toString())
            .put("ownerId", ownerId.toString())
            .put("status", Status.ACTIVE)
            .put("userEmailId", userEmailId);

    Promise<JsonObject> promise = Promise.promise();
    postgresService
        .executePreparedQuery(CHECK_EXISTING_POLICY, params, handler -> {
            if(handler.failed()){
                LOGGER.error(
                        "isPolicyForIdExist fail, DB execution failed :: {}",
                        handler.cause().getMessage());
                promise.fail(
                        generateErrorResponse(
                                INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getDescription()));
            }
            else
            {
                JsonArray policy = handler.result().getJsonArray(RESULT);
                boolean isPolicyNotPresent = policy.isEmpty();
                if (isPolicyNotPresent) {
                    LOGGER.error("No matching policy");
                    promise.complete(new JsonObject());
                } else {
                    LOGGER.debug("policy exists : {} ", handler.result().encode());
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
}
