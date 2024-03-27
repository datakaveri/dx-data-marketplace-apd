package iudx.data.marketplace.product.variant;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
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
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.data.marketplace.apiserver.util.Constants.PRODUCT_VARIANT_ID;
import static iudx.data.marketplace.apiserver.util.Constants.TITLE;
import static iudx.data.marketplace.product.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
  User user;
  @Mock
  Util util;

  @BeforeEach
  @DisplayName("setup")
  public void setup(Vertx vertx, VertxTestContext testContext) {
    configuration = new Configuration();
    JsonObject config = configuration.configLoader(4, vertx);
    postgresService = mock(PostgresService.class);
    variantServiceImpl = new ProductVariantServiceImpl(config, postgresService, util);
    lenient().when(jsonObjectMock.getString(PRODUCT_ID)).thenReturn("urn:datakaeri.org:provider-id:abcde");
    lenient().when(jsonObjectMock.getString(PRODUCT_VARIANT_NAME)).thenReturn("var-name");
    variantServiceSpy = spy(variantServiceImpl);
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test createProductVariant : Success")
  public void testProductVariantSuccess(VertxTestContext vertxTestContext) {
    when(user.getUserId()).thenReturn("someUserId");
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
            user,
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


    variantServiceSpy.updateProductVariant(user, jsonObjectMock, handler -> {
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

    variantServiceSpy.deleteProductVariant(user, jsonObjectMock, handler -> {
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
}
