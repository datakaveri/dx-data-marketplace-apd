package iudx.data.marketplace.consumer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static iudx.data.marketplace.common.Constants.DATASET_ID;
import static iudx.data.marketplace.common.Constants.PROVIDER_ID;
import static iudx.data.marketplace.consumer.util.Constants.*;

public class ConsumerServiceImpl implements ConsumerService {
  public static final Logger LOGGER = LogManager.getLogger(ConsumerServiceImpl.class);
  private final PostgresService pgService;
  private final JsonObject config;

  public ConsumerServiceImpl(JsonObject config, PostgresService postgresService) {
    this.config = config;
    this.pgService = postgresService;
  }

  @Override
  public ConsumerService listDatasets(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String datasetTable = config.getJsonArray(TABLES).getString(1);
    JsonObject params = new JsonObject();
    StringBuilder query = new StringBuilder(LIST_DATASETS_QUERY.replace("$0", datasetTable));
    if (request.containsKey(DATASET_ID)) {
      String datasetID = request.getString(DATASET_ID);
      params.put(DATASET_ID, datasetID);
      query.append(" where ").append(DATASET_ID).append('=').append("$1");
    } else if (request.containsKey(PROVIDER_ID)) {
      String providerID = request.getString(PROVIDER_ID);
      params.put(PROVIDER_ID, providerID);
      query.append(" where ").append(PROVIDER_ID).append('=').append("$1");
    }

    LOGGER.debug(query);
    pgService.executePreparedQuery(
        query.toString(),
        params,
        pgHandler -> {
          if (pgHandler.succeeded()) {
            handler.handle(Future.succeededFuture(pgHandler.result()));
          } else {
            LOGGER.error("get datasets failed");
            handler.handle(Future.failedFuture(pgHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public ConsumerService listProviders(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String datasetTable = config.getJsonArray(TABLES).getString(1);
    JsonObject params = new JsonObject();
    StringBuilder query = new StringBuilder(LIST_PROVIDERS_QUERY.replace("$0", datasetTable));

    if (request.containsKey(PROVIDER_ID)) {
      String providerID = request.getString(PROVIDER_ID);
      params.put(PROVIDER_ID, providerID);
      query.insert(query.indexOf("group by")-1, " where " + PROVIDER_ID + '=' + "$1");
    }

    LOGGER.debug(query);
    pgService.executePreparedQuery(query.toString(), params, pgHandler -> {
      if(pgHandler.succeeded()) {
        handler.handle(Future.succeededFuture(pgHandler.result()));
      } else {
        LOGGER.error("get providers failed");
        handler.handle(Future.failedFuture(pgHandler.cause()));
      }
    });
    return this;
  }

  @Override
  public ConsumerService listPurchases(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    return null;
  }

  @Override
  public ConsumerService listProducts(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    return null;
  }
}
