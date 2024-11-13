package iudx.data.marketplace.consumer.util;

import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;

import java.util.stream.Stream;

import static iudx.data.marketplace.common.ResponseUrn.PAYMENT_STATUS_NOT_FOUND;
public enum PaymentStatus {
  PENDING("PENDING"),
  SUCCESSFUL("SUCCEEDED"),
  FAILED("FAILED");

  private final String paymentStatus;
  PaymentStatus(String value)
  {
    paymentStatus = value;
  }

  public static PaymentStatus fromString(String value)
  {
    return Stream.of(values())
            .filter(element -> element.getPaymentStatus().equalsIgnoreCase(value))
            .findAny()
            .orElseThrow(() -> new DxRuntimeException(404, PAYMENT_STATUS_NOT_FOUND));
  }

  public String getPaymentStatus()
  {
    return paymentStatus;
  }

}
