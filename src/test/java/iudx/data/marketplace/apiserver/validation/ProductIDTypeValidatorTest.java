package iudx.data.marketplace.apiserver.validation;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.validation.types.ProductIDTypeValidator;
import org.junit.jupiter.api.Disabled;
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
public class ProductIDTypeValidatorTest {

  ProductIDTypeValidator productIDTypeValidator;
  String value;
  boolean required;

  @Test
  @DisplayName("Test for valid ID")
  public void testForValidId(VertxTestContext testContext) {
    productIDTypeValidator = new ProductIDTypeValidator("urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:testProduct", true);

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

    productIDTypeValidator = new ProductIDTypeValidator(value, true);
    Exception exception = assertThrows(DxRuntimeException.class, () -> {
      productIDTypeValidator.isValid();
    });
    testContext.completeNow();
  }
}
