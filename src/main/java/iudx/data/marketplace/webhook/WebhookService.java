package iudx.data.marketplace.webhook;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@VertxGen @ProxyGen
public interface WebhookService {

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return WebhookServiceVertxEBProxy which is a service proxy
   */
  @GenIgnore
  static WebhookService createProxy(Vertx vertx, String address) {
    return new WebhookServiceVertxEBProxy(vertx, address);
  }

  /**
   * @param orderId which is a String generated from Razorpay
   * @return Future<JsonObject> which is a vertx Future
   */
  Future<JsonObject> recordOrderPaid(String orderId);

  /**
   * @param orderId which is a String generated from Razorpay
   * @return Future<Void> which is a vertx Future
   */
  Future<Void> recordPaymentFailure(String orderId);
}
