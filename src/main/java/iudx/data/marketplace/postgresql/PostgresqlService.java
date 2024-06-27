package iudx.data.marketplace.postgresql;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.List;

/**
 *  The Postgres Service
 *
 *  <h1>Postgres Service</h1>
 *
 *  <p>The Postgres Service in the IUDX Data Marketplace defines the operations to be performed
 *  on the marketplace database</p>
 *
 * @see ProxyGen
 * @see VertxGen
 * @version 1.0
 * @since 2023-01-20
 */
@VertxGen
@ProxyGen
public interface PostgresqlService {

  /**
   * The executeQuery implements single query operations with the database.
   *
   * @param query which is a String
   * @return PostgresService which is a service
   */
  Future<JsonObject> executeQuery(final String query);

  /**
   * The executeCountQuery implements a count of records operation on the database.
   *
   * @param query which is a String
   * @return PostgresService which is a service
   */
  Future<JsonObject> executeCountQuery(final String query);

  /**
   * The executePreparedQuery implements a single query operation with configurable queryParams on the database.
   *
   * @param query which is a String
   * @param queryparams which is a JsonObject
   * @return PostgresService which is a service
   */
  Future<JsonObject> executePreparedQuery(
      final String query, final JsonObject queryparams);



  /**
   * The executeTransaction implements a transaction operation(with multiple queries) on the database.
   *
   * @param queries which is a List of String
   * @return PostgresService which ia a service
   */
  Future<JsonObject> executeTransaction(final List<String> queries);

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return PostgresServiceVertxRBProxy which is a service proxy
   */
  @GenIgnore
  static PostgresqlService createProxy(Vertx vertx, String address) {
    return new PostgresqlServiceVertxEBProxy(vertx, address);
  }

  Future<JsonObject> checkPolicy(final String query,final JsonObject param);
}
