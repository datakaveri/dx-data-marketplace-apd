package iudx.data.marketplace.postgres;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.List;

/**
 * The Postgres Service
 *
 * <h1>Postgres Service</h1>
 *
 * <p>The Postgres Service in the IUDX Data Marketplace defines the operations to be performed on
 * the marketplace database
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2023-01-20
 */
@VertxGen
@ProxyGen
public interface PostgresService {

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return PostgresServiceVertxRBProxy which is a service proxy
   */
  @GenIgnore
  static PostgresService createProxy(Vertx vertx, String address) {
    return new PostgresServiceVertxEBProxy(vertx, address);
  }

  /**
   * The executeQuery implements single query operations with the database.
   *
   * @param query which is a String
   * @param handler which is a Request Handler
   * @return PostgresService which is a service
   */
  @Fluent
  PostgresService executeQuery(final String query, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The executeCountQuery implements a count of records operation on the database.
   *
   * @param query which is a String
   * @param handler which is a Request Handler
   * @return PostgresService which is a service
   */
  @Fluent
  PostgresService executeCountQuery(final String query, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The executePreparedQuery implements a single query operation with configurable queryParams on
   * the database.
   *
   * @param query which is a String
   * @param queryparams which is a JsonObject
   * @param handler which is a Request Handler
   * @return PostgresService which is a service
   */
  @Fluent
  PostgresService executePreparedQuery(
      final String query, final JsonObject queryparams, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The executeTransaction implements a transaction operation(with multiple queries) on the
   * database.
   *
   * @param queries which is a List of String
   * @param handler which is a Request Handler
   * @return PostgresService which ia a service
   */
  @Fluent
  PostgresService executeTransaction(
      final List<String> queries, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  PostgresService checkPolicy(
      final String query, final JsonObject param, Handler<AsyncResult<JsonObject>> handler);
}
