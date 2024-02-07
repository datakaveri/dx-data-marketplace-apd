package iudx.data.marketplace.apiserver.provider.linkedAccount;


import com.google.common.hash.Hashing;
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

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

import static iudx.data.marketplace.apiserver.provider.linkedAccount.util.Constants.*;
import static iudx.data.marketplace.apiserver.util.Constants.*;

public class CreateLinkedAccount {
  private static final Logger LOGGER = LogManager.getLogger(CreateLinkedAccount.class);
  private PostgresService postgresService;
  private Api api;
  private AuditingService auditingService;
  private String legalBusinessName;
  private String customerFacingBusinessName;
  private String phoneNumber;
  private String emailId;
  private String accountId;
  private String providerId;
  private String status;
  private String accountProductId;
  private RazorPayService razorPayService;

  public CreateLinkedAccount(
      PostgresService postgresService,
      Api api,
      AuditingService auditingService,
      RazorPayService razorPayService) {
    this.postgresService = postgresService;
    this.api = api;
    this.auditingService = auditingService;
    this.razorPayService = razorPayService;
  }

  /**
   * Initiates provider registration flow
   * @param request Information used to create linked account present in payload
   * @param provider User object
   * @return Successful or failure response of type Future JsonObject
   */

  public Future<JsonObject> initiateCreatingLinkedAccount(JsonObject request, User provider) {
    String referenceId = createReferenceId();
    //    TODO: Change emailId
    String emailId = request.getString("email");
    JsonObject merchantDetails = getLinkedAccountDetails(request, referenceId, emailId);
    //    TODO: Get this provider Id from token and set it
    String dummyProviderId = getRandomUuid();
    setProviderId(dummyProviderId);
    //    setProviderId(provider.getUserId());

    Future<Boolean> insertProviderFuture = insertDummyRowInUserTable(emailId);

    var razorpayFlowFuture =
        insertProviderFuture.compose(
            providerInsertedSuccessfully -> {
              if (providerInsertedSuccessfully) {
                return createLinkedAccount(merchantDetails);
              } else {
                return Future.failedFuture(insertProviderFuture.cause().getMessage());
              }
            });
    Future<JsonObject> userResponse =
        razorpayFlowFuture.compose(
            isRazorpayFlowSuccessful -> {
              if (isRazorpayFlowSuccessful) {
                /* insert in DB */
                return insertInfoInDB(INSERT_MERCHANT_INFO_QUERY, referenceId);
              } else {
                return Future.failedFuture(razorpayFlowFuture.cause().getMessage());
              }
            });

    return userResponse;
  }

  /**
   * Initiates creating linked account and accepting terms and conditions
   * Fetches and sets accountId, accountProductId after creating linked account and after accepting TnC respectively
   *
   * @param merchantDetails of type JsonObject to required to create linked account through Razorpay
   * @return true if the flow is successful, respective failure response if any
   * of type Future
   */
  private Future<Boolean> createLinkedAccount(JsonObject merchantDetails) {

    Future<JsonObject> linkedAccountCreationFuture = razorPayService.createLinkedAccount(merchantDetails.toString());
    Future<JsonObject> productConfigurationFuture = linkedAccountCreationFuture.compose(linkedAccountJson -> {
      String accountId = linkedAccountJson.getString("accountId");
      setAccountId(accountId);
      return razorPayService.requestProductConfiguration(linkedAccountJson);
    });
    Future<Boolean> requestProductConfigurationFuture =
            productConfigurationFuture.compose(productConfigJson -> {
    String accountProductId = productConfigJson.getString("razorpayAccountProductId");
    setAccountProductId(accountProductId);
    return Future.succeededFuture(true);
    });
    return requestProductConfigurationFuture;
  }

  // TODO: Insert dummy record in user_table with email given request body + dummy providerId +
  // Dummy (Fname) + testProvider123 (Lname)
  //  TODO: This is for testing, delete this
  public Future<Boolean> insertDummyRowInUserTable(String emailId) {
    Promise<Boolean> promise = Promise.promise();
    String query =
        "INSERT INTO public.user_table"
            + " (_id, email_id, first_name, last_name)"
            + " VALUES ('$1', '$2', 'Dummy', 'testProvider123');";

    String finalQuery = query.replace("$1", getProviderId()).replace("$2", getEmailId());
    postgresService.executeQuery(
        finalQuery,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Provider with _id : {} , inserted successfully", getProviderId());
            promise.complete(true);
          } else {
            LOGGER.info("Failed to insert Provider in user table : " + handler.cause().getMessage());
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
  public JsonObject getLinkedAccountDetails(
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
    if (request.getJsonObject("legalInfo") != null) {
      String pan = request.getJsonObject("legalInfo").getString("pan");
      if (StringUtils.isNotBlank(pan)) {
        legalInfoJson.put("pan", pan);
      }
      String gst = request.getJsonObject("legalInfo").getString("gst");
      if (StringUtils.isNotBlank(gst)) {
        legalInfoJson.put("gst", gst);
      }
    }
    String phoneNumber = request.getString("phone");
    String legalBusinessName = request.getString("legalBusinessName");
    String customerFacingBusinessName = request.getString("customerFacingBusinessName");

    setLegalBusinessName(legalBusinessName);
    setEmailId(emailId);
    setPhoneNumber(phoneNumber);

    JsonObject details =
        new JsonObject()
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
    if (StringUtils.isNotBlank(customerFacingBusinessName)) {
      setCustomerFacingBusinessName(customerFacingBusinessName);
      details.put("customer_facing_business_name", customerFacingBusinessName);
    }
    if (StringUtils.isNotBlank(contactName)) {
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
            .replace("$8", "CREATED")
            .replace("$9", getAccountProductId());

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

//  TODO: Remove this, it is for testing

  private String getRandomUuid() {
    String pk = UUID.randomUUID().toString();
    return pk;
  }


  /**
   * Creates a referenceId of length = 20 based on present time as seed. This referenceId could be sent to Razorpay
   * while creating linked account and it can also act as an internal reference to store relevant
   * merchant information in the DB
   * @return referenceId as string which to act as primary key to store record in DB
   */
  private String createReferenceId() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[40];
    random.nextBytes(bytes);
    String referenceId =
        Hashing.sha512()
            .hashString(Arrays.toString(bytes), StandardCharsets.UTF_8)
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
  public void setAccountProductId(String rzpAccountProductId){
    this.accountProductId = rzpAccountProductId;
  }

  public String getAccountProductId()
  {
    return this.accountProductId;
  }
}
