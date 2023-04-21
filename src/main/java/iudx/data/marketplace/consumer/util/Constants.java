package iudx.data.marketplace.consumer.util;

public class Constants {

  public static final String LIST_DATASETS_QUERY =
      "select datasetID, datasetName, accessPolicy, providerName, totalResources from $0";
  public static final String LIST_PROVIDERS_QUERY =
      "select providerID, providerName, count(datasetID) as totalDatasets from $0 group by providerID, providerName";

  public static final String LIST_ALL_PRODUCTS_QUERY =
      "select productID, providerName from $0 where status=$1";
  public static final String LIST_PRODUCTS_FOR_PROVIDER =
      "select productID, providerName from $0 where status=$1 and providerID=$2";
  public static final String LIST_PRODUCTS_FOR_DATASET =
      "select pt.productId, pt.providerName from $0 as pt inner join $9 as dpt on pt.productId=dpt.productId inner join $8 as dt on dpt.datasetId=dt.datasetId where pt.status=$1 and dt.datasetId=$2";

  public static final String TABLES = "tables";
}
