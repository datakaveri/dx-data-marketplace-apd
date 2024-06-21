package iudx.data.marketplace.consentAgreementGenerator.controller;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import iudx.data.marketplace.policies.User;

@VertxGen
@ProxyGen
public interface ConsentAgreementService {
  /* factory method */
  @GenIgnore
  static ConsentAgreementService createProxy(Vertx vertx, String address) {
    return new ConsentAgreementServiceVertxEBProxy(vertx, address);
  }

  /*service operation*/

  /**
   * @param user info about the user requesting the consent agreement
   * @param policyId based on which consent agreement needs to be fetched
   * @return Pdf buffer as end-user response
   */
  public Future<Buffer> initiatePdfGeneration(User user, String policyId);
}
