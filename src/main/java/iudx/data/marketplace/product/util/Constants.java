package iudx.data.marketplace.product.util;

public class Constants {
  public static final String PRODUCT_ID = "id";
  public static final String IID = "iid";
  public static final String URN_PREFIX = "urn:datakaveri.org:";
  public static final String DATASETS = "datasets";
  public static final String DATASET_IDS = "datasetids";
  public static final String DATASET_CAPS = "datasetCapabilities";
  public static final String TOTAL_RESOURCES = "totalResources";
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
      "select count(*) from $0 where providerID='$1' and productID='$2'";

  public static final String INSERT_PRODUCT_QUERY =
      "insert into $0 (productID, providerID, providerName, status) values ('$1', '$2', '$3', '$4')";
  public static final String INSERT_DATASET_QUERY =
      "insert into $0 (datasetID, datasetName, accessPolicy, providerID, providerName, totalResources) values ('$1', '$2', '$3', '$4', '$5', $6)";

  public static final String INSERT_P_D_REL_QUERY =
      "insert into $0 (productID, datasetID) values ('$1', '$2')";

  public static final String DELETE_PRODUCT_QUERY = "update $0 set status=$1 where productID=$2";
  public static final String LIST_PRODUCT_FOR_DATASET =
      "select pt.productId, pt.providerName "
          + "from $0 as pt "
          + "inner join $9 as dpt on pt.productId = dpt.productId "
          + "inner join $8 as dt on dpt.datasetId = dt.datasetId "
          + "where  pt.status=$1 and pt.providerId=$2 and dt.datasetId=$3";

  public static final String LIST_ALL_PRODUCTS =
      "select productID, providerName from $0 where status=$1 and providerID=$2";

  public static final String SELECT_PRODUCT_DETAILS = "select pt.providerID, pt.productID, "
          + "array_agg(json_build_object('id', dt.datasetID, 'name', dt.datasetName)) as datasets "
          + "from $0 as pt "
          + "inner join $9 as pdt on pt.productID = pdt.productID "
          + " inner join $8 as dt on pdt.datasetID = dt.datasetID "
          + "where pt.productID = '$1' "
          + "group by pt.productID";

  public static final String INSERT_PV_QUERY  = "insert into $0 (_id, providerID, productID, productVariantName, datasets, datasetIDs, datasetCapabilities, price, validity, status) values ('$1', '$2', '$3', '$4', ARRAY[$5], ARRAY[$6], ARRAY[$7], $8, $9, '$s')";
  public static final String UPDATE_PV_STATUS_QUERY = "update $0 set status='$4' where productID='$1' and productVariantName='$2' and status='$3'";
  public static final String SELECT_PV_QUERY = "select count(*) from $0 where productID='$1' and productVariantName='$2' and status='$3'";
}
