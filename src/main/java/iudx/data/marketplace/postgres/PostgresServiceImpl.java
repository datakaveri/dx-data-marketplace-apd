package iudx.data.marketplace.postgres;

import io.vertx.core.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static iudx.data.marketplace.apiserver.util.Constants.RESULTS;
import static iudx.data.marketplace.apiserver.util.Constants.STATUS_CODE;


public class PostgresServiceImpl implements PostgresService {
  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);

  private final PgPool client;
  public PostgresServiceImpl(final PgPool pgclient) {
    this.client = pgclient;
  }


  @Override
  public PostgresService executeQuery(
      final String query, Handler<AsyncResult<JsonObject>> handler) {

    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());

    client
        .withConnection(
            connection ->
                connection.query(query).collecting(rowCollector).execute().map(row -> row.value()))
        .onSuccess(
            successHandler -> {
              JsonArray result = new JsonArray(successHandler);
              JsonObject responseJson =
                  new RespBuilder()
                      .withType(ResponseUrn.SUCCESS_URN.getUrn())
                      .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                      .withResult(result)
                      .getJsonResponse();
              handler.handle(Future.succeededFuture(responseJson));
            })
        .onFailure(
            failureHandler -> {
                LOGGER.debug("Failure : {}",failureHandler.getMessage() );
                String response =
                  new RespBuilder()
                      .withType(ResponseUrn.DB_ERROR_URN.getUrn())
                      .withTitle(ResponseUrn.DB_ERROR_URN.getMessage())
                      .withDetail(failureHandler.getLocalizedMessage())
                      .getResponse();
              handler.handle(Future.failedFuture(response));
            });
    return this;
  }

  public PostgresService executeCountQuery(
      final String query, Handler<AsyncResult<JsonObject>> handler) {

    client
        .withConnection(
            connection ->
                connection.query(query).execute().map(rows -> rows.iterator().next().getInteger(0)))
        .onSuccess(
            count -> {
              handler.handle(Future.succeededFuture(new JsonObject().put("totalHits", count)));
            })
        .onFailure(
            failureHandler -> {
                LOGGER.debug("Failure : {}",failureHandler.getMessage() );
                String response =
                  new RespBuilder()
                      .withType(ResponseUrn.DB_ERROR_URN.getUrn())
                      .withTitle(ResponseUrn.DB_ERROR_URN.getMessage())
                      .withDetail(failureHandler.getLocalizedMessage())
                      .getResponse();
              handler.handle(Future.failedFuture(response));
            });
    return this;
  }

  private static Future<Void> executeBatch(
      SqlConnection conn, ConcurrentLinkedQueue<String> statements) {
    try {
      var statement = statements.poll();
      if (statement == null) {
        return Future.succeededFuture();
      }
      Promise<Void> promise = Promise.promise();

      Collector<Row, ?, List<JsonObject>> rowCollector =
          Collectors.mapping(row -> row.toJson(), Collectors.toList());
      conn.query(statement)
          .collecting(rowCollector)
          .execute()
          .map(rows -> rows.value())
          .onSuccess(
              successHandler -> {
                executeBatch(conn, statements).onComplete(h -> {
                  promise.complete();
                });
              })
          .onFailure(
              failureHandler -> {
                  LOGGER.debug("Failure : {}",failureHandler.getMessage() );
                  LOGGER.error("Fail db");
                promise.fail(failureHandler);
              });
      return promise.future();
    } catch (Throwable t) {
      return Future.failedFuture(t);
    }
  }

  @Override
  public PostgresService executeTransaction(
      final List<String> queries, Handler<AsyncResult<JsonObject>> handler) {

    client
        .withTransaction(
            connection -> {
              ConcurrentLinkedQueue<String> statements = new ConcurrentLinkedQueue<>(queries);
              Future<Void> eB = executeBatch(connection, statements);
              return eB;
            })
        .onComplete(
            completeHandler -> {
              if (completeHandler.succeeded()) {
                LOGGER.debug("transaction successful");
                JsonObject responseJson =
                    new RespBuilder()
                        .withType(ResponseUrn.SUCCESS_URN.getUrn())
                        .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                        .getJsonResponse();
                handler.handle(Future.succeededFuture(responseJson));
              } else {
                LOGGER.debug("transaction failed");
                LOGGER.debug("Failure : {}",completeHandler.cause().getMessage());
                  String response =
                    new RespBuilder()
                        .withType(ResponseUrn.DB_ERROR_URN.getUrn())
                        .withTitle(ResponseUrn.DB_ERROR_URN.getMessage())
                        .withDetail(completeHandler.cause().getMessage())
                        .getResponse();
                handler.handle(Future.failedFuture(response));
              }
            });
    return this;
  }

  // TODO : prepared query works only for String parameters, due to service proxy restriction with
  // allowed type as arguments. needs to work with TupleBuilder class which will parse other types
  // like date appropriately to match with postgres types
  @Override
  public PostgresService executePreparedQuery(
      final String query, final JsonObject queryParams, Handler<AsyncResult<JsonObject>> handler) {

    List<Object> params = new ArrayList<Object>(queryParams.getMap().values());

    Tuple tuple = Tuple.from(params);

    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());

    client
        .withConnection(
            connection ->
                connection
                    .preparedQuery(query)
                    .collecting(rowCollector)
                    .execute(tuple)
                    .map(rows -> rows.value()))
        .onSuccess(
            successHandler -> {
              JsonArray response = new JsonArray(successHandler);
              JsonObject responseJson =
                  new RespBuilder()
                      .withType(ResponseUrn.SUCCESS_URN.getUrn())
                      .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                      .withResult(response)
                      .getJsonResponse();
              handler.handle(Future.succeededFuture(responseJson));
            })
        .onFailure(
            failureHandler -> {
                LOGGER.debug("Failure : {}",failureHandler.getMessage());
              String response =
                  new RespBuilder()
                      .withType(ResponseUrn.DB_ERROR_URN.getUrn())
                      .withTitle(ResponseUrn.DB_ERROR_URN.getMessage())
                      .withDetail(failureHandler.getLocalizedMessage())
                      .getResponse();
              handler.handle(Future.failedFuture(response));
            });
    return this;
  }

  @Override
  public PostgresService checkPolicy(
      final String query, final JsonObject queryParams, Handler<AsyncResult<JsonObject>> handler) {

    JsonArray resourceIds = queryParams.getJsonArray("$1");
    String consumerEmailId = queryParams.getString("$2");

    LOGGER.debug("resourceIds : " + resourceIds);
    LOGGER.debug("queryparams : " + queryParams.encodePrettily());
    LOGGER.debug("list of string : " + queryParams.getValue("resources"));

    //    Tuple tuple = Tuple.of(resourceIds,consumerEmailId);
      UUID[] ids = resourceIds.stream().map(e -> UUID.fromString(e.toString())).toArray(UUID[]::new);
    Tuple tuple = Tuple.of(ids, consumerEmailId);

      Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());

    client
        .withConnection(
            connection ->
                connection
                    .preparedQuery(query)
                    .collecting(rowCollector)
                    .execute(tuple)
                    .map(rows -> rows.value()))
        .onSuccess(
            successHandler -> {
              LOGGER.debug("response from DB from fetch policy : {}", successHandler);
              if (successHandler.isEmpty()) {
                LOGGER.error("Empty from DB while fetching policy related info");
                JsonObject response = new JsonObject()
                        .put(RESULTS, new RespBuilder()
                                .withType(HttpStatusCode.NO_CONTENT.getValue())
                                .withTitle(HttpStatusCode.NO_CONTENT.getUrn())
                                .getJsonResponse());
                handler.handle(
                    Future.succeededFuture(
                        response.put(STATUS_CODE, HttpStatusCode.NO_CONTENT.getValue())));

              } else {
                  List<String> resourceId = successHandler.stream().map(e -> e.getString("resources")).collect(Collectors.toList());
                LOGGER.info(
                    "Policy exists for the resource IDs : {} present in the product variant",
                        resourceId);
                JsonObject responseJson =
                    new JsonObject()
                        .put(
                            RESULTS,
                            new RespBuilder()
                                .withType(ResponseUrn.SUCCESS_URN.getUrn())
                                .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                                .withDetail("Policy exists for the resource ID(s) : " +
                                        resourceId
                                        + " from the product variant")
                                .getJsonResponse())
                        .put(STATUS_CODE, HttpStatusCode.SUCCESS.getValue());
                handler.handle(Future.succeededFuture(responseJson));
              }
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("Failure while executing check policy: {}", failureHandler.getMessage());
              String response =
                  new RespBuilder()
                      .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                      .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                      .withDetail(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                      .getResponse();
              handler.handle(Future.failedFuture(response));
            });
    return this;
  }
}
