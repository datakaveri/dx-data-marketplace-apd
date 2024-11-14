package iudx.data.marketplace.apiserver.provider.linkedAccount;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.provider.linkedaccount.CreateLinkedAccount;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.razorpay.RazorPayService;
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

import static iudx.data.marketplace.apiserver.provider.linkedaccount.util.Constants.FAILURE_MESSAGE;
import static iudx.data.marketplace.apiserver.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestCreateLinkedAccount {
  private static final Logger LOGGER = LogManager.getLogger(TestCreateLinkedAccount.class);

  @Mock PostgresService postgresService;
  @Mock Api api;
  @Mock User provider;
  @Mock RazorPayService razorPayService;
  @Mock AuditingService auditingService;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Throwable throwable;
  private CreateLinkedAccount account;
  private JsonObject request;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    account = new CreateLinkedAccount(postgresService, api, auditingService, razorPayService);
    request =
        new JsonObject(
            "{\n"
                + "    \"phone\": \"9444477733\",\n"
                + "    \"legalBusinessName\": \"Test corp 1\",\n"
                + "    \"customerFacingBusinessName\": \"Test corp 2\",\n"
                + "    \"businessType\": \"partnership\",\n"
                + "    \"contactName\": \"Some name\",\n"
                + "    \"profile\": {\n"
                + "        \"category\": \"healthcare\",\n"
                + "        \"subcategory\": \"clinic\",\n"
                + "        \"addresses\": {\n"
                + "            \"registered\": {\n"
                + "                \"street1\": \"507, Koramangala 1st block\",\n"
                + "                \"street2\": \"MG Road\",\n"
                + "                \"city\": \"Bengaluru\",\n"
                + "                \"state\": \"KARNATAKA\",\n"
                + "                \"postalCode\": \"560038\",\n"
                + "                \"country\": \"IN\"\n"
                + "            }\n"
                + "        }\n"
                + "    },\n"
                + "    \"legalInfo\": {\n"
                + "        \"pan\": \"AAACL1234C\",\n"
                + "        \"gst\": \"18AABCU9603R1ZM\"\n"
                + "    }\n"
                + "}");

    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test initiateCreateLinkedAccount : Success")
  public void testInitiateCreateLinkedAccountSuccess(VertxTestContext vertxTestContext) {

    JsonObject resultFromRzp =
        new JsonObject()
            .put("accountId", "dummyAccountId")
            .put("razorpayAccountProductId", "dummyAccountProductId");
    when(provider.getEmailId()).thenReturn("dummyEmailId");
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(razorPayService.createLinkedAccount(anyString()))
        .thenReturn(Future.succeededFuture(resultFromRzp));
    when(razorPayService.requestProductConfiguration(any(JsonObject.class)))
        .thenReturn(Future.succeededFuture(resultFromRzp));
    when(asyncResult.succeeded()).thenReturn(true);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(anyString(), any());
    when(auditingService.handleAuditLogs(
            any(User.class), any(JsonObject.class), anyString(), anyString()))
        .thenReturn(Future.succeededFuture());

    account
        .initiateCreatingLinkedAccount(request, provider)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(ResponseUrn.SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
                assertEquals(
                    ResponseUrn.SUCCESS_URN.getMessage(), handler.result().getString(TITLE));
                assertEquals(
                    "Linked account created successfully", handler.result().getString(DETAIL));
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Failed to create linked account");
              }
            });
  }

  @Test
  @DisplayName("Test initiateCreateLinkedAccount : Database Failure")
  public void testInitiateCreateLinkedAccountFailure(VertxTestContext vertxTestContext) {

    JsonObject resultFromRzp =
            new JsonObject()
                    .put("accountId", "dummyAccountId")
                    .put("razorpayAccountProductId", "dummyAccountProductId");
    when(provider.getEmailId()).thenReturn("dummyEmailId");
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(razorPayService.createLinkedAccount(anyString()))
            .thenReturn(Future.succeededFuture(resultFromRzp));
    when(razorPayService.requestProductConfiguration(any(JsonObject.class)))
            .thenReturn(Future.succeededFuture(resultFromRzp));
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Some dummy failure message");

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
            .when(postgresService)
            .executeQuery(anyString(), any());

    account
            .initiateCreatingLinkedAccount(request, provider)
            .onComplete(
                    handler -> {
                      if (handler.failed()) {
                          JsonObject result = new JsonObject(handler.cause().getMessage());
                        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(), result.getInteger(TYPE));
                        assertEquals(
                                ResponseUrn.DB_ERROR_URN.getUrn(), result.getString(TITLE));
                        assertEquals(
                                FAILURE_MESSAGE + "Internal Server Error", result.getString(DETAIL));
                        vertxTestContext.completeNow();
                      } else {
                        vertxTestContext.failNow("Linked account created successfully when there was failure in DB execution");
                      }
                    });
  }

    @Test
    @DisplayName("Test initiateCreateLinkedAccount : Razorpay Failure")
    public void testInitiateCreateLinkedAccountRzpFailure(VertxTestContext vertxTestContext) {

        JsonObject resultFromRzp =
                new JsonObject()
                        .put("accountId", "dummyAccountId")
                        .put("razorpayAccountProductId", "dummyAccountProductId");
        when(provider.getEmailId()).thenReturn("dummyEmailId");
        when(provider.getUserId()).thenReturn("dummyProviderId");
        when(razorPayService.createLinkedAccount(anyString()))
                .thenReturn(Future.succeededFuture(resultFromRzp));
        when(razorPayService.requestProductConfiguration(any(JsonObject.class)))
                .thenReturn(Future.failedFuture("Some dummy failure"));


        account
                .initiateCreatingLinkedAccount(request, provider)
                .onComplete(
                        handler -> {
                            if (handler.failed()) {
                            assertEquals("Some dummy failure", handler.cause().getMessage());
                                vertxTestContext.completeNow();
                            } else {
                                vertxTestContext.failNow("Linked account created successfully even when there was failure from Razorpay ");
                            }
                        });
    }


}
