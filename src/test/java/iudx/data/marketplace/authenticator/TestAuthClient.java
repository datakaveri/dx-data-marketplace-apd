package iudx.data.marketplace.authenticator;

import io.vertx.core.buffer.Buffer;
import iudx.data.marketplace.aaaService.AuthClient;
import iudx.data.marketplace.apiserver.handlers.AuthHandler;
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.authenticator.model.UserInfo;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.policies.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.mockito.Mock;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.data.marketplace.apiserver.util.Constants.ROLE;
import static iudx.data.marketplace.product.util.Constants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

//TODO: Fix the unit tests after refactoring
@Disabled
@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestAuthClient {
  private static final Logger LOGGER = LogManager.getLogger(TestAuthClient.class);

  @Mock HttpRequest httpServerRequest;
  @Mock MultiMap multiMapMock;
  @Mock HttpMethod httpMethod;
  @Mock Future<HttpResponse<Buffer>> future;
  @Mock Future<Void> voidFuture;
  @Mock Void aVoid;
  @Mock AsyncResult<Void> voidAsyncResult;
  @Mock Throwable throwable;
  @Mock HttpResponse httpServerResponse;
  @Mock PgPool pgPool;
  @Mock HttpRequest<io.vertx.core.buffer.Buffer> bufferHttpRequest;
  @Mock HttpResponse<io.vertx.core.buffer.Buffer> bufferHttpResponse;
  @Mock Future<HttpResponse<io.vertx.core.buffer.Buffer>> httpResponseFuture;
  @Mock WebClient webClient;
  @Mock AsyncResult<HttpResponse<Buffer>> asyncResult;
  @Mock JsonObject jsonObjectMock;
  private AuthHandler authHandler;
  private JsonObject config;
  private Api api;
  private AuthenticationService authenticationService;
  private AuthClient client;
  private RoutingContext routingContext;
  private String emailId;
  private String firstName;
  private String lastName;
@Mock
  UserInfo userInfo;

  @BeforeEach
  public void init(VertxTestContext vertxTestContext) {
    when(jsonObjectMock.getString(anyString())).thenReturn("dummyValue");
    when(jsonObjectMock.getInteger(anyString())).thenReturn(443);
    client = new AuthClient(jsonObjectMock, webClient);

    lenient().doAnswer(
                    new Answer<AsyncResult<HttpResponse<Buffer>>>() {
                        @Override
                        public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg1)
                                throws Throwable {
                            ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg1.getArgument(0))
                                    .handle(asyncResult);
                            return null;
                        }
                    })
            .when(future)
            .onComplete(any());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test fetchUserInfo method : Success")
  public void testFetchUserInfo(VertxTestContext vertxTestContext) {
    when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(httpServerRequest);
    when(httpServerRequest.putHeader(anyString(), anyString())).thenReturn(httpServerRequest);
    when(httpServerRequest.addQueryParam(anyString(), anyString())).thenReturn(httpServerRequest);
    when(httpServerRequest.send()).thenReturn(future);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(bufferHttpResponse);
    when(bufferHttpResponse.bodyAsJsonObject()).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getString(TYPE)).thenReturn("urn:dx:as:Success");
    when(jsonObjectMock.getJsonObject(anyString())).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getString(ROLE)).thenReturn(Role.CONSUMER.getRole());


    client
        .fetchUserInfo(userInfo)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                  User actualUser = handler.result();
                  assertNotNull(handler.result());
                  assertEquals("dummyValue", actualUser.getEmailId());
                  assertEquals("dummyValue", actualUser.getFirstName());
                  assertEquals("dummyValue", actualUser.getLastName());
                  assertEquals("dummyValue", actualUser.getResourceServerUrl());
                  assertEquals("dummyValue", actualUser.getUserId());
                  assertEquals(Role.CONSUMER.getRole(), actualUser.getUserRole().getRole());
                  vertxTestContext.completeNow();

              } else {

                  vertxTestContext.failNow("Failed to fetch user details");
              }
            });
  }

    @Test
    @DisplayName("Test fetchUserInfo method when user info from auth contains null value : Failure")
    public void testFetchUserInfoWhenLastNameIsNull(VertxTestContext vertxTestContext) {
        when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(httpServerRequest);
        when(httpServerRequest.putHeader(anyString(), anyString())).thenReturn(httpServerRequest);
        when(httpServerRequest.addQueryParam(anyString(), anyString())).thenReturn(httpServerRequest);
        when(httpServerRequest.send()).thenReturn(future);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.bodyAsJsonObject()).thenReturn(jsonObjectMock);
        when(jsonObjectMock.getString(TYPE)).thenReturn("urn:dx:as:Success");
        when(jsonObjectMock.getJsonObject(anyString())).thenReturn(jsonObjectMock);
        when(jsonObjectMock.getString(ROLE)).thenReturn(Role.CONSUMER.getRole());
        when(jsonObjectMock.getString("lastName")).thenReturn(null);

        client
                .fetchUserInfo(userInfo)
                .onComplete(
                        handler -> {
                            if (handler.failed()) {
                                assertEquals("User information is invalid", handler.cause().getMessage());
                                vertxTestContext.completeNow();

                            } else {

                                vertxTestContext.failNow("Succeeded when user details fetched from auth contains null values");
                            }
                        });
    }

    @Test
    @DisplayName("Test fetchUserInfo method when fetch from auth is not successful or user not present in auth: Failure")
    public void testFetchUserInfoWhenFetchFromAuthFailed(VertxTestContext vertxTestContext) {
        when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(httpServerRequest);
        when(httpServerRequest.putHeader(anyString(), anyString())).thenReturn(httpServerRequest);
        when(httpServerRequest.addQueryParam(anyString(), anyString())).thenReturn(httpServerRequest);
        when(httpServerRequest.send()).thenReturn(future);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.bodyAsJsonObject()).thenReturn(jsonObjectMock);
        when(jsonObjectMock.getString(TYPE)).thenReturn("urn:dx:as:badRequest");
        when(jsonObjectMock.getString(ROLE)).thenReturn(Role.CONSUMER.getRole());

        client
                .fetchUserInfo(userInfo)
                .onComplete(
                        handler -> {
                            if (handler.failed()) {
                                assertEquals("User not present in Auth.", handler.cause().getMessage());
                                vertxTestContext.completeNow();

                            } else {

                                vertxTestContext.failNow("Succeeded when user details fetched from auth failed or user not present in auth");
                            }
                        });
    }

    @Test
    @DisplayName("Test fetchUserInfo method when auth handler fails: Failure")
    public void testFetchUserInfoFailure(VertxTestContext vertxTestContext) {
        when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(httpServerRequest);
        when(httpServerRequest.putHeader(anyString(), anyString())).thenReturn(httpServerRequest);
        when(httpServerRequest.addQueryParam(anyString(), anyString())).thenReturn(httpServerRequest);
        when(httpServerRequest.send()).thenReturn(future);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("Some dummy failure message");
         when(jsonObjectMock.getString(ROLE)).thenReturn(Role.CONSUMER.getRole());

        client
                .fetchUserInfo(userInfo)
                .onComplete(
                        handler -> {
                            if (handler.failed()) {
                                assertEquals("Internal Server Error", handler.cause().getMessage());
                                vertxTestContext.completeNow();

                            } else {

                                vertxTestContext.failNow("Succeeded when auth handler failed");
                            }
                        });
    }
}
