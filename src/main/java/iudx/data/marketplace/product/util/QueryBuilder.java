package iudx.data.marketplace.product.util;

import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.product.util.Constants.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryBuilder {

  public static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);
  private final String productTable, datasetTable, productDatasetRelationTable, productVariantTable;

  public QueryBuilder(JsonArray tables) {
    this.productTable = tables.getString(0);
    this.datasetTable = tables.getString(1);
    this.productDatasetRelationTable = tables.getString(2);
    this.productVariantTable = tables.getString(3);
  }

  public List<String> buildCreateProductQueries(JsonObject request, JsonArray datasetDetails) {
    String productID = request.getString(PRODUCT_ID);
    String providerID = request.getString(PROVIDER_ID);
    String providerName = request.getString(PROVIDER_NAME);
    List<String> queries = new ArrayList<String>();

    // Product Table entry
    queries.add(
        new StringBuilder(
                INSERT_PRODUCT_QUERY
                    .replace("$0", productTable)
                    .replace("$1", productID)
                    .replace("$2", providerID)
                    .replace("$3", providerName)
                    .replace("$4", Status.ACTIVE.toString()))
            .toString());

    // Dataset Table entry
    datasetDetails.forEach(
        dataset -> {
          queries.add(
              new StringBuilder(
                      INSERT_DATASET_QUERY
                          .replace("$0", datasetTable)
                          .replace("$1", ((JsonObject) dataset).getString(DATASET_ID))
                          .replace("$2", ((JsonObject) dataset).getString(DATASET_NAME))
                          .replace("$3", ((JsonObject) dataset).getString("accessPolicy"))
                          .replace("$4", providerID)
                          .replace("$5", providerName)
                          .replace(
                              "$6", ((JsonObject) dataset).getInteger(TOTAL_RESOURCES).toString()))
                  .toString());

          // Product-Dataset relationship table entry
          queries.add(
              new StringBuilder(
                      INSERT_P_D_REL_QUERY
                          .replace("$0", productDatasetRelationTable)
                          .replace("$1", productID)
                          .replace("$2", ((JsonObject) dataset).getString(DATASET_ID)))
                  .toString());
        });

    return queries;
  }

  public String buildListProductsQuery(JsonObject request) {

    StringBuilder query;
    if (request.containsKey(DATASET_ID)) {
      query =
          new StringBuilder(
              LIST_PRODUCT_FOR_DATASET
                  .replace("$0", productTable)
                  .replace("$9", productDatasetRelationTable)
                  .replace("$8", datasetTable));
    } else {
      query = new StringBuilder(LIST_ALL_PRODUCTS.replace("$0", productTable));
    }
    return query.toString();
  }

  public String buildProductDetailsQuery(String productID) {
    StringBuilder query =
        new StringBuilder(
            SELECT_PRODUCT_DETAILS
                .replace("$0", productTable)
                .replace("$9", productDatasetRelationTable)
                .replace("$8", datasetTable)
                .replace("$1", productID));

    return query.toString();
  }

  public String buildCreateProductVariantQuery(JsonObject request) {

    JsonArray datasets = request.getJsonArray(DATASETS);
    String datasetIDs =
        datasets.stream()
            .map(JsonObject.class::cast)
            .map(d -> d.getString(ID))
            .collect(Collectors.joining("','", "'", "'"));
    String datasetNames =
        datasets.stream()
            .map(JsonObject.class::cast)
            .map(d -> d.getString(NAME))
            .collect(Collectors.joining("','", "'", "'"));
    String datasetCaps =
        datasets.stream()
            .map(JsonObject.class::cast)
            .map(d -> d.getJsonArray(CAPABILITIES).toString().replace("\"", "'"))
            .collect(Collectors.joining(","));

    // UUID for each product variant.
    String pvID = UUID.randomUUID().toString();

    StringBuilder query =
        new StringBuilder(
            INSERT_PV_QUERY
                .replace("$0", productVariantTable)
                .replace("$1", pvID)
                .replace("$2", request.getString("providerid"))
                .replace("$3", request.getString(ID))
                .replace("$4", request.getString(VARIANT))
                .replace("$5", datasetNames)
                .replace("$6", datasetIDs)
                .replace("$7", datasetCaps)
                .replace("$8", request.getDouble(PRICE).toString())
                .replace("$9", request.getInteger(DURATION).toString())
                .replace("$s", Status.ACTIVE.toString()));

    return query.toString();
  }

  public String   updateProductVariantStatusQuery(String productID, String variant) {
    StringBuilder query =
        new StringBuilder(
            UPDATE_PV_STATUS_QUERY
                .replace("$0", productVariantTable)
                .replace("$1", productID)
                .replace("$2", variant)
                .replace("$3", Status.ACTIVE.toString())
                .replace("$4", Status.INACTIVE.toString()));

    return query.toString();
  }

  public String selectProductVariant(String productID, String variantName) {
    StringBuilder query =
        new StringBuilder(
            SELECT_PV_QUERY
                .replace("$0", productVariantTable)
                .replace("$1", productID)
                .replace("$2", variantName).replace("$3", Status.ACTIVE.toString()));
    return query.toString();
  }
}
