package iudx.data.marketplace.apiserver.handlers;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Future;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.authenticator.AuthClient;
import iudx.data.marketplace.authenticator.AuthenticationService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.postgres.PostgresServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;


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

  @BeforeEach
  public void setup(VertxTestContext testContext) {
    api = Api.getInstance("some/base/path");
    authHandler = AuthHandler.create(authenticationService, api, postgresService, authClient);
    ctx = mock(RoutingContext.class);
    req = mock(HttpServerRequest.class);
    map = mock(MultiMap.class);
    httpMethod = mock(HttpMethod.class);
    httpServerResponse = mock(HttpServerResponse.class);
    asyncResult = mock(AsyncResult.class);
    jsonObject = mock(JsonObject.class);
    requestBody = mock(RequestBody.class);
    throwable = mock(Throwable.class);
    AuthHandler.authenticator = mock(AuthenticationService.class);
    testContext.completeNow();
  }
  @Test
  @DisplayName("Test create method")
  public void testAuthHandlerCreateMethod(VertxTestContext testContext) {
    authenticator = mock(AuthenticationService.class);
    assertNotNull(AuthHandler.create(authenticationService, api, postgresService, authClient));
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
    }).when(AuthHandler.authenticator).tokenIntrospect(any(), any(), any());

    authHandler.handle(ctx);

    assertEquals("/provider/product", ctx.request().path());
    assertEquals("token", ctx.request().headers().get(HEADER_TOKEN));
    assertEquals("POST", ctx.request().method().toString());
    verify(AuthHandler.authenticator, times(1)).tokenIntrospect(any(), any(), any());
    verify(jsonObject, times(3)).getValue(anyString());

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
    }).when(AuthHandler.authenticator).tokenIntrospect(any(), any(), any());

    authHandler.handle(ctx);

    assertEquals("/provider/product", ctx.request().path());
    assertEquals("token", ctx.request().headers().get(HEADER_TOKEN));
    assertEquals("POST", ctx.request().method().toString());
    verify(AuthHandler.authenticator, times(1)).tokenIntrospect(any(), any(), any());
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
    }).when(AuthHandler.authenticator).tokenIntrospect(any(), any(), any());

    authHandler.handle(ctx);

    assertEquals("/provider/product", ctx.request().path());
    assertEquals("token", ctx.request().headers().get(HEADER_TOKEN));
    assertEquals("POST", ctx.request().method().toString());
    verify(AuthHandler.authenticator, times(1)).tokenIntrospect(any(), any(), any());
    verify(jsonObject, times(0)).getValue(anyString());
    verify(httpServerResponse, times(1)).setStatusCode(anyInt());
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).end(anyString());

    testContext.completeNow();
  }
}
