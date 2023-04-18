package iudx.data.marketplace.apiserver.validation;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.validation.types.VariantNameTypeValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class VariantNameTypeValidatorTest {

  VariantNameTypeValidator variantNameTypeValidator;
  String value;
  boolean required;

  @Test
  @DisplayName("Test for valid name")
  public void testForValidName(VertxTestContext testContext) {
    variantNameTypeValidator = new VariantNameTypeValidator("variant99", true);

    boolean valid = variantNameTypeValidator.isValid();
    assertTrue(valid);
    testContext.completeNow();
  }

  public static Stream<Arguments> invalidData() {
    return Stream.of(
        Arguments.of((Object) null),
        Arguments.of(""),
        Arguments.of("qwertyuiopasdfghjklzxcvbnmaksjdndcijwnciwjdcnsijdcnsijdcnsidjcnsdjicnsijcnosncacnoncocnjoecnjnvjsfnjisdncjsdncjsncisjcn"),
        Arguments.of("variant!@@)(")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidData")
  @DisplayName("failure test Name validator")
  public void failureTestNameValidator(String name, VertxTestContext testContext) {
    value = name;

    variantNameTypeValidator = new VariantNameTypeValidator(value, true);
    Exception exception = assertThrows(DxRuntimeException.class, () -> {
      variantNameTypeValidator.isValid();
    });
    testContext.completeNow();
  }
}

