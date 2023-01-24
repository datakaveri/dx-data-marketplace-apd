package iudx.data.marketplace.authenticator.authorization;

import java.util.stream.Stream;

import static iudx.data.marketplace.authenticator.util.Constants.*;

public enum Api {
  PRODUCT(PROVIDER_PRODUCT_ENDPOINT),
  PROVIDER_LIST_PRODUCTS(PROVIDER_LIST_PRODUCTS_ENDPOINT),
  PROVIDER_LIST_PURCHASES(PROVIDER_LIST_PURCHASES_ENDPOINT),
  CONSUMER_LIST_PROVIDERS(CONSUMER_LIST_PROVIDERS_ENDPOINT),
  CONSUMER_LIST_DATASETS(CONSUMER_LIST_DATASETS_ENDPOINT),
  CONSUMER_LIST_PRODUCTS(CONSUMER_LIST_PRODUCTS_ENDNPOINT),
  CONSUMER_LIST_PURCHASES(CONSUMER_LIST_PURCHASES_ENDPOINT),
  USERMAPS(USERMAPS_ENDPOINT),
  VERIFY(VERIFY_ENDPOINT);


  private final String endpoint;

  Api(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getEndpoint() {
    return this.endpoint;
  }

  public static Api fromEndpoint(final String endpoint) {
    return Stream.of(values())
        .filter(v -> v.endpoint.equalsIgnoreCase(endpoint))
        .findAny()
        .orElse(null);
  }
}
