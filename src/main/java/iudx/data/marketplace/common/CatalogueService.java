package iudx.data.marketplace.common;

import static iudx.data.marketplace.common.Constants.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * The Catalogue Service
 *
 * <h1>Catalogue Service</h1>
 *
 * <p>The catalogue service defines the operations to be performed on the IUDX Catalogue Web client
 *
 * @version 1.0
 * @since 2023-01-20
 */
public class CatalogueService {
  private static final Logger LOGGER = LogManager.getLogger(CatalogueService.class);

  static WebClient catWebClient;
  private static String catHost;
  private static int catPort;
  private static String catItemPath, catRelPath;
  private Vertx vertx;

  public CatalogueService(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    catHost = config.getString(CAT_SERVER_HOST);
    catPort = config.getInteger(CAT_SERVER_PORT);
    catItemPath = config.getString(CAT_ITEM_PATH);
    catRelPath = config.getString(CAT_REL_PATH);

    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true);
    if (catWebClient == null) {
      catWebClient = WebClient.create(vertx, options);
    }
  }

  /**
   * The getItemDetails method calls the IUDX Catalogue item endpoint to get corresponding details
   * using a webClient
   *
   * @see io.vertx.ext.web.client.WebClient
   * @param itemID which is String
   * @return Future which is of type JsonObject
   */
  public Future<JsonObject> getItemDetails(String itemID) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject itemDetails = new JsonObject();
    catWebClient
        .get(catPort, catHost, catItemPath)
        .addQueryParam("id", itemID)
        .send(
            catItemHandler -> {
              LOGGER.debug("response for itemId : {} is  : {}", itemID,catItemHandler.result().bodyAsJsonObject().encodePrettily());
              if (catItemHandler.succeeded()) {
                JsonArray response =
                    catItemHandler.result().bodyAsJsonObject().getJsonArray("results");
                if (!response.isEmpty()) {
                  JsonObject result = response.getJsonObject(0);
                  if (result.getJsonArray("type").contains(TYPE_PROVIDER)) {
                    itemDetails
                        .put("type", TYPE_PROVIDER)
                        .put(PROVIDER_ID, itemID)
                        .put("ownerUserId", result.getString("ownerUserId", ""))
                        .put(PROVIDER_NAME, result.getString("description", ""));
                  } else if (result.getJsonArray("type").contains(TYPE_RI)) {
                    itemDetails
                        .put("type", TYPE_RI)
                        .put(RESOURCE_ID, itemID)
                        .put(RESOURCE_NAME, result.getString("label", ""))
                        .put(RESOURCE_SERVER, result.getValue(RESOURCE_SERVER))
                        .put(PROVIDER, result.getValue(PROVIDER))
                        .put("accessPolicy", result.getString("accessPolicy", ""))
                        .put("apdURL", result.getString("apdURL"));
                  }
                  promise.complete(itemDetails);
                } else {
                  promise.fail("Item with id { " + itemID + " } not found ");
                }
              } else {
                LOGGER.debug("Cat web client call to {} failed for id: {} ", catItemPath, itemID);
                promise.fail("Cat web client call to " + catItemPath + " failed for id: " + itemID);
              }
            });

    return promise.future();
  }

  /**
   * The getResourceCount method calls the IUDX catalogue relationship endpoint using a webClient
   *
   * @see io.vertx.ext.web.client.WebClient
   * @param resourceID which is a String
   * @return Future which is of type JsonObject
   */
  public Future<JsonObject> getResourceCount(String resourceID) {
    Promise<JsonObject> promise = Promise.promise();
    catWebClient
        .get(catPort, catHost, catRelPath)
        .addQueryParam("id", resourceID)
        .addQueryParam("rel", "resource")
        .send(
            catRelHandler -> {
              if (catRelHandler.succeeded()) {
                int totalHits = catRelHandler.result().bodyAsJsonObject().getInteger("totalHits");
                JsonObject res =
                    new JsonObject().put(RESOURCE_ID, resourceID).put("totalHits", totalHits);
                promise.complete(res);
              } else {
                LOGGER.debug("Cat web client call to {} failed for id: {} ", catRelPath, resourceID);
                promise.fail(
                    "Cat web client call to " + catItemPath + " failed for id: " + resourceID);
              }
            });

    return promise.future();
  }

  public Future<JsonObject> searchApi(JsonObject params) {
    Promise<JsonObject> promise = Promise.promise();

    JsonArray keysArray = new JsonArray();
    JsonArray valuesArray = new JsonArray();

    params.forEach(entry -> {
      keysArray.add(entry.getKey());
      JsonArray valueArray = new JsonArray().add(entry.getValue());
      valuesArray.add(valueArray.getList());
    });

    LOGGER.debug(keysArray.getList().toString());
    LOGGER.debug(valuesArray.getList().toString());

    catWebClient
        .get(catPort, catHost, "/iudx/cat/v1/search")
        .addQueryParam("property", keysArray.getList().toString())
        .addQueryParam("value", valuesArray.getList().toString())
            .send(searchApiHandler -> {
              if(searchApiHandler.succeeded()) {
                LOGGER.debug(searchApiHandler.result().bodyAsJsonObject());
              } else {
                LOGGER.error(searchApiHandler.cause());
              }
            });

    promise.complete();
    return promise.future();
  }
}
