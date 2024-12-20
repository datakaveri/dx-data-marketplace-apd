package iudx.data.marketplace.apiserver.validation;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.validation.types.UuidTypeValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class UuidTypeValidatorTest {

    UuidTypeValidator UUIDTypeValidator;
    String value;
    boolean required;

    @Test
    @DisplayName("Test for valid ID")
    public void testForValidId(VertxTestContext testContext) {
    value = UUID.randomUUID().toString();
        UUIDTypeValidator = new UuidTypeValidator(value, true);

        boolean valid = UUIDTypeValidator.isValid();
        assertTrue(valid);
        testContext.completeNow();
    }

    public static Stream<Arguments> invalidData() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(""),
                Arguments.of("qwertyuiopasdgfhjklzxncbvashfdjlqweiuasddnifsjchhiiseoweoweropwirpwqiipwipwiepwiepqwiepqiepwqiepqeipqwiepqepqnihdsjnaisjchiajsnxajxnijsnijxnisjxnijsxiajsxnijsxniasxnaisxnisjnxijsnaisjxnaisxnaijsxnaisjnijsnaisjnijsnaijsnijscnijsnijnwihf"),
                Arguments.of("ksad^*#")

        );
    }

    @ParameterizedTest
    @MethodSource("invalidData")
    @DisplayName("failure Test ID validator")
    public void failureTestIDValidator(String id, VertxTestContext testContext) {
        value = id;

        UUIDTypeValidator = new UuidTypeValidator(value, true);
        Exception exception = assertThrows(DxRuntimeException.class, () -> {
            UUIDTypeValidator.isValid();
        });
        testContext.completeNow();
    }
}
