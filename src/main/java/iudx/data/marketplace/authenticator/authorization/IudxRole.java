package iudx.data.marketplace.authenticator.authorization;

import iudx.data.marketplace.authenticator.model.JwtData;

import java.util.stream.Stream;

public enum IudxRole {
  CONSUMER("consumer"),
  PROVIDER("provider"),
  DELEGATE("delegate");

  private final String role;

  IudxRole(String role) {
    this.role = role;
  }

  public static IudxRole fromRole(final JwtData jwtData) {
    String role =
        jwtData.getRole().equalsIgnoreCase(DELEGATE.getRole())
            ? jwtData.getDrl()
            : jwtData.getRole();
    return Stream.of(values()).filter(v -> v.role.equalsIgnoreCase(role)).findAny().orElse(null);
  }

  public String getRole() {
    return this.role;
  }
}
