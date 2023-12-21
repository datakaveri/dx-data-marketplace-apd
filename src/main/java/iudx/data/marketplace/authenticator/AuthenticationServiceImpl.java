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
import iudx.data.marketplace.authenticator.authorization.*;
import iudx.data.marketplace.authenticator.model.JwtData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.data.marketplace.common.Api;

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

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.authenticator.authorization.IudxRole.DELEGATE;
import static iudx.data.marketplace.authenticator.util.Constants.*;

public class AuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(AuthenticationServiceImpl.class);

  final JWTAuth jwtAuth;
  final String audience;
  final String apdUrl;
  final String issuer;
  final Api apis;
  // TODO: Update the client credentials and APD url
  public AuthenticationServiceImpl(Vertx vertx, final JWTAuth jwtAuth, final JsonObject config, final Api apis) {
    this.jwtAuth = jwtAuth;
    this.issuer = config.getString("issuer");
    this.apdUrl = config.getString("apdURL");
    this.audience = config.getString("host");
    this.apis = apis;
  }

//  @Override
//  public AuthenticationService tokenIntrospect(
//      JsonObject request, JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {
//    String endPoint = authenticationInfo.getString(API_ENDPOINT);
//    String token = authenticationInfo.getString(TOKEN);
//    String method = authenticationInfo.getString(METHOD);
//
//    Future<JwtData> jwtDecodeFuture = decodeJwt(token);
//
//    ResultContainer result = new ResultContainer();
//
//    jwtDecodeFuture
//        .compose(
//            decodeHandler -> {
//              result.jwtData = decodeHandler;
//              return isValidAudienceValue(result.jwtData);
//            })
//        .compose(
//            audienceHandler -> {
//              return isValidEndpoint(endPoint);
//            })
//        .compose(
//            validEndpointHandler -> {
//              return validateAccess(result.jwtData, authenticationInfo);
//            })
//        .onComplete(
//            completeHandler -> {
//              if (completeHandler.succeeded()) {
//                handler.handle(Future.succeededFuture(completeHandler.result()));
//              } else {
//                handler.handle(Future.failedFuture(completeHandler.cause().getMessage()));
//              }
//            });
//    return this;
//  }
@Override
public AuthenticationService tokenIntrospect(JsonObject request, JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {
  String token = authenticationInfo.getString(TOKEN);
  ResultContainer resultContainer = new ResultContainer();

  Future<JwtData> jwtDecodeFuture = decodeJwt(token);
  jwtDecodeFuture
          .compose(
                  decodeHandler -> {
                    resultContainer.jwtData = decodeHandler;
                    return validateJwtAccess(resultContainer.jwtData);
                  })
          .compose(isValidJwtAccess -> validateAccess(resultContainer.jwtData, authenticationInfo))
          .onSuccess(successHandler -> handler.handle(Future.succeededFuture(successHandler)))
          .onFailure(
                  failureHandler -> {
                    LOGGER.error("error : " + failureHandler.getMessage());
                    handler.handle(Future.failedFuture(failureHandler.getMessage()));
                  });
  return this;
}
  Future<JsonObject> validateAccess(JwtData jwtData, JsonObject authInfo) {
    LOGGER.info("Authorization check started");
    Promise<JsonObject> promise = Promise.promise();
    Method method = Method.valueOf(authInfo.getString(API_METHOD));
    String api = authInfo.getString(API_ENDPOINT);
    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);

    // converts the delegate user to consumer or provider
    IudxRole role = IudxRole.fromRole(jwtData);

    AuthorizationStatergy authStrategy = AuthorizationContextFactory.create(role, apis);
    LOGGER.info("strategy : " + authStrategy.getClass().getSimpleName());

    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
      JsonObject jsonResponse = new JsonObject();
      boolean isDelegate = jwtData.getRole().equalsIgnoreCase(DELEGATE.getRole());
      jsonResponse.put(USERID, isDelegate ? jwtData.getDid() : jwtData.getSub());
      jsonResponse.put(IS_DELEGATE, isDelegate);
      jsonResponse.put(ROLE, role);
      jsonResponse.put(AUD, jwtData.getAud());
      promise.complete(jsonResponse);
    } else {
      LOGGER.info("Failed in authorization check.");
      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
      promise.fail(result.toString());
    }
    return promise.future();
  }

  public Future<Boolean> validateJwtAccess(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();
    if (!(jwtData.getIss() != null && issuer.equalsIgnoreCase(jwtData.getIss()))) {
      LOGGER.error("Incorrect issuer value in JWT");
      promise.fail("Incorrect issuer value in JWT");
    } else if (jwtData.getAud() == null) {
      LOGGER.error("No audience value in JWT");
      promise.fail("No audience value in JWT");
    } else if (!jwtData.getAud().equalsIgnoreCase(jwtData.getIid().split(":")[1])) {
      LOGGER.error("Incorrect audience value in JWT");
      promise.fail("Incorrect audience value in JWT");
    } else {
      promise.complete(true);
    }
    return promise.future();
  }



  @Override
  public AuthenticationService tokenIntrospect4Verify(
      JsonObject authInfo, Handler<AsyncResult<JsonObject>> handler) {
    String token = authInfo.getString(TOKEN);

    Future<JwtData> jwtDecodeFuture = decodeJwt(token);
    jwtDecodeFuture
        .onSuccess(
            jwtData -> {
              if (jwtData.getSub() == null) {
                LOGGER.error("No sub value in JWT");
                handler.handle(Future.failedFuture("No sub value in JWT"));
              } else if (!(jwtData.getIss() != null && issuer.equalsIgnoreCase(jwtData.getIss()))) {
                LOGGER.error("Incorrect issuer value in JWT");
                handler.handle(Future.failedFuture("Incorrect issuer value in JWT"));
              } else if (jwtData.getAud().isEmpty()) {
                LOGGER.error("No audience value in JWT");
                handler.handle(Future.failedFuture("No audience value in JWT"));
              } else if (!jwtData.getAud().equalsIgnoreCase(apdUrl)) {
                LOGGER.error("Incorrect audience value in JWT");
                handler.handle(Future.failedFuture("Incorrect subject value in JWT"));
              } else {
                LOGGER.info("Auth token verified");
                handler.handle(Future.succeededFuture());
              }
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("Failed to decode the token : {}", failureHandler.getMessage());
              handler.handle(Future.failedFuture(failureHandler.getMessage()));
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
              jwtData.setExp(user.get("exp"));
              jwtData.setIat(user.get("iat"));
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

//  Future<Boolean> isValidEndpoint(String endPoint) {
//    Promise<Boolean> promise = Promise.promise();
//
//    LOGGER.debug("Endpoint in JWT is : " + endPoint);
//
//    if (Api.fromEndpoint(endPoint) != null) {
//      promise.complete(true);
//    } else {
//      LOGGER.error("Incorrect endpoint in jwt");
//      promise.fail("Incorrect endpoint in jwt");
//    }
//    return promise.future();
//  }

  // class to contain intermediate data for token introspection
  final class ResultContainer {
    JwtData jwtData;
  }
//  public Future<JsonObject> validateAccess(JwtData jwtData, JsonObject authenticationInfo) {
//    LOGGER.trace("validateAccess() started");
//    Promise<JsonObject> promise = Promise.promise();
//
//    Method method = Method.valueOf(authenticationInfo.getString(METHOD));
//    Api api = Api.fromEndpoint(authenticationInfo.getString(API_ENDPOINT));
//
//    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);
//
//    AuthorizationStatergy authStrategy = AuthorizationContextFactory.create(jwtData.getRole());
//    LOGGER.debug("strategy: " + authStrategy.getClass().getSimpleName());
//
//    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
//    LOGGER.debug("endpoint: " + authenticationInfo.getString(API_ENDPOINT));
//
//    if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
//      LOGGER.debug("User access is allowed");
//      JsonObject response = new JsonObject();
//
//      response.put(USER_ROLE, jwtData.getRole()).put(USER_ID, jwtData.getSub());
//      if (jwtData.getRole().equalsIgnoreCase(ADMIN)) {
//        response.put(IID, "admin");
//      } else {
//        if (jwtData.getIid().contains("/")) {
//          response.put(
//              IID,
//              (jwtData.getIid().split(":")[1]).split("/")[0]
//                  + "/"
//                  + (jwtData.getIid().split(":")[1]).split("/")[1]);
//        } else {
//          response.put(IID, (jwtData.getIid().split(":")[1]));
//        }
//      }
//      promise.complete(response);
//    } else {
//      LOGGER.error("user access denied");
//      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
//      promise.fail(result.toString());
//    }
//    return promise.future();
//  }
}
