package iudx.data.marketplace.razorpay;

import static iudx.data.marketplace.apiserver.provider.linkedAccount.util.Constants.ACCOUNT_TYPE;
import static iudx.data.marketplace.apiserver.provider.linkedAccount.util.Constants.FAILURE_MESSAGE;
import static iudx.data.marketplace.product.util.Constants.*;
import static iudx.data.marketplace.razorpay.Constants.*;

import com.razorpay.*;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.razorpay.util.*;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RazorPayServiceImpl implements RazorPayService {

  private static final Map<String, String> errorMap = ErrorMessageBuilder.initialiseMap();
  private static Logger LOGGER = LogManager.getLogger(RazorPayServiceImpl.class);
  private final PostgresService postgresService;
  private final String razorPaySecret;
  private final String paymentTable;

  private final String webhookSecret;
  RazorpayClient razorpayClient;

  RazorPayServiceImpl(
      RazorpayClient razorpayClient, PostgresService postgresService, final JsonObject config) {
    this.razorpayClient = razorpayClient;
    this.postgresService = postgresService;
    this.razorPaySecret = config.getString(RAZORPAY_SECRET);
    this.paymentTable = config.getJsonArray(TABLES).getString(9);
    this.webhookSecret = config.getString(WEBHOOK_SECRET);
  }

  @Override
  public Future<JsonObject> createOrder(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.debug(request);

    Integer amountInPaise = (int) (Double.parseDouble((request.getString(PRICE))) * 100);
    JSONObject orderRequest =
        new JSONObject()
            .put(AMOUNT, amountInPaise)
            .put(CURRENCY, INR)
            .put(RECEIPT, "receipt") // TODO: what is receipt?
            .put(
                TRANFERS,
                new JSONArray()
                    .put(
                        0,
                        new JSONObject()
                            .put(ACCOUNT, request.getString(ACCOUNT_ID))
                            .put(AMOUNT, amountInPaise)
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

      Object reason = responseError.get(REASON);
      LOGGER.debug(JSONObject.NULL.equals(reason));
      if (!JSONObject.NULL.equals(reason)) {
        LOGGER.error("razorpay error : ", responseError);
        String message;
        switch (reason.toString()) {
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
      LOGGER.error("parse exception during order creation, : {}", e.getMessage());

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
  public Future<JsonObject> verifyPayment(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    JSONObject verifyOption =
        new JSONObject()
            .put(RAZORPAY_ORDER_ID, request.getString(RAZORPAY_ORDER_ID))
            .put(RAZORPAY_PAYMENT_ID, request.getString(RAZORPAY_PAYMENT_ID))
            .put(RAZORPAY_SIGNATURE, request.getString(RAZORPAY_SIGNATURE));

    try {
      boolean status = Utils.verifyPaymentSignature(verifyOption, razorPaySecret);

      if (status) {
        recordPayment(request)
            .onSuccess(
                successHandler -> {
                  promise.complete(
                      new RespBuilder()
                          .withType(ResponseUrn.SUCCESS_URN.getUrn())
                          .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                          .withDetail(
                              String.format(
                                  "Payment Verified for order id %s",
                                  request.getString(RAZORPAY_ORDER_ID)))
                          .getJsonResponse());
                })
            .onFailure(promise::fail);
      } else {
        throw new DxRuntimeException(
            400, ResponseUrn.INVALID_PAYMENT, ResponseUrn.INVALID_PAYMENT.getMessage());
      }
    } catch (RazorpayException e) {
      throw new DxRuntimeException(
          400, ResponseUrn.INVALID_PAYMENT, ResponseUrn.INVALID_PAYMENT.getMessage());
    } catch (DxRuntimeException e) {
      LOGGER.error("payment not verified on razorpay, : {}", e.getMessage());
      RespBuilder respBuilder =
          new RespBuilder()
              .withType(e.getUrn().getUrn())
              .withTitle("RazorPay Error")
              .withDetail(e.getUrn().getMessage());
      promise.fail(respBuilder.getResponse());
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> webhookSignatureValidator(JsonObject request, String signatureHeader) {
    Promise<JsonObject> promise = Promise.promise();

    try {
      boolean isValidRequest =
          Utils.verifyWebhookSignature(request.toString(), signatureHeader, webhookSecret);
      if (isValidRequest) {
        promise.complete();
      } else {
        throw new DxRuntimeException(
            400,
            ResponseUrn.INVALID_WEBHOOK_REQUEST,
            ResponseUrn.INVALID_WEBHOOK_REQUEST.getMessage());
      }
    } catch (RazorpayException e) {
      throw new DxRuntimeException(
          400,
          ResponseUrn.INVALID_WEBHOOK_REQUEST,
          ResponseUrn.INVALID_WEBHOOK_REQUEST.getMessage());
    } catch (DxRuntimeException e) {
      LOGGER.error("Webhook signature verification failed on razorpay, : {}", e.getMessage());
      RespBuilder respBuilder =
          new RespBuilder()
              .withType(e.getUrn().getUrn())
              .withTitle("RazorPay Error")
              .withDetail(e.getUrn().getMessage());
      promise.fail(respBuilder.getResponse());
    }
    return promise.future();
  }

  public Future<JsonObject> requestProductConfiguration(JsonObject info) {
    Promise<JsonObject> promise = Promise.promise();
    JSONObject request =
        new JSONObject().put("product_name", ACCOUNT_TYPE).put("tnc_accepted", true);
    //    TODO: what if we don't send the IP address of the system
    String accountId = info.getString("accountId");
    try {
      Account productConfiguration =
          razorpayClient.product.requestProductConfiguration(accountId, request);
      JsonObject temp = new JsonObject(productConfiguration.toString());
      String razorpayAccountProductId = temp.getString(ID);
      LOGGER.info(
          "Terms and conditions accepted for accountId with razorpayAccountProductId : {}, {}",
          accountId,
          razorpayAccountProductId);
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
      Account fetchProductConfiguration =
          razorpayClient.product.fetch(razorpayAccountId, razorpayAccountProductId);
      JsonObject temp = new JsonObject(fetchProductConfiguration.toString());
      String activationStatus = temp.getString("activation_status");
      boolean isAccountActivated = activationStatus.toLowerCase().matches("activated");

      if (isAccountActivated) {
        promise.complete(true);
      } else {
        LOGGER.error("Linked account not activated");
        String detail =
            "To activate linked account please complete the KYC, filling account information etc., in your Razorpay merchant dashboard";
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

  private Future<JsonObject> recordPayment(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    StringBuilder query = new StringBuilder(RECORD_PAYMENT.replace("$0", paymentTable));

    JsonObject params =
        new JsonObject()
            .put(RAZORPAY_ORDER_ID, request.getString(RAZORPAY_ORDER_ID))
            .put(RAZORPAY_PAYMENT_ID, request.getString(RAZORPAY_PAYMENT_ID))
            .put(RAZORPAY_SIGNATURE, request.getString(RAZORPAY_SIGNATURE));

    postgresService.executePreparedQuery(
        query.toString(),
        params,
        pgHandler -> {
          if (pgHandler.succeeded()) {
            promise.complete(pgHandler.result());
          } else {
            promise.fail(pgHandler.cause());
          }
        });
    return promise.future();
  }

  @Override
  public Future<JsonObject> fetchLinkedAccount(String accountId) {
    Promise<JsonObject> promise = Promise.promise();
    try {
      Account account = razorpayClient.account.fetch(accountId);
      JsonObject result = new JsonObject(account.toString());
      promise.complete(result);
    } catch (RazorpayException e) {
      LOGGER.error("Razorpay error message: {}", e.getMessage());
      /*handle error messages from Razorpay*/
      String razorpayError = e.getMessage().toLowerCase();
      String failureMessage = errorHandler(razorpayError);
      promise.fail(failureMessage);
    }
    return promise.future();
  }

  @Override
  public Future<Boolean> updateLinkedAccount(String request, String accountId) {
    Promise<Boolean> promise = Promise.promise();
    JSONObject accountRequest = new JSONObject(request);
    try {
      Account account = razorpayClient.account.edit(accountId, accountRequest);
      JsonObject temp = new JsonObject(account.toString());
      String referenceId = temp.getString("reference_id");
      LOGGER.info(
          "Linked account with accountId : {}, referenceId : {} updated successfully",
          accountId,
          referenceId);
      promise.complete(true);
    } catch (RazorpayException e) {
      LOGGER.error("Razorpay error message: {}", e.getMessage());
      /*handle error messages from Razorpay*/
      String razorpayError = e.getMessage().toLowerCase();
      String message = errorHandler(razorpayError);
      String failureMessage = message.replace(FAILURE_MESSAGE, "Linked account updation failed : ");
      promise.fail(failureMessage);
    }
    return promise.future();
  }

  public String errorHandler(String rzpFailureMessage) {
    String failureMessage =
        new RespBuilder()
            .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
            .withTitle(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn())
            .withDetail(FAILURE_MESSAGE + ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
            .getResponse();
    ;
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
