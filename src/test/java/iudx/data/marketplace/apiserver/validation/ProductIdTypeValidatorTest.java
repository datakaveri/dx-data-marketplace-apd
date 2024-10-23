package iudx.data.marketplace.apiserver.validation;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.validation.types.ProductIdTypeValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ProductIdTypeValidatorTest {

  ProductIdTypeValidator productIDTypeValidator;
  String value;
  boolean required;

  @Test
  @DisplayName("Test for valid ID")
  public void testForValidId(VertxTestContext testContext) {
    productIDTypeValidator = new ProductIdTypeValidator("urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:testProduct", true);

    boolean valid = productIDTypeValidator.isValid();
    assertTrue(valid);
    testContext.completeNow();
  }

  public static Stream<Arguments> invalidData() {
    return Stream.of(
        Arguments.of((Object) null),
        Arguments.of(""),
        Arguments.of("qwertyuiopasdgfhjklzxncbvashfdjlqweiuasddnifsjcnihdsjnaisjchiajsnxajxnijsnijxnisjxnijsxiajsxnijsxniasxnaisxnisjnxijsnaisjxnaisxnaijsxnaisjnijsnaisjnijsnaijsnijscnijsnijnwihf"),
        Arguments.of("ksad^*#")

    );
  }

  @ParameterizedTest
  @MethodSource("invalidData")
  @DisplayName("failure Test ID validator")
  public void failureTestIDValidator(String id, VertxTestContext testContext) {
    value = id;

    productIDTypeValidator = new ProductIdTypeValidator(value, true);
    Exception exception = assertThrows(DxRuntimeException.class, () -> {
      productIDTypeValidator.isValid();
    });
    testContext.completeNow();
  }
  public static Stream<Arguments> invalidInput() {
    return Stream.of(
            Arguments.of("", false),
            Arguments.of("     ", false)

    );
  }
  @ParameterizedTest
  @MethodSource("invalidInput")
  @DisplayName("failure Test ID validator")
  public void testIsValidFailure(String value, boolean required, VertxTestContext testContext) {

    productIDTypeValidator = new ProductIdTypeValidator(value, required);
    Exception exception = assertThrows(DxRuntimeException.class, () ->productIDTypeValidator.isValid());
    assertTrue(exception.getMessage().contains("Invalid id"));
    testContext.completeNow();
  }

  @Test
  @DisplayName("test is valid method : Success")
  public void testIsValidFailure( VertxTestContext testContext) {

    productIDTypeValidator = new ProductIdTypeValidator(null, false);
    assertTrue(productIDTypeValidator.isValid());
    testContext.completeNow();
  }
}
