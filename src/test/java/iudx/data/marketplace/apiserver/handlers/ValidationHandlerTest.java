package iudx.data.marketplace.apiserver.handlers;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.data.marketplace.apiserver.util.RequestType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ValidationHandlerTest {
  RequestType requestType;
  static RoutingContext routingContext;
  static HttpServerRequest httpServerRequest;
  static RequestBody requestBody;
  static MultiMap map;
  static int numberOfInvocations;
  JsonObject jsonObject;
  ValidationHandler validationHandler;

  @BeforeAll
  public static void setup(VertxTestContext testContext) {
    routingContext = mock(RoutingContext.class);
    httpServerRequest = mock(HttpServerRequest.class);
    requestBody = mock(RequestBody.class);
    map = MultiMap.caseInsensitiveMultiMap()
        .set("productVariantId", "695e222b-3fae-4325-8db0-3e29d01c4fc0")
        .set("productId", "urn:datakaveri.org:c1757ee9-a168-4cbf-aac7-3e4ff6faaae2:abcbd")
        .set("resourceId", "c1757ee9-a168-4cbf-aac7-3e4ff6faaae2")
        .set("providerId", "d2757ee9-a168-4cbf-aac7-3e4ff6faaae2");
    numberOfInvocations = 0;
    testContext.completeNow();
  }

  @BeforeEach
  void incrementNumberOfInvocations()
  {
    numberOfInvocations+=2;
  }

  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of(
            RequestType.PRODUCT,
            map,
            new JsonObject()
                .put("productId", "newid")
                .put("resourceIds", new JsonArray().add("695e222b-3fae-4325-8db0-3e29d01c4fc0"))),
        Arguments.of(
            RequestType.PRODUCT_VARIANT,
            map,
            new JsonObject()
                .put("productId", "urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:newid")
                .put("productVariantName", "var1")
                .put(
                    "resources",
                    new JsonArray()
                        .add(
                            new JsonObject()
                                .put("id", "695e222b-3fae-4325-8db0-3e29d01c4fc0")
                                .put("capabilities", new JsonArray().add("api").add("sub"))))
                .put("price", 100.0)
                .put("duration", 12)),
        Arguments.of(RequestType.DELETE_PRODUCT_VARIANT, map, new JsonObject()),
        Arguments.of(RequestType.LIST_PRODUCT_VARIANT, map, new JsonObject()),
        Arguments.of(RequestType.RESOURCE, map, new JsonObject()),
        Arguments.of(RequestType.PROVIDER, map, new JsonObject()),
        Arguments.of(
            RequestType.POLICY,
            map,
            new JsonObject().put("policyId", "98f05e95-68c7-4f8a-a150-0cfbe9db7f5e")),
        Arguments.of(RequestType.ORDER, map, new JsonObject()),
        Arguments.of(
            RequestType.VERIFY_PAYMENT,
            map,
            new JsonObject()
                .put("razorpay_order_id", "order_sdjfnsdjfn")
                .put("razorpay_payment_id", "pay_nsdcsdnihwnc")
                .put(
                    "razorpay_signature",
                    "afecace3d9969229b4c76a08c3c38fa771b6ed5430e5eebd4d7cf794da62b9ea")));
  }

  @ParameterizedTest
  @DisplayName("Test Handle Method")
  @MethodSource("data")
  public void testHandleMethod(
      RequestType value, MultiMap reqMap, JsonObject req, Vertx vertx, VertxTestContext testContext) {
    requestType = value;
    Map<String, String> hashMap = new HashMap<>();
    hashMap.put("id", "product-id");

    when(routingContext.request()).thenReturn(httpServerRequest);
    when(routingContext.request().params()).thenReturn(reqMap);
    when(routingContext.body()).thenReturn(requestBody);
    when(routingContext.body().asJsonObject()).thenReturn(req);
    when(routingContext.pathParams()).thenReturn(hashMap);
    validationHandler = new ValidationHandler(requestType);
    validationHandler.handle(routingContext);
    verify(routingContext, times(numberOfInvocations)).request();
    verify(routingContext, times(numberOfInvocations)).body();
    testContext.completeNow();
  }
}
