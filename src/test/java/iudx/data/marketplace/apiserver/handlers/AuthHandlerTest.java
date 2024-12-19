package iudx.data.marketplace.apiserver.handlers;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Future;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.Util;
import iudx.data.marketplace.aaaService.AuthClient;
import iudx.data.marketplace.authenticator.AuthenticationService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.Map;
import java.util.stream.Stream;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

//TODO: Fix the unit tests after refactoring
@Disabled
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class AuthHandlerTest {

  AuthenticationService authenticator;
  AuthHandler authHandler;
  RoutingContext ctx;
  HttpServerRequest req;
  MultiMap map;
  HttpMethod httpMethod;
  HttpServerResponse httpServerResponse;
  AsyncResult<JsonObject> asyncResult;
  JsonObject jsonObject;
  RequestBody requestBody;
  Throwable throwable;
  Future<Void> voidFuture;
  Api api;
  @Mock
  PostgresServiceImpl postgresService;
  @Mock
  AuthClient authClient;
  @Mock
  AuthenticationService authenticationService;
  @Mock JsonObject jsonObjectMock;
  String userId;
  String email;
  String firstName;
  String lastName;


  @BeforeEach
  public void setup(VertxTestContext testContext) {
    api = Api.getInstance("some/base/path");
    authHandler = new AuthHandler(authenticationService);
    ctx = mock(RoutingContext.class);
    req = mock(HttpServerRequest.class);
    map = mock(MultiMap.class);
    httpMethod = mock(HttpMethod.class);
    httpServerResponse = mock(HttpServerResponse.class);
    asyncResult = mock(AsyncResult.class);
    jsonObject = mock(JsonObject.class);
    requestBody = mock(RequestBody.class);
    throwable = mock(Throwable.class);
    userId = Util.generateRandomUuid().toString();
    email = Util.generateRandomEmailId();
    firstName = Util.generateRandomString();
    lastName = Util.generateRandomString();

    testContext.completeNow();
  }
  @Test
  @DisplayName("Test create method")
  public void testAuthHandlerCreateMethod(VertxTestContext testContext) {
    authenticator = mock(AuthenticationService.class);
    assertNotNull(authHandler);
    testContext.completeNow();
  }

  @Test
  @DisplayName("Testing handle method - success")
  public void testHandleMethod(VertxTestContext testContext) {

    when(ctx.request()).thenReturn(req);
    when(requestBody.asJsonObject()).thenReturn(jsonObject);
    when(ctx.body()).thenReturn(requestBody);
    when(ctx.body().asJsonObject()).thenReturn(jsonObject);
    when(ctx.request().method()).thenReturn(httpMethod);
    when(req.headers()).thenReturn(map);
    when(map.get(anyString())).thenReturn("token");
    when(req.path()).thenReturn("/provider/product");
    when(ctx.request().method().toString()).thenReturn("POST");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObject);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
        return null;
      }
    }).when(authenticator).tokenIntrospect(any());

    authHandler.handle(ctx);

    assertEquals("/provider/product", ctx.request().path());
    assertEquals("token", ctx.request().headers().get(HEADER_TOKEN));
    assertEquals("POST", ctx.request().method().toString());
    verify(authenticator, times(1)).tokenIntrospect(any());
    verify(jsonObject, times(1)).getString(anyString());

    testContext.completeNow();
  }

  @Test
  @DisplayName("Testing handle method - auth failure")
  public void authFailureTestHandleMethod(VertxTestContext testContext) {
    when(ctx.request()).thenReturn(req);
    when(requestBody.asJsonObject()).thenReturn(jsonObject);
    when(ctx.body()).thenReturn(requestBody);
    when(ctx.body().asJsonObject()).thenReturn(jsonObject);
    when(ctx.request().method()).thenReturn(httpMethod);
    when(req.headers()).thenReturn(map);
    when(map.get(anyString())).thenReturn("token");
    when(req.path()).thenReturn("/provider/product");
    when(ctx.request().method().toString()).thenReturn("POST");
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(asyncResult.cause().getMessage()).thenReturn("invalid token");
    when(ctx.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(401)).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
        return null;
      }
    }).when(authenticator).tokenIntrospect(any());

    authHandler.handle(ctx);

    assertEquals("/provider/product", ctx.request().path());
    assertEquals("token", ctx.request().headers().get(HEADER_TOKEN));
    assertEquals("POST", ctx.request().method().toString());
    verify(authenticator, times(1)).tokenIntrospect(any());
    verify(jsonObject, times(0)).getValue(anyString());
    verify(httpServerResponse, times(1)).setStatusCode(anyInt());
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).end(anyString());

    testContext.completeNow();
  }

  @Test
  @DisplayName("Testing handle method - item nf failure")
  public void itemNfFailureTestHandleMethod(VertxTestContext testContext) {
    when(ctx.request()).thenReturn(req);
    when(requestBody.asJsonObject()).thenReturn(jsonObject);
    when(ctx.body()).thenReturn(requestBody);
    when(ctx.body().asJsonObject()).thenReturn(jsonObject);
    when(ctx.request().method()).thenReturn(httpMethod);
    when(req.headers()).thenReturn(map);
    when(map.get(anyString())).thenReturn("token");
    when(req.path()).thenReturn("/provider/product");
    when(ctx.request().method().toString()).thenReturn("POST");
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(asyncResult.cause().getMessage()).thenReturn("Not Found");
    when(ctx.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(404)).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
        return null;
      }
    }).when(authenticator).tokenIntrospect(any());

    authHandler.handle(ctx);

    assertEquals("/provider/product", ctx.request().path());
    assertEquals("token", ctx.request().headers().get(HEADER_TOKEN));
    assertEquals("POST", ctx.request().method().toString());
    verify(authenticator, times(1)).tokenIntrospect(any());
    verify(jsonObject, times(0)).getValue(anyString());
    verify(httpServerResponse, times(1)).setStatusCode(anyInt());
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).end(anyString());

    testContext.completeNow();
  }


  public static Stream<Arguments> urls() {
    String dxApiBasePath = "/dummy/path";
    Api apis = Api.getInstance(dxApiBasePath);
    return Stream.of(
            Arguments.of(apis.getConsumerListResourcePath(), apis.getConsumerListResourcePath()),
            Arguments.of(apis.getConsumerProductVariantPath(), apis.getConsumerProductVariantPath()),
            Arguments.of(apis.getVerifyPaymentApi(), apis.getVerifyPaymentApi()),
            Arguments.of(apis.getConsumerOrderApi(), apis.getConsumerOrderApi()),
            Arguments.of(apis.getLinkedAccountService(), apis.getLinkedAccountService()),
            Arguments.of(apis.getConsumerListProducts(), apis.getConsumerListProducts()),
            Arguments.of(apis.getConsumerListPurchases(), apis.getConsumerListPurchases()),
            Arguments.of(apis.getConsumerListProviders(), apis.getConsumerListProviders()),
            Arguments.of(apis.getConsumerListDatasets(), apis.getConsumerListDatasets()),
            Arguments.of(apis.getProductUserMapsPath(), apis.getProductUserMapsPath()),
            Arguments.of(apis.getProviderProductVariantPath(), apis.getProviderProductVariantPath()),
            Arguments.of(apis.getProviderListPurchasesPath(), apis.getProviderListPurchasesPath()),
            Arguments.of(apis.getProviderListProductsPath(), apis.getProviderListProductsPath()),
            Arguments.of(apis.getConsumerListResourcePath(), apis.getConsumerListResourcePath()),
            Arguments.of(apis.getProviderProductPath(), apis.getProviderProductPath()),
            Arguments.of(apis.getPoliciesUrl(), apis.getPoliciesUrl()));
  }


  @ParameterizedTest(name = "{index}) url = {0}, path = {1}")
  @MethodSource("urls")
  @DisplayName("Test handler for succeeded authHandler")
  public void testCanHandleSuccess(String url, String path, VertxTestContext vertxTestContext) {

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
    JsonObject jsonObject = mock(JsonObject.class);
    Map dummyMap = mock(Map.class);
    HttpMethod method = mock(HttpMethod.class);
    RequestBody requestBody = mock(RequestBody.class);

    when(routingContext.pathParams()).thenReturn(dummyMap);
    when(dummyMap.containsKey(anyString())).thenReturn(false);
    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(jsonObject);
    when(routingContext.body().asJsonObject()).thenReturn(jsonObject);
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(httpServerRequest.method()).thenReturn(method);
    when(method.toString()).thenReturn("someMethod");
    when(httpServerRequest.path()).thenReturn(url);
    when(httpServerRequest.headers()).thenReturn(map);
    when(map.get(anyString())).thenReturn("Dummy Token");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObject);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
        return null;
      }
    }).when(authenticator).tokenIntrospect(any());

    authHandler.handle(routingContext);

    assertEquals(path, routingContext.request().path());
    assertEquals("Dummy Token", routingContext.request().headers().get(HEADER_TOKEN));
    assertEquals("someMethod", routingContext.request().method().toString());
    verify(authenticator, times(1)).tokenIntrospect(any());
    verify(routingContext, times(2)).body();

    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test handle method for verify api")
  public void testHandleForVerifyPolicy(VertxTestContext vertxTestContext)
  {
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
    JsonObject jsonObject = mock(JsonObject.class);
    Map dummyMap = mock(Map.class);
    HttpMethod method = mock(HttpMethod.class);
    RequestBody requestBody = mock(RequestBody.class);

    when(routingContext.pathParams()).thenReturn(dummyMap);
    when(dummyMap.containsKey(anyString())).thenReturn(false);
    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(jsonObject);
    when(routingContext.body().asJsonObject()).thenReturn(jsonObject);
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(httpServerRequest.method()).thenReturn(method);
    when(method.toString()).thenReturn("someMethod");
    when(httpServerRequest.path()).thenReturn(api.getVerifyUrl());
    when(httpServerRequest.headers()).thenReturn(map);
    when(map.get(anyString())).thenReturn("Dummy Token");
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(authenticator).tokenIntrospect4Verify(any());

    authHandler.handle(routingContext);

    assertEquals(api.getVerifyUrl(), routingContext.request().path());
    assertEquals("Dummy Token", routingContext.request().headers().get(HEADER_TOKEN));
    assertEquals("someMethod", routingContext.request().method().toString());
    verify(authenticator, times(1)).tokenIntrospect4Verify(any());
    verify(routingContext, times(2)).body();

    vertxTestContext.completeNow();
  }

  public User getUser() {
    JsonObject jsonObject =
            new JsonObject()
                    .put("userId", userId)
                    .put("userRole", "provider")
                    .put("emailId", email)
                    .put("firstName", firstName)
                    .put("resourceServerUrl", "rs.iudx.io")
                    .put("lastName", lastName);
    return new User(jsonObject);
  }

public void mockDbExecution()
{
  lenient()
          .doAnswer(
                  new Answer<AsyncResult<JsonObject>>() {
                    @Override
                    public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                      ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                      return null;
                    }
                  })
          .when(postgresService)
          .executePreparedQuery(anyString(), any(),any());
}
}
