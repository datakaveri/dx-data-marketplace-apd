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
  public RespBuilder withType(int statusCode)
  {
    response.put(JSON_TYPE, statusCode);
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

  public RespBuilder withResult(JsonArray result) {
    response.put(RESULTS, result);
    return this;
  }

  public RespBuilder withResult(JsonObject result) {
    response.put(RESULTS, result);
    return this;
  }
  public JsonObject getJsonResponse() {
    return response;
  }

  public String getResponse() {
    return response.toString();
  }
}
