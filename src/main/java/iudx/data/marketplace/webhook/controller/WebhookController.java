package iudx.data.marketplace.webhook.controller;

import static iudx.data.marketplace.apiserver.response.ResponseUtil.generateResponse;
import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.common.HttpStatusCode.BAD_REQUEST;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.common.ExceptionHandler;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.common.ValidationHandler;
import iudx.data.marketplace.razorpay.service.RazorPayService;
import iudx.data.marketplace.webhook.service.WebhookService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebhookController {
  public static final Logger LOGGER = LogManager.getLogger(WebhookController.class);
  private final Router router;
  private final RazorPayService razorPayService;
  private final WebhookService webhookService;
  ExceptionHandler exceptionHandler;
  ValidationHandler orderPaidRequestValidationHandler;
  ValidationHandler paymentFailedRequestValidationHandler;
  ValidationHandler paymentAuthorizedRequestValidationHandler;

  public WebhookController(Router router, Vertx vertx) {
    this.router = router;
    this.razorPayService = RazorPayService.createProxy(vertx, RAZORPAY_SERVICE_ADDRESS);
    this.webhookService = WebhookService.createProxy(vertx, WEBHOOK_SERVICE_ADDRESS);
    exceptionHandler = new ExceptionHandler();
    paymentFailedRequestValidationHandler =
        new ValidationHandler(RequestType.PAYMENT_FAILED_WEBHOOK);
    orderPaidRequestValidationHandler = new ValidationHandler(RequestType.ORDER_PAID_WEBHOOK);
    paymentAuthorizedRequestValidationHandler =
        new ValidationHandler(RequestType.PAYMENT_AUTHORIZED_WEBHOOK);
  }

  public Router paymentFailedHandler() {
    router
        .post(PAYMENTS_FAILED_PATH)
        .handler(this::handleWebhookSignatureValidation)
        .handler(paymentFailedRequestValidationHandler)
        .handler(this::paymentFailedRequestHandler);
    return router;
  }

  public Router paymentAuthorizedHandler() {
    router
        .post(PAYMENT_AUTHORIZED_PATH)
        .handler(this::handleWebhookSignatureValidation)
        .handler(paymentAuthorizedRequestValidationHandler)
        .handler(this::paymentAuthorizedRequestHandler);
    return router;
  }

  public Router orderPaidHandler() {
    router
        .post(ORDER_PAID_WEBHOOK_PATH)
        .handler(this::handleWebhookSignatureValidation)
        .handler(orderPaidRequestValidationHandler)
        .handler(this::orderPaidRequestHandler)
        .failureHandler(exceptionHandler);
    return router;
  }

  private void paymentFailedRequestHandler(RoutingContext routingContext) {

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();

    LOGGER.debug(requestBody);
    String orderId =
        requestBody
            .getJsonObject(RAZORPAY_PAYLOAD)
            .getJsonObject(RAZORPAY_PAYMENT)
            .getJsonObject(RAZORPAY_ENTITY)
            .getString(RAZORPAY_ORDER_ID, "");
    webhookService
        .recordPaymentFailure(orderId)
        .onSuccess(
            statusUpdated -> {
              handleSuccessResponse(response, 200, "Payment status updated");
            })
        .onFailure(
            statusUpdateFailed -> {
              handleFailureResponse(routingContext, statusUpdateFailed.getMessage());
            });
  }

  private void paymentAuthorizedRequestHandler(RoutingContext routingContext) {

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();

    LOGGER.debug(requestBody);

    handleSuccessResponse(response, 200, requestBody.encode());
  }

  private void orderPaidRequestHandler(RoutingContext routingContext) {

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();

    String orderId =
        requestBody
            .getJsonObject(RAZORPAY_PAYLOAD)
            .getJsonObject(RAZORPAY_ORDER)
            .getJsonObject(RAZORPAY_ENTITY)
            .getString(RAZORPAY_ID, "");

    webhookService
        .recordOrderPaid(orderId)
        .onSuccess(
            policyCreated -> {
              handleSuccessResponse(response, 200, policyCreated.encode());
            })
        .onFailure(
            policyCreationFailed -> {
              handleFailureResponse(routingContext, policyCreationFailed.getMessage());
            });
  }

  private void handleWebhookSignatureValidation(RoutingContext routingContext) {

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    String xrazorpaySignature = request.headers().get(HEADER_X_RAZORPAY_SIGNATURE);

    razorPayService
        .webhookSignatureValidator(requestBody, xrazorpaySignature)
        .onSuccess(
            requestValidated -> {
              LOGGER.debug("Request Validated");
              routingContext.next();
            })
        .onFailure(
            requestInvalidated -> {
              LOGGER.error("Request Validation Failed");
              routingContext.next();
            });
  }

  /**
   * Handles HTTP Success response from the server
   *
   * @param response HttpServerResponse object
   * @param statusCode statusCode to respond with
   * @param result respective result returned from the service
   */
  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  /**
   * Handles Failed HTTP Response
   *
   * @param routingContext Routing context object
   * @param failureMessage Failure message for response
   */
  private void handleFailureResponse(RoutingContext routingContext, String failureMessage) {
    HttpServerResponse response = routingContext.response();
    LOGGER.debug("Failure Message : {} ", failureMessage);

    try {
      JsonObject jsonObject = new JsonObject(failureMessage);
      int type = jsonObject.getInteger(TYPE);
      String title = jsonObject.getString(TITLE);

      HttpStatusCode status = HttpStatusCode.getByValue(type);

      ResponseUrn urn;

      // get the urn by either type or title
      if (title != null) {
        urn = ResponseUrn.fromCode(title);
      } else {

        urn = ResponseUrn.fromCode(String.valueOf(type));
      }
      if (jsonObject.getString(DETAIL) != null) {
        String detail = jsonObject.getString(DETAIL);
        response
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(type)
            .end(generateResponse(status, urn, detail).toString());
      } else {
        response
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(type)
            .end(generateResponse(status, urn).toString());
      }

    } catch (DecodeException exception) {
      LOGGER.error("Error : Expecting JSON from backend service [ jsonFormattingException ] ");
      handleResponse(response, BAD_REQUEST, ResponseUrn.BACKING_SERVICE_FORMAT_URN);
    }
  }

  private void handleResponse(
      HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn) {
    handleResponse(response, statusCode, urn, statusCode.getDescription());
  }

  private void handleResponse(
      HttpServerResponse response,
      HttpStatusCode statusCode,
      ResponseUrn urn,
      String failureMessage) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, failureMessage).toString());
  }
}
