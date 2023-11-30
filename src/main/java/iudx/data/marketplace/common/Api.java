package iudx.data.marketplace.common;

import static iudx.data.marketplace.apiserver.util.Constants.POLICIES_API;
import static iudx.data.marketplace.apiserver.util.Constants.VERIFY_PATH;

public class Api {

  private static volatile Api apiInstance;
  private final String dxApiBasePath;
  private StringBuilder policiesUrl;
  private StringBuilder verifyUrl;
  private StringBuilder requestPoliciesUrl;
  private StringBuilder policy;

  private Api(String dxApiBasePath) {
    this.dxApiBasePath = dxApiBasePath;
    buildPaths();
  }

  public static Api getInstance(String dxApiBasePath) {
    if (apiInstance == null) {
      synchronized (Api.class) {
        if (apiInstance == null) {
          apiInstance = new Api(dxApiBasePath);
        }
      }
    }
    return apiInstance;
  }

  private void buildPaths() {
    policiesUrl = new StringBuilder(dxApiBasePath).append(POLICIES_API);
    verifyUrl = new StringBuilder(VERIFY_PATH);
  }

  public String getPoliciesUrl() {
    return policiesUrl.toString();
  }

  public String getVerifyUrl() {
    return verifyUrl.toString();
  }
}
