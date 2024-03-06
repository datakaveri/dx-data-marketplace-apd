package iudx.data.marketplace.product.variant;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.common.Util;
import iudx.data.marketplace.configuration.Configuration;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.data.marketplace.product.util.Constants.*;
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
  @DisplayName("test create product variant - success")
  public void testCreateVariant(VertxTestContext testContext) {

    ProductVariantServiceImpl variantServiceSpy = spy(variantServiceImpl);
    when(jsonObjectMock.getString("providerid")).thenReturn("provider-id");
    when(jsonObjectMock.getJsonArray(resourceNames)).thenReturn(jsonArrayMock);
    when(jsonObjectMock.getInteger("totalHits")).thenReturn(0);
    doAnswer(
            Answer ->
                Future.succeededFuture(
                    new JsonObject()
                        .put(
                            RESULTS,
                            new JsonArray()
                                .add(
                                    new JsonObject()
                                        .put("providerid", "provider-id")
                                        .put(PRODUCT_ID, "urn:datakaeri.org:provider-id:abcde")
                                        .put(resourceNames, new JsonArray().add(new JsonObject().put(ID, "resource-1").put(NAME, "dat-name")))))))
        .when(variantServiceSpy)
        .getProductDetails(anyString());
    when(jsonArrayMock.size()).thenReturn(1);
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObjectMock);

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock);
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
            .executeCountQuery(anyString(), any());
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
        .executeQuery(anyString(), any());

    variantServiceSpy.createProductVariant(
            user,
        jsonObjectMock,
        handler -> {
          if (handler.succeeded()) {
            verify(variantServiceSpy, times(1)).getProductDetails(anyString());
            verify(postgresService, times(1)).executeQuery(anyString(),any());
            verify(postgresService, times(1)).executeCountQuery(anyString(),any());
            testContext.completeNow();
          } else {
            testContext.failNow("create variant test failed");
          }
        });
  }

  @Test
  @DisplayName("test update product variant - success")
  public void testUpdateVariant(VertxTestContext testContext) {

    doAnswer(Answer -> Future.succeededFuture(true)).when(variantServiceSpy).updateProductVariantStatus(anyString(),anyString());
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(1)).handle(asyncResult);
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

    doAnswer(Answer -> Future.succeededFuture(true)).when(variantServiceSpy).updateProductVariantStatus(anyString(),anyString());

    variantServiceSpy.deleteProductVariant(user, jsonObjectMock, handler -> {
      if(handler.succeeded()) {
        verify(variantServiceSpy, times(1)).updateProductVariantStatus(anyString(),anyString());
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

    variantServiceSpy.getProductDetails(anyString()).onComplete(handler -> {
      if(handler.succeeded()) {
        verify(postgresService, times(1)).executeQuery(anyString(),any());
        testContext.completeNow();
      } else {
        testContext.failNow("get product details test failed");
      }
    });
  }
}
