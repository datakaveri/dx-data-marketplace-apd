package iudx.data.marketplace.consumer.util;

public class Constants {

  public static final String LIST_RESOURCES_QUERY =
      "select resourceID, resourceName, accessPolicy, providerName, totalResources from $0";
  public static final String LIST_PROVIDERS_QUERY =
      "select providerID, providerName, count(resourceID) as totalResources from $0 group by providerID, providerName";

  public static final String LIST_ALL_PRODUCTS_QUERY =
      "select productID, providerName from $0 where status=$1";
  public static final String LIST_PRODUCTS_FOR_PROVIDER =
      "select productID, providerName from $0 where status=$1 and providerID=$2";
  public static final String LIST_PRODUCTS_FOR_RESOURCE =
      "select pt.productId, pt.providerName from $0 as pt inner join $9 as dpt on pt.productId=dpt.productId inner join $8 as dt on dpt.resourceId=dt.resourceId where pt.status=$1 and dt.resourceId=$2";

  public static final String TABLES = "tables";
}
