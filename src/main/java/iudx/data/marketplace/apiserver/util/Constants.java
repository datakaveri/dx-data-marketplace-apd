package iudx.data.marketplace.apiserver.util;

import java.util.regex.Pattern;

public class Constants {

  public static final String ID = "id";
  public static final String API_ENDPOINT = "apiEndpoint";
  public static final String API_METHOD = "method";
  public static final String USER_ID = "userid";
  public static final String EXPIRY = "expiry";
  public static final String IID = "iid";
  public static final String RESULTS = "results";
  public static final String METHOD = "method";
  public static final String STATUS = "title";
  public static final String TYPE_FAIL = "fail";
  public static final String MSG_BAD_QUERY = "Bad query";

  // Header params
  public static final String HEADER_TOKEN = "token";
  public static final String AUTHORIZATION_KEY = "Authorization";

  public static final String HEADER_HOST = "Host";
  public static final String HEADER_ACCEPT = "Accept";
  public static final String HEADER_CONTENT_LENGTH = "Content-Length";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_ORIGIN = "Origin";
  public static final String HEADER_REFERER = "Referer";
  public static final String HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  public static final String HEADER_X_RAZORPAY_SIGNATURE = "X-Razorpay-Signature";

  // request/response params
  public static final String CONTENT_TYPE = "content-type";
  public static final String APPLICATION_JSON = "application/json";
  // json fields
  public static final String JSON_TYPE = "type";
  public static final String JSON_TITLE = "title";
  public static final String JSON_DETAIL = "detail";
  public static final String USERID = "userId";
  public static final String ROLE = "role";
  public static final String AUD = "aud";

  public static final String IS_DELEGATE = "isDelegate";

  public static final String RESOURCE_SERVER_URL = "resourceServerRegURL";
  public static final String USER_ROLE = "userRole";
  public static final String EMAIL_ID = "emailId";
  public static final String RS_SERVER_URL = "resourceServerUrl";
  public static final String FIRST_NAME = "firstName";
  public static final String LAST_NAME = "lastName";
  public static final String POLICY_ID = "policyId";
  public static final String ACCOUNT_ID = "account_id";
  public static final String RAZORPAY_PAYLOAD = "payload";
  public static final String RAZORPAY_ORDER = "order";
  public static final String RAZORPAY_PAYMENT = "payment";
  public static final String RAZORPAY_ENTITY = "entity";
  public static final String RAZORPAY_ID = "id";
  public static final String RAZORPAY_ORDER_ID = "order_id";

  // paths
  public static final String ROUTE_STATIC_SPEC = "/apis/spec";
  public static final String ROUTE_DOC = "/apis";
  public static final String MIME_APPLICATION_JSON = "application/json";
  public static final String MIME_TEXT_HTML = "text/html";

  public static final String PROVIDER_PATH = "/provider";
  public static final String PRODUCT_PATH = "/product";
  public static final String PRODUCT_VARIANT_PATH = "/product-variant";
  public static final String LIST_PRODUCTS_PATH = "/list/products";
  public static final String LIST_PURCHASES_PATH = "/list/purchases";
  public static final String CONSUMER_PATH = "/consumer";
  public static final String LIST_PROVIDERS_PATH = "/list/providers";
  public static final String LIST_RESOURCES_PATH = "/list/resources";
  public static final String ORDERS_PATH = "/order";
  public static final String USERMAPS_PATH = "/product/usermaps";
  public static final String VERIFY_PATH = "/verify";
  public static final String POLICIES_API = "/policies";
  public static final String VERIFY_PAYMENTS_PATH = "/verify-payment";
  public static final String ACCOUNTS_API = "/account";

  // query parameters | request body

  public static final String PRODUCT_ID = "productId";
  public static final String PRODUCT_VARIANT_NAME = "variant";
  public static final String PRODUCT_VARIANT_ID = "productVariantId";
  public static final String RESOURCE_ID = "resourceID";

  public static final String PROVIDER_ID = "providerId";

  // validations
  public static final int VALIDATION_URN_DELIMITER_COUNT = 3;
  public static final String STRING_URN = "urn";
  public static final String DOMAIN = "datakaveri.org";
  public static final int VALIDATION_PRODUCT_ID_MAXLEN = 150;
  public static final Pattern VALIDATION_PRODUCT_ID_REGEX = Pattern.compile("^[a-zA-Z0-9]{3,150}$");
  public static final int VALIDATION_VARIANT_NAME_MAX_LEN = 100;
  public static final Pattern VALIDATION_VARIANT_NAME_REGEX =
      Pattern.compile("^[a-zA-Z0-9-]{3,100}$");
  public static final int VALIDATION_ID_LENGTH = 36;
  public static final Pattern VALIDATION_IUDX_ID_REGEX =
      Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
  public static final Pattern POLICY_ID_PATTERN =
      Pattern.compile(
          "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}");

  // response keys
  public static final String TYPE = "type";
  public static final String TITLE = "title";
  public static final String DETAIL = "detail";
  public static final String RESULT = "results";
  public static final String STATUS_CODE = "statusCode";
}
