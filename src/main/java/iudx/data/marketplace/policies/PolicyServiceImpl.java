package iudx.data.marketplace.policies;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.consentAgreementGenerator.controller.PolicyDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyServiceImpl implements PolicyService {
  private static final Logger LOG = LoggerFactory.getLogger(PolicyServiceImpl.class);
  private final DeletePolicy deletePolicy;
  private final GetPolicyDetails getPolicyDetails;
  private final CreatePolicy createPolicy;
  private final VerifyPolicy verifyPolicy;
  private final FetchPolicyUsingPvId fetchPolicy;
  private final FetchPolicyDetailsWithPolicyId fetchPolicyDetailsWithPolicyId;
  JsonObject config;

  public PolicyServiceImpl(
      DeletePolicy deletePolicy,
      CreatePolicy createPolicy,
      GetPolicyDetails getPolicyDetails,
      VerifyPolicy verifyPolicy,
      FetchPolicyUsingPvId fetchPolicyUsingPvId,
      FetchPolicyDetailsWithPolicyId fetchPolicyDetailsWithPolicyId) {
    this.deletePolicy = deletePolicy;
    this.createPolicy = createPolicy;
    this.getPolicyDetails = getPolicyDetails;
    this.verifyPolicy = verifyPolicy;
    this.fetchPolicy = fetchPolicyUsingPvId;
    this.fetchPolicyDetailsWithPolicyId = fetchPolicyDetailsWithPolicyId;
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
    this.getPolicyDetails
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

  @Override
  public Future<PolicyDetails> fetchPolicyWithPolicyId(User user, String policyId) {
    Promise<PolicyDetails> promise = Promise.promise();

    this.fetchPolicyDetailsWithPolicyId
        .getPolicyDetails(user, policyId)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete(handler.result());
              } else {
                LOG.error("Failed to fetch policy with the given policy ID");
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }
}
