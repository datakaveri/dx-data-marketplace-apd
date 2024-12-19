package iudx.data.marketplace.authenticator.handler;

import iudx.data.marketplace.authenticator.model.JwtData;

public class JwtAuthorization {

  //  private static final Logger LOGGER = LogManager.getLogger(JwtAuthorization.class);

  private final AuthorizationStatergy authStrategy;

  public JwtAuthorization(final AuthorizationStatergy authStrategy) {
    this.authStrategy = authStrategy;
  }

  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return authStrategy.isAuthorized(authRequest, jwtData);
  }
}
