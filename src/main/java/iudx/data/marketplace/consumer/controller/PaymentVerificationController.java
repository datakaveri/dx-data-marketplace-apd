package iudx.data.marketplace.consumer.controller;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.common.HttpStatusCode.BAD_REQUEST;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.dmpAuth.aaaService.AuthClient;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.common.ExceptionHandler;
import iudx.data.marketplace.common.ValidationHandler;
import iudx.data.marketplace.authenticator.handlers.authentication.AuthHandler;
import iudx.data.marketplace.authenticator.handlers.authentication.TokenIntrospectHandler;
import iudx.data.marketplace.authenticator.handlers.authorization.AuthorizationHandler;
import iudx.data.marketplace.dmpAuth.UserInfoFromAuthHandler;
import iudx.data.marketplace.authenticator.service.model.DxRole;
import iudx.data.marketplace.dmpAuth.UserInfo;
import iudx.data.marketplace.authenticator.service.AuthenticationService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.postgres.service.PostgresService;
import iudx.data.marketplace.razorpay.service.RazorPayService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PaymentVerificationController {
  public static final Logger LOGGER = LogManager.getLogger(PaymentVerificationController.class);

  private final Router router;
  private final Api api;
  private final AuthenticationService authenticationService;
  private final PostgresService pgService;
  private final AuthClient authClient;
  private final RazorPayService razorPayService;

  public PaymentVerificationController(Router router, Api api, Vertx vertx, AuthClient authClient) {
    this.router = router;
    this.api = api;
    this.authClient = authClient;
    this.authenticationService = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    this.pgService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    this.razorPayService = RazorPayService.createProxy(vertx, RAZORPAY_SERVICE_ADDRESS);
  }

  public Router init() {

    AuthHandler authHandler = new AuthHandler(authenticationService);
    Handler<RoutingContext> tokenIntrospectHandler = new TokenIntrospectHandler().validateToken();
    UserInfoFromAuthHandler userInfoFromAuthHandler =
        new UserInfoFromAuthHandler(authClient, new UserInfo(), pgService);
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    Handler<RoutingContext> consumerApiAccessHandler =
        new AuthorizationHandler().setUserRolesForEndpoint(DxRole.CONSUMER, DxRole.DELEGATE);
    ValidationHandler verifyPaymentValidationHandler =
        new ValidationHandler(RequestType.VERIFY_PAYMENT);

    router
        .post(api.getVerifyPaymentApi())
        .handler(verifyPaymentValidationHandler)
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(consumerApiAccessHandler)
        .handler(userInfoFromAuthHandler)
        .handler(this::handleVerifyPayment)
        .failureHandler(exceptionHandler);

    return router;
  }

  private void handleVerifyPayment(RoutingContext routingContext) {

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();

    razorPayService
        .verifyPayment(requestBody)
        .onSuccess(
            paymentVerified -> {
              handleSuccessResponse(response, 200, paymentVerified.encode());
            })
        .onFailure(
            verifyFailed -> {
              handleResponse(response, BAD_REQUEST.getValue(), verifyFailed.getMessage());
            });
  }

  private void handleResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
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
}
