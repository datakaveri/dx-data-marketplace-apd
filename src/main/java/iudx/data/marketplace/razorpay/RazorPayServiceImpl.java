package iudx.data.marketplace.razorpay;

import com.razorpay.Account;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static iudx.data.marketplace.apiserver.provider.linkedAccount.util.Constants.ACCOUNT_TYPE;
import static iudx.data.marketplace.apiserver.provider.linkedAccount.util.Constants.FAILURE_MESSAGE;
import static iudx.data.marketplace.product.util.Constants.*;
import static iudx.data.marketplace.razorpay.Constants.*;

public class RazorPayServiceImpl implements RazorPayService {

  private static Logger LOGGER = LogManager.getLogger(RazorPayServiceImpl.class);
  RazorpayClient razorpayClient;
  private static final Map<String, String> errorMap = initialiseMap();
  PostgresService postgresService;
  RazorPayServiceImpl(RazorpayClient razorpayClient, PostgresService postgresService) {
    this.postgresService = postgresService;
    this.razorpayClient = razorpayClient;
  }

  static Map<String, String> initialiseMap() {
    return Map.ofEntries(
        Map.entry(
            "Merchant email already exists for account".toLowerCase(),
            FAILURE_MESSAGE + "merchant email already exists for account"),
        Map.entry("The phone format is invalid".toLowerCase(), FAILURE_MESSAGE + "phone format is invalid"),
        Map.entry(
            "The contact name may only contain alphabets and spaces".toLowerCase(),
            FAILURE_MESSAGE + "name is invalid"),
        Map.entry(
            "Invalid business subcategory for business category".toLowerCase(),
            FAILURE_MESSAGE + "subcategory or category is invalid"),
        Map.entry("The street2 field is required".toLowerCase(), FAILURE_MESSAGE + "street2 field is required"),
        Map.entry("The street1 field is required".toLowerCase(), FAILURE_MESSAGE + "street1 field is required"),
        Map.entry("The city field is required".toLowerCase(), FAILURE_MESSAGE + "city field is required"),
        Map.entry(
            "The business registered city may only contain alphabets, digits and spaces".toLowerCase(),
            FAILURE_MESSAGE + "city name is invalid"),
        Map.entry(
            "State name entered is incorrect. Please provide correct state name".toLowerCase(),
            FAILURE_MESSAGE + "state name is invalid"),
        Map.entry("The postal code must be an integer".toLowerCase(), FAILURE_MESSAGE + "postal code is invalid"),
        Map.entry(
            "The business registered country may only contain alphabets and spaces".toLowerCase(),
            FAILURE_MESSAGE + "country name is invalid"),
        Map.entry("The pan field is invalid".toLowerCase(), FAILURE_MESSAGE + "pan field is invalid"),
        Map.entry("The gst field is invalid".toLowerCase(), FAILURE_MESSAGE + "gst field is invalid"),
        Map.entry(
            "Route code Support feature not enabled to add account code".toLowerCase(),
            FAILURE_MESSAGE + "route code support feature not enabled to add account code"),
        Map.entry(
            "The api key/secret provided is invalid".toLowerCase(),
            FAILURE_MESSAGE + ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage()));

    //    errorMap.put("Invalid type: route", FAILURE_MESSAGE  +
    // ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage());
    //    errorMap.put("The code format is invalid", FAILURE_MESSAGE  +
    // ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage());
    //    errorMap.put("The code must be at least 3 characters",FAILURE_MESSAGE  +
    // ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage());
    //    errorMap.put("The selected tnc accepted is invalid.",FAILURE_MESSAGE  +
    // ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage());
    //    errorMap.put("The product requested is invalid",FAILURE_MESSAGE  +
    // ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage());
    //    errorMap.put("Linked account does not exist",FAILURE_MESSAGE  +
    // ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage());
    //      errorMap.put("no Route matched with those values", FAILURE_MESSAGE +
    // ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn());
    //      errorMap.put("id provided does not exist", FAILURE_MESSAGE +
    // ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn());
  }

