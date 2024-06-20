package iudx.data.marketplace.auditing;

import static iudx.data.marketplace.apiserver.util.Constants.USER_ID;
import static iudx.data.marketplace.auditing.util.Constants.EPOCH_TIME;
import static iudx.data.marketplace.auditing.util.Constants.ISO_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import iudx.data.marketplace.auditing.databroker.DataBrokerService;
import iudx.data.marketplace.policies.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class AuditingServiceImplTest {

    private static AuditingServiceImpl auditingService;
    private static DataBrokerService databroker;

    @Mock
     JsonObject config;
    @Mock
     JsonArray jsonArray;
    @Mock
    User user;

    @BeforeEach
    public void init(VertxTestContext vertxTestContext) {
        databroker = mock(DataBrokerService.class);
        when(config.getJsonArray(anyString())).thenReturn(jsonArray);
        auditingService = new AuditingServiceImpl(databroker, config);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Testing Write Query Successful")
    void writeDataSuccessful(VertxTestContext vertxTestContext) {
        DataBrokerService dataBrokerService = mock(DataBrokerService.class);
        JsonObject request = new JsonObject();
        ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        long time = zst.toInstant().toEpochMilli();
        String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
        request.put(EPOCH_TIME, time);
        request.put(ISO_TIME, isoTime);
        request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        AuditingServiceImpl auditingService = new AuditingServiceImpl(dataBrokerService, config);

        when(dataBrokerService.publishMessage(anyString(), anyString(), any()))
                .thenReturn(Future.succeededFuture());

        auditingService
                .insertAuditLogIntoRmq(request)
                .onSuccess(
                        successHandler -> {
                            verify(dataBrokerService, times(1)).publishMessage(anyString(), anyString(), any());
                            vertxTestContext.completeNow();
                        })
                .onFailure(
                        failure -> {
                            vertxTestContext.failNow(failure.getMessage());
                        });

    }

    @Test
    @DisplayName("Testing Write Query Failure")
    void writeDataFailure(VertxTestContext vertxTestContext) {
        JsonObject request = new JsonObject();
        ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        long time = zst.toInstant().toEpochMilli();
        String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
        request.put(EPOCH_TIME, time);
        request.put(ISO_TIME, isoTime);
        request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        auditingService = new AuditingServiceImpl(databroker, config);

        when(databroker.publishMessage(anyString(), anyString(), any()))
                .thenReturn(Future.failedFuture("failed"));

        auditingService
                .insertAuditLogIntoRmq(request)
                .onFailure(f ->
                {
                    assertEquals(f.getMessage(), "failed");
                    vertxTestContext.completeNow();
                });
    }

    @Test
    @DisplayName("Test handle audit logs method : Success")
    public void testHandleAuditLogsSuccess(VertxTestContext vertxTestContext)
    {

        when(databroker.publishMessage(anyString(), anyString(), any()))
                .thenReturn(Future.succeededFuture());
        when(user.getUserId()).thenReturn("dummyUserId");
    auditingService
        .handleAuditLogs(
            user,
            new JsonObject().put("key", "dummyValue"),
            "/policies",
            HttpMethod.POST.toString())
        .onComplete(
            handler -> {
              System.out.println(handler);
              if (handler.succeeded()) {
                  verify(databroker, times(1)).publishMessage(anyString(), anyString(), any());
                  assertNull(handler.result());
                  vertxTestContext.completeNow();
              } else {

                  vertxTestContext.failNow("Message could not be published to RMQ");
              }
            });
    }
}
