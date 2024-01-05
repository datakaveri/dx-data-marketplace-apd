package iudx.data.marketplace.authenticator;

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

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.apiserver.util.Constants.API_ENDPOINT;
import static iudx.data.marketplace.apiserver.util.Constants.METHOD;
import static iudx.data.marketplace.authenticator.authorization.IudxRole.DELEGATE;
import static iudx.data.marketplace.authenticator.util.Constants.*;

public class AuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(AuthenticationServiceImpl.class);

  final JWTAuth jwtAuth;
  final String audience;
  final String apdUrl;
  final String issuer;
  final Api apis;
  public AuthenticationServiceImpl(Vertx vertx, final JWTAuth jwtAuth, final JsonObject config, final Api apis) {
    this.jwtAuth = jwtAuth;
    this.issuer = config.getString("issuer");
    this.apdUrl = config.getString("apdURL");
    this.audience = config.getString("host");
    this.apis = apis;
  }

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
                      LOGGER.error("error : " + failureHandler.getCause());
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

    // converts the delegate user role to consumer or provider
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
      promise.fail("No access provided to endpoint");
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

  // class to contain intermediate data for token introspection
  final class ResultContainer {
    JwtData jwtData;
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



}
