package iudx.data.marketplace.authenticator.handlers;


import static iudx.data.marketplace.common.ResponseUrn.INTERNAL_SERVER_ERR_URN;
import static iudx.data.marketplace.common.ResponseUrn.INVALID_TOKEN_URN;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.authenticator.AuthenticationService;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RoutingContextHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VerifyAuthHandler implements Handler<RoutingContext> {
  private final Logger LOGGER = LogManager.getLogger(VerifyAuthHandler.class);
   AuthenticationService authenticator;

  public VerifyAuthHandler(AuthenticationService authenticationService) {
    authenticator = authenticationService;
  }

  @Override
  public void handle(RoutingContext context) {
    JsonObject authInfo = RoutingContextHelper.getVerifyAuthInfo(context);
      Future<Void> verifyFuture = authenticator.tokenIntrospect4Verify(authInfo);
      verifyFuture.onComplete(
          verifyHandler -> {
            if (verifyHandler.succeeded()) {
              LOGGER.info("User Verified Successfully.");
              context.next();
            } else if (verifyHandler.failed()) {
              LOGGER.error("User Verification Failed. {}", verifyHandler.cause().getMessage());
              processAuthFailure(context,verifyHandler.cause().getMessage());
            }
          });
  }

  private void processAuthFailure(RoutingContext context, String failureMessage){
    LOGGER.error("Error : Authentication Failure : {}", failureMessage);
    if (failureMessage.equalsIgnoreCase("User information is invalid")) {
      LOGGER.error("User information is invalid");
      context.fail(new DxRuntimeException(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(), INTERNAL_SERVER_ERR_URN));
    }
    context.fail(new DxRuntimeException(HttpStatusCode.getByValue(401).getValue(), INVALID_TOKEN_URN));
  }
}
