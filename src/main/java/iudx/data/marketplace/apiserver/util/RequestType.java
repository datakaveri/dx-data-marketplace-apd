package iudx.data.marketplace.apiserver.util;

public enum RequestType {
  PRODUCT("product"),
  PRODUCT_VARIANT("product_variant"),
  LIST_PRODUCT_VARIANT("list_product_variant"),

  DELETE_PRODUCT_VARIANT("delete_product_variant"),
  RESOURCE("resource"),
  POLICY("policy"),
  VERIFY("verify_policy"),
  PROVIDER("provider"),
  ORDER("order"),
  PURCHASE("purchase"),
  VERIFY_PAYMENT("verify_payment"),
  POST_ACCOUNT("post_account"),
  CONSUMER_PRODUCT_VARIANT("consumer_list_product_variant"),
  PUT_ACCOUNT("put_account"),
  ORDER_PAID_WEBHOOK("order_paid_webhook"),
  PAYMENT_AUTHORIZED_WEBHOOK("payment_authorized_webhook"),
  PAYMENT_CAPTURED_WEBHOOK("payment_captured_webhook"),
  PAYMENT_FAILED_WEBHOOK("payment_failed_webhook"),
  CHECK_POLICY("check_policy"),
  TRANSFER_PROCESSEC_WEBHOOK("transfer_processed_webhook"),
  CONSENT_AGREEMENT("pdf_gen");

  private String filename;

  public String getFilename() {
    return this.filename;
  }

  private RequestType(String filename) {
    this.filename = filename;
  }
}
