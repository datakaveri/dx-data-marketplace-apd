package iudx.data.marketplace.postgres;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.GetPolicy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.policies.GetPolicy.FAILURE_MESSAGE;

public class PostgresServiceImpl implements PostgresService {
  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);

  private final PgPool client;
    private PgConnectOptions connectOptions;
    private PoolOptions poolOptions;
    private String databaseIP;
    private int databasePort;
    private String databaseName;
    private String databaseUserName;
    private String databasePassword;
    private int poolSize;
  public PostgresServiceImpl(final PgPool pgclient) {
    this.client = pgclient;
  }

  public PostgresServiceImpl(final JsonObject dbConfig, final Vertx vertx)
  {
      databaseIP = dbConfig.getString("databaseIP");
      databasePort = dbConfig.getInteger("databasePort");
      databaseName = dbConfig.getString("databaseName");
      databaseUserName = dbConfig.getString("databaseUserName");
      databasePassword = dbConfig.getString("databasePassword");
      poolSize = dbConfig.getInteger("poolSize");

      this.connectOptions =
              new PgConnectOptions()
                      .setPort(databasePort)
                      .setHost(databaseIP)
                      .setDatabase(databaseName)
                      .setUser(databaseUserName)
                      .setPassword(databasePassword)
                      .setReconnectAttempts(2)
                      .setReconnectInterval(1000L);

      this.poolOptions = new PoolOptions().setMaxSize(poolSize);
      this.client = PgPool.pool(vertx, connectOptions, poolOptions);

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

      LOGGER.debug(statement);
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

  public Future<JsonObject> executePreparedQuery(final String query, final Tuple tuple) {
    Promise<JsonObject> promise = Promise.promise();
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
              LOGGER.debug("Success response : {}", successHandler);
              JsonObject responseJson =
                  new RespBuilder()
                      .withType(ResponseUrn.SUCCESS_URN.getUrn())
                      .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                      .withResult(response)
                      .getJsonResponse();

              promise.complete(responseJson);
            })
        .onFailure(
            failureHandler -> {
              LOGGER.debug("Failure : {}", failureHandler.getMessage());
              String response =
                  new RespBuilder()
                      .withType(ResponseUrn.DB_ERROR_URN.getUrn())
                      .withTitle(ResponseUrn.DB_ERROR_URN.getMessage())
                      .withDetail(failureHandler.getLocalizedMessage())
                      .getResponse();
              promise.fail(response);
            });
    return promise.future();
  }
}
