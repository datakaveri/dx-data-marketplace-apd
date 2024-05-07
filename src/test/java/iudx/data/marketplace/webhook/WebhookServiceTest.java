package iudx.data.marketplace.webhook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.consumer.util.PaymentStatus;
import iudx.data.marketplace.policies.PolicyService;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class WebhookServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(WebhookServiceTest.class);

  private static AsyncResult<JsonObject> asyncResult;
  private static PolicyService policyService;
  private static PostgresService postgresService;
  private static WebhookService webhookService;
  private static WebhookServiceImpl webhookServiceSpy;
  private static int expectedInvocationsPostgresService;
  private static int expectedInvocationsPolicyService;
  private static JsonObject mockResult;

  @BeforeAll
  static void setup(VertxTestContext testContext) {
    asyncResult = mock(AsyncResult.class);
    policyService = mock(PolicyService.class);
    postgresService = mock(PostgresService.class);
    mockResult = mock(JsonObject.class);
    expectedInvocationsPostgresService = 0;
    expectedInvocationsPolicyService = 0;
    webhookService = new WebhookServiceImpl(postgresService, policyService, "dummyInvoiceTable");
    webhookServiceSpy = (WebhookServiceImpl) spy(webhookService);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock)
                  throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(2))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executePreparedQuery(anyString(), any(), any());
    lenient().when(asyncResult.result()).thenReturn(mockResult);
    lenient().when(mockResult.encodePrettily()).thenReturn("Some result from database");

    testContext.completeNow();
  }

  @Test
  @DisplayName("Test record order paid - Success")
  public void testOrderPaidSuccess(VertxTestContext testContext) {

    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(Answer -> Future.succeededFuture()).when(policyService).createPolicy(anyString());

    webhookService
        .recordOrderPaid("dummyOrderId")
        .onSuccess(
            ar -> {
              verify(postgresService, times(++expectedInvocationsPostgresService))
                  .executePreparedQuery(anyString(), any(), any());
              verify(policyService, times(++expectedInvocationsPolicyService))
                  .createPolicy(anyString());
              testContext.completeNow();
            })
        .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("Test record order paid - update payment status failed")
  public void testOrderPaidFailureUpdateStatus(VertxTestContext testContext) {

    when(asyncResult.succeeded()).thenReturn(false);
    webhookService
        .recordOrderPaid("dummyOrderId")
        .onSuccess(ar -> testContext.failNow("Unexpected behaviour"))
        .onFailure(
            ar -> {
              verify(postgresService, times(++expectedInvocationsPostgresService))
                  .executePreparedQuery(anyString(), any(), any());
              verify(policyService, times(expectedInvocationsPolicyService))
                  .createPolicy(anyString());
              testContext.completeNow();
            });
  }

  @Test
  @DisplayName("Test record order paid - create policy failed")
  public void testOrderPaidFailureCreatePolicy(VertxTestContext testContext) {

    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(Answer -> Future.failedFuture("Policy Creation failed"))
        .when(policyService)
        .createPolicy(anyString());
    webhookService
        .recordOrderPaid("dummyOrderId")
        .onSuccess(ar -> testContext.failNow("Unexpected behaviour"))
        .onFailure(
            ar -> {
              verify(postgresService, times(++expectedInvocationsPostgresService))
                  .executePreparedQuery(anyString(), any(), any());
              verify(policyService, times(++expectedInvocationsPolicyService))
                  .createPolicy(anyString());
              testContext.completeNow();
            });
  }

  @Test
  @DisplayName("Test record payment failure - success")
  public void testPaymentFailureSuccess(VertxTestContext testContext) {

    when(asyncResult.succeeded()).thenReturn(true);
    webhookService
        .recordPaymentFailure("dummyOrderId")
        .onSuccess(ar -> {
          verify(postgresService, times(++expectedInvocationsPostgresService))
              .executePreparedQuery(anyString(), any(), any());
          verify(policyService, times(expectedInvocationsPolicyService))
              .createPolicy(anyString());
          testContext.completeNow();
        })
        .onFailure(testContext::failNow);
  }

  @Test
  @DisplayName("Test update payment status for invoice - success")
  public void testUpdatePaymentStatus(VertxTestContext testContext) {

    when(asyncResult.succeeded()).thenReturn(true);
    webhookServiceSpy.updatePaymentStatusForInvoice("dummyOrderId", PaymentStatus.SUCCESSFUL)
        .onSuccess(ar -> {
          verify(postgresService, times(++expectedInvocationsPostgresService))
              .executePreparedQuery(anyString(), any(), any());
          verify(policyService, times(expectedInvocationsPolicyService))
              .createPolicy(anyString());
          testContext.completeNow();
        })
        .onFailure(testContext::failNow);
  }
}
