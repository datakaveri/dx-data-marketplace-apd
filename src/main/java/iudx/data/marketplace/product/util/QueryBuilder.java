package iudx.data.marketplace.product.util;

import static iudx.data.marketplace.auditing.util.Constants.*;
import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.consumer.util.Constants.*;
import static iudx.data.marketplace.product.util.Constants.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryBuilder {

  public static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);
  private String productTable, resourceTable, productResourceRelationTable, productVariantTable;
  private Supplier<String> supplier;

  public QueryBuilder(JsonArray tables) {
    this.productTable = tables.getString(0);
    this.resourceTable = tables.getString(1);
    this.productResourceRelationTable = tables.getString(2);
    this.productVariantTable = tables.getString(3);
    this.supplier = () -> UUID.randomUUID().toString();
  }

  public List<String> buildCreateProductQueries(JsonObject request, JsonArray resourceDetails) {
    String productID = request.getString(PRODUCT_ID);
    String providerID = request.getString(PROVIDER_ID);
    String providerName = request.getString(PROVIDER_NAME);
    boolean isProviderNameContainingApostrophe = providerName.contains("'");
    if(isProviderNameContainingApostrophe)
    {
      providerName = providerName.replace("'", "''");
    }
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
    String finalProviderName = providerName;
    resourceDetails.forEach(
            resource -> {
              queries.add(
                      new StringBuilder(
                              INSERT_RESOURCE_QUERY
                                      .replace("$0", resourceTable)
                                      .replace("$1", ((JsonObject) resource).getString(RESOURCE_ID))
                                      .replace("$2", ((JsonObject) resource).getString(RESOURCE_NAME))
                                      .replace("$3", providerID)
                                      .replace("$4", finalProviderName)
                                      .replace("$5", request.getString(RESOURCE_SERVER))
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

    LOGGER.debug("Queries : " + queries);
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
    if (request.containsKey("resourceId")) {
      query.append(" and rt._id=$4");
    }
    query.append(" group by pt.product_id, rt.resource_server");
    query.append(" order by pt.modified_at DESC");

    return query.toString();
  }

  public String buildProductDetailsQuery(String productID, String providerId) {
    StringBuilder query =
            new StringBuilder(
                    SELECT_PRODUCT_DETAILS
                            .replace("$0", productTable)
                            .replace("$9", productResourceRelationTable)
                            .replace("$8", resourceTable)
                            .replace("$1", productID)
                            .replace("$2", providerId));

    return query.toString();
  }

  public String buildCreateProductVariantQuery(JsonObject request) {

    JsonArray resources = request.getJsonArray(RESOURCES_ARRAY);


    // UUID for each product variant.
    String pvID = supplier.get();

    LOGGER.debug("request is : " + request.encodePrettily());
    StringBuilder query =
            new StringBuilder(
                    INSERT_PV_QUERY
                            .replace("$0", productVariantTable)
                            .replace("$1", pvID)
                            .replace("$2", request.getString("provider_id"))
                            .replace("$3", request.getString(PRODUCT_ID))
                            .replace("$4", request.getString(Constants.PRODUCT_VARIANT_NAME))
                            .replace("$5", resources.encode())
                            .replace("$6", request.getDouble(PRICE).toString())
                            .replace("$7", request.getInteger(DURATION).toString())
                            .replace("$s", Status.ACTIVE.toString()));

    LOGGER.debug(query);
    return query.toString();
  }

  public String updateProductVariantStatusQuery(String productID, String variant) {
    StringBuilder query =
            new StringBuilder(
                    UPDATE_PV_STATUS
                            .replace("$0", productVariantTable)
                            .replace("$1", productID)
                            .replace("$2", variant)
                            .replace("$3", Status.ACTIVE.toString())
                            .replace("$4", Status.INACTIVE.toString()));

    return query.toString();
  }

  public String updateProductVariantStatusQuery(String productVariantId) {
    StringBuilder query =
        new StringBuilder(
            UPDATE_PV_STATUS_QUERY
                .replace("$0", productVariantTable)
                .replace("$1", productVariantId)
                .replace("$3", Status.ACTIVE.toString())
                .replace("$4", Status.INACTIVE.toString()));

    LOGGER.debug("Query : " + query);
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
    LOGGER.debug("select product variant query : {}", query);
    return query.toString();
  }

  public String listProductVariants(JsonObject request) {

    StringBuilder query = new StringBuilder(
            FETCH_ACTIVE_PRODUCT_VARIANTS);
    if(request.containsKey(Constants.PRODUCT_VARIANT_NAME)) {
      query.append(" AND P.\"productVariantName\"=$3");
    }
    query.append(" ORDER BY P.\"updatedAt\" DESC");
    LOGGER.debug(query);
    return query.toString();
  }

  public JsonObject buildMessageForRmq(JsonObject request) {
    String primaryKey = supplier.get().replace("-", "");
    request.put(PRIMARY_KEY, primaryKey);
    request.put(ORIGIN, ORIGIN_SERVER);

    LOGGER.debug("Info: Request " + request);
    return request;
  }

  public String listPurchaseForProviderDuringPendingStatus(String providerId, String resourceId, String productId, String rsUrl)
  {
    boolean isProductIdPresent = StringUtils.isNotBlank(productId);
    boolean isResourceIdPresent = StringUtils.isNotBlank(resourceId);

    StringBuilder query =
            new StringBuilder(LIST_PENDING_PAYMENTS_4_PROVIDER.replace("$1", providerId).replace("$2", rsUrl));

    if(isProductIdPresent && !isResourceIdPresent)
    {
      query = new StringBuilder(LIST_PENDING_PAYMENTS_4_PROVIDER_WITH_GIVEN_PRODUCT
              .replace("$1", productId)
              .replace("$2", providerId)
              .replace("$3", rsUrl));
    }

    if(isResourceIdPresent && !isProductIdPresent)
    {
      query = new StringBuilder(LIST_PENDING_PAYMENTS_WITH_GIVEN_RESOURCE
              .replace("$1", resourceId)
              .replace("$2", providerId)
              .replace("$3", rsUrl));
    }
    query.append(" \n ORDER BY I.modified_at DESC ");
    LOGGER.debug("Query :" + query);
    return query.toString();
  }

  public String listPurchaseForProviderDuringFailedPayment(String providerId, String resourceId, String productId, String rsUrl)
  {
    boolean isProductIdPresent = StringUtils.isNotBlank(productId);
    boolean isResourceIdPresent = StringUtils.isNotBlank(resourceId);

    StringBuilder query =
            new StringBuilder(LIST_FAILED_PAYMENTS_4_PROVIDER.replace("$1", providerId).replace("$2",rsUrl));

    if(isProductIdPresent && !isResourceIdPresent)
    {
      query = new StringBuilder(LIST_FAILED_PAYMENTS_4_PROVIDER_WITH_GIVEN_PRODUCT
              .replace("$1", productId)
              .replace("$2", providerId)
              .replace("$3", rsUrl));
    }

    if(isResourceIdPresent && !isProductIdPresent)
    {
      query = new StringBuilder(LIST_FAILED_PAYMENTS_WITH_GIVEN_RESOURCE
              .replace("$1", resourceId)
              .replace("$2", providerId)
              .replace("$3", rsUrl));
    }
    query.append(" \n ORDER BY I.modified_at DESC ");
    LOGGER.debug("Query :" + query);
    return query.toString();
  }

  public String listSuccessfulPurchaseForProvider(String providerId, String resourceId, String productId,
  String rsUrl)
  {
    boolean isProductIdPresent = StringUtils.isNotBlank(productId);
    boolean isResourceIdPresent = StringUtils.isNotBlank(resourceId);

    StringBuilder query =
            new StringBuilder(LIST_SUCCESSFUL_PAYMENTS_4_PROVIDER.replace("$1", providerId).replace("$2", rsUrl));

    if(isProductIdPresent && !isResourceIdPresent)
    {
      query = new StringBuilder(LIST_SUCCESSFUL_PAYMENTS_4_PROVIDER_WITH_GIVEN_PRODUCT
              .replace("$1", productId)
              .replace("$2", providerId)
              .replace("$3", rsUrl));
    }

    if(isResourceIdPresent && !isProductIdPresent)
    {
      query = new StringBuilder(LIST_SUCCESSFUL_PAYMENTS_4_PROVIDER_WITH_GIVEN_RESOURCE
              .replace("$1", resourceId)
              .replace("$2", providerId)
              .replace("$3", rsUrl));
    }


    query.append(" \n ORDER BY I.modified_at DESC ");
    LOGGER.debug("Query :" + query);
    return query.toString();
  }
  public String listPurchaseForConsumerDuringPendingPayment(String consumerId, String resourceId, String productId, String orderId, String rsUrl)
  {
    boolean isProductIdPresent = StringUtils.isNotBlank(productId);
    boolean isResourceIdPresent = StringUtils.isNotBlank(resourceId);
    boolean isOrderIdPresent = StringUtils.isNotBlank(orderId);
    StringBuilder query =
            new StringBuilder(LIST_PENDING_PAYMENTS_4_CONSUMER.replace("$1", consumerId).replace("$2",rsUrl));

    if(isProductIdPresent && !isResourceIdPresent)
    {
      query = new StringBuilder(LIST_PENDING_PAYMENTS_4_CONSUMER_WITH_GIVEN_PRODUCT
              .replace("$1", productId)
              .replace("$2", consumerId)
              .replace("$3", rsUrl));
    }

    if(isResourceIdPresent && !isProductIdPresent)
    {
      query = new StringBuilder(LIST_PENDING_PAYMENTS_4_CONSUMER_WITH_GIVEN_RESOURCE
              .replace("$1", resourceId)
              .replace("$2", consumerId)
              .replace("$3", rsUrl));
    }
    if(isOrderIdPresent)
    {
      query = new StringBuilder(LIST_PENDING_PAYMENTS_4_CONSUMER_WITH_GIVEN_ORDER
              .replace("$1", orderId)
              .replace("$2", consumerId)
              .replace("$3", rsUrl));
    }

    query.append(" \n ORDER BY I.modified_at DESC ");
    LOGGER.debug("Query :" + query);
    return query.toString();
  }


  public String listPurchaseForConsumerDuringFailurePayment(String consumerId, String resourceId, String productId, String orderId, String rsUrl)
  {
    boolean isProductIdPresent = StringUtils.isNotBlank(productId);
    boolean isResourceIdPresent = StringUtils.isNotBlank(resourceId);
    boolean isOrderIdPresent = StringUtils.isNotBlank(orderId);

    StringBuilder query =
            new StringBuilder(LIST_FAILED_PAYMENTS_4_CONSUMER.replace("$1", consumerId).replace("$2", rsUrl));

    if(isProductIdPresent && !isResourceIdPresent)
    {
      query = new StringBuilder(LIST_FAILED_PAYMENTS_4_CONSUMER_WITH_GIVEN_PRODUCT
              .replace("$1", productId)
              .replace("$2", consumerId)
              .replace("$3", rsUrl));
    }

    if(isResourceIdPresent && !isProductIdPresent)
    {
      query = new StringBuilder(LIST_FAILED_PAYMENTS_4_CONSUMER_WITH_GIVEN_RESOURCE
              .replace("$1", resourceId)
              .replace("$2", consumerId)
              .replace("$3", rsUrl));
    }
    if(isOrderIdPresent)
    {
      query = new StringBuilder(LIST_FAILED_PAYMENTS_4_CONSUMER_WITH_GIVEN_ORDER
              .replace("$1", orderId)
              .replace("$2", consumerId)
              .replace("$3", rsUrl));
    }
    query.append(" \n ORDER BY I.modified_at DESC ");
    LOGGER.debug("Query :" + query);
    return query.toString();
  }

  public String listPurchaseForConsumerDuringSuccessfulPayment(String consumerId, String resourceId, String productId, String orderId, String rsUrl)
  {
    boolean isProductIdPresent = StringUtils.isNotBlank(productId);
    boolean isResourceIdPresent = StringUtils.isNotBlank(resourceId);
    boolean isOrderIdPresent = StringUtils.isNotBlank(orderId);

    StringBuilder query =
            new StringBuilder(LIST_SUCCESSFUL_PAYMENTS_PAYMENTS_4_CONSUMER.replace("$1", consumerId).replace("$2", rsUrl));

    if(isProductIdPresent && !isResourceIdPresent)
    {
      query = new StringBuilder(LIST_SUCCESSFUL_PAYMENTS_4_CONSUMER_WITH_GIVEN_PRODUCT
              .replace("$1", productId)
              .replace("$2", consumerId)
              .replace("$3", rsUrl));
    }

    if(isResourceIdPresent && !isProductIdPresent)
    {
      query = new StringBuilder(LIST_SUCCESSFUL_PAYMENTS_4_CONSUMER_WITH_GIVEN_RESOURCE
              .replace("$1", resourceId)
              .replace("$2", consumerId)
              .replace("$3", rsUrl));
    }
    if(isOrderIdPresent)
    {
      query = new StringBuilder(LIST_SUCCESSFUL_PAYMENTS_4_CONSUMER_WITH_GIVEN_ORDER
              .replace("$1", orderId)
              .replace("$2", consumerId)
              .replace("$3", rsUrl));
    }
    query.append(" \n ORDER BY I.modified_at DESC ");
    LOGGER.debug("Query :" + query);
    return query.toString();
  }





}