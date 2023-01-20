package iudx.data.marketplace.authenticator.authorization;

import io.vertx.core.json.JsonArray;
import iudx.data.marketplace.authenticator.model.JwtData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static iudx.data.marketplace.authenticator.authorization.Api.*;
import static iudx.data.marketplace.authenticator.authorization.Method.*;

public class ProviderAuthStatergy implements AuthorizationStatergy {
  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStatergy.class);

  static List<AuthorizationRequest> providerAuthRules = new ArrayList<>();

  static {

    // api access list
    List<AuthorizationRequest> apiAccessList = new ArrayList<>();
    apiAccessList.add(new AuthorizationRequest(POST, PRODUCT));
    apiAccessList.add(new AuthorizationRequest(DELETE, PRODUCT));
    apiAccessList.add(new AuthorizationRequest(GET, PROVIDER_LIST_PRODUCTS));
    apiAccessList.add(new AuthorizationRequest(GET, PROVIDER_LIST_PURCHASES));
    providerAuthRules = apiAccessList;
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authorizationRequest, JwtData jwtData) {
    // JsonArray access = jwtData.getCons() != null ? jwtData.getCons().getJsonArray("access") : null;
//    if (access == null) {
//      return false;
//    }
    String endpoint = authorizationRequest.getApi().getEndpoint();
    Method method = authorizationRequest.getMethod();
    LOGGER.info("authorization request for : " + endpoint + " with method : " + method.name());

    return providerAuthRules.contains(authorizationRequest);
  }
}
