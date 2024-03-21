package iudx.data.marketplace.policy;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.apiserver.util.Role;
import iudx.data.marketplace.policies.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import iudx.data.marketplace.Util;

import java.util.UUID;
import java.util.stream.Stream;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestUser {
    User user;
    JsonObject userDetails;
    UUID userId;
    String role;
    String emailId;
    String firstName;
    String lastName;

    private static final String USER_ID = "userId";
    static Stream<Arguments> values4FailedCases() {
        return Stream.of(
                Arguments.of(false, new JsonObject()),
                Arguments.of(false, mock(User.class)),
                Arguments.of(
                        false,
                        new User(
                                new JsonObject()
                                        .put(USER_ID, Util.generateRandomUuid())
                                        .put(USER_ROLE, Role.CONSUMER)
                                        .put(EMAIL_ID, null)
                                        .put(RS_SERVER_URL, Util.generateRandomString())
                                        .put(LAST_NAME, Util.generateRandomString()))));
    }

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        userId = Util.generateRandomUuid();
        role = Role.CONSUMER.getRole();
        emailId = Util.generateRandomEmailId();
        firstName = Util.generateRandomString();
        lastName = Util.generateRandomString();

        userDetails =
                new JsonObject()
                        .put(USER_ID, userId)
                        .put(USER_ROLE, role)
                        .put(EMAIL_ID, emailId)
                        .put(FIRST_NAME, firstName)
                        .put(LAST_NAME, lastName);
        user = new User(userDetails);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test toJsonMethod : Success")
    public void testToJsonMethod(VertxTestContext vertxTestContext) {
        JsonObject expected = user.toJson();
        assertEquals(emailId, expected.getString(EMAIL_ID));
        assertEquals(firstName, expected.getString(FIRST_NAME));
        assertEquals(lastName, expected.getString(LAST_NAME));
        assertEquals(userId.toString(), expected.getString(USER_ID));
        assertEquals(Role.fromString(role).toString(), expected.getString(USER_ROLE));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test toJson method when role is null")
    public void testWithNullRole(VertxTestContext vertxTestContext) {
        userId = Util.generateRandomUuid();
        role = null;
        emailId = Util.generateRandomEmailId();
        firstName = Util.generateRandomString();
        lastName = Util.generateRandomString();

        JsonObject userDetails =
                new JsonObject()
                        .put(USER_ID, userId)
                        .put(USER_ROLE, role)
                        .put(EMAIL_ID, emailId)
                        .put(FIRST_NAME, firstName)
                        .put(LAST_NAME, lastName);
        assertThrows(DxRuntimeException.class, () -> new User(userDetails));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test toJson method when role is null")
    public void testWithNullEmail(VertxTestContext vertxTestContext) {
        userId = Util.generateRandomUuid();
        role = Role.PROVIDER_DELEGATE.getRole();
        emailId = null;
        firstName = Util.generateRandomString();
        lastName = Util.generateRandomString();

        JsonObject userDetails =
                new JsonObject()
                        .put(USER_ID, userId)
                        .put(USER_ROLE, role)
                        .put(EMAIL_ID, emailId)
                        .put(FIRST_NAME, firstName)
                        .put(LAST_NAME, lastName);
        User user = new User(userDetails);
        JsonObject expected = user.toJson();
        assertNull(expected.getString(EMAIL_ID));
        assertEquals(firstName, expected.getString(FIRST_NAME));
        assertEquals(lastName, expected.getString(LAST_NAME));
        assertEquals(userId.toString(), expected.getString(USER_ID));
        assertEquals(Role.fromString(role).toString(), expected.getString(USER_ROLE));
        vertxTestContext.completeNow();
    }

    @ParameterizedTest
    @MethodSource("values4FailedCases")
    @DisplayName("Test equals method : Failure")
    public void testEqualsMethodFailure(boolean expected, Object input, VertxTestContext context) {
        assertEquals(expected, user.equals(input));
        context.completeNow();
    }

    Stream<Arguments> values() {
        return Stream.of(
                Arguments.of(false, user),
                Arguments.of(
                        false,
                        new User(
                                new JsonObject()
                                        .put(USER_ID, this.userId)
                                        .put(USER_ROLE, Role.CONSUMER.getRole())
                                        .put(EMAIL_ID, this.emailId)
                                        .put(FIRST_NAME, this.firstName)
                                        .put(LAST_NAME, this.lastName))));
    }

    @Test
    @DisplayName("Test equals method ")
    public void testEqualsMethod(VertxTestContext context) {
        User someUser =
                new User(
                        new JsonObject()
                                .put(USER_ID, this.userId)
                                .put(USER_ROLE, Role.CONSUMER.getRole())
                                .put(EMAIL_ID, this.emailId)
                                .put(FIRST_NAME, this.firstName)
                                .put(LAST_NAME, this.lastName));
        assertTrue(user.equals(user));
        assertTrue(user.equals(someUser));
        assertEquals(user.hashCode(), user.hashCode());
        assertEquals(someUser.hashCode(), user.hashCode());
        context.completeNow();
    }
}
