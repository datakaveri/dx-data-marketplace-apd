package iudx.data.marketplace.apiserver.response;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class RestResponseTest {

  private static RestResponse restResponse;

  @BeforeAll
  static void setup(VertxTestContext testContext) {
    restResponse = new RestResponse.Builder()
        .withType("urn:dx:dmp:success")
        .withTitle("Success")
        .withMessage("Success")
        .withStatusCode(200)
        .build();
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test to json with custom status code")
  public void testToJsonWithCustomStatus(VertxTestContext testContext) {
    JsonObject expected = new JsonObject()
        .put("statusCode", 200)
        .put("type", "urn:dx:dmp:success")
        .put("title", "Success")
        .put("detail", "Success");

    JsonObject actual = restResponse.toJson();

    for(String key : expected.getMap().keySet()) {
      assertEquals(expected.getValue(key), actual.getValue(key));
    }
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test to json")
  public void testToJson(VertxTestContext testContext) {
    JsonObject expected = new JsonObject()
        .put("type", "urn:dx:dmp:success")
        .put("title", "Success")
        .put("detail", "Success");
    JsonObject actual = restResponse.toJson();

    for(String key : expected.getMap().keySet()) {
      assertEquals(expected.getValue(key), actual.getValue(key));
    }
    testContext.completeNow();
  }
}
