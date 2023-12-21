package iudx.data.marketplace.apiserver.util;

public enum RequestType {
  PRODUCT("product"),
  PRODUCT_VARIANT("product_variant"),
  DATASET("dataset"),
  POLICY("policy"),
  VERIFY("verify_policy"),
  RESOURCE("resource"),
  DATASET("dataset"),
  POLICY("policy"),
  VERIFY("verify_policy"),
  PROVIDER("provider");

  private String filename;

  public String getFilename() {
    return this.filename;
  }

  private RequestType(String filename) {
    this.filename = filename;
  }
}
