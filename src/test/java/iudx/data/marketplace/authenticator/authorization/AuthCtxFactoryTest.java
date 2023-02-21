package iudx.data.marketplace.authenticator.authorization;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import iudx.data.marketplace.authenticator.model.JwtData;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class AuthCtxFactoryTest {
  public static final Logger LOGGER = LogManager.getLogger(AuthCtxFactoryTest.class);
  @Mock AuthorizationRequest authRequest;
  @Mock JwtData jwtData;
  DelegateAuthStatergy delegateAuthStatergy;
  ConsumerAuthStatergy consumerAuthStatergy;
  AuthorizationContextFactory authorizationContextFactory;

  @Test
  @DisplayName("testing the method create when role is consumer")
  public void testCreate(VertxTestContext vertxTestContext) {
    authorizationContextFactory = new AuthorizationContextFactory();
    String role = "consumer";
    consumerAuthStatergy = new ConsumerAuthStatergy();
    assertFalse(consumerAuthStatergy.isAuthorized(authRequest, jwtData));
    assertNotNull(AuthorizationContextFactory.create(role));
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("testing the method create when role is delegate")
  public void testCreateDelegate(VertxTestContext vertxTestContext) {
    authorizationContextFactory = new AuthorizationContextFactory();
    String role = "delegate";
    delegateAuthStatergy = new DelegateAuthStatergy();
    assertFalse(delegateAuthStatergy.isAuthorized(authRequest, jwtData));
    assertNotNull(AuthorizationContextFactory.create(role));
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("testing the method create with IllegalArgumentException")
  public void testCreateDefault(VertxTestContext vertxTestContext) {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          authorizationContextFactory = new AuthorizationContextFactory();
          String role = "dummy";
          AuthorizationContextFactory.create(role);
        });
    vertxTestContext.completeNow();
  }

  public static Stream<Arguments> roles() {
    return Stream.of(
        Arguments.of(new ProviderAuthStatergy(), "provider"),
        Arguments.of(new DelegateAuthStatergy(), "delegate"),
        Arguments.of(new ConsumerAuthStatergy(), "consumer"),
        Arguments.of(new AdminAuthStatergy(), "admin"));
  }

  @ParameterizedTest
  @MethodSource("roles")
  @DisplayName("test user with x role is authorized")
  public void testConsumerIsAuthorized(
      AuthorizationStatergy authorizationStatergy, String role, VertxTestContext testContext) {

    if(role.equalsIgnoreCase("admin"))
      authRequest = new AuthorizationRequest(Method.POST, Api.VERIFY);
    else
      authRequest = new AuthorizationRequest(Method.POST, Api.PRODUCT);
    lenient().when(jwtData.getCons()).thenReturn(new JsonObject().put("access", new JsonArray()));
    authorizationContextFactory = new AuthorizationContextFactory();
    if (authorizationStatergy instanceof ConsumerAuthStatergy)
      assertFalse(authorizationStatergy.isAuthorized(authRequest, jwtData));
    else
      assertTrue(authorizationStatergy.isAuthorized(authRequest, jwtData));
    assertNotNull(AuthorizationContextFactory.create(role));
    testContext.completeNow();
  }
}
