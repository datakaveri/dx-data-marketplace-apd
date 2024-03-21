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
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.configuration.Configuration;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.product.util.QueryBuilder;
import iudx.data.marketplace.razorpay.RazorPayService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.data.marketplace.apiserver.util.Constants.DETAIL;
import static iudx.data.marketplace.apiserver.util.Constants.TITLE;
import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.product.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
  @Mock
    User user;
  @Mock
  static RazorPayService razorPayService;
    public static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

  @BeforeAll
  public static void setup(Vertx vertx, VertxTestContext testContext) {
    configuration = new Configuration();
    JsonObject config = configuration.configLoader(3, vertx);
    postgresService = mock(PostgresService.class);
    catService = mock(CatalogueService.class);
    //    queryBuilder = new QueryBuilder(config.getJsonArray(TABLES));
    //    productTableName = config.getJsonArray(TABLES).getString(0);
    productServiceImpl = new ProductServiceImpl(config, postgresService, catService, razorPayService, true);
    testContext.completeNow();
  }


  @Test
  @DisplayName("Test create product : Success")
  public void testCreateProductSuccess(VertxTestContext vertxTestContext) {
    JsonObject request = mock(JsonObject.class);
    JsonArray jsonArray = mock(JsonArray.class);
    when(user.getUserId()).thenReturn("someUserId");
    when(user.getResourceServerUrl()).thenReturn("someResourceServerUrl");
    when(request.getString(anyString())).thenReturn("dummyValue");
    when(request.getString("status")).thenReturn("ACTIVATED");
    when(asyncResult.succeeded()).thenReturn(true);
    when(request.getInteger("totalHits")).thenReturn(0);
    when(asyncResult.result()).thenReturn(request);
    when(request.getJsonArray(anyString())).thenReturn(jsonArray);
    when(jsonArray.isEmpty()).thenReturn(false);
    when(jsonArray.getJsonObject(anyInt())).thenReturn(request);
    when(request.getJsonArray(RESOURCE_IDS)).thenReturn(new JsonArray().add("abcd").add("abcd"));
    when(request.put(anyString(), anyString())).thenReturn(request);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(anyString(), any());

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
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeTransaction(anyList(), any());
    doAnswer(
            Answer ->
                Future.succeededFuture(
                    new JsonObject()
                        .put(PROVIDER, "dummyProviderId")
                        .put("ownerUserId", "someUserId")
                        .put("providerName", "dummyName")
                        .put(RESOURCE_ID, "dummyResourceId")
                        .put(RESOURCE_NAME, "dummyResourceName")
                        .put("accessPolicy", "SECURE")))
        .when(catService)
        .getItemDetails(anyString());

    productServiceImpl.createProduct(
        user,
        request,
        handler -> {
          if (handler.succeeded()) {
            JsonObject actual = handler.result();
            assertEquals(ResponseUrn.SUCCESS_URN.getUrn(), actual.getString(TYPE));
            assertEquals(ResponseUrn.SUCCESS_URN.getMessage(), actual.getString(TITLE));
            assertEquals(
                "urn:datakaveri.org:someUserId:dummyValue",
                actual.getJsonObject(RESULTS).getString(PRODUCT_ID));
            vertxTestContext.completeNow();

          } else {

            vertxTestContext.failNow("Failed to create product");
          }
        });
  }

  @Test
  @DisplayName("test delete product - success")
  public void testDeleteProduct(VertxTestContext testContext) {

    JsonObject auth_info = new JsonObject().put(IID, "iid");
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0,new JsonObject().put("key", "deleted successfully"));
    JsonObject request =
        new JsonObject().put(AUTH_INFO, auth_info).put(PRODUCT_ID, "id").put("totalHits", 1)
                        .put(RESULTS, jsonArray);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
      when(user.getUserId()).thenReturn("dummyProviderId");
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
            user,
        request,
        handler -> {
          if (handler.succeeded()) {
              LOGGER.info("handler.result().encodePrettily() : " + handler.result().encodePrettily());
              assertEquals(ResponseUrn.SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
              assertEquals(ResponseUrn.SUCCESS_URN.getMessage(), handler.result().getString(TITLE));
              assertEquals("Successfully deleted", handler.result().getString(DETAIL));
            testContext.completeNow();
          } else {
            testContext.failNow("delete product test failed");
          }
        });
  }

  @Test
  @DisplayName("test list products - success")
  public void testListProducts(VertxTestContext testContext) {

    lenient().when(jsonObjectMock.containsKey(RESOURCE_ID)).thenReturn(true);
    lenient().when(jsonObjectMock.getString(RESOURCE_ID)).thenReturn("resource-id-1");
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
            user,
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
