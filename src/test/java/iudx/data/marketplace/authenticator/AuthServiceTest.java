package iudx.data.marketplace.authenticator;

import static iudx.data.marketplace.common.Constants.PROVIDER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.configuration.Configuration;
import iudx.data.marketplace.authenticator.authorization.Method;
import iudx.data.marketplace.authenticator.model.JwtData;
import iudx.data.marketplace.common.CatalogueService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

  private static Configuration configuration;
  private static AuthenticationServiceImpl authenticationServiceImpl;
  private static CatalogueService catService;
  private static iudx.data.marketplace.common.Api api;

  @BeforeAll
  @DisplayName("Setup")
  public static void setup(Vertx vertx, VertxTestContext testContext) {
    api = Api.getInstance("basePath");
    configuration = new Configuration();
    JsonObject config = configuration.configLoader(1, vertx);
    config.put("catServerHost", "host");
    config.put("catServerPort", 1234);
    config.put("catItemPath", "/item");
    config.put("issuer", "cos.iudx.io");
    config.put("catRelPath", "/relationship");
    JWTAuthOptions jwtAuthOptions =
        new JWTAuthOptions()
            .addPubSecKey(
                new PubSecKeyOptions().setAlgorithm("ES256").setBuffer(config.getString("pubKey")));
    jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);
    JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
    catService = mock(CatalogueService.class);

    authenticationServiceImpl = new AuthenticationServiceImpl(vertx, jwtAuth, config, api);
    testContext.completeNow();
  }

  JsonObject authInfo() {
    JsonObject authInfo =
        new JsonObject()
            .put("token", JwtHelper.providerToken)
            .put("apiEndpoint", api.getProviderProductPath())
            .put("method", Method.POST);
    return authInfo;
  }

  @Test
  @DisplayName("test token introspect - success")
  public void testTokenIntrospect(VertxTestContext testContext) {

    doAnswer(Answer -> Future.succeededFuture(new JsonObject().put("totalHits", 1)))
        .when(catService).searchApi(any());

    authenticationServiceImpl.tokenIntrospect(
        new JsonObject().put(PROVIDER_ID, "provider-id"),
        authInfo(),
        handler -> {
          if (handler.succeeded()) testContext.completeNow();
          else testContext.failNow("Token Introspect test failed : " + handler.cause());
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

    authenticationServiceImpl
        .validateAccess(jwtData, authInfo())
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow("validateAccess passing for wrong values");
              } else {
                testContext.completeNow();
              }
            });
  }
}
