package iudx.data.marketplace.apiserver.provider.linkedAccount;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.policies.User;

@VertxGen
@ProxyGen
public interface LinkedAccountService {

  /* factory method */
  @GenIgnore
  static LinkedAccountService createProxy(Vertx vertx, String address) {
    return new LinkedAccountServiceVertxEBProxy(vertx, address);
  }

  /* service operation */

  /**
   * Used to create a new linked account through for Razorpay
   *
   * @param request Provider's business related information to create a linked account
   * @param user Provider details as User object
   * @return Future of JsonObject when it is successful, Future containing failure message during
   *     failure
   */
  Future<JsonObject> createLinkedAccount(JsonObject request, User user);

  /**
   * Used to fetch a linked account created through DMP for Razorpay
   *
   * @param user Provider details as User object
   * @return Future of JsonObject containing linked account details sent by Razorpay when it is
   *     successful, Future containing failure message during failure
   */
  Future<JsonObject> fetchLinkedAccount(User user);

  /**
   * Used to update a linked account created through DMP for Razorpay
   *
   * @param request Provider's business related information to create a linked account all the
   *     details expect <b>business_type</b>, <b>email</b> can be updated
   * @param user Provider details as User object
   * @return Future of JsonObject when it is successful, Future containing failure message during
   *     failure
   */
  Future<JsonObject> updateLinkedAccount(JsonObject request, User user);
}
