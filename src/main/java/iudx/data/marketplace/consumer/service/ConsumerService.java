package iudx.data.marketplace.consumer.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.policies.service.model.User;

@VertxGen
@ProxyGen
public interface ConsumerService {

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
   * The listResources method fetches some or all resources available on the IUDX marketplace
   *
   * @param consumer as User object
   * @param request which is a JsonObject
   * @return ConsumerService which is a service
   */
  Future<JsonObject> listResources(
      User consumer, JsonObject request);

  /**
   * The listProviders method fetches all or one providers available on the IUDX marketplace
   *
   * @param consumer as User object
   * @param request which is a JsonObject
   * @return ConsumerService which is a service
   */
  Future<JsonObject> listProviders(
      User consumer, JsonObject request);

  /**
   * The listProducts method fetches some or all products available on the IUDX marketplace
   *
   * @param consumer as User object
   * @param request which is a JsonObject
   * @return Future json object
   */
  Future<JsonObject> listProducts(
      User consumer, JsonObject request);

  /**
   * The createOrder method creates an order for the consumer against a product variant
   *
   * @param request to Create order as Json object containing product variant ID
   * @param user Consumer User
   * @return Future json object
   */
  Future<JsonObject> createOrder(
      JsonObject request, User user);

  /**
   * The listProductVariants method fetches all the <b>ACTIVE</b> product variants of a given
   * product
   *
   * @param user as consumer User object
   * @param request containing the productId of a given product
   * @return Future json object
   */
  Future<JsonObject> listProductVariants(
      User user, JsonObject request);

  /**
   * List purchase will fetch invoice related info, provider, consumer and product variant related
   * information After the purchase is made. Pending, successful, failed payments are displayed List
   * purchase will list all the purchases if no query parameters are given It can also list
   * purchases based on the productId, resourceId if it is given in the query parameter
   *
   * @param user Consumer user
   * @param request query param if any
   * @return Future json object that contains the list of purchases
   */
  Future<JsonObject> listPurchase(
      User user, JsonObject request);
}
