package iudx.data.marketplace.authenticator.authorization;

import iudx.data.marketplace.authenticator.model.JwtData;

public interface AuthorizationStatergy {

  boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData);
}
