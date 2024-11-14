package iudx.data.marketplace.authenticator.authorization;

import static iudx.data.marketplace.authenticator.authorization.Method.*;

import iudx.data.marketplace.authenticator.model.JwtData;
import iudx.data.marketplace.common.Api;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProviderAuthStatergy implements AuthorizationStatergy {
  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStatergy.class);
  private static volatile ProviderAuthStatergy instance;
  Map<String, List<AuthorizationRequest>> providerAuthorizationRequest = new HashMap<>();

  private ProviderAuthStatergy(Api api) {
    buildPermissions(api);
  }

  public static ProviderAuthStatergy getInstance(Api api) {
    if (instance == null) {
      synchronized (ProviderAuthStatergy.class) {
        if (instance == null) {
          instance = new ProviderAuthStatergy(api);
        }
      }
    }
    return instance;
  }

  private void buildPermissions(Api api) {
    List<AuthorizationRequest> apiAccessList = new ArrayList<>();
    apiAccessList.add(new AuthorizationRequest(POST, api.getProviderProductPath()));
    apiAccessList.add(new AuthorizationRequest(DELETE, api.getProviderProductPath()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getProviderListProductsPath()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getProviderListPurchasesPath()));
    apiAccessList.add(new AuthorizationRequest(POST, api.getProviderProductVariantPath()));
    apiAccessList.add(new AuthorizationRequest(PUT, api.getProviderProductVariantPath()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getProviderProductVariantPath()));
    apiAccessList.add(new AuthorizationRequest(DELETE, api.getProviderProductVariantPath()));

    //    Linked Account APIs
    apiAccessList.add(new AuthorizationRequest(POST, api.getLinkedAccountService()));
    apiAccessList.add(new AuthorizationRequest(PUT, api.getLinkedAccountService()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getLinkedAccountService()));

    //    policies
    apiAccessList.add(new AuthorizationRequest(GET, api.getPoliciesUrl()));
    apiAccessList.add(new AuthorizationRequest(DELETE, api.getPoliciesUrl()));
    providerAuthorizationRequest.put("api", apiAccessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authorizationRequest, JwtData jwtData) {
    String endpoint = authorizationRequest.getApi();
    Method method = authorizationRequest.getMethod();
    LOGGER.info("authorization request for : " + endpoint + " with method : " + method.name());

    return providerAuthorizationRequest.get("api").contains(authorizationRequest);
  }
}
