package iudx.data.marketplace.consumer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.postgres.PostgresService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.data.marketplace.common.Constants.RESOURCE_ID;
import static iudx.data.marketplace.common.Constants.PROVIDER_ID;
import static iudx.data.marketplace.consumer.util.Constants.TABLES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ConsumerServiceTest {

  @Mock AsyncResult<JsonObject> asyncResult;

  private static PostgresService postgresService;
  private static ConsumerServiceImpl consumerService;
  JsonObject request;
  JsonObject config;
  JsonArray tableArray;

  @BeforeEach
  public void setup(VertxTestContext testContext) {
    postgresService = mock(PostgresService.class);
    request = new JsonObject();
    tableArray = new JsonArray().add("table name").add("table name");
    config = new JsonObject().put(TABLES, tableArray);
    consumerService = new ConsumerServiceImpl(config, postgresService);
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test list Resources - Success ")
  public void testListResourcesSuccess(VertxTestContext testContext) {

    request.put(RESOURCE_ID, "iid");
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock)
                  throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(2))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executePreparedQuery(anyString(), any(), any());

    consumerService.listResources(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("create variant test failed");
          }
        });
  }

  @Test
  @DisplayName("Test list Resources - Failed")
  public void testListResourceFailed(VertxTestContext testContext) {
    request.put(PROVIDER_ID, "iid");
    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock)
                  throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(2))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executePreparedQuery(anyString(), any(), any());

    consumerService.listResources(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Test list Providers - Success ")
  public void testListProviders(VertxTestContext testContext) {

    request.put(PROVIDER_ID, "iid");
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock)
                  throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(2))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executePreparedQuery(anyString(), any(), any());

    consumerService.listProviders(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("create variant test failed");
          }
        });
  }

  @Test
  @DisplayName("Test list Providers - Failed")
  public void testListProvidersFailed2(VertxTestContext testContext) {

    when(asyncResult.succeeded()).thenReturn(false);
    consumerService = new ConsumerServiceImpl(config, postgresService);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock)
                  throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(2))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executePreparedQuery(anyString(), any(), any());

    consumerService.listProviders(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Test list Products - Success ")
  public void testListProducts(VertxTestContext testContext) {

    tableArray.add("table name");
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock)
                  throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(2))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executePreparedQuery(anyString(), any(), any());

    consumerService.listProducts(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("create variant test failed");
          }
        });
  }

  @Test
  @DisplayName("Test list Products - Failed")
  public void testListProductsFailed2(VertxTestContext testContext) {

    tableArray.add("table name");
    request.put(RESOURCE_ID, "did");
    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock)
                  throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(2))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executePreparedQuery(anyString(), any(), any());

    consumerService.listProducts(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Test list Products - Success when key is provider id")
  public void testListProductsProviderSuccess(VertxTestContext testContext) {

    tableArray.add("table name");
    request.put(PROVIDER_ID, "pid");
    when(asyncResult.succeeded()).thenReturn(true);
    consumerService = new ConsumerServiceImpl(config, postgresService);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock)
                  throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(2))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executePreparedQuery(anyString(), any(), any());

    consumerService.listProducts(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }
}
