package iudx.data.marketplace.consumer;

import static iudx.data.marketplace.common.Constants.DATASET_ID;
import static iudx.data.marketplace.common.Constants.PROVIDER_ID;
import static iudx.data.marketplace.consumer.util.Constants.*;
import static iudx.data.marketplace.product.util.Constants.STATUS;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.product.util.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
      query.insert(query.indexOf("group by") - 1, " where " + PROVIDER_ID + '=' + "$1");
    }

    pgService.executePreparedQuery(
        query.toString(),
        params,
        pgHandler -> {
          if (pgHandler.succeeded()) {
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
    String productTable = config.getJsonArray(TABLES).getString(0);
    String datasetTable = config.getJsonArray(TABLES).getString(1);
    String productDatasetRelationTable = config.getJsonArray(TABLES).getString(2);

    JsonObject params = new JsonObject();
    String query;

    if (request.containsKey(DATASET_ID)) {
      String datasetID = request.getString(DATASET_ID);
      params.put(STATUS, Status.ACTIVE.toString()).put(DATASET_ID, datasetID);
      query = LIST_PRODUCTS_FOR_DATASET;
    } else if (request.containsKey(PROVIDER_ID)) {
      String providerID = request.getString(PROVIDER_ID);
      params.put(STATUS, Status.ACTIVE.toString()).put(PROVIDER_ID, providerID);
      query = LIST_PRODUCTS_FOR_PROVIDER;
    } else {
      params.put(STATUS, Status.ACTIVE.toString());
      query = LIST_ALL_PRODUCTS_QUERY;
    }

    StringBuilder finalQuery =
        new StringBuilder(
            query
                .replace("$0", productTable)
                .replace("$9", productDatasetRelationTable)
                .replace("$8", datasetTable));

    pgService.executePreparedQuery(
        finalQuery.toString(),
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
}
