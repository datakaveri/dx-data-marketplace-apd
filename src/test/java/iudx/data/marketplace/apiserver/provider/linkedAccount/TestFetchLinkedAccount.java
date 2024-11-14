package iudx.data.marketplace.apiserver.provider.linkedAccount;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.provider.linkedaccount.FetchLinkedAccount;
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

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestFetchLinkedAccount {
  private static Logger LOGGER = LogManager.getLogger(TestUpdateLinkedAccount.class);
  @Mock PostgresService postgresService;
  @Mock Api api;
  @Mock User provider;
  @Mock RazorPayService razorPayService;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Throwable throwable;
  private JsonObject request;
  private FetchLinkedAccount account;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    account = new FetchLinkedAccount(postgresService, api, razorPayService);

    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test initiateFetchingLinkedAccount method : Success")
  public void testInitiateFetchingLinkedAccountSuccess(VertxTestContext vertxTestContext) {
    JsonObject registeredJson =
        new JsonObject()
            .put("country", "india")
            .put("city", "bengaluru")
            .put("street1", "some street")
            .put("street2", "some other street")
            .put("state", "karnataka")
            .put("postal_code", "566038");
    JsonObject addressJson = new JsonObject().put("registered", registeredJson);
    JsonObject legalJson = new JsonObject().put("gst", "someGstValue").put("pan", "somePanValue");
    JsonObject profileJson =
        new JsonObject()
            .put("category", "healthcare")
            .put("subcategory", "doctors")
            .put("addresses", addressJson);
    JsonObject jsonFromRazorpay =
        new JsonObject()
            .put("id", "some_dummy_id")
            .put("contact_name", "Test Name")
            .put("notes", "[]")
            .put("reference_id", "someReferenceId")
            .put("profile", profileJson)
            .put("created_at", "someValue")
            .put("type", "route")
            .put("legal_business_name", "Dummy Corp V2")
            .put("phone", "29-45249584295")
            .put("business_type", "someBusinessType")
            .put("legal_info", legalJson)
            .put("email", "someEmail")
            .put("status", "created");

    JsonObject jsonObject =
        new JsonObject()
            .put("account_id", "some_dummy_id")
            .put("rzp_account_product_id", "dummy_rzp_account_product_id")
            .put("updatedAt", "dummyUpdatedAt")
            .put("createdAt", "dummyCreatedAt")
            .put("reference_id", "some_reference_id");
    JsonArray result = new JsonArray().add(jsonObject);
    JsonObject resultJson = new JsonObject().put(RESULTS, result);
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(provider.getResourceServerUrl()).thenReturn("dummyResourceServerUrl");
    when(asyncResult.succeeded()).thenReturn(true);
    when(razorPayService.fetchLinkedAccount(anyString()))
        .thenReturn(Future.succeededFuture(jsonFromRazorpay));
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
        .initiateFetchingLinkedAccount(provider)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {

                JsonObject actual = handler.result();
                assertEquals(ResponseUrn.SUCCESS_URN.getUrn(), actual.getString(TYPE));
                assertEquals(ResponseUrn.SUCCESS_URN.getMessage(), actual.getString(TITLE));
                assertEquals("some_dummy_id", actual.getJsonObject(RESULTS).getString("accountId"));
                assertEquals(
                    "dummy_rzp_account_product_id",
                    actual.getJsonObject(RESULTS).getString("accountProductId"));
                assertEquals("someEmail", actual.getJsonObject(RESULTS).getString("email"));
                assertEquals("created", actual.getJsonObject(RESULTS).getString("status"));
                assertEquals(
                    "Dummy Corp V2", actual.getJsonObject(RESULTS).getString("legalBusinessName"));
                assertEquals("Test Name", actual.getJsonObject(RESULTS).getString("contactName"));
                assertEquals(
                    "someReferenceId", actual.getJsonObject(RESULTS).getString("referenceId"));
                assertEquals(
                    "someBusinessType", actual.getJsonObject(RESULTS).getString("businessType"));
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow("Fetch Linked account failed");
              }
            });
  }


  @Test
  @DisplayName("Test initiateFetchingLinkedAccount method when Linked account is not found: Failure")
  public void testInitiateFetchingLinkedAccountFailure(VertxTestContext vertxTestContext) {
    JsonObject resultJson = new JsonObject().put(RESULTS, new JsonArray());
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(provider.getResourceServerUrl()).thenReturn("dummyResourceServerUrl");
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
            .initiateFetchingLinkedAccount(provider)
            .onComplete(
                    handler -> {
                      if (handler.failed()) {
                        String expected = new RespBuilder()
                                .withType(HttpStatusCode.NOT_FOUND.getValue())
                                .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                                .withDetail("Linked account cannot be fetched as, it is not found")
                                .getResponse();
                        assertEquals(expected, handler.cause().getMessage());
                        vertxTestContext.completeNow();
                      } else {

                        vertxTestContext.failNow("Succeeded when Linked account is not found");
                      }
                    });
  }


  @Test
  @DisplayName("Test initiateFetchingLinkedAccount method during DB failure: Failure")
  public void testInitiateFetchingLinkedAccountWhenDbExecutionFailed(VertxTestContext vertxTestContext) {
    JsonObject resultJson = new JsonObject().put(RESULTS, new JsonArray());
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(provider.getResourceServerUrl()).thenReturn("dummyResourceServerUrl");
    when(asyncResult.succeeded()).thenReturn(false);

    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("some failure message");

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
            .initiateFetchingLinkedAccount(provider)
            .onComplete(
                    handler -> {
                      if (handler.failed()) {
                        String expected = new RespBuilder()
                                .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                                .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                                .withDetail("Linked account cannot be fetched : Internal Server Error")
                                .getResponse();
                        assertEquals(expected, handler.cause().getMessage());
                        vertxTestContext.completeNow();
                      } else {

                        vertxTestContext.failNow("Succeeded when DB execution failed");
                      }
                    });
  }
}
