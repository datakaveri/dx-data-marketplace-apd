package iudx.data.marketplace.apiserver.handlers;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.authenticator.util.Constants.GET_USER;
import static iudx.data.marketplace.authenticator.util.Constants.INSERT_USER_TABLE;
import static iudx.data.marketplace.authenticator.util.Constants.TOKEN;
import static iudx.data.marketplace.common.Constants.AUTH_INFO;
import static iudx.data.marketplace.common.ResponseUrn.INVALID_TOKEN_URN;
import static iudx.data.marketplace.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.aaaService.AuthClient;
import iudx.data.marketplace.authenticator.AuthenticationService;
import iudx.data.marketplace.authenticator.model.DxRole;
import iudx.data.marketplace.authenticator.model.JwtData;
import iudx.data.marketplace.authenticator.model.UserInfo;
import iudx.data.marketplace.common.*;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

  private AuthenticationService authenticator;
  private HttpServerRequest request;

  public AuthHandler(
      AuthenticationService authenticationService) {
    this.authenticator = authenticationService;
  }

  @Override
  public void handle(RoutingContext context) {
    JsonObject authInfo = RoutingContextHelper.getAuthInfo(context);
    request = context.request();
    LOGGER.debug("Info : path " + request.path());

    checkIfTokenIsAuthenticated(authInfo)
        .onSuccess(
            jwtData -> {
              LOGGER.info("User Verified Successfully.");
              RoutingContextHelper.setJwtData(context, jwtData);
              context.next();
            })
        .onFailure(
            fail -> {
              LOGGER.error("User Verification Failed. " + fail.getMessage());
              processAuthFailure(context, fail.getMessage());
            });

  }

  Future<JwtData> checkIfTokenIsAuthenticated(JsonObject authenticationInfo) {
    Promise<JwtData> promise = Promise.promise();
    Future<JwtData> tokenIntrospect = authenticator.tokenIntrospect(authenticationInfo);
    tokenIntrospect.onSuccess(promise::complete).onFailure(promise::fail);
    return promise.future();
  }

  private void processAuthFailure(RoutingContext ctx, String result) {
    LOGGER.error("result : " + result);
    if (result.contains("Not Found")) {
      LOGGER.error("Error : Item Not Found");
      LOGGER.error("Error : " + result);
      HttpStatusCode statusCode = HttpStatusCode.getByValue(404);
      generateResponse(ctx, RESOURCE_NOT_FOUND_URN, statusCode);
    } else {
      LOGGER.error("Error : Authentication Failure");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
      LOGGER.error("Error : " + result);
      generateResponse(ctx, INVALID_TOKEN_URN, statusCode);
    }
  }

  private void generateResponse(RoutingContext ctx, ResponseUrn urn, HttpStatusCode statusCode) {
    String response =
        new RespBuilder()
            .withType(urn.getUrn())
            .withTitle(statusCode.getDescription())
            .withDetail(statusCode.getDescription())
            .getResponse();

    ctx.response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(response);
  }

}
