package iudx.data.marketplace.razorpay;

import com.razorpay.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.stream.Stream;

import static iudx.data.marketplace.apiserver.util.Constants.DETAIL;
import static iudx.data.marketplace.apiserver.util.Constants.TITLE;
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
  @Mock ProductClient productClient;
  private RazorPayServiceImpl service;
  private JsonObject request;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {

    when(jsonObjectMock.getString(anyString())).thenReturn("someValue");
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.getString(anyInt())).thenReturn("someTable");
    client.account = accountClient;
    client.orders = orderClient;
    client.product = productClient;
    throwable = mock(RazorpayException.class);
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
  @DisplayName("Test verify payment : Failure")
  public void testVerifyPaymentFailure(VertxTestContext vertxTestContext) {
    request.put(RAZORPAY_ORDER_ID, "someOrder");
    request.put(RAZORPAY_PAYMENT_ID, "somePaymentId");
    request.put(RAZORPAY_SIGNATURE, "somePaymentSignature");

    service
        .verifyPayment(request)
        .onComplete(
            handler -> {
              LOGGER.info(handler);
              if (handler.failed()) {

                JsonObject expected =
                    new JsonObject()
                        .put(TYPE, ResponseUrn.INVALID_PAYMENT.getUrn())
                        .put(TITLE, "RazorPay Error")
                        .put(DETAIL, ResponseUrn.INVALID_PAYMENT.getMessage());
                assertEquals(expected.toString(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Succeeded when payment verification failed");
              }
            });
  }

  @Test
  @DisplayName("Test webhookSignatureValidator : Failure")
  public void testWebhookSignatureValidatorFailure(VertxTestContext vertxTestContext) {
    request.put("someKey", "someValue");

    service
        .webhookSignatureValidator(request, "someSignatureHeader")
        .onComplete(
            handler -> {
              LOGGER.info(handler);
              if (handler.failed()) {

                JsonObject expected =
                    new JsonObject()
                        .put(TYPE, ResponseUrn.INVALID_WEBHOOK_REQUEST.getUrn())
                        .put(TITLE, "RazorPay Error")
                        .put(DETAIL, ResponseUrn.INVALID_WEBHOOK_REQUEST.getMessage());
                assertEquals(expected.toString(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Succeeded when webhook verification failed");
              }
            });
  }

  @Test
  @DisplayName("Test request product configuration : Success")
  public void testRequestProductConfigurationSuccess(VertxTestContext vertxTestContext) {
    request.put("accountId", "someValue");
    try {
      when(productClient.requestProductConfiguration(anyString(), any())).thenReturn(account);
    } catch (RazorpayException e) {
      throw new RuntimeException(e);
    }
    String accountProductId = "someRazorpayAccountProductId";
    JsonObject expected = new JsonObject().put(ID, accountProductId);
    when(account.toString()).thenReturn(expected.encode());
    service
        .requestProductConfiguration(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                expected.put("razorpayAccountProductId", accountProductId);
                expected.remove(ID);
                assertEquals(expected, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Failed to request product configuration");
              }
            });
  }

  @Test
  @DisplayName("Test fetchProductConfiguration : Success")
  public void testFetchProductConfigurationSuccess(VertxTestContext vertxTestContext) {
    request.put("account_id", "some_account_id");
    request.put("rzp_account_product_id", "some_rzp_account_product_id");
    try {
      when(productClient.fetch(anyString(), any())).thenReturn(account);
    } catch (RazorpayException e) {
      throw new RuntimeException(e);
    }
    String activationStatus = "ACTIVATED";
    JsonObject expected = new JsonObject().put("activation_status", activationStatus);
    when(account.toString()).thenReturn(expected.encode());
    service
        .fetchProductConfiguration(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertTrue(handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Failed to fetch product configuration");
              }
            });
  }

  @Test
  @DisplayName("Test fetchProductConfiguration : Failure")
  public void testFetchProductConfigurationFailure(VertxTestContext vertxTestContext) {
    request.put("account_id", "some_account_id");
    request.put("rzp_account_product_id", "some_rzp_account_product_id");
    try {
      when(productClient.fetch(anyString(), any())).thenReturn(account);
    } catch (RazorpayException e) {
      throw new RuntimeException(e);
    }
    String activationStatus = "INACTIVE";
    JsonObject jsonObject = new JsonObject().put("activation_status", activationStatus);
    when(account.toString()).thenReturn(jsonObject.encode());
    service
        .fetchProductConfiguration(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                String actual = handler.cause().getMessage();
                assertEquals(
                    "To activate linked account please complete the KYC, filling account information etc., in your Razorpay merchant dashboard",
                    actual);
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Succeeded when account status is inactive");
              }
            });
  }

  @Test
  @DisplayName("Test request product configuration : Failure")
  public void testRequestProductConfigurationFailure(VertxTestContext vertxTestContext)
      throws RazorpayException {
    throwable = mock(RazorpayException.class);
    request.put("accountId", "someValue");

    when(productClient.requestProductConfiguration(anyString(), any())).thenThrow(throwable);
    when(throwable.getMessage()).thenReturn("The api key/secret provided is invalid");

    service
        .requestProductConfiguration(request)
        .onComplete(
            handler -> {
              LOGGER.info(handler);
              if (handler.failed()) {
                JsonObject actual = new JsonObject(handler.cause().getMessage());
                assertEquals(400, actual.getInteger(TYPE));
                assertEquals(ResponseUrn.BAD_REQUEST_URN.getUrn(), actual.getString(TITLE));
                assertEquals(
                    "User registration incomplete : Internal Server Error",
                    actual.getString(DETAIL));
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Succeeded when account status is inactive");
              }
            });
  }

    @Test
    @DisplayName("Test update linked account : Failure")
    public void testUpdateLinkedAccountFailure(VertxTestContext vertxTestContext) {
        String requestString = request.put("reference_id", "some_reference_id").toString();
        try {
            when(accountClient.edit(anyString(), any())).thenThrow(throwable);
            when(throwable.getMessage()).thenReturn("Merchant activation form has been locked for editing by admin.");
        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }

        service
                .updateLinkedAccount(requestString, requestString)
                .onComplete(
                        handler -> {
                            LOGGER.info(handler);
                            if (handler.failed()) {
                                JsonObject actual = new JsonObject(handler.cause().getMessage());
                                assertEquals(400, actual.getInteger(TYPE));
                                assertEquals(ResponseUrn.BAD_REQUEST_URN.getUrn(), actual.getString(TITLE));
                                assertEquals(
                                        "Linked account updation failed as merchant activation form has been locked for editing by admin",
                                        actual.getString(DETAIL));
                                vertxTestContext.completeNow();

                            } else {
                                vertxTestContext.failNow("Succeeded when account updation failed");
                            }
                        });
    }

    @Test
    @DisplayName("Test fetch linked account : Failure")
    public void testFetchLinkedAccountFailure(VertxTestContext vertxTestContext) {
        String requestString = request.put("someKey", "someValue").toString();
        try {
            when(accountClient.fetch(any())).thenThrow(throwable);
            when(throwable.getMessage()).thenReturn("Linked account does not exist");
        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }

        service
                .fetchLinkedAccount(requestString)
                .onComplete(
                        handler -> {
                            LOGGER.info(handler);
                            if (handler.failed()) {
                                JsonObject actual = new JsonObject(handler.cause().getMessage());
                                assertEquals(500, actual.getInteger(TYPE));
                                assertEquals(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn(), actual.getString(TITLE));
                                assertEquals(
                                        "User registration incomplete : Internal Server Error",
                                        actual.getString(DETAIL));
                                vertxTestContext.completeNow();

                            } else {

                                vertxTestContext.failNow("Succeeded when linked account was not found");
                            }
                        });
    }

    @Test
    @DisplayName("Test create linked account : Failure")
    public void testCreateLinkedAccountFailure(VertxTestContext vertxTestContext) {
        String requestString = request.put(ID, "someId").toString();
        try {
            when(accountClient.create(any())).thenThrow(throwable);
            when(throwable.getMessage()).thenReturn("Invalid business subcategory for business category");
        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }

        service
                .createLinkedAccount(requestString)
                .onComplete(
                        handler -> {
                            LOGGER.info(handler);
                            if (handler.failed()) {
                                JsonObject actual = new JsonObject(handler.cause().getMessage());
                                assertEquals(400, actual.getInteger(TYPE));
                                assertEquals(ResponseUrn.BAD_REQUEST_URN.getUrn(), actual.getString(TITLE));
                                assertEquals(
                                        "User registration incomplete : subcategory or category is invalid",
                                        actual.getString(DETAIL));
                                vertxTestContext.completeNow();

                            } else {

                                vertxTestContext.failNow("Created linked account when the business subcategory is invalid");
                            }
                        });
    }

    public static Stream<Arguments> input() {
        return Stream.of(
                Arguments.of(mock(RazorpayException.class), "Some Razorpay failure"),
                Arguments.of(mock(JSONException.class), "Some json exception"));

    }
    @ParameterizedTest
    @MethodSource("input")
    @DisplayName("Test create order during exception: Failure")
    public void testCreateOrderFailure(Throwable throwable,String failureMessage, VertxTestContext vertxTestContext) throws RazorpayException {
        request
                .put(PRICE, 120.0)
                .put(ACCOUNT_ID, "dummyAccountId")
                .put("product_variant_name", "dummyPvName")
                .put("product_id", "someProductId")
                .put("provider_id", "someProviderId");

        when(orderClient.create(any())).thenThrow(throwable);
        when(throwable.getMessage()).thenReturn(failureMessage);

        service
                .createOrder(request)
                .onComplete(
                        handler -> {
                            LOGGER.info(handler);
                            if (handler.failed()) {
                                JsonObject actual = new JsonObject(handler.cause().getMessage());
                                assertEquals("RazorPay Error", actual.getString(TITLE));
                                assertEquals(failureMessage, actual.getString(DETAIL));
                                assertEquals(ResponseUrn.ORDER_CREATION_FAILED.getUrn(), actual.getString(TYPE));
                                vertxTestContext.completeNow();

                            } else {

                                vertxTestContext.failNow("Failed to create order");
                            }
                        });
    }

    @Test
    @DisplayName("Test create order when there was error from Razorpay: Failure")
    public void testCreateOrder(VertxTestContext vertxTestContext) throws RazorpayException {
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
                                        0, new JSONObject().put(ERROR, new JSONObject().put(REASON, "Some Razorpay technical error"))));
        JSONObject jsonObject = new JSONObject().put("someKey", "someValue");

        service
                .createOrder(request)
                .onComplete(
                        handler -> {
                            LOGGER.info(handler);
                            if (handler.failed()) {
                                JsonObject actual = new JsonObject(handler.cause().getMessage());
                                assertEquals("RazorPay Error", actual.getString(TITLE));
                                assertEquals("Order creation returned with error", actual.getString(DETAIL));
                                assertEquals(ResponseUrn.ORDER_CREATION_FAILED.getUrn(), actual.getString(TYPE));
                                vertxTestContext.completeNow();

                            } else {

                                vertxTestContext.failNow("Failed to create order");
                            }
                        });
    }

    @Test
    @DisplayName("Test record payment method : Success")
    public void testRecordPaymentSuccess(VertxTestContext vertxTestContext)
    {
        request
                .put(RAZORPAY_ORDER_ID, "someOrderId")
                .put(RAZORPAY_PAYMENT_ID, "somePaymentId")
                .put(RAZORPAY_SIGNATURE, "someRzpSignature");
        JsonObject expected = new JsonObject()
                .put("someKey", "someValue");

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(expected);

        service.recordPayment(request)
                .onComplete(handler -> {
                    LOGGER.info(handler);
                    if(handler.succeeded())
                    {

                        assertEquals(expected, handler.result());
                        vertxTestContext.completeNow();

                    }
                    else
                    {

                        vertxTestContext.failNow("Failed to insert payment information in DB");
                    }
                });


    }

    @Test
    @DisplayName("Test record payment method : Failure")
    public void testRecordPaymentFailure(VertxTestContext vertxTestContext)
    {
        request
                .put(RAZORPAY_ORDER_ID, "abcd")
                .put(RAZORPAY_PAYMENT_ID, "asdjdfadfadf")
                .put(RAZORPAY_SIGNATURE, "adfadfadfadfadfasd");

        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("Dummy failure message from database");

        service.recordPayment(request)
                .onComplete(handler -> {
                    LOGGER.info(handler);
                    if(handler.failed())
                    {

                        assertEquals("Dummy failure message from database", handler.cause().getMessage());
                        vertxTestContext.completeNow();

                    }
                    else
                    {

                        vertxTestContext.failNow("Succeeded when insertion failed");
                    }
                });

    }

    @Test
    @DisplayName("Test fetchProductConfiguration : Failure")
    public void testFetchProductConfiguration(VertxTestContext vertxTestContext) {
        request.put("account_id", "sadfsdfsdfsdf");
        request.put("rzp_account_product_id", "asdaddfsdfsdfgsfg");
        try {
            when(productClient.fetch(anyString(), any())).thenThrow(throwable);
            when(throwable.getMessage()).thenReturn("some failure message");
        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }
        service
                .fetchProductConfiguration(request)
                .onComplete(
                        handler -> {
                            LOGGER.info(handler);
                            if (handler.failed()) {
                                JsonObject actual = new JsonObject(handler.cause().getMessage());
                                assertEquals(500, actual.getInteger(TYPE));
                                assertEquals(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn(), actual.getString(TITLE));
                                assertEquals(
                                        "User registration incomplete : Internal Server Error",
                                        actual.getString(DETAIL));
                                vertxTestContext.completeNow();
                            } else {
                                vertxTestContext.failNow("Succeeded when fetch from Razorpay failed");
                            }
                        });
    }
}
