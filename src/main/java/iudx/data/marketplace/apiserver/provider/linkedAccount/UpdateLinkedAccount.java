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

public class UpdateLinkedAccount {
  private static Logger LOGGER = LogManager.getLogger(UpdateLinkedAccount.class);
  PostgresService postgresService;
  Api api;
  AuditingService auditingService;
  RazorPayService razorPayService;
  String legalBusinessName;
  String phoneNumber;
  String referenceId;
  String customerFacingBusinessName;

  public UpdateLinkedAccount(
      PostgresService postgresService, Api api, AuditingService auditingService, RazorPayService razorPayService) {
    this.postgresService = postgresService;
    this.api = api;
    this.auditingService = auditingService;
    this.razorPayService = razorPayService;
  }
  public Future<JsonObject> initiateUpdatingLinkedAccount(JsonObject request, User provider) {

    String accountRequest = getAccountRequest(request);
    // TODO: Replace this with actual provider Id
    String dummyProviderId = "29de4016-6737-4501-8fb2-81d79e2e4398";
//    String providerId = provider.getUserId();
//    TODO: Replace this with actual email id
    String dummyEmail = "01efghseller1@gmail.com";
//    String emailId = provider.getEmailId();

    Future<JsonObject> accountInfoFuture = getAccountId(GET_MERCHANT_INFO_QUERY, dummyProviderId, dummyEmail);
    Future<Boolean> updateAccountInfoInRzpFuture = accountInfoFuture.compose(accountInfoJson -> {
      String accountId = accountInfoJson.getString("accountId");
      return razorPayService.updateLinkedAccount(accountRequest,accountId);
    });
    Future<JsonObject> userResponseFuture = updateAccountInfoInRzpFuture.compose(isEditInRazorpaySuccessful -> {
      if(isEditInRazorpaySuccessful)
      {
        return updateMerchantInfo(UPDATE_MERCHANT_INFO_QUERY, dummyProviderId, dummyEmail);
      }
      return Future.failedFuture(updateAccountInfoInRzpFuture.cause().getMessage());
    });
    return userResponseFuture;
  }

  public String getAccountRequest(JsonObject requestBody)
  {
    String category = requestBody.getJsonObject("profile").getString("category");
    String subcategory = requestBody.getJsonObject("profile").getString("subcategory");
    JsonObject registered =
            requestBody.getJsonObject("profile").getJsonObject("addresses").getJsonObject("registered");
    String street1 = registered.getString("street1");
    String street2 = registered.getString("street2");
    String city = registered.getString("city");
    String state = registered.getString("state");
    String postalCode = registered.getString("postalCode");
    String country = registered.getString("country");
    String contactName = requestBody.getString("contactName");

    JsonObject registeredJson =
            new JsonObject()
                    .put("street1", street1)
                    .put("street2", street2)
                    .put("city", city)
                    .put("state", state)
                    .put("postal_code", postalCode)
                    .put("country", country);

    JsonObject addressJson = new JsonObject().put("registered", registeredJson);
    JsonObject profileJson =
            new JsonObject()
                    .put("category", category)
                    .put("subcategory", subcategory)
                    .put("addresses", addressJson);

    JsonObject legalInfoJson = new JsonObject();
    /* checks if optional field legal info is null */
    if (requestBody.getJsonObject("legalInfo") != null) {
      String pan = requestBody.getJsonObject("legalInfo").getString("pan");
      if (StringUtils.isNotBlank(pan)) {
        legalInfoJson.put("pan", pan);
      }
      String gst = requestBody.getJsonObject("legalInfo").getString("gst");
      if (StringUtils.isNotBlank(gst)) {
        legalInfoJson.put("gst", gst);
      }
    }
    String phoneNumber = requestBody.getString("phone");
    String legalBusinessName = requestBody.getString("legalBusinessName");
    String customerFacingBusinessName = requestBody.getString("customerFacingBusinessName");

    setLegalBusinessName(legalBusinessName);
    setPhoneNumber(phoneNumber);

    JsonObject details =
            new JsonObject()
                    .put("phone", phoneNumber)
                    .put("legal_business_name", legalBusinessName)
                    .put("profile", profileJson)
                    .put("legal_info", legalInfoJson);

    setCustomerFacingBusinessName(legalBusinessName);
    /* customer facing business name is not a necessary field in the request body
     * while inserting in the DB, if customer facing business name is null, it is
     * replaced with the legal business name */
    if (StringUtils.isNotBlank(customerFacingBusinessName)) {
      setCustomerFacingBusinessName(customerFacingBusinessName);
      details.put("customer_facing_business_name", customerFacingBusinessName);
    }
    if (StringUtils.isNotBlank(contactName)) {
      details.put("contact_name", contactName);
    }
    return details.encode();
  }

