package iudx.data.marketplace.authenticator.handlers;

import static iudx.data.marketplace.authenticator.model.DxRole.DELEGATE;
import static iudx.data.marketplace.authenticator.util.Constants.INSERT_USER_TABLE;
import static iudx.data.marketplace.common.ResponseUrn.*;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.aaaService.AuthClient;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.authenticator.model.DxRole;
import iudx.data.marketplace.authenticator.model.JwtData;
import iudx.data.marketplace.authenticator.model.UserInfo;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RoutingContextHelper;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserInfoFromAuthHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(UserInfoFromAuthHandler.class);

  private AuthClient authClient;
  private UserInfo userInfo;
  private PostgresService pgService;

  public UserInfoFromAuthHandler(
      AuthClient authClient, UserInfo userInfo, PostgresService postgresService) {
    this.authClient = authClient;
    this.userInfo = userInfo;
    pgService = postgresService;
  }

  /**
   * After JWT Authentication, User Information from DX Auth Server is fetched and handled here
   *
   * @param event the event to handle
   */
  @Override
  public void handle(RoutingContext event) {
    Future<User> getUserInfoFuture = getUserFromAuth(event);
    getUserInfoFuture
        .onSuccess(
            user -> {
              Future<Void> insertUserInDbIfNotPresent = insertUserIntoDb(user);
              insertUserInDbIfNotPresent
                  .onSuccess(
                      handler -> {
                        LOGGER.debug("User successfully inserted in DB");
                        /* set user in routing context */
                        RoutingContextHelper.setUser(event, user);
                        event.next();
                      })
                  .onFailure(
                      dbFailureMessage -> {
                        LOGGER.error(
                            "Failed to insert user in DB : {}",
                            dbFailureMessage.getCause().getMessage());
                        processAuthFailure(event);
                      });
            })
        .onFailure(
            failureMessage -> {
              LOGGER.error(
                  "User info fetch from DX Auth failed : {}",
                  failureMessage.getCause().getMessage());
              processAuthFailure(event, failureMessage.getCause().getMessage());
            });
  }

  private Future<User> getUserFromAuth(RoutingContext event) {
    JwtData jwtData = RoutingContextHelper.getJwtData(event);
    DxRole role = DxRole.fromRole(jwtData);
    boolean isDelegate = jwtData.getRole().equalsIgnoreCase(DELEGATE.getRole());
    UUID id = UUID.fromString(isDelegate ? jwtData.getDid() : jwtData.getSub());
    userInfo.setDelegate(isDelegate).setRole(role).setAudience(jwtData.getAud()).setUserId(id);
    LOGGER.info("Getting user from Auth");
    return authClient.fetchUserInfo(userInfo);
  }

  private void processAuthFailure(RoutingContext event, String failureMessage) {
    LOGGER.error("Error : Authentication Failure : {}", failureMessage);
    if (failureMessage.equalsIgnoreCase("User information is invalid")) {
      LOGGER.error("User information is invalid");
      event.fail(
          new DxRuntimeException(
              HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(), INTERNAL_SERVER_ERR_URN));
    }
    event.fail(
        new DxRuntimeException(HttpStatusCode.getByValue(401).getValue(), INVALID_TOKEN_URN));
  }

  private void processAuthFailure(RoutingContext event) {
    event.fail(new DxRuntimeException(HttpStatusCode.getByValue(500).getValue(), DB_ERROR_URN));
  }

  private Future<Void> insertUserIntoDb(User user) {
    Promise<Void> promise = Promise.promise();

    JsonObject params =
        new JsonObject()
            .put("$1", user.getUserId())
            .put("$2", user.getEmailId())
            .put("$3", user.getFirstName())
            .put("$4", user.getLastName());
    pgService.executePreparedQuery(
        INSERT_USER_TABLE,
        params,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.debug("User inserted ");
            promise.complete();
          } else {
            LOGGER.debug(
                "Something went wrong while inserting user in DB : {}",
                handler.cause().getMessage());
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }
}
