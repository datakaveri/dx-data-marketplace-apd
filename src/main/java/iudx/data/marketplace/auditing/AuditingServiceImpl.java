package iudx.data.marketplace.auditing;

import static iudx.data.marketplace.auditing.util.Constants.*;
import static iudx.data.marketplace.auditing.util.Constants.ISO_TIME;
import static iudx.data.marketplace.product.util.Constants.TABLES;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.auditing.databroker.DataBrokerService;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.product.util.QueryBuilder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuditingServiceImpl implements AuditingService {
  private static final Logger LOGGER = LogManager.getLogger(AuditingServiceImpl.class);
  private DataBrokerService dataBrokerService;
  private QueryBuilder queryBuilder;

  public AuditingServiceImpl(DataBrokerService dataBrokerService, JsonObject config) {
    this.dataBrokerService = dataBrokerService;
    this.queryBuilder = new QueryBuilder(config.getJsonArray(TABLES));
  }

  @Override
  public Future<Void> handleAuditLogs(
      User user, JsonObject information, String api, String httpMethod) {
    LOGGER.debug("handleAuditLogs started");
    String userId = user.getUserId();
    ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    long time = zst.toInstant().toEpochMilli();
    String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();

    JsonObject auditLog = new JsonObject();
    auditLog.put(USERID, userId);
    auditLog.put(INFORMATION, information);
    auditLog.put(API, api);
    auditLog.put(HTTP_METHOD, httpMethod);
    auditLog.put(EPOCH_TIME, time);
    auditLog.put(ISO_TIME, isoTime);

    Promise<Void> promise = Promise.promise();
    LOGGER.debug("AuditLog: " + auditLog);
    this.insertAuditLogIntoRmq(auditLog)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Audit data published into RMQ.");
                promise.complete();
              } else {
                LOGGER.error("failed: " + handler.cause().getMessage());
                promise.complete();
              }
            });

    return promise.future();
  }

  /**
   * @param request JsonObject
   * @return void future
   */
  @Override
  public Future<Void> insertAuditLogIntoRmq(JsonObject request) {
    Promise<Void> promise = Promise.promise();
    JsonObject writeMessage = queryBuilder.buildMessageForRmq(request);
    dataBrokerService
        .publishMessage(EXCHANGE_NAME, ROUTING_KEY, writeMessage)
        .onSuccess(
            successHandler -> {
              promise.complete();
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(failureHandler.getMessage());
              LOGGER.debug("Fail to Published audit data into rmq");
              promise.fail(failureHandler.getMessage());
            });

    return promise.future();
  }
}
