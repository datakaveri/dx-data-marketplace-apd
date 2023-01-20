package iudx.data.marketplace.product.util;

public class Constants {
  public static final String PRODUCT_ID = "id";
  public static final String IID = "iid";
  public static final String URN_PREFIX = "urn:datakaveri.org:";
  public static final String DATASETS = "datasets";
  public static final String TOTAL_RESOURCES = "totalResources";


  // database related constants
  public static final String PRODUCT_TABLE_NAME = "productTableName";
  public static final String TABLES = "tables";
  public static final String SELECT_PRODUCT_QUERY = "select count(*) from $0 where providerID='$1' and productID='$2'";

  public static final String INSERT_PRODUCT_QUERY = "insert into $0 (productID, providerID, providerName, status) values ('$1', '$2', '$3', '$4')";
  public static final String INSERT_DATASET_QUERY = "insert into $0 (datasetID, datasetName, accessPolicy, providerID, providerName, totalResources) values ('$1', '$2', '$3', '$4', '$5', $6)";

  public static final String INSERT_P_D_REL_QUERY = "insert into $0 (productID, datasetID) values ('$1', '$2')";

}
