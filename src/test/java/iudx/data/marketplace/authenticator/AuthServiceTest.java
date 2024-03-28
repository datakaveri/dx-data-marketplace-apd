package iudx.data.marketplace.authenticator;

import static iudx.data.marketplace.authenticator.util.Constants.TOKEN;
import static iudx.data.marketplace.common.Constants.PROVIDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
   static JsonObject config;

  @BeforeAll
  @DisplayName("Setup")
  public static void setup(Vertx vertx, VertxTestContext testContext) {
    api = Api.getInstance("basePath");
    configuration = new Configuration();
    config = configuration.configLoader(1, vertx);
    config.put("catServerHost", "host");
    config.put("catServerPort", 1234);
    config.put("catItemPath", "/item");
    config.put("issuer", "cos.iudx.io");
    config.put("apdURL", "rs-test-pm.iudx.io");
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

  @Test
  @DisplayName("Test tokenIntrospect4Verify method : Success")
  public void testTokenIntrospectForVerifySuccess(VertxTestContext vertxTestContext)
  {
    JsonObject authInfo = new JsonObject()
            .put(TOKEN, JwtHelper.providerToken);
    authenticationServiceImpl.tokenIntrospect4Verify(
        authInfo,
        handler -> {
          if (handler.succeeded()) {
            assertNull(handler.result());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("token verification failed");
          }
        });
  }

    @Test
    @DisplayName("Test tokenIntrospect4Verify method : Failure")
    public void testTokenIntrospectForVerifyFailure(Vertx vertx, VertxTestContext vertxTestContext)
    {
        JsonObject config = new JsonObject();
        config = configuration.configLoader(1, vertx);
        config.put("catServerHost", "host");
        config.put("catServerPort", 1234);
        config.put("catItemPath", "/item");
        config.put("issuer", "cos.iudx.io");
        config.put("apdURL", "rs.iudx.io");
        config.put("catRelPath", "/relationship");
        JWTAuthOptions jwtAuthOptions =
                new JWTAuthOptions()
                        .addPubSecKey(
                                new PubSecKeyOptions().setAlgorithm("ES256").setBuffer(config.getString("pubKey")));
        jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);
        JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
        catService = mock(CatalogueService.class);

        AuthenticationServiceImpl authenticationServiceImpl = new AuthenticationServiceImpl(vertx, jwtAuth, config, api);

        JsonObject authInfo = new JsonObject()
                .put(TOKEN, JwtHelper.providerToken);
    authenticationServiceImpl.tokenIntrospect4Verify(
        authInfo,
        handler -> {
          System.out.println(handler);
          if (handler.failed()) {
              assertEquals("Incorrect subject value in JWT", handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Succeeded with invalid jwt token");
          }
        });
    }

  @Test
  @DisplayName("Test validateJwtAccess method with incorrect issuer : Failure")
  public void testValidateJwtAccessWithIncorrectIssuer(VertxTestContext vertxTestContext) {
    JwtData jwtData = mock(JwtData.class);
    when(jwtData.getIss()).thenReturn("someIssuer");

    authenticationServiceImpl
        .validateJwtAccess(jwtData)
        .onComplete(
            handler -> {
              System.out.println(handler);
              if (handler.failed()) {
                assertEquals("Incorrect issuer value in JWT", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Succeeded for incorrect issuer");
              }
            });
  }

    @Test
    @DisplayName("Test validateJwtAccess method with incorrect audience : Failure")
    public void testValidateJwtAccessWithIncorrectAudience(VertxTestContext vertxTestContext) {
        JwtData jwtData = mock(JwtData.class);
        when(jwtData.getIss()).thenReturn("cos.iudx.io");
        when(jwtData.getAud()).thenReturn("abcd:someAudience");
        when(jwtData.getIid()).thenReturn("someValue:abcd");

        authenticationServiceImpl
                .validateJwtAccess(jwtData)
                .onComplete(
                        handler -> {
                            if (handler.failed()) {
                                assertEquals("Incorrect audience value in JWT", handler.cause().getMessage());
                                vertxTestContext.completeNow();
                            } else {
                                vertxTestContext.failNow("Succeeded for incorrect issuer");
                            }
                        });
    }

    @Test
    @DisplayName("Test validateJwtAccess method with null audience : Failure")
    public void testValidateJwtAccessWithNullAudience(VertxTestContext vertxTestContext) {
        JwtData jwtData = mock(JwtData.class);
        when(jwtData.getIss()).thenReturn("cos.iudx.io");
        when(jwtData.getAud()).thenReturn(null);

        authenticationServiceImpl
                .validateJwtAccess(jwtData)
                .onComplete(
                        handler -> {
                            if (handler.failed()) {
                                assertEquals("No audience value in JWT", handler.cause().getMessage());
                                vertxTestContext.completeNow();
                            } else {
                                vertxTestContext.failNow("Succeeded for incorrect issuer");
                            }
                        });
    }
}
