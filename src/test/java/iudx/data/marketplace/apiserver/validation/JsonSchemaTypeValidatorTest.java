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

import java.io.IOException;
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
                .put("productId", "newid")
                .put(
                    "resourceIds",
                    new JsonArray()
                        .add(
                            "83c2e5c2-3574-4e11-9530-2b1fbdfce832"))),
        Arguments.of(
            RequestType.PRODUCT_VARIANT,
            new JsonObject()
                .put("productId", "urn:datakaveri.org:83c2e5c2-3574-4e11-9530-2b1fbdfce832:newid")
                .put("productVariantName", "var1")
                .put(
                    "resources",
                    new JsonArray()
                        .add(
                            new JsonObject()
                                .put(
                                    "id",
                                    "83c2e5c2-3574-4e11-9530-2b1fbdfce832")
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

      Exception exception =
              assertThrows(
                      DxRuntimeException.class,
                      () ->
                              new JsonSchemaTypeValidator(body, requestType).isValid()
              );

      assertEquals(
              "object has missing required properties ([\"productId\",\"resourceIds\"])",
              exception.getMessage());
      testContext.completeNow();


  }

  @Test
  @DisplayName("Test schema read error")
  public void testSchemaReadError(VertxTestContext testContext) {
    body = new JsonObject();
    requestType = mock(RequestType.class);
    when(requestType.getFilename()).thenReturn("dummyString");

      Exception exception =
          assertThrows(DxRuntimeException.class, () -> new JsonSchemaTypeValidator(body, requestType).isValid());

      assertEquals(
          "Invalid json format in post request [schema mismatch] [ {} ] ", exception.getMessage());


    testContext.completeNow();
  }
}
