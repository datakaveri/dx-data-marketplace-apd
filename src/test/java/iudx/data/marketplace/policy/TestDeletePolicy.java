package iudx.data.marketplace.policy;

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
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.DeletePolicy;
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
import static iudx.data.marketplace.common.HttpStatusCode.FORBIDDEN;
import static iudx.data.marketplace.common.ResponseUrn.FORBIDDEN_URN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestDeletePolicy {
  private static final Logger LOG = LoggerFactory.getLogger(TestDeletePolicy.class);

  @Mock User provider;
  @Mock PostgresService postgresService;
  @Mock Api api;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Throwable throwable;
  @Mock AuditingService auditingService;
  @Mock JsonObject jsonObjectMock;
  @Mock JsonArray jsonArrayMock;
  private DeletePolicy policy;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    policy = new DeletePolicy(postgresService, auditingService, api);
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
    lenient()
        .when(
            auditingService.handleAuditLogs(
                any(User.class), any(JsonObject.class), anyString(), anyString()))
        .thenReturn(Future.succeededFuture(mock(Void.class)));
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test initiateDeletePolicy method : Success")
  public void testInitiateDeletePolicy(VertxTestContext vertxTestContext) {

    JsonObject jsonObject =
        new JsonObject()
            .put("resource_server", "dummyResourceServer")
            .put("provider_id", "dummyProviderId")
            .put("status", "ACTIVE")
            .put(RESULT, new JsonArray().add(new JsonObject().put("abcd", "abcd")));
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock, jsonObjectMock, jsonObject);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObject);
    String policyId = UUID.randomUUID().toString();
    when(jsonObjectMock.getString(anyString())).thenReturn(policyId);
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(provider.getResourceServerUrl()).thenReturn("dummyResourceServer");

    policy
        .initiateDeletePolicy(jsonObjectMock, provider)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {

                assertEquals(200, handler.result().getInteger(STATUS_CODE));
                assertEquals("Policy deleted successfully", handler.result().getString(DETAIL));
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow("Failed to delete policy");
              }
            });
  }

  @Test
  @DisplayName("Test initiateDeletePolicy when policy was deleted previously: Failure")
  public void testInitiateDeletePolicyWhenPolicyIsInactive(VertxTestContext vertxTestContext) {
    JsonObject jsonObject =
        new JsonObject()
            .put("resource_server", "dummyResourceServer")
            .put("provider_id", "dummyProviderId")
            .put("status", "INACTIVE")
            .put(RESULT, new JsonArray().add(new JsonObject().put("abcd", "abcd")));
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock, jsonObjectMock, jsonObject);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObject);
    String policyId = UUID.randomUUID().toString();
    when(jsonObjectMock.getString(anyString())).thenReturn(policyId);
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(provider.getResourceServerUrl()).thenReturn("dummyResourceServer");

    policy
        .initiateDeletePolicy(jsonObjectMock, provider)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                JsonObject expected =
                    new JsonObject()
                        .put(TYPE, HttpStatusCode.BAD_REQUEST.getValue())
                        .put(TITLE, ResponseUrn.BAD_REQUEST_URN.getUrn())
                        .put(DETAIL, "Policy could not be deleted, as policy is not ACTIVE");
                assertEquals(expected.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow("Deleted an inactive policy");
              }
            });
  }

  @Test
  @DisplayName("Test initiateDeletePolicy when policy doesn't belong to the provider: Failure")
  public void testInitiateDeletePolicyWhenProviderDoesNotMatch(VertxTestContext vertxTestContext) {
    JsonObject jsonObject =
        new JsonObject()
            .put("resource_server", "dummyResourceServer")
            .put("provider_id", "someProviderId")
            .put("status", "ACTIVE")
            .put(RESULT, new JsonArray().add(new JsonObject().put("abcd", "abcd")));
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock, jsonObjectMock, jsonObject);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObject);
    String policyId = UUID.randomUUID().toString();
    when(jsonObjectMock.getString(anyString())).thenReturn(policyId);
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(provider.getResourceServerUrl()).thenReturn("dummyResourceServer");

    policy
        .initiateDeletePolicy(jsonObjectMock, provider)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                JsonObject expected =
                    new JsonObject()
                        .put(TYPE, FORBIDDEN.getValue())
                        .put(TITLE, FORBIDDEN_URN.getUrn())
                        .put(
                            DETAIL,
                            "Policy could not be deleted, as policy doesn't belong to the user");
                assertEquals(expected.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow("Deleted a policy that doesn't belong to the provider");
              }
            });
  }

  @Test
  @DisplayName("Test initiateDeletePolicy when policy is expired: Failure")
  public void testInitiateDeleteWhenPolicyIsExpired(VertxTestContext vertxTestContext) {
    JsonObject jsonObject =
        new JsonObject()
            .put("resource_server", "dummyResourceServer")
            .put("provider_id", "dummyProviderId")
            .put("status", "ACTIVE")
            .put(RESULT, new JsonArray());
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock, jsonObjectMock, jsonObject);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObject);
    String policyId = UUID.randomUUID().toString();
    when(jsonObjectMock.getString(anyString())).thenReturn(policyId);
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(provider.getResourceServerUrl()).thenReturn("dummyResourceServer");

    policy
        .initiateDeletePolicy(jsonObjectMock, provider)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                JsonObject expected =
                    new JsonObject()
                        .put(TYPE, HttpStatusCode.BAD_REQUEST.getValue())
                        .put(TITLE, ResponseUrn.BAD_REQUEST_URN.getUrn())
                        .put(DETAIL, "Policy could not be deleted , as policy is expired");
                assertEquals(expected.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow("Deleted an expired policy");
              }
            });
  }

  @Test
  @DisplayName("Test initiateDeletePolicy when query execution failed: Failure")
  public void testInitiateDeleteWhenQueryExecutionFailed(VertxTestContext vertxTestContext) {
    JsonObject jsonObject =
        new JsonObject()
            .put("resource_server", "dummyResourceServer")
            .put("provider_id", "dummyProviderId")
            .put("status", "ACTIVE")
            .put(RESULT, new JsonArray());
    when(asyncResult.succeeded()).thenReturn(true, false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Some Failure message");
    when(asyncResult.result()).thenReturn(jsonObjectMock, jsonObjectMock, jsonObject);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObject);
    String policyId = UUID.randomUUID().toString();
    when(jsonObjectMock.getString(anyString())).thenReturn(policyId);
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(provider.getResourceServerUrl()).thenReturn("dummyResourceServer");

    policy
        .initiateDeletePolicy(jsonObjectMock, provider)
        .onComplete(
            handler -> {
              LOG.info(handler.cause().getMessage());
              if (handler.failed()) {
                JsonObject expected =
                    new JsonObject()
                        .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                        .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                        .put(DETAIL, "Policy could not be deleted, update query failed");
                assertEquals(expected.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow("Succeeded when query execution failed");
              }
            });
  }

  @Test
  @DisplayName("Test initiateDeletePolicy when resource server url does not match: Failure")
  public void testInitiateDeletePolicyDuringOwnershipError(VertxTestContext vertxTestContext) {
    JsonObject jsonObject =
        new JsonObject()
            .put("resource_server", "dummyResourceServer")
            .put("provider_id", "dummyProviderId")
            .put("status", "ACTIVE")
            .put(RESULT, new JsonArray().add(new JsonObject().put("abcd", "abcd")));
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock, jsonObjectMock, jsonObject);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(false);
    when(jsonArrayMock.getJsonObject(anyInt())).thenReturn(jsonObject);
    String policyId = UUID.randomUUID().toString();
    when(jsonObjectMock.getString(anyString())).thenReturn(policyId);
    when(provider.getUserId()).thenReturn("dummyProviderId");
    when(provider.getResourceServerUrl()).thenReturn("someOtherDummyResourceServerUrl");

    policy
        .initiateDeletePolicy(jsonObjectMock, provider)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                JsonObject expected =
                    new JsonObject()
                        .put(TYPE, FORBIDDEN.getValue())
                        .put(TITLE, FORBIDDEN_URN.getUrn())
                        .put(
                            DETAIL,
                            "Access Denied: You do not have ownership rights for this policy.");
                assertEquals(expected.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow(
                    "Deleted a policy when provider and resource's RS URL are different");
              }
            });
  }

  @Test
  @DisplayName("Test initiateDeletePolicy when response from DB is empty: Failure")
  public void testInitiateDeletePolicyWithEmptyResponseFromDb(VertxTestContext vertxTestContext) {
    JsonObject jsonObject =
        new JsonObject()
            .put("resource_server", "dummyResourceServer")
            .put("provider_id", "dummyProviderId")
            .put("status", "ACTIVE")
            .put(RESULT, new JsonArray().add(new JsonObject().put("abcd", "abcd")));
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock, jsonObjectMock, jsonObject);
    when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
    when(jsonArrayMock.isEmpty()).thenReturn(true);
    String policyId = UUID.randomUUID().toString();
    when(jsonObjectMock.getString(anyString())).thenReturn(policyId);
    when(provider.getUserId()).thenReturn("dummyProviderId");

    policy
        .initiateDeletePolicy(jsonObjectMock, provider)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                JsonObject expected =
                    new JsonObject()
                        .put(TYPE, HttpStatusCode.NOT_FOUND.getValue())
                        .put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                        .put(DETAIL, "Policy could not be deleted, as it doesn't exist");
                assertEquals(expected.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {

                vertxTestContext.failNow(
                    "Deleted a policy when provider and resource's RS URL are different");
              }
            });
  }


}