  public Future<JsonObject> getAccountId(String query, String providerId, String emailId)
  {
    Promise<JsonObject> promise = Promise.promise();
    String finalQuery = query.replace("$1", providerId).replace("$2", emailId);
    postgresService.executeQuery(finalQuery, handler -> {
      if(handler.succeeded())
      {
        boolean isResultEmpty = handler.result().getJsonArray(RESULTS).isEmpty();
        if(!isResultEmpty)
        {
          JsonObject result = handler.result().getJsonArray(RESULTS).getJsonObject(0);
          String accountId = result.getString("account_id");

          /* set referenceId */
          setReferenceId(result.getString("reference_id"));

          promise.complete(new JsonObject().put("accountId", accountId).put("referenceId", referenceId));
        }
        else
        {
          LOGGER.fatal("Linked account is not created or there was a problem while inserting the record in postgres");
          promise.fail(
                  new RespBuilder()
                          .withType(HttpStatusCode.NOT_FOUND.getValue())
                          .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                          .withDetail("Linked account cannot be updated as, it is not found")
                          .getResponse());
        }
      }
      else {
        LOGGER.error("Failed to fetch account id from postgres : {}", handler.cause().getMessage());
        promise.fail(new RespBuilder()
                .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                .withTitle(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn())
                .withDetail("Linked account could not be updated : Internal Server error")
                .getResponse());
      }
    });

    return promise.future();
  }

  public Future<JsonObject> updateMerchantInfo(String query, String providerId, String email) {
    Promise<JsonObject> promise = Promise.promise();
    String finalQuery =
        query
            .replace("$1", getPhoneNumber())
            .replace("$2", getLegalBusinessName())
            .replace("$3", getCustomerFacingBusinessName())
            .replace("$4", providerId)
            .replace("$5", email);

    postgresService.executeQuery(
        finalQuery,
        handler -> {
          if (handler.succeeded()) {
            boolean isResultEmpty = handler.result().getJsonArray(RESULTS).isEmpty();
            if (!isResultEmpty) {
              JsonObject result = handler.result().getJsonArray(RESULTS).getJsonObject(0);
              String accountId = result.getString("account_id");
              LOGGER.info(
                  "Account information of provider with providerId : {}, with reference_id : {}, "
                      + "with account_id : {} updated successfully",
                  providerId,
                  getReferenceId(),
                  accountId);

              JsonObject response =
                  new RespBuilder()
                      .withType(ResponseUrn.SUCCESS_URN.getUrn())
                      .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                      .withDetail("Linked account updated successfully")
                      .getJsonResponse();

              promise.complete(response);
            } else {
              LOGGER.fatal(
                  "Linked account cannot be updated as it"
                      + " is not created or there was a problem while inserting the record in postgres");
              promise.fail(
                  new RespBuilder()
                      .withType(HttpStatusCode.NOT_FOUND.getValue())
                      .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                      .withDetail("Linked account cannot be updated as, it is not found")
                      .getResponse());
            }
          } else {
            LOGGER.error(
                "Failed to update merchant info in postgres : {}", handler.cause().getMessage());
            promise.fail(
                new RespBuilder()
                    .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                    .withTitle(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn())
                    .withDetail("Linked account could not be updated : Internal Server error")
                    .getResponse());
          }
        });

    return promise.future();
  }

  public String getLegalBusinessName() {
    return legalBusinessName;
  }

  public void setLegalBusinessName(String legalBusinessName) {
    this.legalBusinessName = legalBusinessName;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  public String getCustomerFacingBusinessName() {
    return customerFacingBusinessName;
  }

  public void setCustomerFacingBusinessName(String customerFacingBusinessName) {
    this.customerFacingBusinessName = customerFacingBusinessName;
  }


}
