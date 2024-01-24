package iudx.data.marketplace.consumer.util;

public class Constants {

  public static final String LIST_RESOURCES_QUERY =
      "select _id, resource_name, accessPolicy, providerName from $0";
  public static final String LIST_PROVIDERS_QUERY =
      "select distinct provider_id, providerName, resource_server from $0";

  public static final String LIST_PRODUCTS =
      "select pt.product_id as productId, pt.providerName, "
          + "array_agg(json_build_object('id', rt._id, 'name', rt.resource_name)) as resources "
          + "from $0 as pt "
          + "inner join $9 as dpt on pt.product_id = dpt.product_id "
          + "inner join $8 as rt on dpt.resource_id = rt._id "
          + "where  pt.status=$1";

  public static final String TABLES = "tables";
}
