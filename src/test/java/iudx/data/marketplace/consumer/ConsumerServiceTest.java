package iudx.data.marketplace.consumer;

import static iudx.data.marketplace.apiserver.util.Constants.DETAIL;
import static iudx.data.marketplace.apiserver.util.Constants.PRODUCT_VARIANT_ID;
import static iudx.data.marketplace.common.Constants.PROVIDER_ID;
import static iudx.data.marketplace.common.Constants.RESOURCE_ID;
import static iudx.data.marketplace.consumer.util.Constants.TABLES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.common.Util;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.razorpay.RazorPayService;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ConsumerServiceTest {

  private static PostgresService postgresService;
  private static ConsumerServiceImpl consumerService, consumerServiceSpy;
  @Mock AsyncResult<JsonObject> asyncResult;
  JsonObject request;
  JsonObject config;
  JsonArray tableArray;
  @Mock RazorPayService razorPayService;
  @Mock Util util;
  @Mock User consumer;

  private static Stream<Arguments> queryParams() {
    return Stream.of(
        Arguments.of(new JsonObject()),
        Arguments.of(new JsonObject().put("resourceId", "resource-id")),
        Arguments.of(new JsonObject().put("providerId", "provider-id")));
  }

  @BeforeEach
  public void setup(VertxTestContext testContext) {
    postgresService = mock(PostgresService.class);
    razorPayService = mock(RazorPayService.class);
    request = new JsonObject();
    tableArray =
        new JsonArray()
            .add("table name")
            .add("table name")
            .add("someDummyTable")
            .add("someDummyTableName");
    config = new JsonObject().put(TABLES, tableArray);
    consumerService = new ConsumerServiceImpl(config, postgresService, razorPayService, util);
    consumerServiceSpy = spy(consumerService);
    testContext.completeNow();
  }

  @ParameterizedTest
  @MethodSource("queryParams")
  @DisplayName("Test list Resources - Success ")
  public void testListResourcesSuccess(JsonObject requestParams, VertxTestContext testContext) {

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
        consumer,
        requestParams,
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
        consumer,
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

  @ParameterizedTest
  @MethodSource("queryParams")
  @DisplayName("Test list Providers - Success ")
  public void testListProviders(JsonObject requestParams, VertxTestContext testContext) {

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
        consumer,
        requestParams,
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
    consumerService = new ConsumerServiceImpl(config, postgresService, razorPayService, util);
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
        consumer,
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

  @ParameterizedTest
  @MethodSource("queryParams")
  @DisplayName("Test list Products - Success ")
  public void testListProducts(JsonObject requestParams, VertxTestContext testContext) {

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
        consumer,
        requestParams,
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
        consumer,
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
    consumerService = new ConsumerServiceImpl(config, postgresService, razorPayService, util);
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
        consumer,
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

  @Test
  @DisplayName("Test Create Order - Success")
  public void testCreateOrderSuccess(VertxTestContext testContext) {

    when(consumer.getResourceServerUrl()).thenReturn("rs.iudx.io");
    when(consumer.getUserId()).thenReturn("consumer-id");
    request.put(PRODUCT_VARIANT_ID, "pv-id");
    doAnswer(
            Answer ->
                Future.succeededFuture(
                    new JsonObject()
                        .put(
                            "results",
                            new JsonArray()
                                .add(new JsonObject().put("productVariantId", "pv-id")))))
        .when(consumerServiceSpy)
        .getOrderRelatedInfo(anyString());
    doAnswer(Answer -> Future.succeededFuture()).when(razorPayService).createOrder(any());
    doAnswer(Answer -> Future.succeededFuture(new JsonObject()))
        .when(consumerServiceSpy)
        .generateOrderEntry(any(), anyString(), anyString());

    consumerServiceSpy.createOrder(
        request,
        consumer,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(new JsonObject().put(DETAIL, "Order created successfully"), handler.result());
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Test Order Create - Get Order Related Info Failed")
  public void testOrderCreateRelatedInfoFailed(VertxTestContext testContext) {

    when(consumer.getResourceServerUrl()).thenReturn("rs.iudx.io");
    when(consumer.getUserId()).thenReturn("consumer-id");
    request.put(PRODUCT_VARIANT_ID, "pv-id");
    doAnswer(Answer -> Future.failedFuture("Expected Message")).when(consumerServiceSpy).getOrderRelatedInfo(anyString());

    consumerServiceSpy.createOrder(request, consumer, handler -> {
      if(handler.succeeded()) {
        testContext.failNow("Unexpected Behaviour");
      } else {
        assertEquals("Expected Message", handler.cause().getMessage());
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("Test Order Create - RazorPay create order Failed")
  public void testOrderCreateRazorpayFailed(VertxTestContext testContext) {

    when(consumer.getResourceServerUrl()).thenReturn("rs.iudx.io");
    when(consumer.getUserId()).thenReturn("consumer-id");
    request.put(PRODUCT_VARIANT_ID, "pv-id");
    doAnswer(
        Answer ->
            Future.succeededFuture(
                new JsonObject()
                    .put(
                        "results",
                        new JsonArray()
                            .add(new JsonObject().put("productVariantId", "pv-id")))))
        .when(consumerServiceSpy)
        .getOrderRelatedInfo(anyString());
    doAnswer(Answer -> Future.failedFuture("Expected Razorpay Message")).when(razorPayService).createOrder(any());

    consumerServiceSpy.createOrder(request, consumer, handler -> {
      if(handler.succeeded()) {
        testContext.failNow("Unexpected Behaviour");
      } else {
        assertEquals("Expected Razorpay Message", handler.cause().getMessage());
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("Test Order Create - Generate Order Entry Failed")
  public void testOrderCreateGenerateOrderFailed(VertxTestContext testContext) {

    when(consumer.getResourceServerUrl()).thenReturn("rs.iudx.io");
    when(consumer.getUserId()).thenReturn("consumer-id");
    request.put(PRODUCT_VARIANT_ID, "pv-id");
    doAnswer(
        Answer ->
            Future.succeededFuture(
                new JsonObject()
                    .put(
                        "results",
                        new JsonArray()
                            .add(new JsonObject().put("productVariantId", "pv-id")))))
        .when(consumerServiceSpy)
        .getOrderRelatedInfo(anyString());
    doAnswer(Answer -> Future.succeededFuture()).when(razorPayService).createOrder(any());
    doAnswer(Answer -> Future.failedFuture("Expected Postgres Message"))
        .when(consumerServiceSpy)
        .generateOrderEntry(any(), anyString(), anyString());

    consumerServiceSpy.createOrder(request, consumer, handler -> {
      if(handler.succeeded()) {
        testContext.failNow("Unexpected Behaviour");
      } else {
        assertEquals("Expected Postgres Message", handler.cause().getMessage());
        testContext.completeNow();
      }
    });
  }
}
