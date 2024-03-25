package iudx.data.marketplace.apiserver.provider.linkedAccount;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
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

import static iudx.data.marketplace.apiserver.util.Constants.RESULTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestUpdateLinkedAccount {
  private static Logger LOGGER = LogManager.getLogger(TestUpdateLinkedAccount.class);
  @Mock PostgresService postgresService;
  @Mock Api api;
  @Mock User provider;
  @Mock RazorPayService razorPayService;
  @Mock AuditingService auditingService;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Throwable throwable;
  private JsonObject request;
  private UpdateLinkedAccount account;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    account = new UpdateLinkedAccount(postgresService, api, auditingService, razorPayService);
    request =
        new JsonObject(
            "{\n"
                + "  \"phone\": \"9100090000\",\n"
                + "  \"legalBusinessName\": \"Dummy Corp V2\",\n"
                + "  \"customerFacingBusinessName\": \"Some Other Corp\",\n"
                + "  \"contactName\": \"Test Name\",\n"
                + "  \"profile\": {\n"
                + "    \"category\": \"healthcare\",\n"
                + "    \"subcategory\": \"doctors\",\n"
                + "    \"addresses\": {\n"
                + "      \"registered\": {\n"
                + "        \"street1\": \"Outer Ring road\",\n"
                + "        \"street2\": \"MG Road\",\n"
                + "        \"city\": \"Bengaluru\",\n"
                + "        \"state\": \"KARNATAKA\",\n"
                + "        \"postalCode\": \"560038\",\n"
                + "        \"country\": \"india\"\n"
                + "      }\n"
                + "    }\n"
                + "  },\n"
                + "  \"legalInfo\": {\n"
                + "    \"pan\": \"ABACL1234C\",\n"
                + "    \"gst\": \"19AABCU9603R1ZM\"\n"
                + "  }\n"
                + "}");

    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test initiateUpdatingLinkedAccount method : Success")
  public void testUpdateLinkedAccount(VertxTestContext vertxTestContext) {

    JsonObject jsonObject =
        new JsonObject()
            .put("account_id", "some_dummy_id")
            .put("reference_id", "some_reference_id");
    JsonArray result = new JsonArray().add(jsonObject);
    JsonObject resultJson = new JsonObject().put(RESULTS, result);
    when(provider.getEmailId()).thenReturn("dummyEmailId");
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(resultJson);
    when(razorPayService.updateLinkedAccount(anyString(), anyString()))
        .thenReturn(Future.succeededFuture(true));

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
        .initiateUpdatingLinkedAccount(request, provider)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject expected =
                    new RespBuilder()
                        .withType(ResponseUrn.SUCCESS_URN.getUrn())
                        .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                        .withDetail("Linked account updated successfully")
                        .getJsonResponse();
                assertEquals(expected, handler.result());
                assertEquals("Dummy Corp V2", account.getLegalBusinessName());
                assertEquals("some_dummy_id", account.getRzpAccountId());
                assertEquals("some_reference_id", account.getReferenceId());
                vertxTestContext.completeNow();

              } else {

                vertxTestContext.failNow("Failed to update linked account");
              }
            });
  }

  @Test
  @DisplayName("Test initiateUpdatingLinkedAccount method during response from DB : Failure")
  public void testUpdateLinkedAccountDbFailure(VertxTestContext vertxTestContext) {

    JsonArray result = new JsonArray();
    JsonObject resultJson = new JsonObject().put(RESULTS, result);
    when(provider.getEmailId()).thenReturn("dummyEmailId");
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(resultJson);

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
        .initiateUpdatingLinkedAccount(request, provider)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                JsonObject expected =
                    new RespBuilder()
                        .withType(HttpStatusCode.NOT_FOUND.getValue())
                        .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                        .withDetail("Linked account cannot be updated as, it is not found")
                        .getJsonResponse();
                JsonObject actual = new JsonObject(handler.cause().getMessage());
                assertEquals(expected, actual);
                vertxTestContext.completeNow();

              } else {

                vertxTestContext.failNow("Succeeded to update the non-existent linked account");
              }
            });
  }

  @Test
  @DisplayName("Test initiateUpdatingLinkedAccount method during failure from DB : Failure")
  public void testUpdateLinkedAccountFailure(VertxTestContext vertxTestContext) {
    when(provider.getEmailId()).thenReturn("dummyEmailId");
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Some DB failure");

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
        .initiateUpdatingLinkedAccount(request, provider)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                JsonObject expected =
                    new RespBuilder()
                        .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                        .withTitle(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn())
                        .withDetail("Linked account could not be updated : Internal Server error")
                        .getJsonResponse();
                JsonObject actual = new JsonObject(handler.cause().getMessage());
                assertEquals(expected, actual);
                vertxTestContext.completeNow();

              } else {

                vertxTestContext.failNow(
                    "Succeeded to update linked account when DB execution failed");
              }
            });
  }

  @Test
  @DisplayName("Test initiateUpdatingLinkedAccount method during razorpay failure: Failure")
  public void testUpdateLinkedAccountDuringRzpFailure(VertxTestContext vertxTestContext) {

    JsonObject jsonObject =
        new JsonObject()
            .put("account_id", "some_dummy_id")
            .put("reference_id", "some_reference_id");
    JsonArray result = new JsonArray().add(jsonObject);
    JsonObject resultJson = new JsonObject().put(RESULTS, result);
    when(provider.getEmailId()).thenReturn("dummyEmailId");
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(resultJson);
    when(razorPayService.updateLinkedAccount(anyString(), anyString()))
        .thenReturn(Future.failedFuture("Failure ABCD from Razorpay"));

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
        .initiateUpdatingLinkedAccount(request, provider)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Linked account updated during Razorpay failure");
              } else {
                assertEquals("Failure ABCD from Razorpay", handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }
}
