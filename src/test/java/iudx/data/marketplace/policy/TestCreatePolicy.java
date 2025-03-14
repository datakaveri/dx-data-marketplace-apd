package iudx.data.marketplace.policy;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.auditing.service.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.policies.service.CreatePolicy;
import iudx.data.marketplace.policies.service.model.User;
import iudx.data.marketplace.postgres.service.PostgresService;
import iudx.data.marketplace.razorpay.service.RazorPayService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, VertxExtension.class})

public class TestCreatePolicy {
    private static final Logger LOGGER = LogManager.getLogger(TestCreatePolicy.class);

    @Mock
    PostgresService postgresService;
    @Mock
    Api api;
    @Mock
    User provider;
    @Mock
    RazorPayService razorPayService;
    @Mock
    AuditingService auditingService;
    @Mock
    AsyncResult<JsonObject> asyncResult;
    @Mock Throwable throwable;
    private CreatePolicy policy;
    private JsonObject request;
    private String orderId;
    @Mock JsonObject jsonObjectMock;
    @Mock
    JsonArray jsonArrayMock;
    @Mock
    Void aVoid;

    @BeforeEach
    public void init(VertxTestContext vertxTestContext)
    {
        orderId = "dummyOrderId";
        policy = new CreatePolicy(postgresService, auditingService, api);
        lenient().when(auditingService.handleAuditLogs(any(User.class),any(JsonObject.class), anyString(), anyString()))
                        .thenReturn(Future.succeededFuture(aVoid));
        vertxTestContext.completeNow();
    }

  @Test
  @DisplayName("Test createPolicy : Success")
  public void testCreatePolicySuccess(VertxTestContext vertxTestContext) {

    JsonArray jsonArray = new JsonArray();
    jsonArray.add(
        0,
        new JsonObject()
            .put("id", "someResourceId")
            .put("capabilities", new JsonArray().add("sub").add("file")));

    JsonObject jsonObject =
        new JsonObject()
            .put("invoiceId", "someInvoiceId")
            .put("productVariantId", "somePvId")
            .put("expiry", 10)
            .put("consumerEmailId", "someEmailId")
            .put("resourceInfo", jsonArray)
            .put("providerId", "someProviderId");
    when(api.getPoliciesUrl()).thenReturn("dummyPolicyEndpoint");
    when(jsonObjectMock.encodePrettily()).thenReturn(jsonObject.encodePrettily());
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.size()).thenReturn(1);
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObject);
    when(postgresService.executeQuery(anyString())).thenReturn(Future.succeededFuture(jsonObjectMock));
    when(postgresService.executeTransaction(anyList())).thenReturn(Future.succeededFuture(jsonObjectMock));

    policy
        .createPolicy(orderId)
        .onComplete(
            handler -> {
              LOGGER.info("handler : " + handler);
              if (handler.succeeded()) {
                assertTrue(handler.result());
                verify(auditingService, times(1))
                    .handleAuditLogs(
                        any(User.class), any(JsonObject.class), anyString(), anyString());
                verify(postgresService, times(1)).executeQuery(anyString());
                verify(postgresService, atLeast(1)).executeTransaction(anyList());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Failed to create policy");
              }
            });
  }

    @Test
    @DisplayName("Test createPolicy when transaction failed: Failure")
    public void testCreatePolicyDuringTransactionFailure(VertxTestContext vertxTestContext) {

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(
                0,
                new JsonObject()
                        .put("id", "someResourceId")
                        .put("capabilities", new JsonArray().add("sub").add("file")));

        JsonObject jsonObject =
                new JsonObject()
                        .put("invoiceId", "someInvoiceId")
                        .put("productVariantId", "somePvId")
                        .put("expiry", 10)
                        .put("consumerEmailId", "someEmailId")
                        .put("resourceInfo", jsonArray)
                        .put("providerId", "someProviderId");
        when(api.getPoliciesUrl()).thenReturn("dummyPolicyEndpoint");
        when(jsonObjectMock.encodePrettily()).thenReturn(jsonObject.encodePrettily());
        when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
        when(jsonArrayMock.size()).thenReturn(1);
        when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObject);
      when(postgresService.executeQuery(anyString())).thenReturn(Future.succeededFuture(jsonObjectMock));
      when(postgresService.executeTransaction(anyList())).thenReturn(Future.failedFuture("Some failure message"));

        policy
                .createPolicy(orderId)
                .onComplete(
                        handler -> {
                            LOGGER.info("handler : " + handler);
                            if (handler.failed()) {
                                verify(postgresService, times(1)).executeQuery(anyString());
                                verify(postgresService, atLeast(1)).executeTransaction(anyList());
                                assertEquals("Error : Some failure message", handler.cause().getMessage());
                                vertxTestContext.completeNow();
                            } else {
                                vertxTestContext.failNow("Created policy during transaction failure");
                            }
                        });
    }

    @Test
    @DisplayName("Test createPolicy when there are multiple invoices for the same order: Failure")
    public void testCreatePolicyFailure(VertxTestContext vertxTestContext) {

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(
                0,
                new JsonObject()
                        .put("id", "someResourceId")
                        .put("capabilities", new JsonArray().add("sub").add("file")));

        JsonObject jsonObject =
                new JsonObject()
                        .put("invoiceId", "someInvoiceId")
                        .put("productVariantId", "somePvId")
                        .put("expiry", 10)
                        .put("consumerEmailId", "someEmailId")
                        .put("resourceInfo", jsonArray)
                        .put("providerId", "someProviderId");
        when(jsonObjectMock.encodePrettily()).thenReturn(jsonObject.encodePrettily());
        when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
        when(jsonArrayMock.size()).thenReturn(2);
      when(postgresService.executeQuery(anyString())).thenReturn(Future.succeededFuture(jsonObjectMock));

        policy
                .createPolicy(orderId)
                .onComplete(
                        handler -> {
                            if (handler.failed()) {
                                verify(postgresService, times(1)).executeQuery(anyString());
                                assertEquals("Error : No payment found for the given order", handler.cause().getMessage());
                                vertxTestContext.completeNow();
                            } else {
                                vertxTestContext.failNow("Created policy during transaction failure");
                            }
                        });
    }

    @Test
    @DisplayName("Test createPolicy when DB execution failure: Failure")
    public void testCreatePolicyDuringDbFailure(VertxTestContext vertxTestContext) {

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(
                0,
                new JsonObject()
                        .put("id", "someResourceId")
                        .put("capabilities", new JsonArray().add("sub").add("file")));

        JsonObject jsonObject =
                new JsonObject()
                        .put("invoiceId", "someInvoiceId")
                        .put("productVariantId", "somePvId")
                        .put("expiry", 10)
                        .put("consumerEmailId", "someEmailId")
                        .put("resourceInfo", jsonArray)
                        .put("providerId", "someProviderId");
        when(throwable.getMessage()).thenReturn("Some failure message");
      when(postgresService.executeQuery(anyString())).thenReturn(Future.failedFuture(throwable));

        policy
                .createPolicy(orderId)
                .onComplete(
                        handler -> {
                            LOGGER.info("handler : " + handler);
                            if (handler.failed()) {
                                verify(postgresService, times(1)).executeQuery(anyString());
                                assertEquals("Error : Some failure message", handler.cause().getMessage());
                                vertxTestContext.completeNow();
                            } else {
                                vertxTestContext.failNow("Created policy during transaction failure");
                            }
                        });
    }
}
