package iudx.data.marketplace.authenticator.authorization;

import io.vertx.core.json.JsonArray;
import iudx.data.marketplace.authenticator.model.JwtData;
import iudx.data.marketplace.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static iudx.data.marketplace.authenticator.authorization.Method.*;

@Deprecated
public class AdminAuthStatergy implements AuthorizationStatergy {
  private static final Logger LOGGER = LogManager.getLogger(AdminAuthStatergy.class);

  private static volatile AdminAuthStatergy instance;
  Map<String, List<AuthorizationRequest>> adminAuthorizationRules = new HashMap<>();
  private AdminAuthStatergy(Api api)
  {
    buildPermission(api);
  }

  private void buildPermission(Api api) {
    // api access list
    List<AuthorizationRequest> apiAccessList = new ArrayList<>();
    apiAccessList.add(new AuthorizationRequest(POST, api.getProductUserMapsPath()));
    adminAuthorizationRules.put("api", apiAccessList);
  }

  public static AdminAuthStatergy getInstance(Api api)
  {
    if(instance == null)
    {
      synchronized (AdminAuthStatergy.class)
      {
        if(instance == null)
        {
          instance = new AdminAuthStatergy(api);
        }
      }
    }
    return instance;
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authorizationRequest, JwtData jwtData) {
    String endpoint = authorizationRequest.getApi();
    Method method = authorizationRequest.getMethod();
    LOGGER.info("authorization request for : " + endpoint + " with method : " + method.name());
    return adminAuthorizationRules.get("api").contains(authorizationRequest);
  }
}
