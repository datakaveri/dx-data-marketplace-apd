package iudx.data.marketplace.consumer;

import static iudx.data.marketplace.apiserver.util.Constants.RESULT;
import static iudx.data.marketplace.apiserver.util.Constants.TITLE;
import static iudx.data.marketplace.common.Constants.PROVIDER_ID;
import static iudx.data.marketplace.common.Constants.RESOURCE_ID;
import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.consumer.util.Constants.*;
import static iudx.data.marketplace.consumer.util.Constants.TABLES;
import static iudx.data.marketplace.product.util.Constants.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.common.Util;
import iudx.data.marketplace.consumer.util.PaymentStatus;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.product.util.Constants;
import iudx.data.marketplace.product.util.QueryBuilder;
import iudx.data.marketplace.product.util.Status;
import iudx.data.marketplace.product.variant.ProductVariantService;
import iudx.data.marketplace.razorpay.RazorPayService;
import org.apache.commons.lang.StringUtils;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsumerServiceImpl implements ConsumerService {
    public static final Logger LOGGER = LogManager.getLogger(ConsumerServiceImpl.class);
    private final PostgresService pgService;
    private final RazorPayService razorPayService;
    private final JsonObject config;
    private final QueryBuilder queryBuilder;
    private final Util util;

    Supplier<String> uuidSupplier = () -> UUID.randomUUID().toString();

    public ConsumerServiceImpl(
            JsonObject config, PostgresService postgresService, RazorPayService razorPayService, Util util) {
        this.config = config;
        this.pgService = postgresService;
        this.razorPayService = razorPayService;
        this.queryBuilder = new QueryBuilder(config.getJsonArray(Constants.TABLES));
        this.util = util;
    }

    @Override
    public ConsumerService listResources(
            JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

        String resourceTable = config.getJsonArray(TABLES).getString(1);
        JsonObject params = new JsonObject();
        StringBuilder query = new StringBuilder(LIST_RESOURCES_QUERY.replace("$0", resourceTable));
        if (request.containsKey(RESOURCE_ID)) {
            String resourceID = request.getString(RESOURCE_ID);
            params.put(RESOURCE_ID, resourceID);
            query.append(" where ").append("_id").append('=').append("$1");
        } else if (request.containsKey(PROVIDER_ID)) {
            String providerID = request.getString(PROVIDER_ID);
            params.put(PROVIDER_ID, providerID);
            query.append(" where ").append("provider_id").append('=').append("$1");
        }

        pgService.executePreparedQuery(
                query.toString(),
                params,
                pgHandler -> {
                    if (pgHandler.succeeded()) {
                        handler.handle(Future.succeededFuture(pgHandler.result()));
                    } else {
                        LOGGER.error("get resources failed");
                        handler.handle(Future.failedFuture(pgHandler.cause()));
                    }
                });
        return this;
    }

    @Override
    public ConsumerService listProviders(
            JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

        String resourceTable = config.getJsonArray(TABLES).getString(1);
        JsonObject params = new JsonObject();
        StringBuilder query = new StringBuilder(LIST_PROVIDERS_QUERY.replace("$0", resourceTable));

        if (request.containsKey(PROVIDER_ID)) {
            String providerID = request.getString(PROVIDER_ID);
            params.put(PROVIDER_ID, providerID);
            query.append(" where ").append("provider_id").append('=').append("$1");
        }

        pgService.executePreparedQuery(
                query.toString(),
                params,
                pgHandler -> {
                    if (pgHandler.succeeded()) {
                        handler.handle(Future.succeededFuture(pgHandler.result()));
                    } else {
                        LOGGER.error("get providers failed");
                        handler.handle(Future.failedFuture(pgHandler.cause()));
                    }
                });
        return this;
    }


    @Override
    public ConsumerService listProducts(
            JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        String productTable = config.getJsonArray(TABLES).getString(0);
        String resourceTable = config.getJsonArray(TABLES).getString(1);
        String productResourceRelationTable = config.getJsonArray(TABLES).getString(2);

        JsonObject params = new JsonObject();
        StringBuilder query =
                new StringBuilder(
                        LIST_PRODUCTS
                                .replace("$0", productTable)
                                .replace("$9", productResourceRelationTable)
                                .replace("$8", resourceTable));

        if (request.containsKey(RESOURCE_ID)) {
            String resourceID = request.getString(RESOURCE_ID);
            params.put(STATUS, Status.ACTIVE.toString()).put(RESOURCE_ID, resourceID);
            query.append(" and rt._id=$2");
        } else if (request.containsKey(PROVIDER_ID)) {
            String providerID = request.getString(PROVIDER_ID);
            params.put(STATUS, Status.ACTIVE.toString()).put(PROVIDER_ID, providerID);
            query.append(" and pt.provider_id=$2");
        } else {
            params.put(STATUS, Status.ACTIVE.toString());
        }
        query.append(" group by pt.product_id");

        LOGGER.debug(query);

        pgService.executePreparedQuery(
                query.toString(),
                params,
                pgHandler -> {
                    if (pgHandler.succeeded()) {
                        LOGGER.debug(pgHandler.result());
                        handler.handle(Future.succeededFuture(pgHandler.result()));
                    } else {
                        LOGGER.error("get resources failed");
                        handler.handle(Future.failedFuture(pgHandler.cause()));
                    }
                });
        return this;
    }

    @Override
    public ConsumerService createOrder(
            JsonObject request, User user, Handler<AsyncResult<JsonObject>> handler) {
        String variantId = request.getString(VARIANT);
        String consumerId = user.getUserId();
        LOGGER.debug(consumerId);

        getOrderRelatedInfo(variantId)
                .compose(
                        orderInfo ->
                                razorPayService.createOrder(orderInfo.getJsonArray(RESULTS).getJsonObject(0)))
                .compose(x -> generateOrderEntry(x, variantId, consumerId))
                .onComplete(
                        completeHandler -> {
                            if (completeHandler.succeeded()) {
                                LOGGER.info("order created");
                                handler.handle(Future.succeededFuture(completeHandler.result()));
                            } else {
                                LOGGER.info("order creation failed");
                                handler.handle(Future.failedFuture(completeHandler.cause()));
                            }
                        });

        return this;
    }

  @Override
  public ConsumerService listPurchase(
      User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String resourceId = request.getString("resourceId");
    String productId = request.getString("productId");
    JsonArray userResponse = new JsonArray();

    String fetchInvoiceDuringSuccessfulPayment =
        queryBuilder.listPurchaseForConsumerDuringSuccessfulPayment(
            user.getUserId(), resourceId, productId);
    String fetchInvoiceForOtherPaymentStatus =
        queryBuilder.listPurchaseForConsumer(user.getUserId(), resourceId, productId);

    Future<JsonArray> successfulPaymentFuture =
        executePurchaseQuery(fetchInvoiceDuringSuccessfulPayment, resourceId, productId, user);
    Future<JsonArray> pendingOrFailedPaymentFuture =
        successfulPaymentFuture.compose(
            jsonArray -> {
              userResponse.add(jsonArray);
              return executePurchaseQuery(fetchInvoiceForOtherPaymentStatus, resourceId, productId, user);
            });
    Future<JsonArray> userResponseFuture =
        pendingOrFailedPaymentFuture.onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                userResponse.add(pgHandler.result());
                JsonObject response =
                    new RespBuilder()
                        .withType(ResponseUrn.SUCCESS_URN.getUrn())
                        .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
                        .withResult(userResponse)
                        .getJsonResponse();
                handler.handle(Future.succeededFuture(response));

              } else {
                handler.handle(Future.failedFuture(pgHandler.cause().getMessage()));
              }
            });

    return this;
  }


  public Future<JsonArray> executePurchaseQuery(
      String query, String resourceId, String productId, User user) {
    Promise<JsonArray> promise = Promise.promise();
    pgService.executeQuery(
        query,
        queryHandler -> {
          if (queryHandler.succeeded()) {
            LOGGER.debug("Fetched invoice related information from postgres successfully");
            JsonArray result = queryHandler.result().getJsonArray(RESULTS);
            if (!result.isEmpty()) {
              JsonArray userResponse = new JsonArray();
              for (Object row : result) {
                JsonObject rowEntry = JsonObject.mapFrom(row);

                // gets provider info, consumer info, product info
                rowEntry
                    .mergeIn(util.getUserJsonFromRowEntry(rowEntry, Role.PROVIDER))
                    .mergeIn(util.generateUserJson(user))
                    .mergeIn(util.getProductInfo(rowEntry));
                userResponse.add(rowEntry);
              }
              promise.complete(userResponse);
            } else {
              LOGGER.debug(
                  "No invoice present for the given resource "
                      + ": {} or product : {}, for the consumer : {}",
                  resourceId,
                  productId,
                  user.getUserId());

              boolean isAnyQueryParamSent =
                  StringUtils.isNotBlank(resourceId) || StringUtils.isNotBlank(productId);

              String failureMessage =
                  new RespBuilder()
                      .withType(HttpStatusCode.NO_CONTENT.getValue())
                      .withTitle(HttpStatusCode.NO_CONTENT.getUrn())
                      .getResponse();
              if (isAnyQueryParamSent) {
                failureMessage =
                    new RespBuilder()
                        .withType(HttpStatusCode.NOT_FOUND.getValue())
                        .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                        .withDetail("Purchase info not found")
                        .getResponse();
              }
              promise.fail(failureMessage);
            }
          } else {
            String failureMessage =
                new RespBuilder()
                    .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                    .withTitle(ResponseUrn.DB_ERROR_URN.getUrn())
                    .withDetail(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                    .getResponse();
            promise.fail(failureMessage);
          }
        });
    return promise.future();
  }



    @Override
    public ConsumerService listProductVariants(
            User user, JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        String productId = request.getString("productId");
        String query = FETCH_ACTIVE_PRODUCT_VARIANTS.replace("$1", productId);
        pgService.executeQuery(
                query,
                pgHandler -> {
                    if (pgHandler.succeeded()) {
                        boolean isResponseEmpty = pgHandler.result().getJsonArray(RESULTS).isEmpty();
                        if (!isResponseEmpty) {
                            LOGGER.info("Product variants fetched successfully");
                            handler.handle(Future.succeededFuture(pgHandler.result()));
                        } else {
                            LOGGER.info("Response from DB is empty while fetching " + "product variant");
                            String failureMessage =
                                    new RespBuilder()
                                            .withType(ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                                            .withTitle(ResponseUrn.RESOURCE_NOT_FOUND_URN.getMessage())
                                            .withDetail("Product variants not found")
                                            .getResponse();
                            handler.handle(Future.failedFuture(failureMessage));
                        }
                    } else {
                        LOGGER.error(
                                "Failure while fetching product variant : {}", pgHandler.cause().getMessage());
                        String failureMessage =
                                new RespBuilder()
                                        .withType(ResponseUrn.DB_ERROR_URN.getUrn())
                                        .withTitle(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
                                        .withDetail(
                                                "Product variants could not be fetched as there was internal server error")
                                        .getResponse();
                        handler.handle(Future.failedFuture(failureMessage));
                    }
                });
        return this;
    }
    
    private Future<JsonObject> generateOrderEntry(
            JsonObject orderInfo, String variantId, String consumerId) {
        Promise<JsonObject> promise = Promise.promise();
        QueryContainer queryContainer = getQueryContainer(orderInfo, variantId, consumerId);

        pgService.executeTransaction(
                queryContainer.queries,
                pgHandler -> {
                    if (pgHandler.succeeded()) {
                        LOGGER.info("order created : {}", pgHandler.result());
                        promise.complete(
                                pgHandler
                                        .result()
                                        .put(
                                                "results",
                                                new JsonArray()
                                                        .add(new JsonObject().put("order_id", queryContainer.orderId))));
                    } else {
                        LOGGER.error("Failed to create order : {}", pgHandler.cause().getMessage());
                        promise.fail(pgHandler.cause());
                    }
                });

        return promise.future();
    }

    private QueryContainer getQueryContainer(
            JsonObject orderInfo, String variantId, String consumerId) {
        String orderTable = config.getJsonArray(TABLES).getString(7);
        String invoiceTable = config.getJsonArray(TABLES).getString(6);
        String productVariantTable = config.getJsonArray(TABLES).getString(3);
        JsonObject transferInfo = orderInfo.getJsonArray(TRANSFERS).getJsonObject(0);

        String orderId = transferInfo.getString(SOURCE);

        StringBuilder orderQuery =
                new StringBuilder(
                        INSERT_ORDER_QUERY
                                .replace("$0", orderTable)
                                .replace("$1", orderId)
                                .replace("$2", transferInfo.getInteger(AMOUNT).toString())
                                .replace("$3", INR)
                                .replace("$4", transferInfo.getString(RECIPIENT))
                                .replace("$5", transferInfo.getJsonObject(NOTES).toString()));

        LOGGER.debug("order query : {}", orderQuery);

        StringBuilder invoiceQuery =
                new StringBuilder(
                        INSERT_INVOICE_QUERY
                                .replace("$0", invoiceTable)
                                .replace("$p", productVariantTable)
                                .replace("$1", uuidSupplier.get())
                                .replace("$2", consumerId)
                                .replace("$3", orderId)
                                .replace("$4", variantId)
                                .replace("$5", String.valueOf(PaymentStatus.PENDING))
                                .replace(
                                        "$6",
                                        ZonedDateTime.now()
                                                .withZoneSameInstant(ZoneId.of("UTC"))
                                                .toLocalDateTime()
                                                .toString()) // TODO: how to set payment time at time of order creation?
                );

        LOGGER.debug("invoice info: {}", invoiceQuery);

        List<String> queries = new ArrayList<>();
        queries.add(orderQuery.toString());
        queries.add(invoiceQuery.toString());
        return new QueryContainer(orderId, queries);
    }

    Future<JsonObject> getOrderRelatedInfo(String variantId) {
        Promise<JsonObject> promise = Promise.promise();
        String productVariantTable = config.getJsonArray(TABLES).getString(3);
        String merchantTable = config.getJsonArray(TABLES).getString(8);

        StringBuilder query =
                new StringBuilder(
                        GET_PRODUCT_VARIANT_INFO
                                .replace("$0", productVariantTable)
                                .replace("$9", merchantTable));

        JsonObject params =
                new JsonObject().put(VARIANT, variantId).put(STATUS, Status.ACTIVE.toString());

        LOGGER.debug("select variant query : {}", query);

        pgService.executePreparedQuery(
                query.toString(),
                params,
                pgHandler -> {
                    if (pgHandler.succeeded()) {
                        LOGGER.info("variant for order : {}", pgHandler.result());
                        promise.complete(pgHandler.result());
                    } else {
                        LOGGER.error("Couldn't fetch variant for order");
                        promise.fail(pgHandler.cause());
                    }
                });
        return promise.future();
    }

    private static class QueryContainer {
        public final String orderId;
        public final List<String> queries;

        public QueryContainer(String orderId, List<String> queries) {
            this.orderId = orderId;
            this.queries = queries;
        }
    }
}