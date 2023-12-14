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
    consumerListDatasets = new StringBuilder(CONSUMER_PATH + LIST_DATASETS_PATH);
    consumerListProviders = new StringBuilder(CONSUMER_PATH + LIST_PROVIDERS_PATH);
    consumerListPurchases = new StringBuilder(CONSUMER_PATH + LIST_PURCHASES_PATH);
    consumerListProducts = new StringBuilder(CONSUMER_PATH + LIST_PRODUCTS_PATH);
  }

  public String getPoliciesUrl() {
    return policiesUrl.toString();
  }

  public String getVerifyUrl() {
    return verifyUrl.toString();
  }

  public StringBuilder getProviderProductPath() {
    return providerProductPath;
  }

  public StringBuilder getProviderListProductsPath() {
    return providerListProductsPath;
  }

  public StringBuilder getProviderListPurchasesPath() {
    return providerListPurchasesPath;
  }

  public StringBuilder getProviderProductVariantPath() {
    return providerProductVariantPath;
  }

  public StringBuilder getProductUserMapsPath() {
    return productUserMapsPath;
  }

  public StringBuilder getConsumerListDatasets() {
    return consumerListDatasets;
  }

  public StringBuilder getConsumerListProviders() {
    return consumerListProviders;
  }

  public StringBuilder getConsumerListPurchases() {
    return consumerListPurchases;
  }

  public StringBuilder getConsumerListProducts() {
    return consumerListProducts;
  }
}
