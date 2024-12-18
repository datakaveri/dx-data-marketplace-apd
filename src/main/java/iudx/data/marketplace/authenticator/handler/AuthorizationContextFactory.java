package iudx.data.marketplace.authenticator.handler;

import iudx.data.marketplace.common.Api;

public class AuthorizationContextFactory {
  public static AuthorizationStatergy create(IudxRole role, Api api) {
    if (role == null) {
      throw new IllegalArgumentException(role + " role is not defined in IUDX");
    }
    switch (role) {
      case PROVIDER: {
        return ProviderAuthStatergy.getInstance(api);
      }
      case CONSUMER: {
        return ConsumerAuthStatergy.getInstance(api);
      }
      default:
        throw new IllegalArgumentException(role + " role is not defined in IUDX");
    }
  }
}
