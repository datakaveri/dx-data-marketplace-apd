package iudx.data.marketplace.authenticator.authorization;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import iudx.data.marketplace.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class AuthCtxFactoryTest {
  public static final Logger LOGGER = LogManager.getLogger(AuthCtxFactoryTest.class);
  @Mock AuthorizationRequest authRequest;
  @Mock JwtData jwtData;
  @Mock Method method;

  Api api;
  @Mock ConsumerAuthStatergy consumerAuthStatergy;
  AuthorizationContextFactory authorizationContextFactory;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext)
  {
    api = Api.getInstance("basePath");
  }
  @Test
  @DisplayName("testing the method create when role is consumer")
  public void testCreate(VertxTestContext vertxTestContext) {
    authorizationContextFactory = new AuthorizationContextFactory();
    IudxRole role = IudxRole.CONSUMER;
    consumerAuthStatergy = ConsumerAuthStatergy.getInstance(api);
    when(authRequest.getApi()).thenReturn("dummy endpoint");
    when(authRequest.getMethod()).thenReturn(method);
    assertFalse(consumerAuthStatergy.isAuthorized(authRequest, jwtData));
    assertNotNull(AuthorizationContextFactory.create(role, api));
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("testing the method create when role is delegate")
  public void testCreateDelegate(VertxTestContext vertxTestContext) {
    authorizationContextFactory = new AuthorizationContextFactory();
    IudxRole role = IudxRole.DELEGATE;
    assertNotNull(AuthorizationContextFactory.create(role, api));
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("testing the method create with IllegalArgumentException")
  public void testCreateDefault(VertxTestContext vertxTestContext) {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          authorizationContextFactory = new AuthorizationContextFactory();
          IudxRole role = IudxRole.ADMIN;
          AuthorizationContextFactory.create(role, api);
        });
    vertxTestContext.completeNow();
  }

  public static Stream<Arguments> roles() {
    return Stream.of(
        Arguments.of(ProviderAuthStatergy.getInstance(any()),IudxRole.PROVIDER),
        Arguments.of(ConsumerAuthStatergy.getInstance(any()), IudxRole.CONSUMER),
        Arguments.of(AdminAuthStatergy.getInstance(any()), IudxRole.ADMIN));
  }

  @ParameterizedTest
  @MethodSource("roles")
  @DisplayName("test user with x role is authorized")
  public void testConsumerIsAuthorized(
      AuthorizationStatergy authorizationStatergy, IudxRole role, VertxTestContext testContext) {

    if(role.equals(IudxRole.ADMIN))
      authRequest = new AuthorizationRequest(Method.POST, api.getVerifyUrl());
    else
      authRequest = new AuthorizationRequest(Method.POST, api.getProviderProductPath());
    lenient().when(jwtData.getCons()).thenReturn(new JsonObject().put("access", new JsonArray()));
    authorizationContextFactory = new AuthorizationContextFactory();
    if (authorizationStatergy instanceof ConsumerAuthStatergy)
      assertFalse(authorizationStatergy.isAuthorized(authRequest, jwtData));
    else
      assertTrue(authorizationStatergy.isAuthorized(authRequest, jwtData));
    assertNotNull(AuthorizationContextFactory.create(role, api));
    testContext.completeNow();
  }
}
