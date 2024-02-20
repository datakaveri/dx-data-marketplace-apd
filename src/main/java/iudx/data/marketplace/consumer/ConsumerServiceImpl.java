package iudx.data.marketplace.consumer;

import static iudx.data.marketplace.common.Constants.PROVIDER_ID;
import static iudx.data.marketplace.common.Constants.RESOURCE_ID;
import static iudx.data.marketplace.consumer.util.Constants.*;
import static iudx.data.marketplace.product.util.Constants.RESULTS;
import static iudx.data.marketplace.product.util.Constants.STATUS;
import static iudx.data.marketplace.product.util.Constants.VARIANT;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.consumer.util.PaymentStatus;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.product.util.Status;
import iudx.data.marketplace.razorpay.RazorPayService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsumerServiceImpl implements ConsumerService {
  public static final Logger LOGGER = LogManager.getLogger(ConsumerServiceImpl.class);
  private final PostgresService pgService;
  private final RazorPayService razorPayService;
  private final JsonObject config;

  Supplier<String> uuidSupplier = () -> UUID.randomUUID().toString();

  public ConsumerServiceImpl(
      JsonObject config, PostgresService postgresService, RazorPayService razorPayService) {
    this.config = config;
    this.pgService = postgresService;
    this.razorPayService = razorPayService;
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
  public ConsumerService listPurchases(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    return null;
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
  public ConsumerService createOrder(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String variantId = request.getString(VARIANT);

    getOrderRelatedInfo(variantId)
        .compose(
            orderInfo ->
                razorPayService.createOrder(orderInfo.getJsonArray(RESULTS).getJsonObject(0)))
        .compose(this::generateOrderEntry)
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

  private Future<JsonObject> generateOrderEntry(JsonObject orderInfo) {
    Promise<JsonObject> promise = Promise.promise();
    String orderTable = config.getJsonArray(TABLES).getString(7);
    String invoiceTable = config.getJsonArray(TABLES).getString(6);
    JsonObject transferInfo = orderInfo.getJsonArray(TRANSFERS).getJsonObject(0);
    JsonObject tranferNotes = transferInfo.getJsonObject(NOTES);

    StringBuilder orderQuery =
        new StringBuilder(
            INSERT_ORDER_QUERY
                .replace("$0", orderTable)
                .replace("$1", transferInfo.getString(SOURCE))
                .replace("$2", transferInfo.getInteger(AMOUNT).toString())
                .replace("$3", INR)
                .replace("$4", transferInfo.getString(RECIPIENT))
                .replace("$5", transferInfo.getJsonObject(NOTES).toString()));

    LOGGER.debug("order query : {}", orderQuery);

    StringBuilder invoiceQuery =
        new StringBuilder(
            INSERT_INVOICE_QUERY
                .replace("$0", invoiceTable)
                .replace("$1", uuidSupplier.get())
                .replace("$2", tranferNotes.getString(VARIANT))
                .replace("$3", PaymentStatus.PENDING.getStatus())
                .replace("$4", "")
                .replace("$5", "")
                .replace("$6", tranferNotes.toString()));

    LOGGER.debug("invoice info: {}", invoiceQuery);

    List<String> queries = new ArrayList<>();
    queries.add(orderQuery.toString());
    queries.add(invoiceQuery.toString());

    pgService.executeTransaction(
        queries,
        pgHandler -> {
          if (pgHandler.succeeded()) {
            LOGGER.info("order created : {}", pgHandler.result());
            promise.complete();
          } else {
            LOGGER.error("Failed to create order : {}", pgHandler.cause().getMessage());
            promise.fail(pgHandler.cause());
          }
        });

    return promise.future();
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
}
