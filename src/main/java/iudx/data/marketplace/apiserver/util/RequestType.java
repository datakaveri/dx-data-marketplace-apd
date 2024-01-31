package iudx.data.marketplace.apiserver.util;

public enum RequestType {
  PRODUCT("product"),
  PRODUCT_VARIANT("product_variant"),
  RESOURCE("resource"),
  POLICY("policy"),
  VERIFY("verify_policy"),
  ACCOUNT("account"),
  PROVIDER("provider"),
  ORDER("order");

  private String filename;

  public String getFilename() {
    return this.filename;
  }

  private RequestType(String filename) {
    this.filename = filename;
  }
}
