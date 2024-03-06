package iudx.data.marketplace.product.util;

public class Constants {
  public static final String PRODUCT_ID = "productId";
  public static final String IID = "iid";
  public static final String URN_PREFIX = "urn:datakaveri.org:";
  public static final String resourceNames = "resource_name";
  public static final String RESOURCES_ARRAY = "resources";
  public static final String RESOURCE_IDS = "resourceIds";
  public static final String STATUS = "status";
  public static final String RESULTS = "results";
  public static final String PRICE = "price";
  public static final String DURATION = "duration";
  public static final String ID = "id";
  public static final String VARIANT = "variant";
  public static final String NAME = "name";
  public static final String CAPABILITIES = "capabilities";
  public static final String TYPE = "type";

  // database related constants
  public static final String PRODUCT_TABLE_NAME = "productTableName";
  public static final String TABLES = "tables";
  public static final String SELECT_PRODUCT_QUERY =
          "select count(*) from $0 where provider_id='$1' and product_id='$2'";

  public static final String INSERT_PRODUCT_QUERY =
          "insert into $0 (product_id, provider_id, provider_name, status) values ('$1', '$2', '$3', '$4')";
  public static final String INSERT_RESOURCE_QUERY =
          "insert into $0 (_id, resource_name, provider_id, provider_name, resource_server, accessPolicy) values ('$1', '$2', '$3', '$4', '$5', '$6') on conflict (_id) do nothing";

  public static final String INSERT_P_R_REL_QUERY =
          "insert into $0 (product_id, resource_id) values ('$1', '$2')";

  public static final String DELETE_PRODUCT_QUERY = "update $0 set status=$1 where product_id=$2";
  public static final String LIST_PRODUCT_FOR_RESOURCE =
      "select pt.product_id AS \"productId\", pt.provider_name AS \"providerName\", "
          + " pt.modified_at AS \"updatedAt\" ,  pt.created_at AS \"createdAt\" ,"
          + "array_agg(json_build_object('id', rt._id, 'name', rt.resource_name)) as resources "
          + "from $0 as pt "
          + "inner join $9 as dpt on pt.product_id = dpt.product_id "
          + "inner join $8 as rt on dpt.resource_id = rt._id "
          + "where  pt.status=$1 and pt.provider_id=$2";

  public static final String LIST_ALL_PRODUCTS =
          "select product_id, provider_name from $0 where status=$1 and provider_id=$2";

  public static final String SELECT_PRODUCT_DETAILS =
          "select pt.provider_id, pt.product_id, "
                  + "array_agg(json_build_object('id', rt._id, 'name', rt.resource_name)) as resources "
                  + "from $0 as pt "
                  + "inner join $9 as prt on pt.product_id = prt.product_id "
                  + " inner join $8 as rt on prt.resource_id = rt._id "
                  + "where pt.product_id = '$1' "
                  + "group by pt.product_id";

  public static final String INSERT_PV_QUERY  = "insert into $0 (_id, provider_id, product_id, product_variant_name, resource_name, resource_ids_and_capabilities, price, validity, status) values ('$1', '$2', '$3', '$4', ARRAY[$5],'$6'::JSON, '$7', $8, '$s')";
  public static final String UPDATE_PV_STATUS_QUERY = "update $0 set status='$4' where product_id='$1' and product_variant_name='$2' and status='$3'";
  public static final String SELECT_PV_QUERY = "select count(*) from $0 where product_id='$1' and product_variant_name='$2' and status='$3'";

