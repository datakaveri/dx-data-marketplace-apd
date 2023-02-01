package iudx.data.marketplace.product.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.product.util.Constants.*;

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
                .map(d -> d.getString("id"))
                .collect(Collectors.joining("','","'","'"));
    String datasetNames =
             datasets.stream()
                .map(JsonObject.class::cast)
                .map(d -> d.getString("name"))
                .collect(Collectors.joining("','","'","'"));
    String datasetCaps =
             datasets.stream()
                .map(JsonObject.class::cast)
                .map(d -> d.getJsonArray("capabilities").toString().replace("\"","'"))
                .collect(Collectors.joining(","));

    StringBuilder query =
        new StringBuilder(
            INSERT_PV_QUERY
                .replace("$0", productVariantTable)
                .replace("$1", request.getString("providerid"))
                .replace("$2", request.getString("id"))
                .replace("$3", request.getString("variant"))
                .replace("$4", datasetNames)
                .replace("$5", datasetIDs)
                .replace("$6", datasetCaps)
                .replace("$7", request.getDouble(PRICE).toString())
                .replace("$8", request.getInteger("duration").toString()));

    return query.toString();
  }
}
