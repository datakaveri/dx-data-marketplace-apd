package iudx.data.marketplace.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.apiserver.util.Constants.RESULTS;

public class RespBuilder {
  private JsonObject response = new JsonObject();

  public RespBuilder withType(String type) {
    response.put(JSON_TYPE, type);
    return this;
  }

  public RespBuilder withTitle(String title) {
    response.put(JSON_TITLE, title);
    return this;
  }

  public RespBuilder withDetail(String detail) {
    response.put(JSON_DETAIL, detail);
    return this;
  }

  public RespBuilder withResult(String id, String method, String status) {
    JsonObject resultAttrs = new JsonObject().put(ID, id).put(METHOD, method).put(STATUS, status);
    response.put(RESULTS, new JsonArray().add(resultAttrs));
    return this;
  }

  public RespBuilder withResult(String id, String method, String status, String detail) {
    JsonObject resultAttrs =
        new JsonObject()
            .put(ID, id)
            .put(METHOD, method)
            .put(STATUS, status)
            .put(JSON_DETAIL, detail);
    response.put(RESULTS, new JsonArray().add(resultAttrs));
    return this;
  }

  public RespBuilder withResult(JsonArray result) {
    response.put(RESULTS, result);
    return this;
  }

  public RespBuilder withResult() {
    response.put(RESULTS, new JsonArray());
    return this;
  }

  public JsonObject getJsonResponse() {
    return response;
  }

  public String getResponse() {
    return response.toString();
  }

  public RespBuilder withResult(JsonObject jsonObject) {
    response.put(RESULTS, jsonObject);
    return this;
  }
}
