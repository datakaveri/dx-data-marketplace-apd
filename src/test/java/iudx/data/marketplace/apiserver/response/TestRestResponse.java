package iudx.data.marketplace.apiserver.response;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestRestResponse {
    ResponseUtil responseUtil;

    @Test
    void generateResponseWithMesssageTest(VertxTestContext vertxTestContext) {
        JsonObject response = ResponseUtil.generateResponse(HttpStatusCode.SUCCESS, ResponseUrn.SUCCESS_URN, "Success");
        assertEquals("urn:dx:dmp:success", response.getString("type"));
        assertEquals("Success", response.getString("title"));
        assertEquals("Success", response.getString("detail"));
        vertxTestContext.completeNow();
    }
    @Test
    void generateResponseWithoutMesssageTest(VertxTestContext vertxTestContext) {
        JsonObject response = ResponseUtil.generateResponse(HttpStatusCode.SUCCESS, ResponseUrn.SUCCESS_URN);
        assertEquals("urn:dx:dmp:success", response.getString("type"));
        assertEquals("Success", response.getString("title"));
        assertEquals("Success", response.getString("detail"));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test toJson method when the status code is non-zero")
    public void testToJsonWithStatusCode(VertxTestContext vertxTestContext)
    {
        RestResponse response =  new RestResponse.Builder()
                .build(200, ResponseUrn.SUCCESS_URN.getUrn(), "Success", ResponseUrn.SUCCESS_URN.getMessage());
        JsonObject expected = new JsonObject()
                .put("statusCode", 200)
                .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                .put("title", "Success")
                .put("detail", ResponseUrn.SUCCESS_URN.getMessage());
        assertEquals(expected, response.toJson());
        vertxTestContext.completeNow();


    }
}
