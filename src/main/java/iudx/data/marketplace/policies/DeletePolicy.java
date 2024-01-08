package iudx.data.marketplace.policies;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.HttpStatusCode.BAD_REQUEST;
import static iudx.data.marketplace.common.HttpStatusCode.FORBIDDEN;
import static iudx.data.marketplace.common.ResponseUrn.FORBIDDEN_URN;
import static iudx.data.marketplace.policies.util.Constants.CHECK_IF_POLICY_PRESENT_QUERY;
import static iudx.data.marketplace.policies.util.Constants.DELETE_POLICY_QUERY;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeletePolicy {
  private static final Logger LOG = LoggerFactory.getLogger(DeletePolicy.class);
  private static final String FAILURE_MESSAGE = "Policy could not be deleted";
  private final PostgresService postgresService;
  private PgPool pool;

  public DeletePolicy(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  private String getFailureResponse(JsonObject response, String detail) {
    return response
        .put(TYPE, BAD_REQUEST.getValue())
        .put(TITLE, BAD_REQUEST.getUrn())
        .put(DETAIL, detail)
        .encode();
  }

  /**
   * Executes delete policy by setting the status field in record to DELETED from ACTIVE and by
   * checking if the policy is expired
   *
   * @param query SQL query to update the status of the policy
   * @param policyUuid policy id as type UUID
   * @return The response of the query execution
   */
  public Future<JsonObject> executeUpdateQuery(String query, UUID policyUuid) {
    LOG.debug("inside executeUpdateQuery");
    Promise<JsonObject> promise = Promise.promise();
    Tuple tuple = Tuple.of(policyUuid);
    JsonObject param = new JsonObject()
            .put("$1", policyUuid.toString());

    postgresService.executePreparedQuery(
        query,
        param,
        queryHandler -> {
          if (queryHandler.succeeded()) {
            /* policy has expired */
            if (queryHandler.result().getJsonArray(RESULT).isEmpty()) {
              promise.fail(
                  getFailureResponse(
                      new JsonObject(), FAILURE_MESSAGE + " , as policy is expired"));
            } else {
              LOG.info("update query succeeded");
              JsonObject responseJson =
                  queryHandler
                      .result()
                      .put(STATUS_CODE, HttpStatusCode.SUCCESS.getValue())
                      .put(DETAIL, "Policy deleted successfully");
              promise.complete(responseJson);
            }
          } else {
            LOG.debug("update query failed");
            LOG.error("Failure while executing the query : {}", queryHandler.cause().getMessage());
            promise.fail(
                getFailureResponse(new JsonObject(), FAILURE_MESSAGE + ", update query failed"));
          }
        });
    return promise.future();
  }


  /**
   * Queries postgres table to check if the policy given in the request is owned by the provider or
   * provider delegate, Checks if the policy that is about to be deleted is ACTIVE or DELETED Checks
   * If one of the policy id fails any of the checks, it returns false
   *
   * @param query SQL query
   * @param policyUuid list of policies of UUID
   * @return true if qualifies all the checks
   */
  public Future<Boolean> verifyPolicy(User user, String query, UUID policyUuid) {
    LOG.debug("inside verifyPolicy");
    Promise<Boolean> promise = Promise.promise();
    String ownerId = user.getUserId();
    LOG.trace("What's the ownerId : " + ownerId);
    Tuple tuple = Tuple.of(policyUuid);

    JsonObject param = new JsonObject()
            .put("$1", policyUuid.toString());
    postgresService.executePreparedQuery(
       query,
        param,
        queryHandler -> {
          if (queryHandler.succeeded()) {
            if (queryHandler.result().getJsonArray(RESULT).isEmpty()) {
              JsonObject failureResponse =
                  new JsonObject()
                      .put(TYPE, HttpStatusCode.NOT_FOUND.getValue())
                      .put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                      .put(DETAIL, FAILURE_MESSAGE + ", as it doesn't exist");
              promise.fail(failureResponse.encode());
            } else {
              JsonObject result = queryHandler.result().getJsonArray(RESULT).getJsonObject(0);
              String rsServerUrl = result.getString("resource_server_url");
              String ownerIdValue = result.getString("provider_id");
              String status = result.getString("status");
              /* does the policy belong to the owner who is requesting */
              if (!rsServerUrl.equalsIgnoreCase(user.getResourceServerUrl())) {
                LOG.error("Failure : OwnerShip error, rsServerUrl does not match");
                promise.fail(
                    new JsonObject()
                        .put(TYPE, FORBIDDEN.getValue())
                        .put(TITLE, FORBIDDEN_URN.getUrn())
                        .put(
                            DETAIL,
                            "Access Denied: You do not have ownership rights for this policy.")
                        .encode());
                // TODO replace the OwnerIdValue to ownerId
              } else if (ownerIdValue.equals(ownerIdValue)) {
                /* is policy in ACTIVE status */
                if (status.equals("ACTIVE")) {
                  LOG.info("Success : policy verified");
                  promise.complete(true);
                } else {
                  LOG.error("Failure : policy is not active");
                  promise.fail(
                      getFailureResponse(
                          new JsonObject(), FAILURE_MESSAGE + ", as policy is not ACTIVE"));
                }
              } else {
                LOG.error("Failure : policy does not belong to the user");
                JsonObject failureResponse =
                    new JsonObject()
                        .put(TYPE, FORBIDDEN.getValue())
                        .put(TITLE, FORBIDDEN_URN.getUrn())
                        .put(DETAIL, FAILURE_MESSAGE + ", as policy doesn't belong to the user");
                promise.fail(failureResponse.encode());
              }
            }
          } else {
            LOG.error("Failure while executing the query : {}", queryHandler.cause().getMessage());
            JsonObject response =
                new JsonObject()
                    .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                    .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                    .put(DETAIL, "Failure while executing query");
            LOG.error("Failed {}", queryHandler.cause().getMessage());
            promise.fail(response.encode());
          }
        });
    return promise.future();
  }

  /**
   * Acts as an entry point for count query and update query execution
   *
   * @param policy to be deleted
   * @return result of the execution as Json Object
   */
  public Future<JsonObject> initiateDeletePolicy(JsonObject policy, User user) {
    UUID policyUuid = UUID.fromString(policy.getString("id"));
    Future<Boolean> policyVerificationFuture =
        verifyPolicy(user, CHECK_IF_POLICY_PRESENT_QUERY, policyUuid);
    return policyVerificationFuture.compose(
        isVerified -> {
          if (isVerified) {
            return executeUpdateQuery(DELETE_POLICY_QUERY, policyUuid);
          }
          return Future.failedFuture(policyVerificationFuture.cause().getMessage());
        });
  }
}
