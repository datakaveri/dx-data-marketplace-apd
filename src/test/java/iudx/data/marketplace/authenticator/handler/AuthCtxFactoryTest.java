package iudx.data.marketplace.authenticator.handler;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import iudx.data.marketplace.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class AuthCtxFactoryTest {
  public static final Logger LOGGER = LogManager.getLogger(AuthCtxFactoryTest.class);
  @Mock AuthorizationRequest authRequest;
  @Mock JwtData jwtData;
  @Mock Method method;

  static Api apis = Api.getInstance("basePath");
  @Mock Api api;
  @Mock ConsumerAuthStatergy consumerAuthStatergy;
  AuthorizationContextFactory authorizationContextFactory;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext)
  {
    authorizationContextFactory = new AuthorizationContextFactory();
    vertxTestContext.completeNow();
  }
  @Test
  @DisplayName("testing the method create when role is consumer")
  public void testCreate(VertxTestContext vertxTestContext) {
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
    IudxRole role = IudxRole.DELEGATE;
    Exception exception = assertThrows(IllegalArgumentException.class,() ->AuthorizationContextFactory.create(role, api));
    assertEquals("DELEGATE role is not defined in IUDX", exception.getMessage());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("testing the method create when role is null")
  public void testWhenRoleIsNull(VertxTestContext vertxTestContext) {
    IudxRole role = null;
    Exception exception = assertThrows(IllegalArgumentException.class,() ->AuthorizationContextFactory.create(role, api));
    assertEquals("null role is not defined in IUDX", exception.getMessage());
    vertxTestContext.completeNow();
  }


  public static Stream<Arguments> roles() {
    return Stream.of(
        Arguments.of(ProviderAuthStatergy.getInstance(apis),IudxRole.PROVIDER),
        Arguments.of(ConsumerAuthStatergy.getInstance(apis), IudxRole.CONSUMER));
  }

  @ParameterizedTest
  @MethodSource("roles")
  @DisplayName("test user with x role is authorized")
  public void testIsAuthorized(
      AuthorizationStatergy authorizationStatergy, IudxRole role, VertxTestContext testContext) {

    lenient().when(api.getProviderProductPath()).thenReturn("somePath");
    lenient().when(api.getConsumerListProviders()).thenReturn("someOtherPath");
    lenient().when(authRequest.getApi()).thenReturn("dummyString");
    lenient().when(authRequest.getMethod()).thenReturn(method);
    if(role.equals(IudxRole.PROVIDER))
    {
      authRequest = new AuthorizationRequest(Method.POST, api.getProviderProductPath());
    }
    else
    {
      authRequest = new AuthorizationRequest(Method.POST, api.getConsumerListProviders());
    }
    lenient().when(jwtData.getCons()).thenReturn(new JsonObject().put("access", new JsonArray()));
    if (authorizationStatergy instanceof ConsumerAuthStatergy)
      assertFalse(authorizationStatergy.isAuthorized(authRequest, jwtData));
    else
      assertFalse(authorizationStatergy.isAuthorized(authRequest, jwtData));
    assertNotNull(AuthorizationContextFactory.create(role, api));
    testContext.completeNow();
  }
}
