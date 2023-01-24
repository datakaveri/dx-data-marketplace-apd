package iudx.data.marketplace.common;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.client.WebClientOptions;

import static iudx.data.marketplace.common.Constants.*;

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

  public Future<JsonObject> getItemDetails(String itemID) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject itemDetails = new JsonObject();
    catWebClient
        .get(catPort, catHost, catItemPath)
        .addQueryParam("id", itemID)
        .send(
            catItemHandler -> {
              if (catItemHandler.succeeded()) {
                JsonArray response =
                    catItemHandler.result().bodyAsJsonObject().getJsonArray("results");
                if (!response.isEmpty()) {
                  JsonObject result = response.getJsonObject(0);
                  if (result.getJsonArray("type").contains(TYPE_PROVIDER)) {
                    itemDetails
                        .put(PROVIDER_ID, itemID)
                        .put(PROVIDER_NAME, result.getString("description"));
                  } else if (result.getJsonArray("type").contains(TYPE_RG)) {
                    itemDetails
                        .put(DATASET_ID, itemID)
                        .put(DATASET_NAME, result.getString("label"))
                        .put("accessPolicy", result.getString("accessPolicy"));

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

  public Future<JsonObject> getResourceCount(String datasetID) {
    Promise<JsonObject> promise = Promise.promise();
    catWebClient
        .get(catPort, catHost, catRelPath)
        .addQueryParam("id", datasetID)
        .addQueryParam("rel", "resource")
        .send(catRelHandler -> {
          if (catRelHandler.succeeded()) {
            int totalHits = catRelHandler.result().bodyAsJsonObject().getInteger("totalHits");
            JsonObject res = new JsonObject().put(DATASET_ID, datasetID).put("totalHits", totalHits);
            promise.complete(res);
          } else {
            LOGGER.debug("Cat web client call to {} failed for id: {} ", catRelPath, datasetID);
            promise.fail("Cat web client call to " + catItemPath + " failed for id: " + datasetID);
          }
        });

    return promise.future();
  }
}
