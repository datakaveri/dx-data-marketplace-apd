package iudx.data.marketplace.razorpay;

import com.razorpay.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.Util;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.policies.VerifyPolicy;
import iudx.data.marketplace.policy.TestCreatePolicy;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.data.marketplace.product.util.Constants.*;
import static iudx.data.marketplace.razorpay.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestRazorpayServiceImpl {
  private static final Logger LOGGER = LogManager.getLogger(TestRazorpayServiceImpl.class);

  @Mock PostgresService postgresService;
  @Mock Api api;
  @Mock User provider;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Throwable throwable;
  @Mock JsonObject jsonObjectMock;
  @Mock JsonArray jsonArrayMock;
  JsonObject userJson;
  JsonObject ownerJson;
  JsonObject resourceJson;
  @Mock RazorpayClient client;
  @Mock Order order;
  @Mock OrderClient orderClient;
  @Mock AccountClient accountClient;
  @Mock Account account;
  private RazorPayService service;
  private JsonObject request;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {

    when(jsonObjectMock.getString(anyString())).thenReturn("someValue");
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.getString(anyInt())).thenReturn("someTable");
    client.account = accountClient;
    client.orders = orderClient;

    request = new JsonObject();
    service = new RazorPayServiceImpl(client, postgresService, jsonObjectMock);

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
        .executePreparedQuery(anyString(), any(), any());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test create order: Success")
  public void testCreateOrderSuccess(VertxTestContext vertxTestContext) throws RazorpayException {
    request
        .put(PRICE, 120.0)
        .put(ACCOUNT_ID, "dummyAccountId")
        .put("product_variant_name", "dummyPvName")
        .put("product_id", "someProductId")
        .put("provider_id", "someProviderId");

    when(orderClient.create(any())).thenReturn(order);
    when(order.get(anyString()))
        .thenReturn(
            CREATED,
            new JSONArray()
                .put(
                    0, new JSONObject().put(ERROR, new JSONObject().put(REASON, JSONObject.NULL))));
    JSONObject jsonObject = new JSONObject().put("someKey", "someValue");
    when(order.toJson()).thenReturn(jsonObject);

    service
        .createOrder(request)
        .onComplete(
            handler -> {
              LOGGER.info(handler);
              if (handler.succeeded()) {
                assertEquals(jsonObject.toString(), handler.result().toString());
                vertxTestContext.completeNow();

              } else {

                vertxTestContext.failNow("Failed to create order");
              }
            });
  }

  @Test
  @DisplayName("Test create linked account : Success")
  public void testCreateLinkedAccountSuccess(VertxTestContext vertxTestContext) {
    String requestString = request.put(ID, "someId").toString();
    try {
      when(accountClient.create(any())).thenReturn(account);
    } catch (RazorpayException e) {
      throw new RuntimeException(e);
    }
    when(account.toString()).thenReturn(requestString);

    service
        .createLinkedAccount(requestString)
        .onComplete(
            handler -> {
              LOGGER.info(handler);
              if (handler.succeeded()) {
                assertEquals("someId", handler.result().getString("accountId"));
                vertxTestContext.completeNow();

              } else {

                vertxTestContext.failNow("Failed to create account");
              }
            });
  }

  @Test
  @DisplayName("Test fetch linked account : Success")
  public void testFetchLinkedAccountSuccess(VertxTestContext vertxTestContext) {
    String requestString = request.put("someKey", "someValue").toString();
    try {
      when(accountClient.fetch(any())).thenReturn(account);
    } catch (RazorpayException e) {
      throw new RuntimeException(e);
    }
    when(account.toString()).thenReturn(requestString);

    service
        .fetchLinkedAccount(requestString)
        .onComplete(
            handler -> {
              LOGGER.info(handler);
              if (handler.succeeded()) {
                assertEquals("someValue", handler.result().getString("someKey"));
                vertxTestContext.completeNow();

              } else {

                vertxTestContext.failNow("Failed to fetch account");
              }
            });
  }

  @Test
  @DisplayName("Test update linked account : Success")
  public void testUpdateLinkedAccountSuccess(VertxTestContext vertxTestContext) {
    String requestString = request.put("reference_id", "some_reference_id").toString();
    try {
      when(accountClient.edit(anyString(), any())).thenReturn(account);
    } catch (RazorpayException e) {
      throw new RuntimeException(e);
    }
    when(account.toString()).thenReturn(requestString);

    service
        .updateLinkedAccount(requestString, requestString)
        .onComplete(
            handler -> {
              LOGGER.info(handler);
              if (handler.succeeded()) {
                assertTrue(handler.result());
                vertxTestContext.completeNow();

              } else {

                vertxTestContext.failNow("Failed to update account");
              }
            });
  }

    @Test
    @Disabled
    @DisplayName("Test verify payment : Success")
    public void testVerifyPaymentSuccess(VertxTestContext vertxTestContext) {
    request.put(RAZORPAY_ORDER_ID, "order_NmTravDc9v3NhH");
    request.put(RAZORPAY_PAYMENT_ID, "somePaymentId");
    request.put(RAZORPAY_SIGNATURE, "someSignature");
        try {
            when(accountClient.edit(anyString(), any())).thenReturn(account);
        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }

        service
                .verifyPayment(request)
                .onComplete(
                        handler -> {
                            LOGGER.info(handler);
                            if (handler.succeeded()) {

                                vertxTestContext.completeNow();

                            } else {

                                vertxTestContext.failNow("Failed to update account");
                            }
                        });
    }
}
