package iudx.data.marketplace.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.authenticator.AuthenticationService;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.Constants.AUTH_INFO;
import static iudx.data.marketplace.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.ResponseUrn.INVALID_TOKEN_URN;
import static iudx.data.marketplace.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;

public class AuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

  static AuthenticationService authenticator;
  private HttpServerRequest request;

  public static AuthHandler create(Vertx vertx) {
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    return new AuthHandler();
  }

  @Override
  public void handle(RoutingContext context) {
    request = context.request();
    JsonObject requestJson = context.body().asJsonObject();

    if (requestJson == null) {
      requestJson = new JsonObject();
    }

    LOGGER.debug("Info : path " + request.path());

    String token = request.headers().get(HEADER_TOKEN);
    final String path = request.path();
    final String method = context.request().method().toString();

    if (token == null) token = "public";

    JsonObject authInfo =
        new JsonObject().put(API_ENDPOINT, path).put(HEADER_TOKEN, token).put(API_METHOD, method);

    authenticator.tokenIntrospect(
        requestJson,
        authInfo,
        authHandler -> {
          if (authHandler.succeeded()) {
            authInfo.put(IID, authHandler.result().getValue(IID));
            authInfo.put(USER_ID, authHandler.result().getValue(USER_ID));
            authInfo.put(EXPIRY, authHandler.result().getValue(EXPIRY));
            context.data().put(AUTH_INFO, authInfo);
          } else {
            processAuthFailure(context, authHandler.cause().getMessage());
            return;
          }
          context.next();
        });
  }

  private void processAuthFailure(RoutingContext ctx, String result) {
    if (result.contains("Not Found")) {
      LOGGER.error("Error : Item Not Found");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(404);
      generateResponse(ctx, RESOURCE_NOT_FOUND_URN, statusCode, result);
    } else {
      LOGGER.error("Error : Authentication Failure");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
      generateResponse(ctx, INVALID_TOKEN_URN, statusCode, result);
    }
  }

  private void generateResponse(RoutingContext ctx, ResponseUrn urn, HttpStatusCode statusCode, String errMessage) {
    String response =
        new RespBuilder()
            .withType(urn.getUrn())
            .withTitle(statusCode.getDescription())
            .withDetail(errMessage)
            .getResponse();

    ctx.response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(response);
  }
}
