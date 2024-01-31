package iudx.data.marketplace.apiserver.provider.linkedAccount;


import com.google.common.hash.Hashing;
import com.razorpay.Account;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
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
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static iudx.data.marketplace.apiserver.provider.linkedAccount.util.Constants.INSERT_MERCHANT_INFO;
import static iudx.data.marketplace.apiserver.util.Constants.*;

public class CreateLinkedAccount {
  public static final String ACCOUNT_TYPE = "route";
  private static final String FAILURE_MESSAGE = "User registration incomplete : ";
  private static final Logger LOGGER = LogManager.getLogger(CreateLinkedAccount.class);
  PostgresService postgresService;
  Api api;
  AuditingService auditingService;
  RazorpayClient razorpayClient;
  private String legalBusinessName;
  private String customerFacingBusinessName;
  private String phoneNumber;
  private String emailId;
  private String accountId;
  private String providerId;
  private String status;
  private CreateLinkedAccount(CreateLinkedAccountBuilder builder) {
    this.postgresService = builder.postgresService;
    this.api = builder.api;
    this.auditingService = builder.auditingService;
    this.razorpayClient = builder.razorpayClient;
  }


  public Future<JsonObject> initiateCreatingLinkedAccount(JsonObject request, User provider) {

    String referenceId = createReferenceId();
//    TODO: Change emailId
    String emailId = request.getString("email");
    JSONObject merchantDetails = getLinkedAccountDetails(request, referenceId, emailId);
    setProviderId(provider.getUserId());
    JsonObject response = null;
    try {
//      TODO: Remove reinitialisation
      razorpayClient =
          new RazorpayClient("rzp_test_kbgO0jNB4Q4loL", "Qi7uOrDcXF5Ll6PRwiWCEDWx", true);

      Account account = razorpayClient.account.create(merchantDetails);
      response = new JsonObject(account.toString());
      String accountId = response.getString(ID);

      LOGGER.info("Linked account created with accountId : {}", accountId);
      setAccountId(accountId);
      return insertInfoInDB(INSERT_MERCHANT_INFO, referenceId);

    } catch (RazorpayException e) {
      LOGGER.error("Razorpay error message: {}", e.getMessage());
      String errorMessage = errorHandler(e.getMessage());
      return Future.failedFuture(errorMessage);
    }
  }

