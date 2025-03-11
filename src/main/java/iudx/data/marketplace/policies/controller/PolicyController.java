package iudx.data.marketplace.policies.controller;

import static iudx.data.marketplace.apiserver.response.ResponseUtil.generateResponse;
import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.common.HttpStatusCode.BAD_REQUEST;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.aaaService.AuthClient;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.authenticator.handlers.authentication.AuthHandler;
import iudx.data.marketplace.authenticator.handlers.authentication.TokenIntrospectHandler;
import iudx.data.marketplace.authenticator.handlers.authorization.AuthorizationHandler;
import iudx.data.marketplace.authenticator.handlers.authorization.UserInfoFromAuthHandler;
import iudx.data.marketplace.authenticator.service.AuthenticationService;
import iudx.data.marketplace.authenticator.service.model.DxRole;
import iudx.data.marketplace.authenticator.service.model.UserInfo;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.ExceptionHandler;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.common.RoutingContextHelper;
import iudx.data.marketplace.common.ValidationHandler;
import iudx.data.marketplace.policies.service.PolicyService;
import iudx.data.marketplace.policies.service.model.User;
import iudx.data.marketplace.postgres.service.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PolicyController {
  public static final Logger LOGGER = LogManager.getLogger(PolicyController.class);
  private final Router router;
  private final Api api;
  private final PolicyService policyService;
  AuthHandler authHandler;
  Handler<RoutingContext> tokenIntrospectHandler;
  Handler<RoutingContext> kcTokenIntrospectHandler;
  Handler<RoutingContext> apiAccessHandler;
  UserInfoFromAuthHandler userInfoFromAuthHandler;
  ExceptionHandler exceptionHandler;
  ValidationHandler verifyValidationHandler;
  ValidationHandler checkPolicyValidationHandler;
  Handler<RoutingContext> consumerApiAccessHandler;

  public PolicyController(
      Router router, Api api, Vertx vertx, String apdUrl, AuthClient authClient) {
    this.router = router;
    this.api = api;
    AuthenticationService authenticationService = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    PostgresService pgService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    this.policyService = PolicyService.createProxy(vertx, POLICY_SERVICE_ADDRESS);
    authHandler = new AuthHandler(authenticationService);
    tokenIntrospectHandler = new TokenIntrospectHandler().validateToken();
    kcTokenIntrospectHandler = new TokenIntrospectHandler().validateKeycloakToken(apdUrl);
    apiAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(DxRole.CONSUMER, DxRole.PROVIDER, DxRole.DELEGATE);
    userInfoFromAuthHandler = new UserInfoFromAuthHandler(authClient, new UserInfo(), pgService);
    exceptionHandler = new ExceptionHandler();
    verifyValidationHandler = new ValidationHandler(RequestType.VERIFY);
    checkPolicyValidationHandler = new ValidationHandler(RequestType.CHECK_POLICY);
    consumerApiAccessHandler =
        new AuthorizationHandler().setUserRolesForEndpoint(DxRole.CONSUMER, DxRole.DELEGATE);
  }

  public Router policyHandler() {
    router
        .get(api.getPoliciesUrl())
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(apiAccessHandler)
        .handler(userInfoFromAuthHandler)
        .handler(this::getPoliciesHandler)
        .failureHandler(exceptionHandler);
    return router;
  }

  public Router verifyHandler() {
    router
        .post(api.getVerifyUrl())
        .handler(verifyValidationHandler)
        .handler(authHandler)
        .handler(kcTokenIntrospectHandler)
        .handler(this::handleVerify)
        .failureHandler(exceptionHandler);
    return router;
  }

  public Router checkPolicyHandler() {
    router
        .get(api.getCheckPolicyPath())
        .handler(checkPolicyValidationHandler)
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(consumerApiAccessHandler)
        .handler(userInfoFromAuthHandler)
        .handler(this::checkPolicyHandler)
        .failureHandler(exceptionHandler);
    return router;
  }

  private void getPoliciesHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    User user = RoutingContextHelper.getUser(routingContext);
    policyService
        .getPolicies(user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                String result = handler.result().getJsonObject(RESULT).encode();
                handleSuccessResponse(response, handler.result().getInteger(STATUS_CODE), result);
              } else {
                handleFailureResponse(routingContext, handler.cause().getMessage());
              }
            });
  }

  private void handleVerify(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();
    policyService
        .verifyPolicy(requestBody)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Policy verified successfully ");
                handleSuccessResponse(
                    response, HttpStatusCode.SUCCESS.getValue(), handler.result().toString());
              } else {
                LOGGER.error("Policy could not be verified {}", handler.cause().getMessage());
                handleFailureResponse(routingContext, handler.cause().getMessage());
              }
            });
  }

  private void checkPolicyHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();

    User user = RoutingContextHelper.getUser(routingContext);
    String productVariantId = routingContext.request().getParam("productVariantId");
    policyService
        .checkPolicy(productVariantId, user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                int statusCode = handler.result().getInteger(STATUS_CODE);
                String result = handler.result().getJsonObject(RESULTS).encode();
                handleSuccessResponse(response, statusCode, result);
              } else {
                handleFailureResponse(routingContext, handler.cause().getMessage());
              }
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
