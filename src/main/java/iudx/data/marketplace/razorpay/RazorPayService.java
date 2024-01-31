package iudx.data.marketplace.razorpay;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.policies.User;

@VertxGen @ProxyGen
public interface RazorPayService {
  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return RazorPayServiceVertxEBProxy which is a service proxy
   */
  @GenIgnore
  static RazorPayService createProxy(Vertx vertx, String address) {
    return new RazorPayServiceVertxEBProxy(vertx, address);
  }

  /**
   * The create order method implements the creation of an order using the razorpayClient
   *  This method also creates a transfer from the order to the respective merchant account
   *
   * @param request which is a JsonObject
   * @retun Future<JsonObject> which is a vertx Future
   */
  Future<JsonObject> createOrder(JsonObject request);

  /**
   * The verifyPayment method verifies the payment signature for every payment that is made using RazorPay
   *  This method also creates a payment record in the marketplace database
   * @param request which is a JsonObject
   * @retun Future<JsonObject> which is a vertx Future
   */
  Future<JsonObject> verifyPayment(JsonObject request);
}
