package iudx.data.marketplace.authenticator.model;

import java.util.UUID;

public class UserInfo {
  private UUID userId;
  private boolean isDelegate;
  private DxRole role;
  private String audience;

  public UUID getUserId() {
    return userId;
  }

  public UserInfo setUserId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public boolean isDelegate() {
    return isDelegate;
  }

  public UserInfo setDelegate(boolean delegate) {
    isDelegate = delegate;
    return this;
  }

  public DxRole getRole() {
    return role;
  }

  public UserInfo setRole(DxRole role) {
    this.role = role;
    return this;
  }

  public String getAudience() {
    return audience;
  }

  public UserInfo setAudience(String audience) {
    this.audience = audience;
    return this;
  }

  @Override
  public String toString() {
    return "UserInfo{" +
        "userId=" + userId +
        ", isDelegate=" + isDelegate +
        ", role=" + role +
        ", audience='" + audience + '\'' +
        '}';
  }
}
