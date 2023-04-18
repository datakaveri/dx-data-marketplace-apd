package iudx.data.marketplace.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class ResponseBuilderTest {

  RespBuilder respBuilder = new RespBuilder();

  @Test
  @DisplayName("test type, title, detail and result methods")
  public void testWithType(VertxTestContext testContext) {
    respBuilder
        .withType(ResponseUrn.SUCCESS_URN.getUrn())
        .withTitle(ResponseUrn.SUCCESS_URN.getMessage())
        .withDetail("Successful Operation")
        .withResult(new JsonArray());
    assertEquals(
        new JsonObject()
            .put("type", ResponseUrn.SUCCESS_URN.getUrn())
            .put("title", ResponseUrn.SUCCESS_URN.getMessage())
            .put("detail", "Successful Operation")
            .put("results", new JsonArray()),
        respBuilder.getJsonResponse());
    testContext.completeNow();
  }
}
