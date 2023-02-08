package iudx.data.marketplace.apiserver.handlers;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.ResponseUrn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class ExceptionHandlerTest {

  DxRuntimeException dxRuntimeException;
  DecodeException decodeException;
  ClassCastException classCastException;
  RuntimeException runtimeException;
  RoutingContext routingContext;
  HttpServerResponse httpServerResponse;
  HttpServerRequest httpServerRequest;
  ResponseUrn responseUrn;
  Future<Void> voidFuture;
  Throwable throwable;

  @BeforeEach
  public void setup(VertxTestContext testContext) {
    dxRuntimeException = mock(DxRuntimeException.class);
    decodeException = mock(DecodeException.class);
    classCastException = mock(ClassCastException.class);
    runtimeException = mock(RuntimeException.class);
    routingContext = mock(RoutingContext.class);
    httpServerResponse = mock(HttpServerResponse.class);
    httpServerRequest = mock(HttpServerRequest.class);
    responseUrn = mock(ResponseUrn.class);
    voidFuture = mock(Future.class);
    throwable = mock(Throwable.class);
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test Handle method - dx runtime exp")
  public void testHandleMethod(VertxTestContext testContext) {
    when(routingContext.failure()).thenReturn(dxRuntimeException);
    when(dxRuntimeException.getUrn()).thenReturn(responseUrn);
    when(responseUrn.getUrn()).thenReturn("urn");
    when(dxRuntimeException.getMessage()).thenReturn("dx runtime exp");
    when(dxRuntimeException.getStatusCode()).thenReturn(400);
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);

    ExceptionHandler exceptionHandler = new ExceptionHandler();
    exceptionHandler.handle(routingContext);

    verify(routingContext, times(1)).response();
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).setStatusCode(400);
    verify(httpServerResponse, times(1)).end(anyString());
    assertEquals("dx runtime exp", dxRuntimeException.getMessage());
    assertEquals("urn", dxRuntimeException.getUrn().getUrn());
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test Handle Method - decode exp")
  public void testHandleMethodForDecode(VertxTestContext testContext) {
    String path = "/provider";
    when(routingContext.failure()).thenReturn(decodeException);
    when(decodeException.getLocalizedMessage()).thenReturn("json is invalid");
    when(routingContext.request()).thenReturn(httpServerRequest);
    when(httpServerRequest.uri()).thenReturn(path);
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    exceptionHandler.handle(routingContext);
    verify(routingContext, times(1)).response();
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).setStatusCode(400);
    verify(httpServerResponse, times(1)).end(anyString());
    assertEquals(path, routingContext.request().uri());
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test Handle Method - class cast exp")
  public void testHandleMethodForClassCast(VertxTestContext testContext) {
    when(routingContext.failure()).thenReturn(classCastException);
    when(classCastException.getLocalizedMessage()).thenReturn("invalid payload");
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    exceptionHandler.handle(routingContext);
    verify(routingContext, times(1)).response();
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).setStatusCode(400);
    verify(httpServerResponse, times(1)).end(anyString());
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test Handle Method - runtime exp")
  public void testHandleMethodForRuntimeExp(VertxTestContext testContext) {
    when(routingContext.failure()).thenReturn(runtimeException);
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    exceptionHandler.handle(routingContext);
    verify(routingContext, times(1)).response();
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).setStatusCode(400);
    verify(httpServerResponse, times(1)).end(anyString());
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test Handle Method - exception")
  public void testHandleMethodForException(VertxTestContext testContext) {
    when(routingContext.failure()).thenReturn(new Exception());
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    exceptionHandler.handle(routingContext);
    verify(routingContext, times(1)).response();
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).setStatusCode(500);
    verify(httpServerResponse, times(1)).end(anyString());
    testContext.completeNow();
  }
}
