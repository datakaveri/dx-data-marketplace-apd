package iudx.data.marketplace.policies;

import static iudx.data.marketplace.policies.util.Constants.GET_POLICY_WITH_POLICY_ID_QUERY;
import static iudx.data.marketplace.product.util.Constants.RESULTS;
import static iudx.data.marketplace.product.util.Constants.TYPE;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.consentAgreementGenerator.controller.PolicyDetails;
import iudx.data.marketplace.consentAgreementGenerator.util.Assets;
import iudx.data.marketplace.postgresql.PostgresqlService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FetchPolicyDetailsWithPolicyId {
  private static final Logger LOGGER = LogManager.getLogger(FetchPolicyDetailsWithPolicyId.class);
  private final PostgresqlService postgresService;
  private final Assets assets;

  public FetchPolicyDetailsWithPolicyId(PostgresqlService postgresService, Assets assets) {
    this.postgresService = postgresService;
    this.assets = assets;
  }

  public Future<PolicyDetails> getPolicyDetails(User user, String policyId) {
    LOGGER.debug("check if policyId is present in the DB ");
    Future<PolicyDetails> future =
        postgresService
            .executePreparedQuery(
                GET_POLICY_WITH_POLICY_ID_QUERY, new JsonObject().put("policyId", policyId))
            .compose(
                dbResult -> {
                  boolean isPolicyNotPresent = dbResult.getJsonArray(RESULTS).isEmpty();
                  if (isPolicyNotPresent) {
                    LOGGER.debug("Policy not found");
                    return Future.failedFuture(
                        new RespBuilder()
                            .withType(HttpStatusCode.NOT_FOUND.getValue())
                            .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                            .withDetail("Policy not found")
                            .getResponse());
                  }
                  // policy is present
                  JsonObject result = dbResult.getJsonArray(RESULTS).getJsonObject(0);
                  // check if the policy belongs to the consumer or provider
                  String consumerEmailId = result.getString("consumer_email_id");
                  String providerId = result.getString("provider_id");
                  boolean isPolicyForConsumer =
                      user.getUserRole().equals(Role.CONSUMER)
                          && consumerEmailId.equals(user.getEmailId());
                  boolean isPolicyBelongingToProvider =
                      user.getUserRole().equals(Role.PROVIDER)
                          && providerId.equals(user.getUserId());
                  if (isPolicyForConsumer || isPolicyBelongingToProvider) {
                    // check if the resource server url of user and resource is the same
                    boolean isRsUrlEqual =
                        result
                            .getString("resource_server")
                            .equalsIgnoreCase(user.getResourceServerUrl());
                    if (isRsUrlEqual) {
                      // Rate limit
                      // get the htmlstring
                      // generate the pdf
                      // audit data
                      JsonObject policyDetailsJson =
                          new JsonObject()
                              .put("policyId", policyId)
                              .put("policyStatus", result.getString("status"))
                              .put("consumerEmailId", consumerEmailId)
                              .put("assets", this.assets)
                              .put("resourceId", result.getString("resource_id"))
                              .put("invoiceId", result.getString("invoice_id"))
                              .put("constraints", result.getJsonObject("constraints"))
                              .put("providerId", providerId)
                              .put("productVariantId", result.getString("product_variant_id"))
                              .put("createdAt", result.getString("created_at"))
                              .put(
                                  "isPolicyExpired",
                                  result.getString("is_policy_expired").equalsIgnoreCase("true"))
                              .put("resourceServerUrl", result.getString("resource_server"))
                              .put("expiryAt", result.getString("expiry_at"));

                      PolicyDetails policyDetails = new PolicyDetails(policyDetailsJson);
                      LOGGER.info("policy info : {}", policyDetails);

                      return Future.succeededFuture(policyDetails);

                    } else {
                      return Future.failedFuture(
                          new RespBuilder()
                              .withType(HttpStatusCode.FORBIDDEN.getValue())
                              .withTitle(ResponseUrn.FORBIDDEN_URN.getUrn())
                              .withDetail("consent agreement is forbidden to access")
                              .getResponse());
                    }
                  } else {
                    return Future.failedFuture(
                        new RespBuilder()
                            .withType(HttpStatusCode.FORBIDDEN.getValue())
                            .withTitle(ResponseUrn.FORBIDDEN_URN.getUrn())
                            .withDetail(
                                "Fetching the consent agreement is forbidden as it does not belong to the user")
                            .getResponse());
                  }
                });
    if (future.failed() && !future.cause().getMessage().contains(TYPE)) {
      future =
          Future.failedFuture(
              new RespBuilder()
                  .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                  .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                  .withDetail(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                  .getResponse());
    }
    return future;
  }
}
