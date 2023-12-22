package iudx.data.marketplace.authenticator.authorization;

import iudx.data.marketplace.common.Api;

public class AuthorizationContextFactory {
  public static AuthorizationStatergy create(IudxRole role, Api api){
    if (role == null) {
      throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
    switch (role) {
      case PROVIDER: {
        return ProviderAuthStatergy.getInstance(api);
      }
      case CONSUMER: {
        return ConsumerAuthStatergy.getInstance(api);
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }

  }

//  private static final AuthorizationStatergy consumerAuth = new ConsumerAuthStatergy();
//  private static final AuthorizationStatergy providerAuth = new ProviderAuthStatergy();
//  private static final AuthorizationStatergy delegateAuth = new DelegateAuthStatergy();
//  private static final AuthorizationStatergy adminAuth = AdminAuthStatergy.getInstance();
//
//  public static AuthorizationStatergy create(String role) {
//    switch (role) {
//      case "consumer":
//        {
//          return consumerAuth;
//        }
//      case "provider":
//        {
//          return providerAuth;
//        }
//      case "delegate":
//        {
//          return delegateAuth;
//        }
//      case "admin":
//        {
//          return adminAuth;
//        }
//      default:
//        throw new IllegalArgumentException(role + "role is not defined in IUDX");
//    }
//  }
}
