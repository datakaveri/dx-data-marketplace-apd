package iudx.data.marketplace.product.util;

import static iudx.data.marketplace.auditing.util.Constants.*;
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
  private String productTable, resourceTable, productResourceRelationTable, productVariantTable;

  public QueryBuilder(JsonArray tables) {
    this.productTable = tables.getString(0);
    this.resourceTable = tables.getString(1);
    this.productResourceRelationTable = tables.getString(2);
    this.productVariantTable = tables.getString(3);
  }

  public QueryBuilder() {}

  public List<String> buildCreateProductQueries(JsonObject request, JsonArray resourceDetails) {
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

    // Resource Table entry
    resourceDetails.forEach(
        resource -> {
          queries.add(
              new StringBuilder(
                      INSERT_RESOURCE_QUERY
                          .replace("$0", resourceTable)
                          .replace("$1", ((JsonObject) resource).getString(RESOURCE_ID))
                          .replace("$2", ((JsonObject) resource).getString(RESOURCE_NAME))
                          .replace("$3", providerID)
                          .replace("$4", providerName)
                          .replace("$5", ((JsonObject) resource).getString(RESOURCE_SERVER))
                          .replace("$6", ((JsonObject) resource).getString("accessPolicy")))
                  .toString());

          // Product-Resource relationship table entry
          queries.add(
              new StringBuilder(
                      INSERT_P_R_REL_QUERY
                          .replace("$0", productResourceRelationTable)
                          .replace("$1", productID)
                          .replace("$2", ((JsonObject) resource).getString(RESOURCE_ID)))
                  .toString());
        });

    return queries;
  }

  public String buildListProductsQuery(JsonObject request) {

    StringBuilder query;
    query =
        new StringBuilder(
            LIST_PRODUCT_FOR_RESOURCE
                .replace("$0", productTable)
                .replace("$9", productResourceRelationTable)
                .replace("$8", resourceTable));
    if (request.containsKey(RESOURCE_ID)) {
      query.append(" and rt._id=$3");
    }
    query.append(" group by pt.product_id");
    
    return query.toString();
  }

  public String buildProductDetailsQuery(String productID) {
    StringBuilder query =
        new StringBuilder(
            SELECT_PRODUCT_DETAILS
                .replace("$0", productTable)
                .replace("$9", productResourceRelationTable)
                .replace("$8", resourceTable)
                .replace("$1", productID));

    return query.toString();
  }

  public String buildCreateProductVariantQuery(JsonObject request) {

    JsonArray resources = request.getJsonArray(RESOURCES_ARRAY);
    String resourceIDs =
        resources.stream()
            .map(JsonObject.class::cast)
            .map(d -> d.getString(ID))
            .collect(Collectors.joining("','", "'", "'"));
    String resourceNames =
        resources.stream()
            .map(JsonObject.class::cast)
            .map(d -> d.getString(NAME))
            .collect(Collectors.joining("','", "'", "'"));
    String resourceCaps =
        resources.stream()
            .map(JsonObject.class::cast)
            .map(d -> d.getJsonArray(CAPABILITIES).toString().replace("\"", "'"))
            .collect(Collectors.joining(","));

    // UUID for each product variant.
    String pvID = UUID.randomUUID().toString();

    LOGGER.debug("resourceIds : {} ", resourceIDs);

    StringBuilder query =
        new StringBuilder(
            INSERT_PV_QUERY
                .replace("$0", productVariantTable)
                .replace("$1", pvID)
                .replace("$2", request.getString("providerid"))
                .replace("$3", request.getString(ID))
                .replace("$4", request.getString(VARIANT))
                .replace("$5", resourceNames)
                .replace("$6", resourceIDs)
                .replace("$7", resourceCaps)
                .replace("$8", request.getDouble(PRICE).toString())
                .replace("$9", request.getInteger(DURATION).toString())
                .replace("$s", Status.ACTIVE.toString()));

    LOGGER.debug(query);
    return query.toString();
  }

  public String updateProductVariantStatusQuery(String productID, String variant) {
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
                .replace("$2", variantName)
                .replace("$3", Status.ACTIVE.toString()));
    return query.toString();
  }

  public String listProductVariants(JsonObject request) {

    StringBuilder query = new StringBuilder(
          LIST_PVS_QUERY.replace("$0", productVariantTable));
    if(request.containsKey(VARIANT)) {
      query.append(" and product_variant_name=$3");
    }

    LOGGER.debug(query);
    return query.toString();
  }

  public JsonObject buildMessageForRmq(JsonObject request) {
    String primaryKey = UUID.randomUUID().toString().replace("-", "");
    request.put(PRIMARY_KEY, primaryKey);
    request.put(ORIGIN, ORIGIN_SERVER);

    LOGGER.debug("Info: Request " + request);
    return request;
  }
}
