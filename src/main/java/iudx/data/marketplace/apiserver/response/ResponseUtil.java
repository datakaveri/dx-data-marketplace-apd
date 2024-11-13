package iudx.data.marketplace.apiserver.response;

import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;

public class ResponseUtil {
  public static JsonObject generateResponse(HttpStatusCode statusCode, ResponseUrn urn) {
    return generateResponse(statusCode, urn, statusCode.getDescription());
  }

  public static JsonObject generateResponse(
      HttpStatusCode statusCode, ResponseUrn urn, String message) {
    String type = urn.getUrn();

    return new RestResponse.Builder()
        .withMessage(message)
        .withType(type)
        .withTitle(statusCode.getDescription())
        .build()
        .toJson();
  }
}
