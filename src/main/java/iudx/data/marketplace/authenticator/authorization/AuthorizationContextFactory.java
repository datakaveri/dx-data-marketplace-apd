package iudx.data.marketplace.authenticator.authorization;

public class AuthorizationContextFactory {

  private static final AuthorizationStatergy consumerAuth = new ConsumerAuthStatergy();
  private static final AuthorizationStatergy providerAuth = new ProviderAuthStatergy();
  private static final AuthorizationStatergy delegateAuth = new DelegateAuthStatergy();
  private static final AuthorizationStatergy adminAuth = new AdminAuthStatergy();

  public static AuthorizationStatergy create(String role) {
    switch (role) {
      case "consumer":
        {
          return consumerAuth;
        }
      case "provider":
        {
          return providerAuth;
        }
      case "delegate":
        {
          return delegateAuth;
        }
      case "admin":
        {
          return adminAuth;
        }
        //      case "trustee":
        //        {
        //          return trusteeAuth;
        //        }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }
}
