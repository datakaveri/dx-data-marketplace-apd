package iudx.data.marketplace.product.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.product.util.Constants.*;

public class QueryBuilder {

  private final String productTable, datasetTable, productDatasetRelationTable;

  public QueryBuilder(JsonArray tables) {
    this.productTable = tables.getString(0);
    this.datasetTable = tables.getString(1);
    this.productDatasetRelationTable = tables.getString(2);
  }

  public List<String> buildCreateProductQueries(
      JsonObject request, JsonArray datasetDetails) {
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
}
