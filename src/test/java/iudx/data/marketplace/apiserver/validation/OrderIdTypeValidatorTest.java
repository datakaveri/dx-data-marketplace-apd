package iudx.data.marketplace.apiserver.validation;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.validation.types.OrderIdTypeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class OrderIdTypeValidatorTest {
    private static final Logger LOGGER = LogManager.getLogger(OrderIdTypeValidatorTest.class);
    OrderIdTypeValidator validator;

    public static Stream<Arguments> validInput() {
        return Stream.of(
                Arguments.of((Object) null, false),
                Arguments.of("order_E9uTczH8uWPCyQ", true)


        );
    }
    @ParameterizedTest
    @MethodSource("validInput")
    @DisplayName("Test with valid orderId")
    public void testIsValidSuccess(String value, boolean required, VertxTestContext vertxTestContext)
    {
        validator  = new OrderIdTypeValidator(value, required);
        assertTrue(validator.isValid());
        vertxTestContext.completeNow();
    }

    public static Stream<Arguments> inValidInput() {
        return Stream.of(
                Arguments.of((Object) null, true),
                Arguments.of((Object) "", true),
                Arguments.of((Object) "           ", false),
                Arguments.of((Object) "abcd", true),
                Arguments.of((Object) "ajdfskljfgksfjglkfgjslkfgjlkgierutergfmgndfghueiortgh", true)


        );
    }
    @ParameterizedTest
    @MethodSource("inValidInput")
    @DisplayName("Test with invalid orderId")
    public void testIsValidFailure(String value, boolean required, VertxTestContext vertxTestContext)
    {
        validator  = new OrderIdTypeValidator(value, required);
        Exception exception = assertThrows(DxRuntimeException.class, () -> validator.isValid());
        assertTrue(exception.getMessage().contains("Invalid id"));
        vertxTestContext.completeNow();
    }

}
