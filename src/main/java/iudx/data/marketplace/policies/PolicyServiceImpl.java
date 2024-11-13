package iudx.data.marketplace.policies;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyServiceImpl implements PolicyService {
  private static final Logger LOG = LoggerFactory.getLogger(PolicyServiceImpl.class);
  private final DeletePolicy deletePolicy;
  private final GetPolicy getPolicy;
  private final CreatePolicy createPolicy;
  private final VerifyPolicy verifyPolicy;
  private final FetchPolicyUsingPvId fetchPolicy;

  JsonObject config;

  public PolicyServiceImpl(
      DeletePolicy deletePolicy,
      CreatePolicy createPolicy,
      GetPolicy getPolicy,
      VerifyPolicy verifyPolicy,
      FetchPolicyUsingPvId fetchPolicyUsingPvId) {
    this.deletePolicy = deletePolicy;
    this.createPolicy = createPolicy;
    this.getPolicy = getPolicy;
    this.verifyPolicy = verifyPolicy;
    this.fetchPolicy = fetchPolicyUsingPvId;
  }

  @Override
  public Future<JsonObject> createPolicy(String orderId) {
    Promise<JsonObject> promise = Promise.promise();
    createPolicy
        .createPolicy(orderId)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete(new JsonObject().put("Key", "Policy created successfully"));
              } else {
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> deletePolicy(JsonObject policy, User user) {
    Promise<JsonObject> promise = Promise.promise();
    this.deletePolicy
        .initiateDeletePolicy(policy, user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOG.info("Successfully deleted the policy");
                promise.complete(handler.result());
              } else {
                LOG.error("Failed to delete the policy");
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getPolicies(User user) {
    Promise<JsonObject> promise = Promise.promise();
    this.getPolicy
        .initiateGetPolicy(user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOG.info("GET policy successful");
                promise.complete(handler.result());
              } else {
                LOG.error("Failed to execute GET policy");
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> verifyPolicy(JsonObject jsonObject) {
    Promise<JsonObject> promise = Promise.promise();

    this.verifyPolicy
        .initiateVerifyPolicy(jsonObject)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete(handler.result());
              } else {
                LOG.error("Failed to verify policy");
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> checkPolicy(String productVariantId, User user) {
    Promise<JsonObject> promise = Promise.promise();

    this.fetchPolicy
        .checkIfPolicyExists(productVariantId, user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete(handler.result());
              } else {
                LOG.error("Failed to verify policy");
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }
}
