package iudx.data.marketplace.product;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.policies.User;

@VertxGen @ProxyGen
public interface ProductService {

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return ProductServiceVertxEBProxy which is a service proxy
   */
  @GenIgnore
  static ProductService createProxy(Vertx vertx, String address) {
    return new ProductServiceVertxEBProxy(vertx, address);
  }


  /**
   * The createProduct method implements the creation of a product on the IUDX data marketplace.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ProductService which is a service
   */
  @Fluent
  ProductService createProduct(User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteProduct method implements the soft delete of a product on the IUDX data marketplace.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ProductService which is a service
   */
  @Fluent
  ProductService deleteProduct(User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listProduct method fetches some or all the products available on the IUDX data marketplace
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ProductService which is a service
   */
  @Fluent
  ProductService listProducts(User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler);

}
