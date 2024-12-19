package iudx.data.marketplace.authenticator.handler;

import java.util.Objects;

public class AuthorizationRequest {

  private final Method method;
  private final String api;

  public AuthorizationRequest(Method method, String apis) {
    this.method = method;
    this.api = apis;
  }

  public String getApi() {
    return api;
  }

  public Method getMethod() {
    return method;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AuthorizationRequest)) {
      return false;
    }
    AuthorizationRequest that = (AuthorizationRequest) o;
    return method == that.method && Objects.equals(api, that.api);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, api);
  }
}
