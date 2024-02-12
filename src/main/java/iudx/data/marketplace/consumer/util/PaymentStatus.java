package iudx.data.marketplace.consumer.util;

public enum PaymentStatus {
  PENDING("Pending"),
  SUCCESSFUL("Successful"),
  FAILED("Failed");

  private String status;

  private PaymentStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return this.status;
  }
}
