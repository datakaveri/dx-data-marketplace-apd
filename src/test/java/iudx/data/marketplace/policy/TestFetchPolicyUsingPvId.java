package iudx.data.marketplace.policy;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.apiserver.util.Constants.RS_SERVER_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
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
import iudx.data.marketplace.postgresql.PostgresqlService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestFetchPolicyUsingPvId {
  private static final Logger LOG = LoggerFactory.getLogger(TestFetchPolicyUsingPvId.class);
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Throwable throwable;
  @Mock JsonObject jsonObjectMock;
  @Mock JsonArray jsonArrayMock;
  @Mock Void aVoid;
  FetchPolicyUsingPvId fetchPolicyUsingPvId;
  @Mock private PostgresqlService postgresService;
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
    when(postgresService.executeQuery(anyString()))
        .thenReturn(
            Future.succeededFuture(jsonObjectMock),
            Future.succeededFuture(jsonObjectMock),
            Future.succeededFuture(jsonObjectMock),
            Future.succeededFuture(expected));
    when(postgresService.checkPolicy(anyString(), any()))
        .thenReturn(Future.succeededFuture(expected));
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.encode()).thenReturn("Some dummy string");
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getString(RS_SERVER_URL)).thenReturn(this.resourceServerUrl);
    when(jsonObjectMock.getJsonArray("resources")).thenReturn(resources);
    when(jsonObjectMock.getJsonArray(RESULTS)).thenReturn(jsonArrayMock);

    this.fetchPolicyUsingPvId
        .checkIfPolicyExists(pvId, consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {

                assertEquals(expected, handler.result());
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
    when(postgresService.executeQuery(anyString()))
        .thenReturn(Future.succeededFuture(jsonObjectMock));
    when(postgresService.checkPolicy(anyString(), any()))
        .thenReturn(Future.failedFuture("some failure message from DB"));
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
              if (handler.failed()) {
                assertEquals(
                    new RespBuilder()
                        .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                        .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                        .withDetail(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                        .getResponse(),
                    handler.cause().getMessage());

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
    when(postgresService.executeQuery(anyString()))
        .thenReturn(
            Future.succeededFuture(jsonObjectMock), Future.failedFuture("Some failure message"));
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.encode()).thenReturn("Some dummy string");
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObjectMock);
    when(jsonObjectMock.getString(RS_SERVER_URL)).thenReturn("some.other.resource.server");
    when(jsonObjectMock.getJsonArray("resources")).thenReturn(resources);
    when(jsonObjectMock.getJsonArray(RESULTS)).thenReturn(jsonArrayMock);

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
    when(postgresService.executeQuery(anyString()))
        .thenReturn(Future.succeededFuture(jsonObjectMock));
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
    when(postgresService.executeQuery(anyString()))
        .thenReturn(Future.failedFuture("Some failure message from DB"));
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
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("check policy failed");
              }
            });
  }
}
