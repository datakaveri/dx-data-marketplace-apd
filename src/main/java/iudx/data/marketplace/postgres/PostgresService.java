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

@VertxGen
@ProxyGen
public interface PostgresService {

  @Fluent
  PostgresService executeQuery(final String query, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  PostgresService executeCountQuery(final String query, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  PostgresService executePreparedQuery(
      final String query, final JsonObject queryparams, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  PostgresService executeTransaction(final List<String> queries, Handler<AsyncResult<JsonObject>> handler);
  @GenIgnore
  static PostgresService createProxy(Vertx vertx, String address) {
    return new PostgresServiceVertxEBProxy(vertx, address);
  }
}
