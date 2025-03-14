package iudx.data.marketplace.product;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.catalogueService.CatalogueService;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.configuration.Configuration;
import iudx.data.marketplace.policies.service.model.User;
import iudx.data.marketplace.postgres.service.PostgresService;
import iudx.data.marketplace.product.service.ProductServiceImpl;
import iudx.data.marketplace.product.util.QueryBuilder;
import iudx.data.marketplace.razorpay.service.RazorPayService;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
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
    when(request.getInteger("totalHits")).thenReturn(0);
    when(request.getJsonArray(anyString())).thenReturn(jsonArray);
    when(jsonArray.isEmpty()).thenReturn(false);
    when(jsonArray.getJsonObject(anyInt())).thenReturn(request);
    when(request.getJsonArray(RESOURCE_IDS)).thenReturn(new JsonArray().add("abcd").add("abcd"));
    when(request.put(anyString(), anyString())).thenReturn(request);
    when(postgresService.executeQuery(anyString())).thenReturn(Future.succeededFuture(request));
    when(postgresService.executeTransaction(anyList())).thenReturn(Future.succeededFuture(request));
    when(postgresService.executeCountQuery(anyString())).thenReturn(Future.succeededFuture(request));

    doAnswer(
            Answer ->
                Future.succeededFuture(
                    new JsonObject()
                        .put(PROVIDER, "dummyProviderId")
                        .put("ownerUserId", "someUserId")
                        .put("providerName", "dummyName")
                        .put(RESOURCE_ID, "dummyResourceId")
                        .put(RESOURCE_NAME, "dummyResourceName")
                        .put("accessPolicy", "SECURE")
                         .put(APD_URL, "dmp-apd.iudx.io")))
        .when(catService)
        .getItemDetails(anyString());

    productServiceImpl.createProduct(
        user,
        request).onComplete(
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
      when(user.getUserId()).thenReturn("dummyProviderId");
    when(postgresService.executeCountQuery(anyString())).thenReturn(Future.succeededFuture(request));

    when(postgresService.executePreparedQuery(anyString(),any())).thenReturn(Future.succeededFuture(request));

    productServiceImpl.deleteProduct(
            user,
        request).onComplete(
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
    when(postgresService.executePreparedQuery(anyString(),any())).thenReturn(Future.succeededFuture(jsonObjectMock));

    productServiceImpl.listProducts(
            user,
        new JsonObject()).onComplete(
        handler -> {
          if (handler.succeeded()) {
            verify(postgresService, times(1)).executePreparedQuery(anyString(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("list products test failed");
          }
        });
  }

  @Test
  @DisplayName("test product exists future")
  public void testProductExistsFuture(VertxTestContext testContext) {
    when(jsonObjectMock.getInteger("totalHits")).thenReturn(1);
    when(postgresService.executeCountQuery(anyString())).thenReturn(Future.succeededFuture(jsonObjectMock));

    productServiceImpl
        .checkIfProductExists(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(postgresService, times(1)).executeCountQuery(anyString());
                testContext.completeNow();
              } else {
                testContext.failNow("product exists future test failed");
              }
            });
  }
}
