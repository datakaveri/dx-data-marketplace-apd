package iudx.data.marketplace.policies;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface PolicyService {

  /* factory method */
  @GenIgnore
  static PolicyService createProxy(Vertx vertx, String address) {
    return new PolicyServiceVertxEBProxy(vertx, address);
  }

  /*service operation*/
  Future<JsonObject> createPolicy(String orderId);

  Future<JsonObject> deletePolicy(JsonObject policy, User user);

  Future<JsonObject> getPolicies(User user);

  Future<JsonObject> verifyPolicy(JsonObject jsonObject);
}
