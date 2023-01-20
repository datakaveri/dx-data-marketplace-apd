package iudx.data.marketplace.product.util;

import java.util.stream.Stream;

public enum Status {
  ACTIVE("ACTIVE"),
  INACTIVE("INACTIVE"),
  EXPIRED("EXPIRED");

  private final String status;

  Status(String status) {
    this.status = status;
  }

  public static Status getStatus(final String status) {
    return Stream.of(values())
        .filter(v -> v.status.equalsIgnoreCase(status))
        .findAny()
        .orElse(null);
  }
}
