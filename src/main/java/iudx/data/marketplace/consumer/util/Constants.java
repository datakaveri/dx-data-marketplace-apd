package iudx.data.marketplace.consumer.util;

public class Constants {

  public static final String LIST_RESOURCES_QUERY =
      "select _id, resource_name, accessPolicy, providerName from $0";
  public static final String LIST_PROVIDERS_QUERY =
      "select distinct provider_id, providerName, resource_server from $0";

  public static final String LIST_ALL_PRODUCTS_QUERY =
      "select product_id, providerName from $0 where status=$1";
  public static final String LIST_PRODUCTS_FOR_PROVIDER =
      "select product_id, providerName from $0 where status=$1 and providerID=$2";
  public static final String LIST_PRODUCTS_FOR_RESOURCE =
      "select pt.product_id, pt.providerName from $0 as pt inner join $9 as dpt on pt.product_id=dpt.product_id inner join $8 as dt on dpt.resource_id=dt._id where pt.status=$1 and dt._id=$2";

  public static final String TABLES = "tables";
}
