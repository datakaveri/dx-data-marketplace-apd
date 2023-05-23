package iudx.data.marketplace.apiserver.validation;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.apiserver.validation.types.JsonSchemaTypeValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class JsonSchemaTypeValidatorTest {

  JsonSchemaTypeValidator jsonSchemaTypeValidator;
  JsonObject body;
  RequestType requestType;

  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of(
            RequestType.PRODUCT,
            new JsonObject()
                .put("id", "newid")
                .put(
                    "datasets",
                    new JsonArray()
                        .add(
                            "datakaveri.org/b8bd3e3f39615c8ec96722131ae95056b5938f2f/rs.iudx.io/agra-swachhata-app"))),
        Arguments.of(
            RequestType.PRODUCT_VARIANT,
            new JsonObject()
                .put("id", "urn:datakaveri.org:iisc.ac.in/123qwerty:newid")
                .put("variant", "var1")
                .put(
                    "datasets",
                    new JsonArray()
                        .add(
                            new JsonObject()
                                .put(
                                    "id",
                                    "datakaveri.org/b8bd3e3f39615c8ec96722131ae95056b5938f2f/rs.iudx.io/agra-swachhata-app")
                                .put("capabilities", new JsonArray().add("api").add("sub"))))
                .put("price", 100.0)
                .put("duration", 12)));
  }

  @ParameterizedTest
  @MethodSource("data")
  @DisplayName("Test JsonSchema Validator")
  public void testJsonSchemaValidator(
      RequestType type, JsonObject jsonObject, VertxTestContext testContext) {
    body = jsonObject;
    requestType = type;
    jsonSchemaTypeValidator = new JsonSchemaTypeValidator(body, requestType);

    boolean valid = jsonSchemaTypeValidator.isValid();
    assertTrue(valid);
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test json invalid")
  public void testInvalidSchemaOrInput(VertxTestContext testContext) {
    body = new JsonObject();
    requestType = RequestType.PRODUCT;
    jsonSchemaTypeValidator = new JsonSchemaTypeValidator(body, requestType);

    Exception exception =
        assertThrows(
            DxRuntimeException.class,
            () -> {
              jsonSchemaTypeValidator.isValid();
            });

    testContext.completeNow();
  }

  @Test
  @DisplayName("Test schema read error")
  public void testSchemaReadError(VertxTestContext testContext) {
    body = new JsonObject();
    requestType = mock(RequestType.class);
    when(requestType.getFilename()).thenReturn("dummyString");
    jsonSchemaTypeValidator = new JsonSchemaTypeValidator(body, requestType);

    Exception exception =
        assertThrows(
            DxRuntimeException.class,
            () -> {
              jsonSchemaTypeValidator.isValid();
            });

    testContext.completeNow();
  }
}
