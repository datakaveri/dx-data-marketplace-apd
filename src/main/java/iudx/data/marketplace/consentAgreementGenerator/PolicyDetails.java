package iudx.data.marketplace.consentAgreementGenerator;

import io.vertx.core.json.JsonObject;

public final class PolicyDetails {
    private static final String CUSTOMER_FACING_BUSINESS_NAME = "Dummy Corp";
    private   String policyId;
    private   Assets assets;
    private   String resourceId;
    private   String invoiceId;
    private   JsonObject constraints;
    private   String providerId;
    private   String consumerEmailId;
    private   String policyStatus;
    private   String productVariantId;
    private   String policyCreatedAt;
    private   String policyExpiryAt;
    private   String resourceServerUrl;
    private   boolean isPolicyExpired;

    public String getPolicyId() {
        return policyId;
    }

    public String getCustomerFacingBusinessName()
    {
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

    public boolean isPolicyExpired() {
        return isPolicyExpired;
    }

    public String getPolicyExpiryAt() {
        return policyExpiryAt;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }
    public void setAssets(Assets assets) {
        this.assets = assets;
     }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
     }
    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
     }
    public void setConstraints(JsonObject constraints) {
        this.constraints = constraints;
     }
    public void setProviderId(String providerId) {
        this.providerId = providerId;
     }
    public void setConsumerEmailId(String consumerEmailId) {
        this.consumerEmailId = consumerEmailId;
     }

    public void setPolicyStatus(String policyStatus) {
        this.policyStatus = policyStatus;
     }
    public void setProductVariantId(String productVariantId) {
        this.productVariantId = productVariantId;
     }
    public void setPolicyCreatedAt(String policyCreatedAt) {
        this.policyCreatedAt = policyCreatedAt;
     }

    public void setResourceServerUrl(String resourceServerUrl) {
        this.resourceServerUrl = resourceServerUrl;
     }
    public void setPolicyExpired(boolean policyExpired) {
        isPolicyExpired = policyExpired;
     }
    public void setPolicyExpiryAt(String policyExpiryAt) {
        this.policyExpiryAt = policyExpiryAt;
    }


    @Override
    public String toString() {
        return "PolicyDetails{" +
                "policyId='" + policyId + '\'' +
                ", assets=" + assets +
                ", resourceId='" + resourceId + '\'' +
                ", invoiceId='" + invoiceId + '\'' +
                ", constraints=" + constraints +
                ", providerId='" + providerId + '\'' +
                ", consumerEmailId='" + consumerEmailId + '\'' +
                ", policyStatus='" + policyStatus + '\'' +
                ", productVariantId='" + productVariantId + '\'' +
                ", policyCreatedAt='" + policyCreatedAt + '\'' +
                ", policyExpiryAt='" + policyExpiryAt + '\'' +
                ", resourceServerUrl='" + resourceServerUrl + '\'' +
                ", isPolicyExpired=" + isPolicyExpired +
                '}';
    }
}
