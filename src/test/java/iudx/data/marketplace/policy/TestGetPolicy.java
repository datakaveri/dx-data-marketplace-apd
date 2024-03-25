package iudx.data.marketplace.policy;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.GetPolicy;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.razorpay.RazorPayService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.apiserver.util.Constants.RESULTS;
import static iudx.data.marketplace.policies.GetPolicy.FAILURE_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, VertxExtension.class})

public class TestGetPolicy {
    private static final Logger LOGGER = LogManager.getLogger(TestGetPolicy.class);
    @Mock
    User provider;
    @Mock
    PostgresService postgresService;
    @Mock
    Api api;
    @Mock
    AsyncResult<JsonObject> asyncResult;
    @Mock Throwable throwable;
    @Mock
    JsonObject jsonObjectMock;
    @Mock
    JsonArray jsonArrayMock;
    private GetPolicy policy;

    private static JsonObject getProviderResult()
    {
        JsonObject jsonObject = new JsonObject()
                .put("providerFirstName", "dummyValue")
                .put("providerLastName", "dummyProviderLastName")
                .put("providerId", "dummyProviderId")
                .put("providerEmailId", "dummyProviderEmailId")
                .put("dummyResourceInfoKey", "dummyResourceInfoValue")
                .put("dummyPolicyInfoKey", "dummyPolicyInfoValue")
                .put("provider",new JsonObject()
                        .put("email", "dummyEmailId")
                        .put(
                                "name",
                                new JsonObject().put("firstName", "dummyFirstName").put("lastName", "dummyLastName"))
                        .put("id", "dummyUserId"))
                .put("consumer", new JsonObject()
                        .put("email", "dummyConsumerEmailId")
                        .put(
                                "name",
                                new JsonObject().put("firstName", "dummyConsumerFirstName").put("lastName", "dummyConsumerLastName"))
                        .put("id", "dummyConsumerId"));

        JsonArray jsonArray = new JsonArray()
                .add(jsonObject);
        JsonObject response =
                new JsonObject()
                        .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                        .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                        .put(RESULT, jsonArray);

        return new JsonObject().put(RESULTS, response).put("statusCode", 200);
    }

    private static JsonObject getConsumerResult()
    {
        JsonObject jsonObject = new JsonObject()
                .put("consumerFirstName", "dummyConsumerFirstName")
                .put("consumerLastName", "dummyConsumerLastName")
                .put("consumerId", "dummyConsumerId")
                .put("dummyResourceInfoKey", "dummyResourceInfoValue")
                .put("dummyPolicyInfoKey", "dummyPolicyInfoValue")
                .put("consumerEmailId", "dummyConsumerEmailId")
                .put("consumer",new JsonObject()
                        .put("email", "dummyEmailId")
                        .put(
                                "name",
                                new JsonObject().put("firstName", "dummyFirstName").put("lastName", "dummyLastName"))
                        .put("id", "dummyUserId"))
                .put("provider", new JsonObject()
                        .put("email", "dummyProviderEmailId")
                        .put(
                                "name",
                                new JsonObject().put("firstName", "dummyValue").put("lastName", "dummyProviderLastName"))
                        .put("id", "dummyProviderId"));

        JsonArray jsonArray = new JsonArray()
                .add(jsonObject);
        JsonObject response =
                new JsonObject()
                        .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                        .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                        .put(RESULT, jsonArray);

    return new JsonObject().put(RESULTS, response).put("statusCode", 200);
    }

