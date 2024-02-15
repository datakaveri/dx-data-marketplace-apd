package iudx.data.marketplace.product.util;

public class Constants {
  public static final String PRODUCT_ID = "id";
  public static final String IID = "iid";
  public static final String URN_PREFIX = "urn:datakaveri.org:";
  public static final String resourceNames = "resource_name";
  public static final String RESOURCES_ARRAY = "resources";
  public static final String RESOURCE_IDS = "resourceIds";
  public static final String RESOURCE_CAPABILITIES = "resource_capabilities";
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
      "select pt.product_id as productId, pt.provider_name, "
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

  public static final String INSERT_PV_QUERY  = "insert into $0 (_id, provider_id, product_id, product_variant_name, resource_name, resource_ids_and_capabilities, price, validity, status) values ('$1', '$2', '$3', '$4', ARRAY[$5],$6::JSON, ARRAY[$7], $8, '$s')";
  public static final String UPDATE_PV_STATUS_QUERY = "update $0 set status='$4' where product_id='$1' and product_variant_name='$2' and status='$3'";
  public static final String SELECT_PV_QUERY = "select count(*) from $0 where product_id='$1' and product_variant_name='$2' and status='$3'";

  public static final String LIST_PVS_QUERY = "select product_variant_name, product_id, resource_name from $0 where product_id=$1 and status=$2";
}