  public String errorHandler(String rzpFailureMessage)
  {
    Map<String, String> errorMap = new HashMap<>();
    errorMap.put("Merchant email already exists for account", FAILURE_MESSAGE +"merchant email already exists for account");
    errorMap.put("The phone format is invalid", FAILURE_MESSAGE + "phone format is invalid");
    errorMap.put("The contact name may only contain alphabets and spaces", FAILURE_MESSAGE + "name is invalid");
    errorMap.put("Invalid business subcategory for business category", FAILURE_MESSAGE+ "subcategory or category is invalid");
    errorMap.put("The street2 field is required", FAILURE_MESSAGE+ "street2 field is required");
    errorMap.put("The street1 field is required", FAILURE_MESSAGE+ "street1 field is required");
    errorMap.put("The city field is required", FAILURE_MESSAGE+ "city field is required");
    errorMap.put("The business registered city may only contain alphabets, digits and spaces", FAILURE_MESSAGE + "city name is invalid");
    errorMap.put("State name entered is incorrect. Please provide correct state name", FAILURE_MESSAGE+ "state name is invalid");
    errorMap.put("The postal code must be an integer", FAILURE_MESSAGE + "postal code is invalid");
    errorMap.put("The business registered country may only contain alphabets and spaces", FAILURE_MESSAGE + "country name is invalid");
    errorMap.put("The pan field is invalid", FAILURE_MESSAGE+"pan field is invalid");
    errorMap.put("The gst field is invalid", FAILURE_MESSAGE+ "gst field is invalid");
    errorMap.put("Route code Support feature not enabled to add account code", FAILURE_MESSAGE + "route code support feature not enabled to add account code");
    errorMap.put("The api key/secret provided is invalid", FAILURE_MESSAGE + ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage());
//    errorMap.put("Invalid type: route", FAILURE_MESSAGE  + ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage());
//    errorMap.put("The code format is invalid", FAILURE_MESSAGE  + ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage());
//    errorMap.put("The code must be at least 3 characters",FAILURE_MESSAGE  + ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage());


    String failureMessage  = new RespBuilder()
            .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
            .withTitle(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn())
            .withDetail(FAILURE_MESSAGE  + ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
            .getResponse();;
    for (var error : errorMap.entrySet()) {
      boolean isErrorPresent = StringUtils.containsIgnoreCase(rzpFailureMessage, error.getKey());
      if (isErrorPresent) {
        failureMessage =
            new RespBuilder()
                .withType(HttpStatusCode.BAD_REQUEST.getValue())
                .withTitle(ResponseUrn.BAD_REQUEST_URN.getUrn())
                .withDetail(error.getValue())
                .getResponse();
      }
    }
    return failureMessage;
  }
  /**
   * Creates a request body to create linked account through Razorpay based on the request
   * payload <br>
   * Options fields in the request payload are :
   *  <br> customerFacingBusinessName
   *  <br> legalInfo JSON
   *  <br> GST
   *  <br> PAN
   *
   * @param request Request payload of type JsonObject
   * @param referenceId random Id generated to act as internal reference
   * @param emailId provider email Id
   * @return request body of type JSONObject with required fields
   */
  public JSONObject getLinkedAccountDetails(
      JsonObject request, String referenceId, String emailId) {
    String businessType = request.getString("businessType");
    String category = request.getJsonObject("profile").getString("category");
    String subcategory = request.getJsonObject("profile").getString("subcategory");
    JsonObject registered =
        request.getJsonObject("profile").getJsonObject("addresses").getJsonObject("registered");
    String street1 = registered.getString("street1");
    String street2 = registered.getString("street2");
    String city = registered.getString("city");
    String state = registered.getString("state");
    String postalCode = registered.getString("postalCode");
    String country = registered.getString("country");
    String contactName = request.getString("contactName");

    JSONObject registeredJson =
        new JSONObject()
            .put("street1", street1)
            .put("street2", street2)
            .put("city", city)
            .put("state", state)
            .put("postal_code", postalCode)
            .put("country", country);

    JSONObject addressJson = new JSONObject().put("registered", registeredJson);
    JSONObject profileJson =
        new JSONObject()
            .put("category", category)
            .put("subcategory", subcategory)
            .put("addresses", addressJson);

    JSONObject legalInfoJson = new JSONObject();
    /* checks if optional field legal info is null */
    if (request.getJsonObject("legalInfo") != null) {
      String pan = request.getJsonObject("legalInfo").getString("pan");
      if (!StringUtils.isBlank(pan)) {
        legalInfoJson.put("pan", pan);
      }
      String gst = request.getJsonObject("legalInfo").getString("gst");
      if (!StringUtils.isBlank(gst)) {
        legalInfoJson.put("gst", gst);
      }
    }
    String phoneNumber = request.getString("phone");
    String legalBusinessName = request.getString("legalBusinessName");
    String customerFacingBusinessName = request.getString("customerFacingBusinessName");

    setLegalBusinessName(legalBusinessName);
    setEmailId(emailId);
    setPhoneNumber(phoneNumber);

    JSONObject details =
        new JSONObject()
            .put("email", emailId)
            .put("phone", phoneNumber)
            .put("legal_business_name", legalBusinessName)
            .put("type", ACCOUNT_TYPE)
            .put("reference_id", referenceId)
            .put("business_type", businessType)
            .put("profile", profileJson)
            .put("legal_info", legalInfoJson);

    setCustomerFacingBusinessName(legalBusinessName);
    /* customer facing business name is not a necessary field in the request body
    * while inserting in the DB, if customer facing business name is null, it is
    * replaced with the legal business name */
    if (!StringUtils.isBlank(customerFacingBusinessName)) {
      setCustomerFacingBusinessName(customerFacingBusinessName);
      details.put("customer_facing_business_name", customerFacingBusinessName);
    }
    if (!StringUtils.isBlank(contactName)) {
      details.put("contact_name", contactName);
    }
    return details;
  }

  /**
   * Inserts a record in DB with required merchant details like referenceId, phoneNumber,
   * provider email id, business name, Razorpay generated account id, account status
   * @param query Insert query of type string
   * @param referenceId Acts as primary key to store record
   * @return Success or failure response of type Future Json object
   */
  Future<JsonObject> insertInfoInDB(String query, String referenceId) {
    Promise<JsonObject> promise = Promise.promise();
    String finalQuery =
        query
            .replace("$1", referenceId)
            .replace("$2", getPhoneNumber())
            .replace("$3", getEmailId())
            .replace("$4", getLegalBusinessName())
            .replace("$5", getCustomerFacingBusinessName())
            .replace("$6", getAccountId())
            .replace("$7", getProviderId())
            .replace("$8", "CREATED");

    postgresService.executeQuery(
        finalQuery,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info(
                "Linked account info with referenceId : {} , inserted successfully", referenceId);
            JsonObject response =
                new JsonObject()
                    .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                    .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                    .put(DETAIL, "Linked account created successfully");
            promise.complete(response);
          } else {
            LOGGER.info("Failed to insert linked account info : " + handler.cause().getMessage());
            promise.fail(
                new RespBuilder()
                    .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                    .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                    .withDetail(FAILURE_MESSAGE + "Internal Server Error")
                    .getResponse());
          }
        });
    return promise.future();
  }

  /**
   * Creates a referenceId of length = 20 based on present time as seed. This referenceId could be sent to Razorpay
   * while creating linked account and it can also act as an internal reference to store relevant
   * merchant information in the DB
   * @return referenceId as string which to act as primary key to store record in DB
   */
  private String createReferenceId() {
    String referenceId =
        Hashing.sha256()
            .hashString(LocalDateTime.now().toString(), StandardCharsets.UTF_8)
            .toString()
            .substring(0, 20);
    return referenceId;
  }

  public String getLegalBusinessName() {
    return legalBusinessName;
  }

  public void setLegalBusinessName(String legalBusinessName) {
    this.legalBusinessName = legalBusinessName;
  }

  public String getCustomerFacingBusinessName() {
    return customerFacingBusinessName;
  }

  public void setCustomerFacingBusinessName(String customerFacingBusinessName) {
    this.customerFacingBusinessName = customerFacingBusinessName;
  }

  public String getPhoneNumber() {
    return this.phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getEmailId() {
    return this.emailId;
  }

  public void setEmailId(String emailId) {
    this.emailId = emailId;
  }

  public String getAccountId() {
    return this.accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getProviderId() {
    return this.providerId;
  }

  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  public String getStatus() {
    return this.status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public static class CreateLinkedAccountBuilder {
    private final PostgresService postgresService;
    private final Api api;
    private final AuditingService auditingService;
    private RazorpayClient razorpayClient;

    public CreateLinkedAccountBuilder(
        PostgresService postgresService, Api api, AuditingService auditingService) {
      this.postgresService = postgresService;
      this.api = api;
      this.auditingService = auditingService;
    }


    public CreateLinkedAccountBuilder setRazorpayClient(RazorpayClient razorpayClient)
    {
      this.razorpayClient = razorpayClient;
      return this;
    }


    public CreateLinkedAccount build() {
      return new CreateLinkedAccount(this);
    }
  }
}
