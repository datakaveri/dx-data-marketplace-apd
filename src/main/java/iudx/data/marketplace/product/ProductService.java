package iudx.data.marketplace.product;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@VertxGen @ProxyGen
public interface ProductService {

  @GenIgnore
  static ProductService createProxy(Vertx vertx, String address) {
    return new ProductServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  ProductService createProduct(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  ProductService deleteProduct(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  ProductService listProducts(JsonObject request, Handler<AsyncResult<JsonObject>> handler);
}
