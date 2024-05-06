package iudx.data.marketplace.webhook;

public class Constants {

  public static final String PAYMENT_STATUS = "payment_status";
  public static final String ORDER_ID = "order_id";

  public static final String UPDATE_PAYMENT_STATUS_QUERY =
      "update $0 set payment_status = $1 where order_id = $2 returning _id, payment_status;";
}
