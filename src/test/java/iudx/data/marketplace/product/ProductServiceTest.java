package iudx.data.marketplace.product;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.common.CatalogueService;
import iudx.data.marketplace.configuration.Configuration;
import iudx.data.marketplace.postgres.PostgresService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.product.util.Constants.*;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

  private static Configuration configuration;
  private static PostgresService postgresService;
  private static CatalogueService catService;
  private static ProductServiceImpl productServiceImpl;
  @Mock JsonObject jsonObjectMock;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Future<Boolean> boolFuture;

  @BeforeAll
  public static void setup(Vertx vertx, VertxTestContext testContext) {
    configuration = new Configuration();
    JsonObject config = configuration.configLoader(3, vertx);
    postgresService = mock(PostgresService.class);
    catService = mock(CatalogueService.class);
    //    queryBuilder = new QueryBuilder(config.getJsonArray(TABLES));
    //    productTableName = config.getJsonArray(TABLES).getString(0);
    productServiceImpl = new ProductServiceImpl(config, postgresService, catService);
    testContext.completeNow();
  }

  @Test
  @DisplayName("test create product - success")
  public void testCreateProduct(VertxTestContext testContext) {

    JsonArray datasetIDs = new JsonArray().add("dataset-1");

    ProductServiceImpl productServiceSpy = spy(productServiceImpl);
    when(jsonObjectMock.getJsonObject(AUTH_INFO)).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getString(IID)).thenReturn("provider-id");
    when(jsonObjectMock.getString(PROVIDER_ID)).thenReturn("provider-id");
    when(jsonObjectMock.getString(PROVIDER_NAME)).thenReturn("new-provider");
    when(jsonObjectMock.getString(PRODUCT_ID)).thenReturn("abcde");
    when(jsonObjectMock.put(anyString(), anyString())).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray(DATASETS)).thenReturn(datasetIDs);

    doAnswer(Answer -> Future.succeededFuture(false))
        .when(productServiceSpy)
        .checkIfProductExists(anyString(), anyString());
    doAnswer(
            Answer ->
                Future.succeededFuture(
                    new JsonObject()
                        .put(DATASET_ID, datasetIDs.getString(0))
                        .put(DATASET_NAME, "dat-name")
                        .put("accessPolicy", "OPEN")))
        .when(catService)
        .getItemDetails(anyString());
    doAnswer(
            Answer ->
                Future.succeededFuture(
                    new JsonObject().put(DATASET_ID, datasetIDs.getString(0)).put("totalHits", 1)))
        .when(catService)
        .getResourceCount(anyString());

    when(asyncResult.result()).thenReturn(jsonObjectMock);
    when(asyncResult.succeeded()).thenReturn(true);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeTransaction(anyList(), any());

    productServiceSpy.createProduct(
        jsonObjectMock,
        handler -> {
          if (handler.succeeded()) {
            //            verify(catService, times(2)).getItemDetails(anyString());
            verify(postgresService, times(1)).executeTransaction(anyList(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("create product test failed");
          }
        });
  }

  @Test
  @DisplayName("test delete product - success")
  public void testDeleteProduct(VertxTestContext testContext) {

    JsonObject auth_info = new JsonObject().put(IID, "iid");
    JsonObject request =
        new JsonObject().put(AUTH_INFO, auth_info).put(PRODUCT_ID, "id").put("totalHits", 1);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeCountQuery(anyString(), any());

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executePreparedQuery(anyString(), any(), any());

    productServiceImpl.deleteProduct(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(postgresService, times(2)).executePreparedQuery(anyString(), any(), any());
            verify(postgresService, times(2)).executeCountQuery(anyString(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("delete product test failed");
          }
        });
  }

  @Test
  @DisplayName("test list products - success")
  public void testListProducts(VertxTestContext testContext) {

    lenient().when(jsonObjectMock.containsKey(DATASET_ID)).thenReturn(true);
    lenient().when(jsonObjectMock.getString(DATASET_ID)).thenReturn("dataset-id-1");
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

    productServiceImpl.listProducts(
        new JsonObject(),
        handler -> {
          if (handler.succeeded()) {
            verify(postgresService, times(1)).executePreparedQuery(anyString(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("list products test failed");
          }
        });
  }

  @Test
  @DisplayName("test product exists future")
  public void testProductExistsFuture(VertxTestContext testContext) {

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getInteger("totalHits")).thenReturn(1);
    Mockito.doAnswer(
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

    productServiceImpl
        .checkIfProductExists(anyString(), anyString())
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(postgresService, times(1)).executeCountQuery(anyString(), any());
                testContext.completeNow();
              } else {
                testContext.failNow("product exists future test failed");
              }
            });
  }
}
