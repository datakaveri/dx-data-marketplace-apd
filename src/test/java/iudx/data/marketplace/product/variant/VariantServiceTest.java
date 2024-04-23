package iudx.data.marketplace.product.variant;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.common.Util;
import iudx.data.marketplace.configuration.Configuration;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import java.util.stream.Stream;

import static iudx.data.marketplace.apiserver.util.Constants.PRODUCT_VARIANT_ID;
import static iudx.data.marketplace.apiserver.util.Constants.RESULTS;
import static iudx.data.marketplace.apiserver.util.Constants.TITLE;
import static iudx.data.marketplace.apiserver.util.Constants.TYPE;
import static iudx.data.marketplace.product.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class VariantServiceTest {

  private static Configuration configuration;
  private static PostgresService postgresService;
  private static ProductVariantServiceImpl variantServiceImpl, variantServiceSpy;

  @Mock
  JsonObject jsonObjectMock;
  @Mock
  JsonArray jsonArrayMock;
  @Mock
  AsyncResult<JsonObject> asyncResult;
  @Mock
  User provider;
  Util util;
  String failureMessage;
  JsonObject request;
  @Mock JsonObject jsonMock;
  @Mock Throwable throwable;
  @BeforeEach
  @DisplayName("setup")
  public void setup(Vertx vertx, VertxTestContext testContext) {
    configuration = new Configuration();
    failureMessage = "Some failure message";
    util = new Util();
    request = new JsonObject();
    JsonObject config = configuration.configLoader(4, vertx);
    postgresService = mock(PostgresService.class);
    variantServiceImpl = new ProductVariantServiceImpl(config, postgresService, util);
    lenient().when(jsonObjectMock.getString(PRODUCT_ID)).thenReturn("urn:datakaeri.org:provider-id:abcde");
    lenient().when(jsonObjectMock.getString(PRODUCT_VARIANT_NAME)).thenReturn("var-name");
    variantServiceSpy = spy(variantServiceImpl);
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
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test createProductVariant : Success")
  public void testProductVariantSuccess(VertxTestContext vertxTestContext) {
    when(provider.getUserId()).thenReturn("someUserId");
    JsonObject request = mock(JsonObject.class);
    JsonArray mockJsonArray = mock(JsonArray.class);
    JsonObject existenceJson = new JsonObject().put("status", "active").put("provider_id", "someUserId");
    JsonArray resources = new JsonArray().add(new JsonObject().put(ID, "resourceId1"));
    JsonObject resourceJson = new JsonObject()
            .put(RESOURCES_ARRAY,resources);

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    when(request.getInteger("totalHits")).thenReturn(0);
    when(request.getString(PRODUCT_ID)).thenReturn("someProductId");
    when(request.getString(PRODUCT_VARIANT_NAME)).thenReturn("someProductVariantName");
    when(request.getJsonArray(RESULTS)).thenReturn(mockJsonArray);
    when(request.getJsonArray(RESOURCES_ARRAY)).thenReturn(resources);
    when(request.getString("provider_id")).thenReturn("someUserId");
    when(mockJsonArray.isEmpty()).thenReturn(false);
    when(mockJsonArray.getJsonObject(anyInt()))
            .thenReturn(existenceJson, resourceJson, new JsonObject().put("_id", "someDummyValue"));

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock)
                      throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(1))
                        .handle(asyncResult);
                return null;
              }
            })
            .when(postgresService)
            .executeCountQuery(anyString(), any(Handler.class));

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock)
                      throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(1))
                        .handle(asyncResult);
                return null;
              }
            })
            .when(postgresService)
            .executeQuery(anyString(), any(Handler.class));

    variantServiceImpl.createProductVariant(
            provider,
            request,
            handler -> {
              if (handler.succeeded()) {
                assertEquals(ResponseUrn.SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
                assertEquals(ResponseUrn.SUCCESS_URN.getMessage(), handler.result().getString(TITLE));
                assertEquals(
                        "someDummyValue",
                        handler
                                .result()
                                .getJsonArray(RESULTS)
                                .getJsonObject(0)
                                .getString(PRODUCT_VARIANT_ID));
                assertEquals(
                        "someProductId",
                        handler.result().getJsonArray(RESULTS).getJsonObject(0).getString(PRODUCT_ID));
                assertEquals(
                        "someProductVariantName",
                        handler
                                .result()
                                .getJsonArray(RESULTS)
                                .getJsonObject(0)
                                .getString(PRODUCT_VARIANT_NAME));
                assertEquals("Product Variant created successfully", handler.result().getString("detail"));
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("product variant creation failed");
              }
            });
  }

  @Test
  @DisplayName("test update product variant - success")
  public void testUpdateVariant(VertxTestContext testContext) {

    doAnswer(Answer -> Future.succeededFuture(true)).when(variantServiceSpy).updateProductVariantStatus(anyString(),anyString());
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonObjectMock.getString(anyString())).thenReturn("someValue");

    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(postgresService).executeQuery(any(),any());

    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(2)).handle(asyncResult);
        return null;
      }
    }).when(variantServiceSpy).createProductVariant(any(),any(), any());


    variantServiceSpy.updateProductVariant(provider, jsonObjectMock, handler -> {
      if(handler.succeeded()) {
        verify(variantServiceSpy, times(1)).createProductVariant(any(),any(), any());
        verify(variantServiceSpy, times(1)).updateProductVariantStatus(anyString(),anyString());
        testContext.completeNow();
      } else {
        testContext.failNow("update variant test failed");
      }
    });
  }

  @Test
  @DisplayName("test delete product variant - success")
  public void testDeleteVariant(VertxTestContext testContext) {

    JsonObject result = new JsonObject();
    when(jsonObjectMock.getString(PRODUCT_VARIANT_ID)).thenReturn("someDummyValue");
    doAnswer(Answer -> Future.succeededFuture(result)).when(variantServiceSpy).updateProductVariantStatus(anyString());

    variantServiceSpy.deleteProductVariant(provider, jsonObjectMock, handler -> {
      if(handler.succeeded()) {
        verify(variantServiceSpy, times(1)).updateProductVariantStatus(anyString());
        testContext.completeNow();
      } else {
        testContext.failNow("delete variant test failed");
      }
    });
  }

  @Test
  @DisplayName("test update status future")
  public void testUpdateStatusFuture(VertxTestContext testContext) {

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(postgresService).executeQuery(anyString(),any());

    variantServiceSpy.updateProductVariantStatus(anyString()).onComplete(handler -> {
      if(handler.succeeded()) {
        verify(postgresService, times(1)).executeQuery(anyString(),any());
        testContext.completeNow();
      } else {
        testContext.failNow("update status test failed");
      }
    });
  }

  @Test
  @DisplayName("test get product details")
  public void testGetProductDetails(VertxTestContext testContext) {
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(postgresService).executeQuery(anyString(),any());

    variantServiceSpy.getProductDetails(anyString(), anyString()).onComplete(handler -> {
      if(handler.succeeded()) {
        verify(postgresService, times(1)).executeQuery(anyString(),any());
        testContext.completeNow();
      } else {
        testContext.failNow("get product details test failed");
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
  @ValueSource(strings = {"pending", "succeeded", "failed"})
  @DisplayName("Test listPurchase method : Success")
  public void testListPurchaseSuccess(String paymentStatus, VertxTestContext vertxTestContext) {
    JsonObject info =
        new JsonObject()
            .put("additionalInfo", "someAdditionalInfo")
            .put("consumerEmailId", "dummyConsumerEmailId")
            .put("consumerFirstName", "dummyFirstName")
            .put("consumerLastName", "dummyLastName")
            .put("consumerId", "dummyConsumerId")
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

    when(provider.getResourceServerUrl()).thenReturn("dummyRsUrl");
    when(provider.getUserId()).thenReturn("someUserId");
    when(provider.getUserRole()).thenReturn(Role.PROVIDER);
    when(provider.getEmailId()).thenReturn("dummyProviderEmailId");
    when(provider.getFirstName()).thenReturn("dummyProviderFirstName");
    when(provider.getLastName()).thenReturn("dummyProviderLastName");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    variantServiceImpl.listPurchase(
        provider,
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
                                        "consumer",
                                        new JsonObject()
                                            .put("email", "dummyConsumerEmailId")
                                            .put(
                                                "name",
                                                new JsonObject()
                                                    .put("firstName", "dummyFirstName")
                                                    .put("lastName", "dummyLastName"))
                                            .put("id", "dummyConsumerId"))
                                    .put(
                                        "provider",
                                        new JsonObject()
                                            .put("email", "dummyProviderEmailId")
                                            .put(
                                                "name",
                                                new JsonObject()
                                                    .put("firstName", "dummyProviderFirstName")
                                                    .put("lastName", "dummyProviderLastName"))
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

    when(provider.getResourceServerUrl()).thenReturn("dummyRsUrl");
    when(provider.getUserId()).thenReturn("someUserId");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    variantServiceImpl.listPurchase(
            provider,
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

    when(provider.getResourceServerUrl()).thenReturn("dummyRsUrl");
    when(provider.getUserId()).thenReturn("someUserId");
    when(asyncResult.succeeded()).thenReturn(false);
    variantServiceImpl.listPurchase(
            provider,
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

    when(provider.getResourceServerUrl()).thenReturn("dummyRsUrl");
    variantServiceImpl.listPurchase(
            provider,
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
    when(provider.getResourceServerUrl()).thenReturn("dummyRsUrl");

    JsonArray jsonArray = new JsonArray().add("abcd");
    request.put("productId", "someDummyProductId")
            .put("resourceServerUrl", "abcd")
            .put(PRODUCT_VARIANT_NAME, "variant2");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonMock);
    when(jsonMock.getJsonArray(anyString())).thenReturn(jsonArray);

    variantServiceImpl.listProductVariants(
            provider,
            request,
            handler -> {
              if (handler.succeeded()) {
                assertNotNull(handler.result());
                assertEquals(jsonMock, handler.result());
                verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
                verify(provider, times(1)).getResourceServerUrl();
                vertxTestContext.completeNow();

              } else {

                vertxTestContext.failNow("Failed to fetch product variants");
              }
            });

  }

  @Test
  @DisplayName("Test list product variants when the product doesn't belong to the provider : Failure")
  public void testListProductVariantsOfDifferentProvider(VertxTestContext vertxTestContext) {
    when(provider.getResourceServerUrl()).thenReturn("dummyRsUrl");
    when(provider.getUserId()).thenReturn("someUserId");

    JsonArray jsonArray = new JsonArray().add("abcd");
    request.put("productId", "someDummyProductId")
            .put("resourceServerUrl", "abcd")
            .put(PRODUCT_VARIANT_NAME, "variant2");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonMock);
    when(jsonMock.getJsonArray(RESULTS)).thenReturn(new JsonArray());

    variantServiceImpl.listProductVariants(
        provider,
        request,
        handler -> {
          if (handler.failed()) {
            String expectedFailureMessage = new RespBuilder()
                    .withType(HttpStatusCode.NOT_FOUND.getValue())
                    .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                    .withDetail("Product variants not found")
                    .getResponse();
            assertEquals(expectedFailureMessage, handler.cause().getMessage());
            verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
            verify(provider, times(1)).getResourceServerUrl();
            vertxTestContext.completeNow();

          } else {

            vertxTestContext.failNow(
                "Succeeded to fetch product variants for a different provider : Ownership error");
          }
        });
  }

  @Test
  @DisplayName("Test list product variants when DB response is Empty : Failure")
  public void testListProductVariantsWithEmptyResponse(VertxTestContext vertxTestContext) {
    when(provider.getResourceServerUrl()).thenReturn("dummyRsUrl");

    JsonArray jsonArray = new JsonArray();
    request.put("productId", "someDummyProductId").put("resourceServerUrl", "abcd");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonMock);
    when(jsonMock.getJsonArray(anyString())).thenReturn(jsonArray);

    variantServiceImpl.listProductVariants(
            provider,
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
                verify(provider, times(1)).getResourceServerUrl();
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow("Succeeded when the response from DB is empty");
              }
            });
  }

  @Test
  @DisplayName("Test list product variants when DB execution failed : Failure")
  public void testListProductVariantsWhenDbExecutionFailed(VertxTestContext vertxTestContext) {
    when(provider.getResourceServerUrl()).thenReturn("dummyRsUrl");

    JsonArray jsonArray = new JsonArray();
    request.put("productId", "someDummyProductId").put("resourceServerUrl", "abcd");
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);

    variantServiceImpl.listProductVariants(
            provider,
            request,
            handler -> {
              if (handler.failed()) {
                String expectedFailureMessage =
                        new RespBuilder()
                                .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                                .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                                .withDetail("Internal Server Error")
                                .getResponse();
                assertEquals(expectedFailureMessage, handler.cause().getMessage());
                verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
                verify(provider, times(1)).getResourceServerUrl();
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow("Succeeded when the response from DB execution failed");
              }
            });
  }

  @Test
  @DisplayName("Test create product variant method when the product is not found : Failure")
  public void testCreatingProductVariantWhenProductIsAbsent(VertxTestContext vertxTestContext)
  {
    request
        .put(PRODUCT_ID, "someDummyProductId")
        .put(RESULTS, new JsonArray())
        .put(PRODUCT_VARIANT_NAME, "variant3");
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);

    variantServiceImpl.createProductVariant(
        provider,
        request,
        handler -> {
          System.out.println(handler);
          if (handler.failed()) {
            String expected =
                    new RespBuilder()
                            .withType(ResponseUrn.BAD_REQUEST_URN.getUrn())
                            .withTitle(ResponseUrn.BAD_REQUEST_URN.getMessage())
                            .withDetail(
                                    "Product Variant cannot be created as product is in INACTIVE state or is not found")
                            .getResponse();
            assertEquals(expected, handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {

            vertxTestContext.failNow("Created a product variant when the product is not found");
          }
        });
  }

  @Test
  @DisplayName("Test create product variant method when the product is deleted : Failure")
  public void testCreatingProductVariantWhenProductIsDeleted(VertxTestContext vertxTestContext)
  {
    request
        .put(PRODUCT_ID, "someDummyProductId")
        .put(RESULTS, new JsonArray().add(new JsonObject().put("status", "inactive").put("provider_id", "dummyProviderId")))
        .put(PRODUCT_VARIANT_NAME, "variant3");
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);

    variantServiceImpl.createProductVariant(
            provider,
            request,
            handler -> {
              if (handler.failed()) {
                String expected =
                        new RespBuilder()
                                .withType(ResponseUrn.BAD_REQUEST_URN.getUrn())
                                .withTitle(ResponseUrn.BAD_REQUEST_URN.getMessage())
                                .withDetail(
                                        "Product Variant cannot be created as product is in INACTIVE state or is not found")
                                .getResponse();
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow("Created a product variant when the product is inactive");
              }
            });
  }

  @Test
  @DisplayName("Test create product variant method when the product belongs to other provider : Failure")
  public void testCreatingProductVariantWhenProviderDoesNotOwnTheProduct(VertxTestContext vertxTestContext)
  {
    request
            .put(PRODUCT_ID, "someDummyProductId")
            .put(RESULTS, new JsonArray().add(new JsonObject().put("status", "active").put("provider_id", "someUserId")))
            .put(PRODUCT_VARIANT_NAME, "variant3");
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);

    variantServiceImpl.createProductVariant(
            provider,
            request,
            handler -> {
              if (handler.failed()) {
                String expected =
                        new RespBuilder()
                                .withType(ResponseUrn.FORBIDDEN_URN.getUrn())
                                .withTitle(ResponseUrn.FORBIDDEN_URN.getMessage())
                                .withDetail(
                                        "Product variant cannot be created, as the provider does not own the product")
                                .getResponse();
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow("Created a product variant when the product when product does not belong to the provider");
              }
            });
  }

  @Test
  @DisplayName("Test create product variant method when DB execution failed : Failure")
  public void testCreatingProductVariantWhenDbExecutionFailed(VertxTestContext vertxTestContext)
  {
    request
            .put(PRODUCT_ID, "someDummyProductId")
            .put(RESULTS, new JsonArray().add(new JsonObject().put("status", "active").put("provider_id", "dummyProviderId")))
            .put(PRODUCT_VARIANT_NAME, "variant3");
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);

    variantServiceImpl.createProductVariant(
            provider,
            request,
            handler -> {
              if (handler.failed()) {
                String expected =
                        new RespBuilder()
                                .withType(ResponseUrn.DB_ERROR_URN.getUrn())
                                .withTitle(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                                .withDetail("Internal Server Error")
                                .getResponse();
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow("Created a product variant when the product when product does not belong to the provider");
              }
            });
  }

  @Test
  @DisplayName("Test updateProductVariantStatus method when the product variant is not present : Failure")
  public void testUpdateProductVariantStatusWhenProductVariantIsAbsent(VertxTestContext vertxTestContext)
  {
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray(RESULTS)).thenReturn(new JsonArray());
    request
            .put(PRODUCT_ID, "someDummyProductId")
            .put(RESULTS, new JsonArray().add(new JsonObject().put("status", "active").put("provider_id", "dummyProviderId")))
            .put(PRODUCT_VARIANT_NAME, "variant3");
    variantServiceImpl.updateProductVariant(
        provider,
        request,
        handler -> {
          if (handler.failed()) {
            String expectedFailureMessage = new RespBuilder()
                    .withType(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                    .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getMessage())
                    .withDetail(
                            "Product Variant cannot be updated as the product is in INACTIVE state or product is not found")
                    .getResponse();
            assertEquals(expectedFailureMessage, handler.cause().getMessage());
            verify(postgresService, times(1)).executeQuery(anyString(), any());
            vertxTestContext.completeNow();

          } else {

            vertxTestContext.failNow("Updated a product variant when the product is not found");
          }
        });
  }

  @Test
  @DisplayName("Test updateProductVariantStatus method when DB Execution failed : Failure")
  public void testUpdateProductVariantStatusWhenDbExecutionFailed(VertxTestContext vertxTestContext)
  {
    when(asyncResult.succeeded()).thenReturn(false);
    Handler<AsyncResult<JsonObject>> handler = mock(Handler.class);
    request
            .put(PRODUCT_ID, "someDummyProductId")
            .put(RESULTS, new JsonArray().add(new JsonObject().put("status", "active").put("provider_id", "dummyProviderId")))
            .put(PRODUCT_VARIANT_NAME, "variant3");
    assertThrows(DxRuntimeException.class, () -> variantServiceImpl.updateProductVariant(provider, request, handler));
    vertxTestContext.completeNow();
  }
}
