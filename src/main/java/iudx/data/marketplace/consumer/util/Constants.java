package iudx.data.marketplace.consumer.util;

public class Constants {

  public static final String LIST_RESOURCES_QUERY =
      "select _id AS \"resourceId\", resource_name AS \"resourceName\", accessPolicy AS \"accessPolicy\","
          + " modified_at AS \"updatedAt\" , created_at AS \"createdAt\""
          + " ,provider_name AS \"providerName\" from $0 ";
  public static final String LIST_PROVIDERS_QUERY =
      "SELECT DISTINCT U._id AS \"providerId\", COUNT(R._id) AS \"numberOfResources\", \n"
          + "R.provider_name AS \"providerName\", \n"
          + "R.resource_server AS \"resourceServerUrl\",\n"
          + "U.modified_at AS \"updatedAt\"\n"
          + "U.created_at AS \"createdAt\"\n"
          + "FROM user_table U\n"
          + "INNER JOIN resource_entity R \n"
          + "ON U._id = R.provider_id\n"
          + "GROUP BY U._id, R.provider_name, R.resource_server, U.modified_at\n"
          + "ORDER BY U.modified_at DESC";
  public static final String LIST_PROVIDER_WITH_GIVEN_PROVIDER_ID =
      "SELECT DISTINCT U._id AS \"providerId\", COUNT(R._id) AS \"numberOfResources\", \n"
          + "R.provider_name AS \"providerName\", \n"
          + "R.resource_server AS \"resourceServerUrl\",\n"
          + "U.modified_at AS \"updatedAt\"\n"
          + "U.created_at AS \"createdAt\"\n"
          + "FROM user_table U\n"
          + "INNER JOIN resource_entity R \n"
          + "ON U._id = R.provider_id\n"
          + "WHERE U._id = $1 \n"
          + "GROUP BY U._id, R.provider_name, R.resource_server, U.modified_at\n"
          + "ORDER BY U.modified_at DESC";

  public static final String LIST_PRODUCTS =
      "select pt.product_id AS \"productId\", pt.provider_name AS \"providerName\", "
          + " pt.modified_at AS \"updatedAt\" , "
          + " pt.created_at AS \"createdAt\" , "
          + "array_agg(json_build_object('id', rt._id, 'name', rt.resource_name)) as resources "
          + "from $0 as pt "
          + "inner join $9 as dpt on pt.product_id = dpt.product_id "
          + "inner join $8 as rt on dpt.resource_id = rt._id "
          + "where  pt.status=$1";

  public static final String GET_PRODUCT_VARIANT_INFO =
          "select pv._id, pv.product_variant_name, pv.product_id, pv.provider_id, pv.price, m.account_id "
                  + "from $0 as pv inner join $9 as m on pv.provider_id = m.provider_id "
                  + "where pv._id=$1 and pv.status=$2";

  public static final String INSERT_ORDER_QUERY =
          "insert into $0 (order_id, amount, currency, account_id, notes) values ('$1', '$2', '$3', '$4', '$5')";
  public static final String LIST_FAILED_OR_PENDING_PAYMENTS =
      "SELECT DISTINCT I._id AS \"invoiceId\", P.provider_id AS \"providerId\",\n"
          + "U.email_id AS \"providerEmailId\", U.first_name AS \"providerFirstName\",\n"
          + "U.last_name AS \"providerLastName\", \n"
          + "I.order_id AS \"orderId\", I.product_variant_id AS \"productVariantId\",\n"
          + "P.product_id AS \"productId\", P.resource_info AS \"resources\",\n"
          + "P.product_variant_name as \"productVariantName\",\n"
          + "P.price,\n"
          + "I.payment_status AS \"paymentStatus\", I.payment_time AS \"paymentTime\",\n"
          + "I.expiry AS \"expiryInMonths\" \n"
          +  ", I.modified_at AS \"updatedAt\" ,  I.created_at AS \"createdAt\" "
          + "FROM invoice I\n"
          + "INNER JOIN product_variant P\n"
          + "ON I.product_variant_id = P._id\n"
          + "INNER JOIN user_table U \n"
          + "ON U._id = P.provider_id \n";
  public static final String LIST_SUCCESSFUL_PAYMENTS =
      "SELECT DISTINCT I._id AS \"invoiceId\", P.provider_id AS \"providerId\",\n"
          + "U.email_id AS \"providerEmailId\", U.first_name AS \"providerFirstName\",\n"
          + "U.last_name AS \"providerLastName\",  \n"
          + "I.order_id AS \"orderId\", I.product_variant_id AS \"productVariantId\",\n"
          + "P.product_id AS \"productId\", P.resource_info AS \"resources\",\n"
          + "P.product_variant_name as \"productVariantName\",\n"
          + "P.price,\n"
          + "I.payment_status AS \"paymentStatus\", I.payment_time AS \"paymentTime\",\n"
          + "I.expiry AS \"expiryInMonths\", policy.expiry_at AS \"expiryAt\" \n"
          +  ", I.modified_at AS \"updatedAt\" ,  I.created_at AS \"createdAt\" "
          + "FROM policy inner join invoice I \n"
          + "ON policy.invoice_id = I._id\n"
          + "INNER JOIN product_variant P\n"
          + "ON I.product_variant_id = P._id\n"
          + "INNER JOIN user_table U\n"
          + "ON U._id = P.provider_id";

