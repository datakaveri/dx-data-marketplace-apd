package iudx.data.marketplace.authenticator.handler;

import iudx.data.marketplace.authenticator.model.JwtData;

public interface AuthorizationStatergy {

  boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData);
}
