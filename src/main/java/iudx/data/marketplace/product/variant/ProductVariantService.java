package iudx.data.marketplace.product.variant;

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
public interface ProductVariantService {

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return ProductVariantServiceVertxEBProxy which is a service proxy
   */
  @GenIgnore
  static ProductVariantService createProxy(Vertx vertx, String address) {
    return new ProductVariantServiceVertxEBProxy(vertx, address);
  }

  /**
   * The createProductVariant method implements the creation of a product variant on the IUDX data marketplace.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ProductVariantService which is a service
   */
  @Fluent
  ProductVariantService createProductVariant(User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateProductVariant method implements the update of a product variant on the IUDX data marketplace.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ProductVariantService which is a service
   */
  @Fluent
  ProductVariantService updateProductVariant(User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteProductVariant method implements the soft delete of a product variant on the IUDX data marketplace.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ProductVariantService which is a service
   */
  @Fluent
  ProductVariantService deleteProductVariant(User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listProductVariant method implements the fetch of product variant(s) on the IUDX data marketplace.
   * @param user which is the calling user Object
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ProductVariantService which is a service
   */
  @Fluent
  ProductVariantService listProductVariants(User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler);
}
