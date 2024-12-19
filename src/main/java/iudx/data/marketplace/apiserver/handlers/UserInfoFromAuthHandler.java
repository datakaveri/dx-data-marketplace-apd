package iudx.data.marketplace.apiserver.handlers;

import static iudx.data.marketplace.authenticator.model.DxRole.DELEGATE;
import static iudx.data.marketplace.common.ResponseUrn.INTERNAL_SERVER_ERR_URN;
import static iudx.data.marketplace.common.ResponseUrn.INVALID_TOKEN_URN;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.aaaService.AuthClient;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.authenticator.model.DxRole;
import iudx.data.marketplace.authenticator.model.JwtData;
import iudx.data.marketplace.authenticator.model.UserInfo;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RoutingContextHelper;
import iudx.data.marketplace.policies.User;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserInfoFromAuthHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(UserInfoFromAuthHandler.class);

  private AuthClient authClient;
  private UserInfo userInfo;
  public UserInfoFromAuthHandler(AuthClient authClient, UserInfo userInfo)
  {
    this.authClient = authClient;
    this.userInfo = userInfo;
  }
  /**
   * After JWT Authentication, User Information from DX Auth Server is fetched and handled here
   *
   * @param event the event to handle
   */
  @Override
  public void handle(RoutingContext event) {
    Future<User> getUserInfoFuture = getUserFromAuth(event);
    getUserInfoFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            /* set user in routing context */
            RoutingContextHelper.setUser(event, handler.result());
            event.next();
          } else {
            LOGGER.error(
                "User info fetch from DX Auth failed : {}", handler.cause().getMessage());
            processAuthFailure(event,handler.cause().getMessage());
          }
        });
  }

  private Future<User> getUserFromAuth(RoutingContext event){
    JwtData jwtData = RoutingContextHelper.getJwtData(event);
    DxRole role = DxRole.fromRole(jwtData);
    boolean isDelegate = jwtData.getRole().equalsIgnoreCase(DELEGATE.getRole());
    UUID id = UUID.fromString(isDelegate ? jwtData.getDid() : jwtData.getSub());
    userInfo
        .setDelegate(isDelegate)
        .setRole(role)
        .setAudience(jwtData.getAud())
        .setUserId(id);
    LOGGER.info("Getting user from Auth");
    return authClient.fetchUserInfo(userInfo);

  }
  private void processAuthFailure(RoutingContext event, String failureMessage) {
    LOGGER.error("Error : Authentication Failure : {}", failureMessage);
    if (failureMessage.equalsIgnoreCase("User information is invalid")) {
      LOGGER.error("User information is invalid");
      event.fail(new DxRuntimeException(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(), INTERNAL_SERVER_ERR_URN));
    }
    event.fail(new DxRuntimeException(HttpStatusCode.getByValue(401).getValue(), INVALID_TOKEN_URN));
  }
}
