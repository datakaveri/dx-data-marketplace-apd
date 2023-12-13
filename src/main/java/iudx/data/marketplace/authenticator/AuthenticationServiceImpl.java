package iudx.data.marketplace.authenticator;

import static iudx.data.marketplace.authenticator.util.Constants.API_ENDPOINT;
import static iudx.data.marketplace.authenticator.util.Constants.TOKEN;
import static iudx.data.marketplace.authenticator.util.Constants.METHOD;
import static iudx.data.marketplace.authenticator.util.Constants.USER_ROLE;
import static iudx.data.marketplace.authenticator.util.Constants.USER_ID;
import static iudx.data.marketplace.authenticator.util.Constants.ADMIN;
import static iudx.data.marketplace.authenticator.util.Constants.IID;
import static iudx.data.marketplace.common.Constants.EXPIRY;
import static iudx.data.marketplace.common.Constants.PROVIDER_ID;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import iudx.data.marketplace.authenticator.authorization.*;
import iudx.data.marketplace.authenticator.model.JwtData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(AuthenticationServiceImpl.class);

  final JWTAuth jwtAuth;
  final String audience;

  public AuthenticationServiceImpl(Vertx vertx, final JWTAuth jwtAuth, final JsonObject config) {
    this.jwtAuth = jwtAuth;
    this.audience = config.getString("host");
  }

  @Override
  public AuthenticationService tokenIntrospect(
      JsonObject request, JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {
    String endPoint = authenticationInfo.getString(API_ENDPOINT);
    String token = authenticationInfo.getString(TOKEN);
    String method = authenticationInfo.getString(METHOD);

    Future<JwtData> jwtDecodeFuture = decodeJwt(token);

    ResultContainer result = new ResultContainer();

    jwtDecodeFuture
        .compose(
            decodeHandler -> {
              result.jwtData = decodeHandler;
              return isValidAudienceValue(result.jwtData);
            })
        .compose(
            audienceHandler -> {
              return isValidEndpoint(endPoint);
            })
        .compose(
            validEndpointHandler -> {
              return validateAccess(result.jwtData, authenticationInfo);
            })
        .onComplete(
            completeHandler -> {
              if (completeHandler.succeeded()) {
                handler.handle(Future.succeededFuture(completeHandler.result()));
              } else {
                handler.handle(Future.failedFuture(completeHandler.cause().getMessage()));
              }
            });
    return this;
  }

  Future<JwtData> decodeJwt(String jwtToken) {
    Promise<JwtData> promise = Promise.promise();
    TokenCredentials credentials = new TokenCredentials(jwtToken);

    jwtAuth
        .authenticate(credentials)
        .onSuccess(
            user -> {
              JwtData jwtData = new JwtData(user.principal());
              promise.complete(jwtData);
            })
        .onFailure(
            err -> {
              LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
              promise.fail("failed to decode/validate jwt token : " + err.getMessage());
            });
    return promise.future();
  }

  Future<Boolean> isValidAudienceValue(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();

    LOGGER.debug("Audience in jwt is: " + jwtData.getAud());
    if (audience != null && audience.equalsIgnoreCase(jwtData.getAud())) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect audience value in jwt");
      promise.fail("Incorrect audience value in jwt");
    }
    return promise.future();
  }

  Future<Boolean> isValidEndpoint(String endPoint) {
    Promise<Boolean> promise = Promise.promise();

    LOGGER.debug("Endpoint in JWT is : " + endPoint);

    if (Api.fromEndpoint(endPoint) != null) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect endpoint in jwt");
      promise.fail("Incorrect endpoint in jwt");
    }
    return promise.future();
  }

  public Future<JsonObject> validateAccess(JwtData jwtData, JsonObject authenticationInfo) {
    LOGGER.trace("validateAccess() started");
    Promise<JsonObject> promise = Promise.promise();

    Method method = Method.valueOf(authenticationInfo.getString(METHOD));
    Api api = Api.fromEndpoint(authenticationInfo.getString(API_ENDPOINT));

    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);

    AuthorizationStatergy authStrategy = AuthorizationContextFactory.create(jwtData.getRole());
    LOGGER.debug("strategy: " + authStrategy.getClass().getSimpleName());

    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    LOGGER.debug("endpoint: " + authenticationInfo.getString(API_ENDPOINT));

    if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
      LOGGER.debug("User access is allowed");
      JsonObject response = new JsonObject();

      response.put(USER_ROLE, jwtData.getRole()).put(USER_ID, jwtData.getSub());
      response.put(EXPIRY,jwtData.getExp());
      if (jwtData.getRole().equalsIgnoreCase(ADMIN)) {
        response.put(IID, "admin");
      } else {
          response.put(IID, (jwtData.getIid().split(":")[1]));
      }

      // Add provider user's id for provider APIs
      if(jwtData.getRole().equalsIgnoreCase("provider")) {
        response.put(PROVIDER_ID, jwtData.getSub());
      } else if(jwtData.getDrl().equalsIgnoreCase("provider")) {
        response.put(PROVIDER_ID, jwtData.getDid());
      }

      promise.complete(response);
    } else {
      LOGGER.error("user access denied");
      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
      promise.fail(result.toString());
    }
    return promise.future();
  }

  // class to contain intermediate data for token introspection
  final class ResultContainer {
    JwtData jwtData;
  }
}
