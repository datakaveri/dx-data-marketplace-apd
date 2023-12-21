package iudx.data.marketplace.apiserver.util;

public enum RequestType {
  PRODUCT("product"),
  PRODUCT_VARIANT("product_variant"),
  DATASET("dataset"),
  POLICY("policy"),
  VERIFY("verify_policy"),
  RESOURCE("resource"),
  PROVIDER("provider");

  private String filename;

  public String getFilename() {
    return this.filename;
  }

  private RequestType(String filename) {
    this.filename = filename;
  }
}
