package iudx.data.marketplace.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.authenticator.model.DxRole;
import iudx.data.marketplace.authenticator.model.JwtData;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.common.RoutingContextHelper;
import java.util.Arrays;

public class AccessHandler implements Handler<RoutingContext> {
/**
*
 * @param event
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
      event.fail(new DxRuntimeException(
          HttpStatusCode.UNAUTHORIZED.getValue(),
          ResponseUrn.INVALID_TOKEN_URN,
          "No access provided to endpoint"));
    }
    event.next();
  }

}
