package iudx.data.marketplace.consumer;

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
public interface ConsumerService {

  /**
   * The listResources method fetches some or all resources available on the IUDX marketplace
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ConsumerService which ia service
   */
  @Fluent
  ConsumerService listResources(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listProviders method fetches all or one providers available on the IUDX marketplace
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ConsumerService which ia service
   */
  @Fluent
  ConsumerService listProviders(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listProducts method fetches some or all products available on the IUDX marketplace
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ConsumerService which ia service
   */
  @Fluent
  ConsumerService listProducts(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createOrder method creates an order for the consumer against a product variant
   * @param request
   * @param handler
   * @return
   */
  @Fluent
  ConsumerService createOrder(JsonObject request, User user, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return ProductServiceVertxEBProxy which is a service proxy
   */
  @GenIgnore
  static ConsumerService createProxy(Vertx vertx, String address) {
    return new ConsumerServiceVertxEBProxy(vertx, address);
  }

  /**
   * List purchase will fetch invoice related info, provider, consumer and product variant related information
   * After the purchase is made. Pending, successful, failed payments are displayed
   * List purchase will list all the purchases if no query parameters are given
   * It can also list purchases based on the productId, resourceId if it is given in the query parameter
   * @param user Consumer user
   * @param request query param if any
   * @param handler Asynchronous JsonObject handler that contains the list of purchases
   * @return
   */
  @Fluent
  ConsumerService listPurchase(User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler);

}
