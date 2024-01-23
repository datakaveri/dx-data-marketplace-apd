package iudx.data.marketplace.auditing;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.policies.User;

@ProxyGen
@VertxGen
public interface AuditingService {
  @GenIgnore
  static AuditingService createProxy(Vertx vertx, String address) {
    return new AuditingServiceVertxEBProxy(vertx, address);
  }

  /***
   * This methods creates is used to create audit logs to be stored in respective databases
   * by sending the messages through the data broker
   * @param user The user Object that is created during user authentication
   * @param information would contain the request body, response and essential additional information
   * @param api Endpoint that is called by the user
   * @param httpMethod Http Method of the endpoint
   * @return Future of type void
   */
  Future<Void> handleAuditLogs (User user, JsonObject information, String api, String httpMethod);
  Future<Void> insertAuditLogIntoRmq(JsonObject request);
}
