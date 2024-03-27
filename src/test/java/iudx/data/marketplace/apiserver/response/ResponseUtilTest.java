package iudx.data.marketplace.apiserver.response;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class ResponseUtilTest {

  @Test
  @DisplayName("Test generate response with custom message")
  public void testGenerateRespCustMessage(VertxTestContext testContext) {
    JsonObject expected =
        new JsonObject()
            .put("detail", "any-detail")
            .put("type", "urn:dx:dmp:success")
            .put("title", "Success");

    HttpStatusCode statusCode = HttpStatusCode.SUCCESS;
    ResponseUrn urn = ResponseUrn.SUCCESS_URN;

    JsonObject actual = ResponseUtil.generateResponse(statusCode, urn, "any-detail");

    for (String key : expected.getMap().keySet()) {
      assertEquals(expected.getValue(key), actual.getValue(key));
    }
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test generate response with custom message")
  public void testGenerateResp(VertxTestContext testContext) {
    JsonObject expected =
        new JsonObject()
            .put("detail", "Success")
            .put("type", "urn:dx:dmp:success")
            .put("title", "Success");

    HttpStatusCode statusCode = HttpStatusCode.SUCCESS;
    ResponseUrn urn = ResponseUrn.SUCCESS_URN;

    JsonObject actual = ResponseUtil.generateResponse(statusCode, urn);

    for (String key : expected.getMap().keySet()) {
      assertEquals(expected.getValue(key), actual.getValue(key));
    }
    testContext.completeNow();
  }
}
