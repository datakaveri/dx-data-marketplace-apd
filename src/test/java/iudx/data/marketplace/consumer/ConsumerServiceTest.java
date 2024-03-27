package iudx.data.marketplace.consumer;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.Constants.PROVIDER_ID;
import static iudx.data.marketplace.common.Constants.RESOURCE_ID;
import static iudx.data.marketplace.consumer.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
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
import org.junit.jupiter.params.provider.ValueSource;
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
  Util util;
  @Mock User consumer;
  @Mock Throwable throwable;
  String failureMessage;
  @Mock JsonObject jsonMock;

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
    util = new Util();
    failureMessage = "Some failure message";
    tableArray =
        new JsonArray()
            .add("table name")
            .add("table name")
            .add("someDummyTable")
            .add("someDummyTableName")
                .add("abcd")
                .add("efgh")
                .add("sometable")
                .add("tableName")
                .add("xyzTable");
    config = new JsonObject().put(TABLES, tableArray);
    consumerService = new ConsumerServiceImpl(config, postgresService, razorPayService, util);
    consumerServiceSpy = spy(consumerService);
      lenient()
              .doAnswer(
                      new Answer<AsyncResult<JsonObject>>() {
                          @Override
                          public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                              ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                              return null;
                          }
                      })
              .when(postgresService)
              .executeQuery(anyString(), any());
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
      lenient()
              .doAnswer(
                      new Answer<AsyncResult<JsonObject>>() {
                          @Override
                          public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                              ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                              return null;
                          }
                      })
              .when(postgresService)
              .executeTransaction(anyList(), any());
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

  @ParameterizedTest
  @ValueSource(strings = {"pending", "succeeded", "failed"})
  @DisplayName("Test listPurchase method : Success")
  public void testListPurchaseSuccess(String paymentStatus, VertxTestContext vertxTestContext) {
    JsonObject info =
        new JsonObject()
            .put("additionalInfo", "someAdditionalInfo")
            .put("providerEmailId", "dummyProviderEmailId")
            .put("providerFirstName", "dummyFirstName")
            .put("providerLastName", "dummyLastName")
            .put("providerId", "dummyProviderId")
            .put("productId", "dummyProductId")
            .put("productVariantId", "dummyProductVariantId")
            .put("resources", new JsonArray().add("resource1"))
            .put("productVariantName", "dummyProductVariantName")
            .put("price", 15f)
            .put("expiryInMonths", 12);

    JsonArray jsonArray = new JsonArray().add(info);

    request
        .put("resourceId", "dummyResourceId")
        .put("productId", "dummyProductId")
        .put("paymentStatus", paymentStatus)
        .put(RESULTS, jsonArray)
        .put("orderId", "dummyOrderId");

    when(consumer.getResourceServerUrl()).thenReturn("dummyRsUrl");
    when(consumer.getUserId()).thenReturn("someUserId");
    when(consumer.getUserRole()).thenReturn(Role.CONSUMER);
    when(consumer.getEmailId()).thenReturn("dummyConsumerEmailId");
    when(consumer.getFirstName()).thenReturn("dummyConsumerFirstName");
    when(consumer.getLastName()).thenReturn("dummyConsumerLastName");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    consumerService.listPurchase(
        consumer,
        request,
        handler -> {
          if (handler.succeeded()) {

            JsonObject expected =
                new JsonObject()
                    .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                    .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                    .put(
                        RESULTS,
                        new JsonArray()
                            .add(
                                new JsonObject()
                                    .put("additionalInfo", "someAdditionalInfo")
                                    .put(
                                        "provider",
                                        new JsonObject()
                                            .put("email", "dummyProviderEmailId")
                                            .put(
                                                "name",
                                                new JsonObject()
                                                    .put("firstName", "dummyFirstName")
                                                    .put("lastName", "dummyLastName"))
                                            .put("id", "dummyProviderId"))
                                    .put(
                                        "consumer",
                                        new JsonObject()
                                            .put("email", "dummyConsumerEmailId")
                                            .put(
                                                "name",
                                                new JsonObject()
                                                    .put("firstName", "dummyConsumerFirstName")
                                                    .put("lastName", "dummyConsumerLastName"))
                                            .put("id", "someUserId"))
                                    .put(
                                        "product",
                                        new JsonObject()
                                            .put("productId", "dummyProductId")
                                            .put("productVariantId", "dummyProductVariantId")
                                            .put("resources", new JsonArray().add("resource1"))
                                            .put("productVariantName", "dummyProductVariantName")
                                            .put("price", 15f)
                                            .put("expiryInMonths", 12))));
            assertEquals(expected, handler.result());
            vertxTestContext.completeNow();

          } else {

            vertxTestContext.failNow("Failed to fetch consumer-list purchase results ");
          }
        });
  }

    public static Stream<Arguments> input() {
    return Stream.of(
        Arguments.of("dummyProductId", "", new RespBuilder()
                .withType(HttpStatusCode.NOT_FOUND.getValue())
                .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                .withDetail("Purchase info not found")
                .getResponse()),
        Arguments.of("", "", new RespBuilder()
                .withType(HttpStatusCode.NO_CONTENT.getValue())
                .withTitle(HttpStatusCode.NO_CONTENT.getUrn())
                .getResponse()),
        Arguments.of((Object) null, "", new RespBuilder()
                .withType(HttpStatusCode.NO_CONTENT.getValue())
                .withTitle(HttpStatusCode.NO_CONTENT.getUrn())
                .getResponse()),
        Arguments.of("abcd", "abcd", new RespBuilder()
                .withType(HttpStatusCode.NOT_FOUND.getValue())
                .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                .withDetail("Purchase info not found")
                .getResponse()));
    }
    @ParameterizedTest
    @MethodSource("input")
    @DisplayName("Test listPurchase when there is empty response from DB : Failure")
    public void testListPurchaseWithEmptyResponse(String productId, String resourceId, String expectedFailureMessage, VertxTestContext vertxTestContext) {

    request
        .put("resourceId", resourceId)
        .put("productId", productId)
        .put("paymentStatus", "pending")
        .put(RESULTS, new JsonArray())
        .put("orderId", "dummyOrderId");

        when(consumer.getResourceServerUrl()).thenReturn("dummyRsUrl");
        when(consumer.getUserId()).thenReturn("someUserId");
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
    consumerService.listPurchase(
        consumer,
        request,
        handler -> {
          if (handler.failed()) {
            assertEquals(expectedFailureMessage, handler.cause().getMessage());
            vertxTestContext.completeNow();

          } else {

            vertxTestContext.failNow("Succeeded when the response from DB is empty");
          }
        });
    }

    @Test
    @DisplayName("Test listPurchase when DB execution failed : Failure")
    public void testListPurchaseWithDbExecutionFailure(VertxTestContext vertxTestContext) {

        request
                .put("resourceId", "resourceId")
                .put("productId", "productId")
                .put("paymentStatus", "pending")
                .put(RESULTS, new JsonArray())
                .put("orderId", "dummyOrderId");

        when(consumer.getResourceServerUrl()).thenReturn("dummyRsUrl");
        when(consumer.getUserId()).thenReturn("someUserId");
        when(asyncResult.succeeded()).thenReturn(false);
    consumerService.listPurchase(
        consumer,
        request,
        handler -> {
          if (handler.failed()) {
              String expected = new RespBuilder()
                      .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                      .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                      .withDetail(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                      .getResponse();
            assertEquals(expected, handler.cause().getMessage());
            vertxTestContext.completeNow();

          } else {

            vertxTestContext.failNow("Succeeded when the response from DB is empty");
          }
        });
    }

    @Test
    @DisplayName("Test listPurchase when payment status is invalid : Failure")
    public void testListPurchaseWithInvalidUserRole(VertxTestContext vertxTestContext) {

        request
                .put("resourceId", "resourceId")
                .put("productId", "productId")
                .put("paymentStatus", "successful")
                .put(RESULTS, new JsonArray())
                .put("orderId", "dummyOrderId");

        when(consumer.getResourceServerUrl()).thenReturn("dummyRsUrl");
        consumerService.listPurchase(
                consumer,
                request,
                handler -> {
                    if (handler.failed()) {
                        String expected = new RespBuilder()
                                .withType(HttpStatusCode.BAD_REQUEST.getValue())
                                .withTitle(ResponseUrn.BAD_REQUEST_URN.getUrn())
                                .withDetail("Invalid payment status")
                                .getResponse();
                        assertEquals(expected, handler.cause().getMessage());
                        vertxTestContext.completeNow();

                    } else {

                        vertxTestContext.failNow("Succeeded when the response from DB is empty");
                    }
                });
    }

  @Test
  @DisplayName("Test list product variants method : Success")
  public void testListProductVariantsSuccess(VertxTestContext vertxTestContext) {
    when(consumer.getResourceServerUrl()).thenReturn("dummyRsUrl");

    JsonArray jsonArray = new JsonArray().add("abcd");
    request.put("productId", "someDummyProductId").put("resourceServerUrl", "abcd");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonMock);
    when(jsonMock.getJsonArray(anyString())).thenReturn(jsonArray);

    consumerService.listProductVariants(
        consumer,
        request,
        handler -> {
          if (handler.succeeded()) {

            assertNotNull(handler.result());
            assertEquals(jsonMock, handler.result());
            verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
            verify(consumer, times(1)).getResourceServerUrl();
            vertxTestContext.completeNow();
          } else {

            vertxTestContext.failNow("Failed to fetch product variants");
          }
        });
  }

  @Test
  @DisplayName("Test list product variants when DB response is Empty : Failure")
  public void testListProductVariantsWithEmptyResponse(VertxTestContext vertxTestContext) {
    when(consumer.getResourceServerUrl()).thenReturn("dummyRsUrl");

    JsonArray jsonArray = new JsonArray();
    request.put("productId", "someDummyProductId").put("resourceServerUrl", "abcd");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonMock);
    when(jsonMock.getJsonArray(anyString())).thenReturn(jsonArray);

    consumerService.listProductVariants(
        consumer,
        request,
        handler -> {
          if (handler.failed()) {
            String expectedFailureMessage =
                new RespBuilder()
                    .withType(HttpStatusCode.NOT_FOUND.getValue())
                    .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                    .withDetail("Product variants not found")
                    .getResponse();
            assertEquals(expectedFailureMessage, handler.cause().getMessage());
            verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
            verify(consumer, times(1)).getResourceServerUrl();
            vertxTestContext.completeNow();
          } else {

            vertxTestContext.failNow("Succeeded when the response from DB is empty");
          }
        });
  }

    @Test
    @DisplayName("Test list product variants when DB execution failed : Failure")
    public void testListProductVariantsWhenDbExecutionFailed(VertxTestContext vertxTestContext) {
        when(consumer.getResourceServerUrl()).thenReturn("dummyRsUrl");

        JsonArray jsonArray = new JsonArray();
        request.put("productId", "someDummyProductId").put("resourceServerUrl", "abcd");
        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(failureMessage);

        consumerService.listProductVariants(
                consumer,
                request,
                handler -> {
                    if (handler.failed()) {
                        String expectedFailureMessage =
                                new RespBuilder()
                                        .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                                        .withTitle(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn())
                                        .withDetail("Product variants could not be fetched as there was internal server error")
                                        .getResponse();
                        assertEquals(expectedFailureMessage, handler.cause().getMessage());
                        verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
                        verify(consumer, times(1)).getResourceServerUrl();
                        vertxTestContext.completeNow();
                    } else {

                        vertxTestContext.failNow("Succeeded when the response from DB execution failed");
                    }
                });
    }

  @Test
  @DisplayName("Test generate order entry method : Success")
  public void testGenerateOrderEntry(VertxTestContext vertxTestContext) {

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    request.put(
        TRANSFERS,
        new JsonArray()
            .add(
                new JsonObject()
                    .put(SOURCE, "someSource")
                    .put(AMOUNT, 10)
                    .put(RECIPIENT, "abcd")
                    .put(NOTES, new JsonObject().put("key", "value"))));
    request.put(AMOUNT, 10);

    consumerService
        .generateOrderEntry(request, "somePvId", "someConsumerId")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonArray expected =
                    new JsonArray()
                        .add(
                            new JsonObject()
                                .put("productVariantId", "somePvId")
                                .put("orderId", "someSource")
                                .put("amount", 10)
                                .put("currency", "INR")
                                .put("status", "Created"));
                assertEquals(expected, handler.result().getJsonArray(RESULTS));
                vertxTestContext.completeNow();

              } else {

                vertxTestContext.failNow("Failed to create order");
              }
            });
  }

  @Test
  @DisplayName("Test generate order entry method when query execution failed: Failure")
  public void testGenerateOrderEntryFailure(VertxTestContext vertxTestContext) {

    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn(failureMessage);
    request.put(
        TRANSFERS,
        new JsonArray()
            .add(
                new JsonObject()
                    .put(SOURCE, "someSource")
                    .put(AMOUNT, 10)
                    .put(RECIPIENT, "abcd")
                    .put(NOTES, new JsonObject().put("key", "value"))));
    request.put(AMOUNT, 10);

    consumerService
        .generateOrderEntry(request, "somePvId", "someConsumerId")
        .onComplete(
            handler -> {
              if (handler.failed()) {

                assertEquals(failureMessage, handler.cause().getMessage());
                vertxTestContext.completeNow();

              } else {

                vertxTestContext.failNow("Succeeded when query execution failed");
              }
            });
  }

  @Test
  @DisplayName("Test getOrderRelatedInfo method : Success")
  public void testGetOrderRelatedInfoSuccess(VertxTestContext vertxTestContext) {

    JsonArray jsonArray = new JsonArray().add("some random value");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonMock);
    when(jsonMock.getJsonArray(anyString())).thenReturn(jsonArray);

    consumerService
        .getOrderRelatedInfo("somePvId")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {

                assertEquals(new JsonObject(), handler.result());
                vertxTestContext.completeNow();

              } else {

                vertxTestContext.failNow("Failed to get order related info");
              }
            });
  }

  @Test
  @DisplayName("Test getOrderRelatedInfo method with empty response from DB: Failure")
  public void testGetOrderRelatedInfoWithEmptyResponse(VertxTestContext vertxTestContext) {

    JsonArray jsonArray = new JsonArray();
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonMock);
    when(jsonMock.getJsonArray(anyString())).thenReturn(jsonArray);

    consumerService
        .getOrderRelatedInfo("somePvId")
        .onComplete(
            handler -> {
              if (handler.failed()) {
                String failureMessage =
                    new RespBuilder()
                        .withType(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                        .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getMessage())
                        .withDetail("Product Variant Not Found")
                        .getResponse();
                assertEquals(failureMessage, handler.cause().getMessage());
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow(
                    "Succeeded to get order related info when the response from DB is empty");
              }
            });
  }

  @Test
  @DisplayName("Test getOrderRelatedInfo method failure in DB execution: Failure")
  public void testGetOrderRelatedInfoFailure(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn(failureMessage);

    consumerService
        .getOrderRelatedInfo("somePvId")
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(failureMessage, handler.cause().getMessage());
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Succeeded when DB execution failed");
              }
            });
  }
}
