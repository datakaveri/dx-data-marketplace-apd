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


import static iudx.data.marketplace.authenticator.authorization.Method.GET;
import static iudx.data.marketplace.authenticator.authorization.Method.POST;

public class ConsumerAuthStatergy implements AuthorizationStatergy {
  private static final Logger LOGGER = LogManager.getLogger(ConsumerAuthStatergy.class);
  private static volatile ConsumerAuthStatergy instance;

  Map<String, List<AuthorizationRequest>> consumerAuthorizationRules = new HashMap<>();
  private ConsumerAuthStatergy(Api apis) {
    buildPermissions(apis);
  }
  public static ConsumerAuthStatergy getInstance(Api apis) {
    if (instance == null) {
      synchronized (ConsumerAuthStatergy.class) {
        if (instance == null) {
          instance = new ConsumerAuthStatergy(apis);
        }
      }
    }
    return instance;
  }
  private void buildPermissions(Api apis) {
    // api access list
    List<AuthorizationRequest> apiAccessList = new ArrayList<>();
    apiAccessList.add(new AuthorizationRequest(GET, apis.getConsumerListDatasets()));
    apiAccessList.add(new AuthorizationRequest(GET, apis.getConsumerListProviders()));
    apiAccessList.add(new AuthorizationRequest(GET, apis.getConsumerListProducts()));
    apiAccessList.add(new AuthorizationRequest(GET, apis.getConsumerListPurchases()));
    apiAccessList.add(new AuthorizationRequest(POST, apis.getConsumerOrderApi()));
    apiAccessList.add(new AuthorizationRequest(GET, apis.getConsumerListResourcePath()));
    apiAccessList.add(new AuthorizationRequest(GET, apis.getConsumerProductVariantPath()));
    apiAccessList.add(new AuthorizationRequest(POST, apis.getVerifyPaymentApi()));
    apiAccessList.add(new AuthorizationRequest(GET, apis.getCheckPolicyPath()));

    // policy
    apiAccessList.add(new AuthorizationRequest(GET, apis.getPoliciesUrl()));
    consumerAuthorizationRules.put("api", apiAccessList);
  }
  @Override
  public boolean isAuthorized(AuthorizationRequest authorizationRequest, JwtData jwtData) {
    String endpoint = authorizationRequest.getApi();
    Method method = authorizationRequest.getMethod();
    LOGGER.info("authorization request for : " + endpoint + " with method : " + method.name());

    return consumerAuthorizationRules.get("api").contains(authorizationRequest);
  }
}
