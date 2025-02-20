package iudx.data.marketplace.authenticator.handlers.authorization;

import static iudx.data.marketplace.common.HttpStatusCode.UNAUTHORIZED;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.authenticator.model.DxRole;
import iudx.data.marketplace.authenticator.model.JwtData;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.common.RoutingContextHelper;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthorizationHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LogManager.getLogger(AuthorizationHandler.class);

  /**
   * handling roles allowed for the endpoint by checking if the role in the token matches it throws
   * DxRuntimeException if there is a mismatch
   *
   * @param event the event to handle
   */
  @Override
  public void handle(RoutingContext event) {
    event.next();
  }

  public Handler<RoutingContext> setUserRolesForEndpoint(DxRole... roleForApi) {
    return context -> handleWithRoles(context, roleForApi);
  }

  private void handleWithRoles(RoutingContext event, DxRole[] roleForApi) {
    JwtData jwtData = RoutingContextHelper.getJwtData(event);
    DxRole userRole = DxRole.fromRole(jwtData);
    boolean isUserAllowedToAccessApi = Arrays.asList(roleForApi).contains(userRole);
    if (!isUserAllowedToAccessApi) {
      LOGGER.error( "No access provided to endpoint");
      event.fail(
          new DxRuntimeException(
              UNAUTHORIZED.getValue(),
              ResponseUrn.INVALID_TOKEN_URN,
              UNAUTHORIZED.getDescription()));
    }
    event.next();
  }
}
