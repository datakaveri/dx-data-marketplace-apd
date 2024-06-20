package iudx.data.marketplace.consentAgreementGenerator.controller;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.consentAgreementGenerator.util.Assets;

import java.nio.file.Path;
import java.nio.file.Paths;

import static iudx.data.marketplace.consentAgreementGenerator.util.Assets.FILE_EXTENSION;
import static iudx.data.marketplace.consentAgreementGenerator.util.Assets.HTML_FILE_NAME;

// model class = db table
@DataObject(generateConverter = true)
public final class PolicyDetails {
  private static final String CUSTOMER_FACING_BUSINESS_NAME = "Dummy Corp";
  private String policyId;
  Assets assets;
  private String resourceId;
  private String invoiceId;
  private JsonObject constraints;
  private String providerId;
  private String consumerEmailId;
  private String policyStatus;
  private String productVariantId;
  private String policyCreatedAt;
  private String policyExpiryAt;
  private String resourceServerUrl;
  private boolean isPolicyExpired;

  public PolicyDetails(JsonObject policyDetails) {
    this.policyId = policyDetails.getString("policyId");
    this.resourceId = policyDetails.getString("resourceId");
    this.invoiceId = policyDetails.getString("invoiceId");
    this.constraints = policyDetails.getJsonObject("constraints");
    this.providerId = policyDetails.getString("providerId");
    this.consumerEmailId = policyDetails.getString("consumerEmailId");
    this.policyStatus = policyDetails.getString("policyStatus");
    this.productVariantId = policyDetails.getString("productVariantId");
    this.policyCreatedAt = policyDetails.getString("createdAt");
    this.policyExpiryAt = policyDetails.getString("expiryAt");
    this.resourceServerUrl = policyDetails.getString("resourceServerUrl");
    this.isPolicyExpired = policyDetails.getBoolean("isPolicyExpired");
    Path path = Paths.get(HTML_FILE_NAME+FILE_EXTENSION);
    assets = new Assets(path);

  PolicyDetailsConverter.fromJson(policyDetails, this);
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    PolicyDetailsConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public String getPolicyId() {
    return policyId;
  }

  public String getCustomerFacingBusinessName() {
    return CUSTOMER_FACING_BUSINESS_NAME;
  }

  public Assets getAssets() {
    return assets;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getInvoiceId() {
    return invoiceId;
  }

  public JsonObject getConstraints() {
    return constraints;
  }

  public String getProviderId() {
    return providerId;
  }

  public String getConsumerEmailId() {
    return consumerEmailId;
  }

  public String getPolicyStatus() {
    return policyStatus;
  }

  public String getProductVariantId() {
    return productVariantId;
  }

  public String getPolicyCreatedAt() {
    return policyCreatedAt;
  }

  public String getResourceServerUrl() {
    return resourceServerUrl;
  }

  public boolean getIsPolicyExpired() {
    return isPolicyExpired;
  }

  public String getPolicyExpiryAt() {
    return policyExpiryAt;
  }

  @Override
  public String toString() {
    return "PolicyDetails{"
        + "policyId='"
        + policyId
        + '\''
        + ", assets="
        + assets
        + ", resourceId='"
        + resourceId
        + '\''
        + ", invoiceId='"
        + invoiceId
        + '\''
        + ", constraints="
        + constraints
        + ", providerId='"
        + providerId
        + '\''
        + ", consumerEmailId='"
        + consumerEmailId
        + '\''
        + ", policyStatus='"
        + policyStatus
        + '\''
        + ", productVariantId='"
        + productVariantId
        + '\''
        + ", policyCreatedAt='"
        + policyCreatedAt
        + '\''
        + ", policyExpiryAt='"
        + policyExpiryAt
        + '\''
        + ", resourceServerUrl='"
        + resourceServerUrl
        + '\''
        + ", isPolicyExpired="
        + isPolicyExpired
        + '}';
  }




}
