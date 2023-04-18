package iudx.data.marketplace.consumer.util;

public class Constants {

  public static final String LIST_DATASETS_QUERY = "select datasetID, datasetName, accessPolicy, providerName, totalResources from $0";
  public static final String LIST_PROVIDERS_QUERY = "select providerID, providerName, count(datasetID) as totalDatasets from $0 group by providerID, providerName";

  public static final String TABLES = "tables";

}
