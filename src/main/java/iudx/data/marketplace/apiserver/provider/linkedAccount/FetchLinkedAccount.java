package iudx.data.marketplace.apiserver.provider.linkedAccount;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.razorpay.RazorPayService;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.apiserver.provider.linkedAccount.util.Constants.*;
import static iudx.data.marketplace.apiserver.util.Constants.RESULTS;

public class FetchLinkedAccount {
  private static final Logger LOGGER = LogManager.getLogger(FetchLinkedAccount.class);
  PostgresService postgresService;
  Api api;
  RazorPayService razorPayService;
  AuditingService auditingService;

  public FetchLinkedAccount(
      PostgresService postgresService,
      Api api,
      RazorPayService razorPayService,
      AuditingService auditingService) {
    this.postgresService = postgresService;
    this.api = api;
    this.razorPayService = razorPayService;
    this.auditingService = auditingService;
  }

  public Future<JsonObject> initiateFetchingLinkedAccount(User provider) {
    String providerId = provider.getUserId();
    /*get accountId associated with the given provider ID*/
    Future<JsonObject> getAccountFuture = getAccountId(GET_ACCOUNT_ID_QUERY, providerId);
    Future<JsonObject> accountDetailsFromRzpFuture =
        getAccountFuture.compose(
            accountFutureJson -> {
              String accountId = accountFutureJson.getString("accountId");
              return razorPayService.fetchLinkedAccount(accountId);
            });
    Future<JsonObject> successResponse =
        accountDetailsFromRzpFuture.compose(
            response -> {
              return generateSuccessResponse(response);
            });

    return successResponse;
  }

  public Future<JsonObject> generateSuccessResponse(JsonObject rzpResponseJson) {
    String emailId = rzpResponseJson.getString("email");
    String accountId = rzpResponseJson.getString("id");
    String type = rzpResponseJson.getString("type");
    String status = rzpResponseJson.getString("status");
    String referenceId = rzpResponseJson.getString("reference_id");
    String businessType = rzpResponseJson.getString("business_type");
    String category = rzpResponseJson.getJsonObject("profile").getString("category");
    String subcategory = rzpResponseJson.getJsonObject("profile").getString("subcategory");
    JsonObject registered =
        rzpResponseJson
            .getJsonObject("profile")
            .getJsonObject("addresses")
            .getJsonObject("registered");
    String street1 = registered.getString("street1");
    String street2 = registered.getString("street2");
    String city = registered.getString("city");
    String state = registered.getString("state");
    String postalCode = registered.getString("postal_code");
    String country = registered.getString("country");
    String contactName = rzpResponseJson.getString("contact_name");

    JsonObject registeredJson =
        new JsonObject()
            .put("street1", street1)
            .put("street2", street2)
            .put("city", city)
            .put("state", state)
            .put("postalCode", postalCode)
            .put("country", country);

    JsonObject addressJson = new JsonObject().put("registered", registeredJson);
    JsonObject profileJson =
        new JsonObject()
            .put("category", category)
            .put("subcategory", subcategory)
            .put("addresses", addressJson);

    JsonObject legalInfoJson = new JsonObject();
    /* checks if optional fields are null */
    if (rzpResponseJson.getJsonObject("legal_info") != null) {
      String pan = rzpResponseJson.getJsonObject("legal_info").getString("pan");
      if (StringUtils.isNotBlank(pan)) {
        legalInfoJson.put("pan", pan);
      }
      String gst = rzpResponseJson.getJsonObject("legal_info").getString("gst");
      if (StringUtils.isNotBlank(gst)) {
        legalInfoJson.put("gst", gst);
      }
    }
    String phoneNumber = rzpResponseJson.getString("phone");
    String legalBusinessName = rzpResponseJson.getString("legal_business_name");
    String customerFacingBusinessName = rzpResponseJson.getString("customer_facing_business_name");

    JsonObject details =
        new JsonObject()
            .put("id", accountId)
            .put("type", type)
            .put("status", status)
            .put("email", emailId)
            .put("profile", profileJson)
            .put("phone", phoneNumber);
    if (StringUtils.isNotBlank(contactName)) {
      details.put("contactName", contactName);
    }
    details.put("referenceId", referenceId);
    details.put("businessType", businessType);
    details.put("legalBusinessName", legalBusinessName);
    if (StringUtils.isNotBlank(customerFacingBusinessName)) {
      details.put("customerFacingBusinessName", customerFacingBusinessName);
    }
    details.put("legalInfo", legalInfoJson);
    JsonObject response =
        new RespBuilder()
            .withType(ResponseUrn.SUCCESS_URN.getUrn())
            .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
            .withResult(details)
            .getJsonResponse();

    return Future.succeededFuture(response);
  }

  Future<JsonObject> getAccountId(String query, String providerId) {
    Promise<JsonObject> promise = Promise.promise();
    String finalQuery = query.replace("$1", providerId);
    LOGGER.debug("Final query : " + finalQuery);
    postgresService.executeQuery(
        finalQuery,
        handler -> {
          if (handler.succeeded()) {
            if (!handler.result().getJsonArray(RESULTS).isEmpty()) {
              JsonObject result = handler.result().getJsonArray(RESULTS).getJsonObject(0);
              String accountId = result.getString("account_id");
              promise.complete(new JsonObject().put("accountId", accountId));
            } else {
              promise.fail(
                  new RespBuilder()
                      .withType(HttpStatusCode.NOT_FOUND.getValue())
                      .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                      .withDetail("Linked account cannot be fetched as, it is not found")
                      .getResponse());
            }

          } else {
            LOGGER.error("Failure : {}", handler.cause().getMessage());
            promise.fail(
                new RespBuilder()
                    .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                    .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                    .withDetail("Linked account cannot be fetched : Internal Server Error")
                    .getResponse());
          }
        });
    return promise.future();
  }
}
