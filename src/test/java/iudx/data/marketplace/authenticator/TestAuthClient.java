package iudx.data.marketplace.authenticator;

import io.vertx.core.buffer.Buffer;
import iudx.data.marketplace.apiserver.handlers.AuthHandler;
import iudx.data.marketplace.common.Api;
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
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
@ExtendWith({VertxExtension.class, MockitoExtension.class})

public class TestAuthClient {
    @Mock
    HttpRequest httpServerRequest;
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
    WebClient webClient;
    @Mock AsyncResult<HttpResponse<Buffer>> asyncResult;
    private AuthHandler authHandler;
    private JsonObject config;
    private Api api;
    private AuthenticationService authenticationService;
    private AuthClient client;
    private RoutingContext routingContext;
    private String emailId;
    private String firstName;
    private String lastName;
}
