package iudx.data.marketplace.razorpay;

public class Constants {

  public static final String RAZORPAY_KEY = "razorPayKey";
  public static final String RAZORPAY_SECRET = "razorPaySecret";
  public static final String AMOUNT = "amount";
  public static final String CURRENCY = "currency";
  public static final String INR = "INR";
  public static final String RECEIPT = "receipt";
  public static final String ACCOUNT = "account";
  public static final String ACCOUNT_ID = "account_id";
  public static final String NOTES = "notes";
  public static final String ON_HOLD = "on_hold";
  public static final String TRANFERS = "transfers";
  public static final String CREATED = "created";
  public static final String ERROR = "error";
  public static final String REASON = "reason";
  public static final Integer ZERO = 0;
  public static final String RAZORPAY_ORDER_ID = "razorpay_order_id";
  public static final String RAZORPAY_PAYMENT_ID = "razorpay_payment_id";
  public static final String RAZORPAY_SIGNATURE = "razorpay_signature";

  public static final String RECORD_PAYMENT = "insert into $0 (order_id, payment_id, payment_signature) values ($1,$2,$3)";
}
