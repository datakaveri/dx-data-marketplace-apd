package iudx.data.marketplace.apiserver.provider.linkedaccount.controller;

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
import iudx.data.marketplace.apiserver.provider.linkedaccount.service.LinkedAccountService;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.common.ExceptionHandler;
import iudx.data.marketplace.common.ValidationHandler;
import iudx.data.marketplace.authenticator.handlers.authentication.AuthHandler;
import iudx.data.marketplace.authenticator.handlers.authentication.TokenIntrospectHandler;
import iudx.data.marketplace.authenticator.handlers.authorization.AuthorizationHandler;
import iudx.data.marketplace.authenticator.handlers.authorization.UserInfoFromAuthHandler;
import iudx.data.marketplace.authenticator.service.model.DxRole;
import iudx.data.marketplace.authenticator.service.model.UserInfo;
import iudx.data.marketplace.authenticator.service.AuthenticationService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.common.RoutingContextHelper;
import iudx.data.marketplace.policies.service.model.User;
import iudx.data.marketplace.postgres.service.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkedAccountController {
  public static final Logger LOGGER = LogManager.getLogger(LinkedAccountController.class);
  private final Router router;
  private final Api api;
  private final AuthenticationService authenticationService;
  private final PostgresService pgService;
  private final AuthClient authClient;
  private final LinkedAccountService linkedAccountService;

  public LinkedAccountController(Router router, Api api, Vertx vertx, AuthClient authClient) {
    this.router = router;
    this.api = api;
    this.authClient = authClient;
    this.authenticationService = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    this.pgService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    this.linkedAccountService = LinkedAccountService.createProxy(vertx, LINKED_ACCOUNT_ADDRESS);
    this.init();
  }

  public Router init() {

    AuthHandler authHandler = new AuthHandler(authenticationService);
    Handler<RoutingContext> tokenIntrospectHandler = new TokenIntrospectHandler().validateToken();
    UserInfoFromAuthHandler userInfoFromAuthHandler =
        new UserInfoFromAuthHandler(authClient, new UserInfo(), pgService);
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    ValidationHandler postLinkedAccountHandler = new ValidationHandler(RequestType.POST_ACCOUNT);
    ValidationHandler putLinkedAccountHandler = new ValidationHandler(RequestType.PUT_ACCOUNT);
    Handler<RoutingContext> providerApiAccessHandler =
        new AuthorizationHandler().setUserRolesForEndpoint(DxRole.PROVIDER, DxRole.DELEGATE);

    router
        .post(api.getLinkedAccountService())
        .handler(postLinkedAccountHandler)
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(providerApiAccessHandler)
        .handler(userInfoFromAuthHandler)
        .handler(this::handlePostLinkedAccount)
        .failureHandler(exceptionHandler);

    router
        .put(api.getLinkedAccountService())
        .handler(putLinkedAccountHandler)
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(providerApiAccessHandler)
        .handler(userInfoFromAuthHandler)
        .handler(this::handlePutLinkedAccount)
        .failureHandler(exceptionHandler);

    router
        .get(api.getLinkedAccountService())
        .handler(authHandler)
        .handler(tokenIntrospectHandler)
        .handler(providerApiAccessHandler)
        .handler(userInfoFromAuthHandler)
        .handler(this::handleFetchLinkedAccount)
        .failureHandler(exceptionHandler);

    return router;
  }

  private void handlePostLinkedAccount(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    User user = RoutingContextHelper.getUser(routingContext);
    HttpServerResponse response = routingContext.response();
    linkedAccountService
        .createLinkedAccount(requestBody, user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Linked account created successfully ");
                handleSuccessResponse(
                    response, HttpStatusCode.SUCCESS.getValue(), handler.result().toString());

              } else {
                LOGGER.error(
                    "Linked account could not be created {}", handler.cause().getMessage());
                handleFailureResponse(routingContext, handler.cause().getMessage());
              }
            });
  }

  private void handlePutLinkedAccount(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();
    User user = RoutingContextHelper.getUser(routingContext);

    linkedAccountService
        .updateLinkedAccount(requestBody, user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Linked account updated successfully ");
                handleSuccessResponse(
                    response, HttpStatusCode.SUCCESS.getValue(), handler.result().toString());
              } else {
                LOGGER.error(
                    "Linked account could not be updated {}", handler.cause().getMessage());
                handleFailureResponse(routingContext, handler.cause().getMessage());
              }
            });
  }

  private void handleFetchLinkedAccount(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    User user = RoutingContextHelper.getUser(routingContext);
    linkedAccountService
        .fetchLinkedAccount(user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Linked account fetched successfully ");
                handleSuccessResponse(
                    response, HttpStatusCode.SUCCESS.getValue(), handler.result().toString());
              } else {
                LOGGER.error(
                    "Linked account could not be fetched {}", handler.cause().getMessage());
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
