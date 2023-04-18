package iudx.data.marketplace.authenticator;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.configuration.Configuration;
import iudx.data.marketplace.authenticator.authorization.Api;
import iudx.data.marketplace.authenticator.authorization.Method;
import iudx.data.marketplace.authenticator.model.JwtData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

  private static Configuration configuration;
  private static AuthenticationServiceImpl authenticationServiceImpl;

  @BeforeAll
  @DisplayName("Setup")
  public static void setup(Vertx vertx, VertxTestContext testContext) {
    configuration = new Configuration();
    JsonObject config = configuration.configLoader(1, vertx);
    JWTAuthOptions jwtAuthOptions =
        new JWTAuthOptions()
            .addPubSecKey(
                new PubSecKeyOptions().setAlgorithm("ES256").setBuffer(config.getString("pubKey")));
    jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);
    JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
    authenticationServiceImpl = new AuthenticationServiceImpl(vertx, jwtAuth, config);
    testContext.completeNow();
  }

  JsonObject authInfo() {
    JsonObject authInfo =
        new JsonObject()
            .put("token", JwtHelper.providerToken)
            .put("apiEndpoint", Api.PRODUCT.getEndpoint())
            .put("method", Method.POST);
    return authInfo;
  }

  @Test
  @DisplayName("test token introspect - success")
  public void testTokenIntrospect(VertxTestContext testContext) {

    authenticationServiceImpl.tokenIntrospect(
        new JsonObject(),
        authInfo(),
        handler -> {
          if (handler.succeeded()) testContext.completeNow();
          else testContext.failNow("Token Introspect test failed");
        });
  }

  @Test
  @DisplayName("test invalid token")
  public void testInvalidToken(VertxTestContext testContext) {
    JsonObject authInfo = authInfo();
    authInfo.put("token", JwtHelper.invalidToken);

    authenticationServiceImpl.tokenIntrospect(
        new JsonObject(),
        authInfo,
        handler -> {
          if (handler.succeeded()) testContext.failNow("invalid token is passing");
          else testContext.completeNow();
        });
  }

  @Test
  @DisplayName("test invalid audience in token")
  public void testInvalidAudience(VertxTestContext testContext) {
    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("abc.iudx.io1");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rs:rs.iudx.io");
    jwtData.setRole("provider");
    authenticationServiceImpl
        .isValidAudienceValue(jwtData)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                testContext.completeNow();
              } else {
                testContext.failNow("fail");
              }
            });
  }

  @Test
  @DisplayName("test invalid endpoint")
  public void testInvalidEndpoint(VertxTestContext testContext) {
    JsonObject authInfo = authInfo();
    authInfo.put("apiEndpoint", "/invalid");
    authenticationServiceImpl.tokenIntrospect(
        new JsonObject(),
        authInfo,
        handler -> {
          if (handler.succeeded()) testContext.failNow("invalid endpoint is passing");
          else testContext.completeNow();
        });
  }

  @Test
  @DisplayName("Test validate access - access denied")
  public void testValidateAccess(VertxTestContext testContext) {
    JwtData jwtData = new JwtData();
    jwtData.setAud("rs.iudx.io");
    jwtData.setIid("rs:rs.iudx.io");
    jwtData.setRole("consumer");

    authenticationServiceImpl.validateAccess(jwtData, authInfo()).onComplete(handler -> {
      if(handler.succeeded()) {
        testContext.failNow("validateAccess passing for wrong values");
      } else {
        testContext.completeNow();
      }
    });
  }
}