  //TODO: Should we only fetch all the active product variants only? Are the users allowed to input active or inactive as query parameter?
  public static final String LIST_PVS_QUERY = "select product_variant_name, product_id, resource_name from $0 where product_id=$1 and status=$2";
  public static final String LIST_FAILED_OR_PENDING_PAYMENTS =  "SELECT DISTINCT I._id AS \"invoiceId\", I.consumer_id AS \"consumerId\",\n"
          + "U.email_id AS \"consumerEmailId\", U.first_name AS \"consumerFirstName\",\n"
          + "U.last_name AS \"consumerLastName\", \n"
          + "I.order_id AS \"orderId\", I.product_variant_id AS \"productVariantId\",\n"
          + "P.product_id AS \"productId\", P.resource_name AS \"resourceName\",\n"
          + "P.product_variant_name as \"productVariantName\",\n"
          + "P.price, P.resource_ids_and_capabilities AS \"resourcesAndCapabilities\",\n"
          + "I.payment_status AS \"paymentStatus\", I.payment_time AS \"paymentTime\",\n"
          + "I.expiry AS \"expiryInMonths\"\n"
          + "FROM invoice I\n"
          + "INNER JOIN product_variant P\n"
          + "ON I.product_variant_id = P._id\n"
          + "INNER JOIN user_table U\n"
          + "ON U._id = I.consumer_id\n";
  public static final String LIST_SUCCESSFUL_PURCHASE =
          " SELECT DISTINCT I._id AS \"invoiceId\", I.consumer_id AS \"consumerId\",\n" +
           " U.email_id AS \"consumerEmailId\", U.first_name AS \"consumerFirstName\",\n" +
           " U.last_name AS \"consumerLastName\",  \n" +
           " I.order_id AS \"orderId\", I.product_variant_id AS \"productVariantId\",\n" +
           " P.product_id AS \"productId\", P.resource_name AS \"resourceName\",\n" +
           " P.product_variant_name as \"productVariantName\",\n" +
           " P.price, P.resource_ids_and_capabilities AS \"resourcesAndCapabilities\",\n" +
           " I.payment_status AS \"paymentStatus\", I.payment_time AS \"paymentTime\",\n" +
           " I.expiry AS \"expiryInMonths\", policy.expiry_at AS \"expiryAt\"\n" +
           " FROM policy \n" +
           " INNER JOIN invoice I \n" +
           " ON policy.invoice_id = I._id\n" +
           " INNER JOIN product_variant P\n" +
           " ON I.product_variant_id = P._id\n" +
           " INNER JOIN user_table U\n" +
           " ON U._id = I.consumer_id ";
  public static final String LIST_SUCCESSFUL_PAYMENTS_4_PROVIDER =
          LIST_SUCCESSFUL_PURCHASE + "WHERE P.provider_id = '$1' ";
  public static final String LIST_SUCCESSFUL_PAYMENTS_4_PROVIDER_WITH_GIVEN_PRODUCT =
          LIST_SUCCESSFUL_PURCHASE
                  + "WHERE\n"
                  + "P.product_id = '$1'\n"
                  + "AND P.provider_id = '$2' ";
  public static final String LIST_SUCCESSFUL_PAYMENTS_4_PROVIDER_WITH_GIVEN_RESOURCE =
          LIST_SUCCESSFUL_PURCHASE
                  + "WHERE\n"
                  + "P.product_id IN (SELECT product_id FROM product_resource_relation WHERE resource_id = '$1'\n"
                  + ")\n"
                  + "\n"
                  + "AND P.provider_id = '$2' ;";
  public static final String LIST_FAILED_OR_PENDING_PAYMENTS_4_PROVIDER =
          LIST_FAILED_OR_PENDING_PAYMENTS + "WHERE P.provider_id = '$1'  AND I.payment_status != 'SUCCEEDED' ";
  public static final String LIST_FAILED_OR_PENDING_PAYMENTS_4_PROVIDER_WITH_GIVEN_PRODUCT =
          LIST_FAILED_OR_PENDING_PAYMENTS
                  + "WHERE\n"
                  + "P.product_id = '$1'\n"
                  + "AND P.provider_id = '$2'  AND I.payment_status != 'SUCCEEDED' ";
  public static final String LIST_FAILED_OR_PENDING_PAYMENTS_WITH_GIVEN_RESOURCE =
          LIST_FAILED_OR_PENDING_PAYMENTS
                  + "WHERE\n"
                  + "P.product_id IN (SELECT product_id FROM product_resource_relation WHERE resource_id = '$1'\n"
                  + ")\n"
                  + "\n"
                  + "AND P.provider_id = '$2'  AND I.payment_status != 'SUCCEEDED' ;";
}