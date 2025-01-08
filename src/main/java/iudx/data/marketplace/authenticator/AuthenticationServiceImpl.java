package iudx.data.marketplace.authenticator;
import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.authenticator.util.Constants.*;
import static iudx.data.marketplace.common.Constants.APD_URL;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import iudx.data.marketplace.authenticator.model.JwtData;
import iudx.data.marketplace.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(AuthenticationServiceImpl.class);

  final JWTAuth jwtAuth;
  final String apdUrl;
  final String issuer;
  final Api apis;

  public AuthenticationServiceImpl(
      final JWTAuth jwtAuth, final JsonObject config, final Api apis) {
    this.jwtAuth = jwtAuth;
    this.issuer = config.getString("issuer");
    this.apdUrl = config.getString(APD_URL);
    this.apis = apis;
  }

  @Override
  public Future<JwtData> tokenIntrospect(JsonObject authenticationInfo) {
    Promise<JwtData> promise = Promise.promise();
    String token = authenticationInfo.getString(HEADER_TOKEN);
    ResultContainer resultContainer = new ResultContainer();
    /* token would can be of the type : Bearer <JWT-Token>, <JWT-Token> */
    /* allowing both the tokens to be authenticated for now */
    /* TODO: later, 401 error is thrown if the token does not contain Bearer keyword */
    boolean isItABearerToken = token.contains(HEADER_TOKEN_BEARER);
    if(isItABearerToken && token.trim().split(" ").length == 2)
    {
      String[] tokenWithoutBearer = token.split(HEADER_TOKEN_BEARER);
      token = tokenWithoutBearer[1].replaceAll("\\s", "");
    }
    Future<JwtData> jwtDecodeFuture = decodeJwt(token);

    Future<Boolean> validateJwtAccessFuture = jwtDecodeFuture
        .compose(
            decodeHandler -> {
              resultContainer.jwtData = decodeHandler;
              return validateJwtAccess(resultContainer.jwtData);
            });
    validateJwtAccessFuture
        .onSuccess(isValidJwt -> {
          promise.complete(resultContainer.jwtData);
        }).onFailure(failureHandler ->{
          LOGGER.error("error : " + failureHandler.getMessage());
          promise.fail(failureHandler.getLocalizedMessage());
        });

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
  public Future<Void> tokenIntrospect4Verify(
      JsonObject authInfo) {
    String token = authInfo.getString(TOKEN);
    Promise<Void> promise = Promise.promise();
    Future<JwtData> jwtDecodeFuture = decodeJwt(token);
    jwtDecodeFuture
        .onSuccess(
            jwtData -> {
              String errorMessage = validateJwt4Verify(jwtData);
              if (errorMessage != null) {
                LOGGER.error(errorMessage);
                promise.fail(errorMessage);
              } else {
                LOGGER.info("Auth token verified");
                promise.complete();
              }
            })
        .onFailure(
            failureHandler -> {
              String failure = String.format("Failed to decode the token: %s", failureHandler.getMessage());
              LOGGER.error(failure);
              promise.fail(failure);
            });
    return promise.future();
  }

  private String validateJwt4Verify(JwtData jwtData) {
    if (jwtData.getSub() == null) {
      return ("No sub value in JWT");
    } else if (!(jwtData.getIss() != null && issuer.equalsIgnoreCase(jwtData.getIss()))) {
      return ("Incorrect issuer value in JWT");
    } else if (jwtData.getAud().isEmpty()) {
      return ("No audience value in JWT");
    } else if (!jwtData.getAud().equalsIgnoreCase(apdUrl)) {
      return ("Incorrect audience value in JWT");
    } else {
      return null;
    }
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

  // class to contain intermediate data for token introspection
  final class ResultContainer {
    JwtData jwtData;
  }
}
