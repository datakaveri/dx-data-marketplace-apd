package iudx.data.marketplace.policy;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.Util;
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.FetchPolicyUsingPvId;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.apiserver.util.Constants.RS_SERVER_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestFetchPolicyUsingPvId {
  private static final Logger LOG = LoggerFactory.getLogger(TestFetchPolicyUsingPvId.class);
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Throwable throwable;
  @Mock JsonObject jsonObjectMock;
  @Mock JsonArray jsonArrayMock;
  @Mock Void aVoid;
  FetchPolicyUsingPvId fetchPolicyUsingPvId;
  @Mock private PostgresService postgresService;
  private User consumer;
  private UUID consumerId;
  private String emailId;
  private String firstName;
  private String lastName;
  private String resourceServerUrl;
  private String pvId;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    this.consumerId = Util.generateRandomUuid();
    this.emailId = Util.generateRandomEmailId();
    this.firstName = Util.generateRandomString();
    this.lastName = Util.generateRandomString();
    this.resourceServerUrl = "rs.iudx.io";
    this.consumer = new User(getUserDetails());
    this.pvId = Util.generateRandomUuid().toString();
    this.fetchPolicyUsingPvId = new FetchPolicyUsingPvId(postgresService);
    lenient()
        .doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(anyString(), any());

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
        .checkPolicy(anyString(), any(JsonObject.class), any());
    vertxTestContext.completeNow();
  }

  public JsonObject getUserDetails() {
    return new JsonObject()
        .put(USERID, this.consumerId)
        .put(USER_ROLE, Role.CONSUMER)
        .put(EMAIL_ID, this.emailId)
        .put(FIRST_NAME, this.firstName)
        .put(LAST_NAME, this.lastName)
        .put(RS_SERVER_URL, this.resourceServerUrl);
  }

  @Test
  @DisplayName("Test checkIfPolicyExists : Success")
  public void testCheckIfPolicyExistsSuccess(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = new JsonObject().put("id", Util.generateRandomUuid());
    JsonArray resources = new JsonArray();
    resources.add(jsonObject);
    String detail =
        "Policy present for the resource ["
            + Util.generateRandomUuid().toString()
            + "] present in product variant";
    JsonObject expected =
        new RespBuilder()
            .withType(ResponseUrn.SUCCESS_URN.getUrn())
            .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
            .withDetail(detail)
            .getJsonResponse();
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock, jsonObjectMock, jsonObjectMock, expected);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.encode()).thenReturn("Some dummy string");
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getString(RS_SERVER_URL)).thenReturn(this.resourceServerUrl);
    when(jsonObjectMock.getJsonArray("resources")).thenReturn(resources);

    this.fetchPolicyUsingPvId
        .checkIfPolicyExists(pvId, consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {

                assertEquals(expected, handler.result());
                verify(asyncResult, times(4)).result();
                verify(asyncResult, times(2)).succeeded();

                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("check policy failed");
              }
            });
  }

  @Test
  @DisplayName(
      "Test checkIfPolicyExists when database throws some error while fetching resources based on policy existence : Failure")
  public void testCheckIfPolicyExistsDbFailure(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = new JsonObject().put("id", Util.generateRandomUuid());
    JsonArray resources = new JsonArray();
    resources.add(jsonObject);
    when(asyncResult.succeeded()).thenReturn(true, false);
    when(asyncResult.result()).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.encode()).thenReturn("Some dummy string");
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getString(RS_SERVER_URL)).thenReturn(this.resourceServerUrl);
    when(jsonObjectMock.getJsonArray("resources")).thenReturn(resources);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Some dummy failure message from DB");

    this.fetchPolicyUsingPvId
        .checkIfPolicyExists(pvId, consumer)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Some dummy failure message from DB", handler.cause().getMessage());
                verify(asyncResult, times(3)).result();
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("check policy failed");
              }
            });
  }

  @Test
  @DisplayName("Test checkIfPolicyExists when delegation flow fails : Failure")
  public void testCheckIfPolicyExistsRsUrlMismatch(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = new JsonObject().put("id", Util.generateRandomUuid());
    JsonArray resources = new JsonArray();
    resources.add(jsonObject);
    when(asyncResult.succeeded()).thenReturn(true, false);
    when(asyncResult.result()).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.encode()).thenReturn("Some dummy string");
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getString(RS_SERVER_URL)).thenReturn("some.other.resource.server");
    when(jsonObjectMock.getJsonArray("resources")).thenReturn(resources);

    this.fetchPolicyUsingPvId
        .checkIfPolicyExists(pvId, consumer)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                String expected =
                    new RespBuilder()
                        .withType(HttpStatusCode.FORBIDDEN.getValue())
                        .withTitle(ResponseUrn.FORBIDDEN_URN.getUrn())
                        .withDetail(ResponseUrn.FORBIDDEN_URN.getMessage())
                        .getResponse();
                assertEquals(expected, handler.cause().getMessage());
                verify(asyncResult, times(3)).result();
                verify(asyncResult, times(1)).succeeded();
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("check policy failed");
              }
            });
  }

  @Test
  @DisplayName("Test checkIfPolicyExists when the DB results are Empty : Failure")
  public void testCheckIfPolicyExistsWithEmptyDbResult(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = new JsonObject().put("id", Util.generateRandomUuid());
    JsonArray resources = new JsonArray();
    resources.add(jsonObject);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(true);
    this.fetchPolicyUsingPvId
        .checkIfPolicyExists(pvId, consumer)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                String expected =
                    new RespBuilder()
                        .withType(HttpStatusCode.NOT_FOUND.getValue())
                        .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                        .withDetail("Product variant not found")
                        .getResponse();
                assertEquals(expected, handler.cause().getMessage());
                verify(asyncResult, times(1)).result();
                verify(asyncResult, times(1)).succeeded();
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("check policy failed");
              }
            });
  }

  @Test
  @DisplayName("Test checkIfPolicyExists when DB execution fails : Failure")
  public void testCheckIfPolicyExistsWithDbExecutionFailure(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = new JsonObject().put("id", Util.generateRandomUuid());
    JsonArray resources = new JsonArray();
    resources.add(jsonObject);
    when(asyncResult.succeeded()).thenReturn(false);
    when(throwable.getMessage()).thenReturn("Some failure message from DB");
    when(asyncResult.cause()).thenReturn(throwable);
    this.fetchPolicyUsingPvId
        .checkIfPolicyExists(pvId, consumer)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                String expected =
                    new RespBuilder()
                        .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                        .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                        .withDetail(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                        .getResponse();
                assertEquals(expected, handler.cause().getMessage());
                verify(asyncResult, times(1)).cause();
                verify(asyncResult, times(1)).succeeded();
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("check policy failed");
              }
            });
  }
}
