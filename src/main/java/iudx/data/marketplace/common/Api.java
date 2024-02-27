package iudx.data.marketplace.common;

import static iudx.data.marketplace.apiserver.util.Constants.*;

public class Api {

  private static volatile Api apiInstance;
  private final String dxApiBasePath;
  private StringBuilder policiesUrl;
  private StringBuilder verifyUrl;
  private StringBuilder providerProductPath;
  private StringBuilder providerListProductsPath;
  private StringBuilder providerListPurchasesPath;
  private StringBuilder providerProductVariantPath;
  private StringBuilder productUserMapsPath;
  private StringBuilder consumerListDatasets;
  private StringBuilder consumerListProviders;
  private StringBuilder consumerListPurchases;
  private StringBuilder consumerListProducts;
  private StringBuilder linkedAccountService;
  private StringBuilder consumerOrdersApi;
  private StringBuilder verifyPaymentApi;

  private Api(String dxApiBasePath) {
    this.dxApiBasePath = dxApiBasePath;
    buildPaths();
  }

  public static Api getInstance(String dxApiBasePath) {
    if (apiInstance == null) {
      synchronized (Api.class) {
        if (apiInstance == null) {
          apiInstance = new Api(dxApiBasePath);
        }
      }
    }
    return apiInstance;
  }

  private void buildPaths() {
    policiesUrl = new StringBuilder(dxApiBasePath).append(POLICIES_API);
    verifyUrl = new StringBuilder(VERIFY_PATH);
    providerProductPath = new StringBuilder(PROVIDER_PATH + PRODUCT_PATH);
    providerListProductsPath = new StringBuilder(PROVIDER_PATH + LIST_PRODUCTS_PATH);
    providerListPurchasesPath = new StringBuilder(PROVIDER_PATH + LIST_PURCHASES_PATH);
    providerProductVariantPath = new StringBuilder(PROVIDER_PATH + PRODUCT_VARIANT_PATH);
    productUserMapsPath = new StringBuilder(USERMAPS_PATH);
    consumerListDatasets = new StringBuilder(CONSUMER_PATH + LIST_RESOURCES_PATH);
    consumerListProviders = new StringBuilder(CONSUMER_PATH + LIST_PROVIDERS_PATH);
    consumerListPurchases = new StringBuilder(CONSUMER_PATH + LIST_PURCHASES_PATH);
    consumerListProducts = new StringBuilder(CONSUMER_PATH + LIST_PRODUCTS_PATH);
    linkedAccountService = new StringBuilder(dxApiBasePath).append(ACCOUNTS_API);
    consumerOrdersApi = new StringBuilder(CONSUMER_PATH + ORDERS_PATH);
    verifyPaymentApi = new StringBuilder(VERIFY_PAYMENTS_PATH);
  }

  public String getPoliciesUrl() {
    return policiesUrl.toString();
  }

  public String getVerifyUrl() {
    return verifyUrl.toString();
  }

  public String getProviderProductPath() {
    return providerProductPath.toString();
  }

  public String getProviderListProductsPath() {
    return providerListProductsPath.toString();
  }

  public String getProviderListPurchasesPath() {
    return providerListPurchasesPath.toString();
  }

  public String getProviderProductVariantPath() {
    return providerProductVariantPath.toString();
  }

  public String getProductUserMapsPath() {
    return productUserMapsPath.toString();
  }

  public String getConsumerListDatasets() {
    return consumerListDatasets.toString();
  }

  public String getConsumerListProviders() {
    return consumerListProviders.toString();
  }

  public String getConsumerListPurchases() {
    return consumerListPurchases.toString();
  }

  public String getConsumerListProducts() {
    return consumerListProducts.toString();
  }

  public String getLinkedAccountService() {
    return linkedAccountService.toString();
}
  public String getConsumerOrderApi() {
    return consumerOrdersApi.toString();
  }

  public String getVerifyPaymentApi() {
    return verifyPaymentApi.toString();
  }
}
