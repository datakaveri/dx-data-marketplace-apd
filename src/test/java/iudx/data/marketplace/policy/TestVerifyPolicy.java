package iudx.data.marketplace.policy;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.Util;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.policies.VerifyPolicy;
import iudx.data.marketplace.postgres.PostgresService;
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
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestVerifyPolicy {
  private static final Logger LOGGER = LogManager.getLogger(TestCreatePolicy.class);

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
  private VerifyPolicy policy;
  private JsonObject request;
  private String orderId;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    policy = new VerifyPolicy(postgresService);
    userJson =
        new JsonObject()
            .put("id", Util.generateRandomUuid())
            .put(
                "name",
                new JsonObject()
                    .put("firstName", Util.generateRandomString())
                    .put("lastName", Util.generateRandomString()))
            .put("email", Util.generateRandomEmailId());
    ownerJson =
        new JsonObject()
            .put("id", Util.generateRandomUuid())
            .put(
                "name",
                new JsonObject()
                    .put("firstName", Util.generateRandomString())
                    .put("lastName", Util.generateRandomString()))
            .put("email", Util.generateRandomEmailId());
    resourceJson =
        new JsonObject().put("itemId", Util.generateRandomUuid()).put("itemType", "RESOURCE");
    request =
        new JsonObject().put("user", userJson).put("owner", ownerJson).put("item", resourceJson);
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
  @DisplayName("Test initiateVerifyPolicy : Success")
  public void testInitiateVerifyPolicySuccess(VertxTestContext vertxTestContext) {
    String policyId = Util.generateRandomUuid().toString();
    JsonObject constraints =
        new JsonObject()
            .put(
                "constraints",
                new JsonObject().put("access", new JsonArray().add("sub").add("200 APIs")));
    when(asyncResult.result()).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray(RESULT)).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(constraints.put("_id", policyId));

    policy
        .initiateVerifyPolicy(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(
                    ResponseUrn.VERIFY_SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
                assertEquals(
                    constraints.getJsonObject("constraints"),
                    handler.result().getJsonObject("apdConstraints"));
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Failed to verify policy");
              }
            });
  }

  @Test
  @DisplayName("Test when no policy exists : Failure")
  public void testWhenNoPolicyExists(VertxTestContext vertxTestContext) {
    when(asyncResult.result()).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray(RESULT)).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(true);

    policy
        .initiateVerifyPolicy(request)
        .onComplete(
            handler -> {
              LOGGER.info(handler);
              if (handler.failed()) {
                JsonObject actual = new JsonObject(handler.cause().getMessage());
                assertEquals(HttpStatusCode.FORBIDDEN.getValue(), actual.getInteger(TYPE));
                assertEquals(ResponseUrn.VERIFY_FORBIDDEN_URN.getUrn(), actual.getString(TITLE));
                assertEquals("No policy exists for the given resource", actual.getString(DETAIL));
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Succeeded when there was no policy");
              }
            });
  }

  @Test
  @DisplayName("Test when DB Execution failed : Failure")
  public void testWhenDbExecutionFailed(VertxTestContext vertxTestContext) {
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Some error message");

    policy
        .initiateVerifyPolicy(request)
        .onComplete(
            handler -> {
              LOGGER.info(handler);
              if (handler.failed()) {
                JsonObject actual = new JsonObject(handler.cause().getMessage());
                assertEquals(
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(), actual.getInteger(TYPE));
                assertEquals(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn(), actual.getString(TITLE));
                assertEquals("Internal Server Error", actual.getString(DETAIL));
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Succeeded when DB execution failed");
              }
            });
  }

  @Test
  @DisplayName("Test initiateVerifyPolicy with orderId in context object of request body: Success")
  public void testInitiateVerifyPolicyWithOrderIdSuccess(VertxTestContext vertxTestContext) {
    String policyId = Util.generateRandomUuid().toString();
    JsonObject constraints =
        new JsonObject()
            .put(
                "constraints",
                new JsonObject().put("access", new JsonArray().add("sub").add("200 APIs")));
    when(asyncResult.result()).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray(RESULT)).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(constraints.put("_id", policyId));

    this.request.put("context", new JsonObject().put("orderId", "someOrderId"));
    policy
        .initiateVerifyPolicy(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(
                    ResponseUrn.VERIFY_SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
                assertEquals(
                    constraints.getJsonObject("constraints"),
                    handler.result().getJsonObject("apdConstraints"));
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Failed to verify policy");
              }
            });
  }

  @Test
  @DisplayName(
      "Test initiateVerifyPolicy with orderId in context object of request body with more than 1 policy for a given orderId and resource: Failure")
  public void testInitiateVerifyPolicyWithOrderIdFailure(VertxTestContext vertxTestContext) {
    String policyId = Util.generateRandomUuid().toString();
    JsonObject constraints =
        new JsonObject()
            .put(
                "constraints",
                new JsonObject().put("access", new JsonArray().add("sub").add("200 APIs")));
    when(asyncResult.result()).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray(RESULT)).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.size()).thenReturn(2);

    this.request.put("context", new JsonObject().put("orderId", "someOrderId"));
    policy
        .initiateVerifyPolicy(request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                String expected =
                    new RespBuilder()
                        .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                        .withTitle(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn())
                        .withDetail(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                        .getResponse();
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow(
                    "Succeeded when there are more than a single policy present for a given consumer, orderId and resource");
              }
            });
  }
}