    static Stream<Arguments> input()
    {
        return Stream.of(
                Arguments.of(mock(User.class), Role.PROVIDER, getProviderResult()),
                Arguments.of(mock(User.class), Role.CONSUMER, getConsumerResult())
        );
    }

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        policy = new GetPolicy(postgresService);
        lenient().doAnswer(
                        new Answer<AsyncResult<JsonObject>>() {
                            @Override
                            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                                return null;
                            }
                        })
                .when(postgresService)
                .executeQuery(anyString(), any());
        vertxTestContext.completeNow();
    }

    @ParameterizedTest
    @DisplayName("Test method : Success")
    @MethodSource("input")
    public void testInitiateGetPolicy(User user,Role role, JsonObject expected, VertxTestContext vertxTestContext)
    {

    JsonObject jsonObject =
        new JsonObject()
            .put("providerFirstName", "dummyValue")
            .put("providerLastName", "dummyProviderLastName")
            .put("providerId", "dummyProviderId")
            .put("providerEmailId", "dummyProviderEmailId")
            .put("consumerFirstName", "dummyConsumerFirstName")
            .put("consumerLastName", "dummyConsumerLastName")
            .put("consumerId", "dummyConsumerId")
            .put("dummyResourceInfoKey", "dummyResourceInfoValue")
            .put("dummyPolicyInfoKey", "dummyPolicyInfoValue")
            .put("consumerEmailId", "dummyConsumerEmailId");
        JsonArray jsonArray = new JsonArray()
                .add(jsonObject);
        when(user.getUserRole()).thenReturn(role);
        when(user.getUserId()).thenReturn("dummyUserId");
        when(user.getResourceServerUrl()).thenReturn("dummyResourceServerUrl");
        when(user.getEmailId()).thenReturn("dummyEmailId");
        when(user.getFirstName()).thenReturn("dummyFirstName");
        when(user.getLastName()).thenReturn("dummyLastName");
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(jsonObjectMock);
        when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArray);

        policy.initiateGetPolicy(user)
                        .onComplete(handler -> {
                            if(handler.succeeded())
                            {
                                assertEquals(expected, handler.result());
                                vertxTestContext.completeNow();

                            }
                            else
                            {
                                vertxTestContext.failNow("Failed to fetch policy");

                            }
                        });

    }

    @Test
    @DisplayName("Test initiateGetPolicy empty results from DB : Failure")
    public void testInitiateGetPolicyWithEmptyResult( VertxTestContext vertxTestContext)
    {

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(jsonObjectMock);
        when(jsonObjectMock.getJsonArray(anyString())).thenReturn(jsonArrayMock);
        when(jsonArrayMock.isEmpty()).thenReturn(true);
        when(provider.getUserRole()).thenReturn(Role.PROVIDER);

        policy.initiateGetPolicy(provider)
                .onComplete(handler -> {
                    LOGGER.info(handler);
                    if(handler.failed())
                    {
                        JsonObject expected =
                                new JsonObject()
                                        .put(TYPE, HttpStatusCode.NOT_FOUND.getValue())
                                        .put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                                        .put(DETAIL, "Policy Not found");
                        assertEquals(expected.encode(), handler.cause().getMessage());
                        vertxTestContext.completeNow();

                    }
                    else
                    {
                        vertxTestContext.failNow("Succeeded when the response from DB is empty");

                    }
                });

    }

    @Test
    @DisplayName("Test initiateGetPolicy during DB failure : Failure")
    public void testInitiateGetPolicyFailure( VertxTestContext vertxTestContext)
    {

        when(asyncResult.succeeded()).thenReturn(false);
        when(provider.getUserRole()).thenReturn(Role.PROVIDER);
        when(asyncResult.cause()).thenReturn(throwable);

        policy.initiateGetPolicy(provider)
                .onComplete(handler -> {
                    LOGGER.info(handler);
                    if(handler.failed())
                    {
                        JsonObject expected =
                                new JsonObject()
                                        .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                                        .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                                        .put(DETAIL, FAILURE_MESSAGE + ", Failure while executing query");
                        assertEquals(expected.encode(), handler.cause().getMessage());
                        vertxTestContext.completeNow();

                    }
                    else
                    {
                        vertxTestContext.failNow("Succeeded when the response from DB is empty");

                    }
                });

    }

    @Test
    @DisplayName("Test initiateGetPolicy with null role : Failure")
    public void testInitiateGetPolicyWithNullRole( VertxTestContext vertxTestContext)
    {

        when(provider.getUserRole()).thenReturn(null);
        assertThrows(NullPointerException.class, ()-> policy.initiateGetPolicy(provider));
        vertxTestContext.completeNow();

    }

}