  @Override
  public Future<JsonObject> createOrder(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    Integer amount = (int) (request.getDouble(PRICE) * 100);
    JSONObject orderRequest =
        new JSONObject()
            .put(AMOUNT, amount)
            .put(CURRENCY, INR)
            .put(RECEIPT, "receipt") // TODO: what is receipt?
            .put(
                TRANFERS,
                new JSONArray()
                    .put(
                        0,
                        new JSONObject()
                            .put(ACCOUNT, request.getString(ACCOUNT_ID))
                            .put(AMOUNT, amount)
                            .put(CURRENCY, INR)
                            .put(
                                NOTES,
                                new JSONObject()
                                    .put("VARIANT", request.getString("product_variant_name"))
                                    .put("PRODUCT", request.getString("product_id"))
                                    .put("PROVIDER", request.getString("provider_id")))
                            .put(ON_HOLD, ZERO)));

    try {
      LOGGER.debug("order request at razorpay: {} ", orderRequest);
      Order order = razorpayClient.orders.create(orderRequest);
      String status = order.get(STATUS);
      if (!status.equals(CREATED)) {
        throw new DxRuntimeException(
            400,
            ResponseUrn.ORDER_NOT_CREATED,
            ("Order creation returned with status : " + status));
      }
      JSONArray transfersArray = order.get(TRANFERS);
      JSONObject transferResponse = transfersArray.getJSONObject(0);
      JSONObject responseError = transferResponse.getJSONObject(ERROR);
      LOGGER.debug("error response from razorpay : {}", responseError);

      Object reason =  responseError.get(REASON);
      if(reason!=null) {
        LOGGER.error("razorpay error : ", responseError);
        String message;
        switch ((String) reason) {
          case "amount_less_than_minimum_amount":
          case "authentication_failed":
          case "gateway_technical_error":
          case "invalid_response_from_gateway":
          case "server_error":
            message = "RazorPay Error - Try again later";
            break;
          case "invalid_request":
            message = "Invalid Request Made to RazorPay";
            break;
          default:
            message = "Unexpected Error while order creation - Try again later";
        }
        throw new DxRuntimeException(400, ResponseUrn.ORDER_CREATION_FAILED, message);
      }

      LOGGER.info("order response razorpay : {}", order.toJson());

      /* Converting org.json.JSONObject to Java.Util.String to io.vertx.core.json.JsonObject */
      promise.complete(new JsonObject(order.toJson().toString()));
    } catch (RazorpayException e) {
      LOGGER.error("order created on razorpay failed with exception : {}", e.getMessage());
      RespBuilder respBuilder =
          new RespBuilder()
              .withType(ResponseUrn.ORDER_CREATION_FAILED.getUrn())
              .withTitle("RazorPay Error")
              .withDetail(e.getMessage());
      promise.fail(respBuilder.getResponse());
    } catch (DxRuntimeException e) {
      LOGGER.error("order not created on razorpay, : {}", e.getMessage());
      RespBuilder respBuilder =
          new RespBuilder()
              .withType(e.getUrn().getUrn())
              .withTitle("RazorPay Error")
              .withDetail(e.getUrn().getMessage());
      promise.fail(respBuilder.getResponse());
    } catch (JSONException e) {
      LOGGER.error("parse execption during order creation, : {}", e.getMessage());

      RespBuilder respBuilder =
          new RespBuilder()
              .withType(ResponseUrn.ORDER_CREATION_FAILED.getUrn())
              .withTitle("RazorPay Error")
              .withDetail(e.getMessage());
      promise.fail(respBuilder.getResponse());
    }

    return promise.future();
  }

  @Override
  public Future<JsonObject> requestProductConfiguration(JsonObject info) {
    Promise<JsonObject> promise = Promise.promise();
    JSONObject request = new JSONObject()
            .put("product_name", ACCOUNT_TYPE)
            .put("tnc_accepted", true);
//    TODO: what if we don't send the IP address of the system
    String accountId = info.getString("accountId");
      try {
        Account productConfiguration = razorpayClient.product.requestProductConfiguration(accountId, request);
        JsonObject temp = new JsonObject(productConfiguration.toString());
        String razorpayAccountProductId = temp.getString(ID);
        LOGGER.info("Terms and conditions accepted for accountId with razorpayAccountProductId : {}, {}", accountId, razorpayAccountProductId);
        promise.complete(new JsonObject().put("razorpayAccountProductId", razorpayAccountProductId));
      } catch (RazorpayException e) {
        e.printStackTrace();
        LOGGER.error("Razorpay error message: {}", e.getMessage());
        /*handle error messages from Razorpay*/
        String razorpayError = e.getMessage().toLowerCase();
        String failureMessage = errorHandler(razorpayError);
        promise.fail(failureMessage);
      }
      return promise.future();
  }

  @Override
  public Future<Boolean> fetchProductConfiguration(JsonObject request) {
    Promise<Boolean> promise = Promise.promise();
    String razorpayAccountId = request.getString("account_id");
    String razorpayAccountProductId = request.getString("rzp_account_product_id");
      try {
        Account fetchProductConfiguration = razorpayClient.product.fetch(razorpayAccountId, razorpayAccountProductId);
        JsonObject temp = new JsonObject(fetchProductConfiguration.toString());
          String activationStatus = temp.getString("activation_status");
          boolean isAccountActivated = activationStatus.toLowerCase().matches("activated");

          if(isAccountActivated)
          {
            promise.complete(true);
          }
          else
          {
            LOGGER.error("Linked account not activated");
            String detail = "To activate linked account please complete the KYC, filling account information etc., in your Razorpay merchant dashboard";
        String failureMessage =
            new RespBuilder()
                .withType(HttpStatusCode.FORBIDDEN.getValue())
                .withTitle(ResponseUrn.FORBIDDEN_PRODUCT_CREATION.getUrn())
                .withDetail(detail)
                .getResponse();
            promise.fail(failureMessage);
          }
      } catch (RazorpayException e) {
        e.printStackTrace();
        LOGGER.error("Razorpay error message: {}", e.getMessage());
        /*handle error messages from Razorpay*/
        String razorpayError = e.getMessage().toLowerCase();
        String failureMessage = errorHandler(razorpayError);
        promise.fail(failureMessage);
      }
      return promise.future();
  }

  @Override
  public Future<JsonObject> createLinkedAccount(String request) {
    Promise<JsonObject> promise = Promise.promise();
    /*convert string to maven JSONObject */
    JSONObject requestJson = new JSONObject(request);
    try {
      Account account = razorpayClient.account.create(requestJson);
      JsonObject temp = new JsonObject(account.toString());
      String accountId = temp.getString(ID);
      LOGGER.info("Linked account created with accountId : {}", accountId);
      promise.complete(new JsonObject().put("accountId", accountId));

    } catch (RazorpayException e) {
      LOGGER.error("Razorpay error message: {}", e.getMessage());
      /*handle error messages from Razorpay*/
      String razorpayError = e.getMessage().toLowerCase();
      String failureMessage = errorHandler(razorpayError);
      promise.fail(failureMessage);
    }
    return promise.future();
  }

  public String errorHandler(String rzpFailureMessage) {
    String failureMessage  = new RespBuilder()
            .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
            .withTitle(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn())
            .withDetail(FAILURE_MESSAGE  + ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
            .getResponse();;
    for (var error : errorMap.entrySet()) {
      boolean isErrorPresent = rzpFailureMessage.contains(error.getKey());
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
}
