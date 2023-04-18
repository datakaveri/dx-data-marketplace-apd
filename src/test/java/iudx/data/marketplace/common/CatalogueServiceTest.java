package iudx.data.marketplace.common;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.configuration.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.data.marketplace.common.Constants.PROVIDER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class CatalogueServiceTest {

  private static Configuration configuration;
  private static CatalogueService catalogueService;

  @Mock HttpRequest<Buffer> httpRequest;
  @Mock AsyncResult<HttpResponse<Buffer>> asyncResult;
  @Mock HttpResponse<Buffer> httpResponse;
  @Mock JsonObject jsonObjectMock;
  @Mock JsonArray jsonArrayMock;

  @BeforeEach
  @DisplayName("setup")
  public void setup(Vertx vertx, VertxTestContext testContext) {
    configuration = new Configuration();
    JsonObject config = configuration.configLoader(3, vertx);
    CatalogueService.catWebClient = mock(WebClient.class);
    when(CatalogueService.catWebClient.get(anyInt(), anyString(), anyString()))
        .thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.bodyAsJsonObject()).thenReturn(jsonObjectMock);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {

                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(httpRequest)
        .send(any());

    catalogueService = new CatalogueService(vertx, config);
    testContext.completeNow();
  }

  @Test
  @DisplayName("test get provider details")
  public void testGetProviderDetails(VertxTestContext testContext) {

    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray("type").contains("iudx:Provider")).thenReturn(true);
    when(jsonObjectMock.getString("description")).thenReturn("new desc");
    catalogueService
        .getItemDetails("new-item-id")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(CatalogueService.catWebClient, times(1))
                    .get(anyInt(), anyString(), anyString());
                verify(httpRequest, times(1)).addQueryParam(anyString(), anyString());
                assertEquals("new desc", handler.result().getString(PROVIDER_NAME));
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("test get dataset details")
  public void testGetDatasetDetails(VertxTestContext testContext) {

    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray("type").contains("iudx:Provider")).thenReturn(false);
    when(jsonObjectMock.getJsonArray("type").contains("iudx:ResourceGroup")).thenReturn(true);
    when(jsonObjectMock.getString("label")).thenReturn("labelxyz");
    when(jsonObjectMock.getString("accessPolicy")).thenReturn("OPEN");
    catalogueService
        .getItemDetails("new-item-id")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(CatalogueService.catWebClient, times(1))
                    .get(anyInt(), anyString(), anyString());
                verify(httpRequest, times(1)).addQueryParam(anyString(), anyString());
                assertEquals(
                    new JsonObject()
                        .put("datasetID", "new-item-id")
                        .put("datasetName", "labelxyz")
                        .put("accessPolicy", "OPEN"),
                    handler.result());
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("test get resource count")
  public void testGetResourceCount(VertxTestContext testContext) {

    when(jsonObjectMock.getInteger("totalHits")).thenReturn(5);
    catalogueService
        .getResourceCount("dataset-id")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(CatalogueService.catWebClient, times(1))
                    .get(anyInt(), anyString(), anyString());
                verify(httpRequest, times(2)).addQueryParam(anyString(), anyString());
                assertEquals(
                    new JsonObject().put("datasetID", "dataset-id").put("totalHits", 5),
                    handler.result());
                testContext.completeNow();
              }
            });
  }
}