  public static final String LIST_SUCCESSFUL_PAYMENTS_PAYMENTS_4_CONSUMER =
          LIST_SUCCESSFUL_PAYMENTS + " WHERE I.consumer_id = '$1' ";
  public static final String LIST_SUCCESSFUL_PAYMENTS_4_CONSUMER_WITH_GIVEN_PRODUCT =
          LIST_SUCCESSFUL_PAYMENTS
                  + " WHERE\n"
                  + " P.product_id = '$1'\n"
                  + " AND I.consumer_id = '$2'";
  public static final String LIST_SUCCESSFUL_PAYMENTS_4_CONSUMER_WITH_GIVEN_RESOURCE =
          LIST_SUCCESSFUL_PAYMENTS
                  + " WHERE\n"
                  + " P.product_id IN (SELECT product_id FROM product_resource_relation WHERE resource_id = '$1'\n"
                  + " )\n"
                  + " AND I.consumer_id = '$2'";

  public static final String LIST_PENDING_PAYMENTS_4_CONSUMER =
          LIST_FAILED_OR_PENDING_PAYMENTS + " WHERE I.consumer_id = '$1'  AND I.payment_status == 'PENDING' ";
  public static final String LIST_PENDING_PAYMENTS_4_CONSUMER_WITH_GIVEN_PRODUCT =
          LIST_FAILED_OR_PENDING_PAYMENTS
                  + " WHERE\n"
                  + " P.product_id = '$1'\n"
                  + " AND I.consumer_id = '$2' AND I.payment_status == 'PENDING' ";
  public static final String LIST_PENDING_PAYMENTS_4_CONSUMER_WITH_GIVEN_RESOURCE =
          LIST_FAILED_OR_PENDING_PAYMENTS
                  + " WHERE\n"
                  + " P.product_id IN (SELECT product_id FROM product_resource_relation WHERE resource_id = '$1'\n"
                  + " )\n"
                  + " AND I.consumer_id = '$2' AND I.payment_status != 'SUCCEEDED' ";

  public static final String LIST_FAILED_PAYMENTS_4_CONSUMER = LIST_PENDING_PAYMENTS_4_CONSUMER.replace("PENDING","FAILED");
  public static final String LIST_FAILED_PAYMENTS_4_CONSUMER_WITH_GIVEN_PRODUCT = LIST_PENDING_PAYMENTS_4_CONSUMER_WITH_GIVEN_PRODUCT
          .replace("PENDING", "FAILED");
  public static final String LIST_FAILED_PAYMENTS_4_CONSUMER_WITH_GIVEN_RESOURCE = LIST_PENDING_PAYMENTS_4_CONSUMER_WITH_GIVEN_RESOURCE
          .replace("PENDING", "FAILED");



  public static final String INSERT_INVOICE_QUERY =
          "insert into $0 (_id, consumer_id, order_id, product_variant_id, payment_status, payment_time, expiry) "
                  + "values ('$1', '$2', '$3', '$4', '$5', '$6', (select validity from $p where _id = '$4'))";

  public static final String TRANSFER_ID = "id";
  public static final String SOURCE = "source";
  public static final String AMOUNT = "amount";
  public static final String CURRENCY = "currency";
  public static final String INR = "INR";
  public static final String ACCOUNT_ID = "account_id";
  public static final String RECIPIENT = "recipient";
  public static final String NOTES = "notes";
  public static final String TRANSFERS = "transfers";
  public static final String TABLES = "tables";

  public static final String FETCH_ACTIVE_PRODUCT_VARIANTS =
      "SELECT _id AS \"productVariantId\","
          + " product_variant_name AS \"productVariantName\", \"product_id\" AS \"productId\",\n"
          + "provider_id AS \"providerId\", resource_info AS \"resources\", \n"
          + "price AS \"price\", validity AS \"expiryInMonths\"\n"
          + " , modified_at AS \"updatedAt\" "
          + " , created_at AS \"createdAt\" "
          + "FROM product_variant\n"
          + "WHERE product_id = '$1'\n"
          + "AND status = 'ACTIVE'  "
          + " ORDER BY modified_at DESC";
  public static final String FETCH_ACTIVE_PRODUCT_VARIANTS_4_PROVIDER =
          "SELECT _id AS \"productVariantId\","
                  + " product_variant_name AS \"productVariantName\", \"product_id\" AS \"productId\",\n"
                  + "provider_id AS \"providerId\", resource_info AS \"resources\", \n"
                  + "price AS \"price\", validity AS \"expiryInMonths\"\n"
                  + " , modified_at AS \"updatedAt\" "
                  + " , created_at AS \"createdAt\" "
                  + "FROM product_variant\n"
                  + "WHERE product_id = '$1'\n"
                  + "AND status = 'ACTIVE'  ";
}
