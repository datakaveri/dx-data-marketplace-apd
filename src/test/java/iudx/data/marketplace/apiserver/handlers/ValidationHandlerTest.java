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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
  RoutingContext routingContext;
  HttpServerRequest httpServerRequest;
  RequestBody requestBody;
  MultiMap map;
  JsonObject jsonObject;
  ValidationHandler validationHandler;

  @BeforeEach
  public void setup(VertxTestContext testContext) {
    routingContext = mock(RoutingContext.class);
    httpServerRequest = mock(HttpServerRequest.class);
    requestBody = mock(RequestBody.class);
    map = mock(MultiMap.class);
    //    jsonObject = mock(JsonObject.class);
    testContext.completeNow();
  }

  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of(
            RequestType.PRODUCT,
            new JsonObject()
                .put("productId", "newid")
                .put(
                    "resourceIds",
                    new JsonArray()
                        .add(
                            "695e222b-3fae-4325-8db0-3e29d01c4fc0"))),
        Arguments.of(
            RequestType.PRODUCT_VARIANT,
            new JsonObject()
                .put("productId", "urn:datakaveri.org:b2c27f3f-2524-4a84-816e-91f9ab23f837:newid")
                .put("productVariantName", "var1")
                .put(
                    "resources",
                    new JsonArray()
                        .add(
                            new JsonObject()
                                .put(
                                    "id",
                                    "695e222b-3fae-4325-8db0-3e29d01c4fc0")
                                .put("capabilities", new JsonArray().add("api").add("sub"))))
                .put("price", 100.0)
                .put("duration", 12)));
  }

  @ParameterizedTest
  @DisplayName("Test Handle Method")
  @MethodSource("data")
  public void testHandleMethod(
      RequestType value, JsonObject req, Vertx vertx, VertxTestContext testContext) {
    requestType = value;
    jsonObject = req;
    Map<String, String> hashMap = new HashMap<>();
    hashMap.put("id", "product-id");

    when(routingContext.request()).thenReturn(httpServerRequest);
    when(routingContext.request().params()).thenReturn(map);
    when(routingContext.body()).thenReturn(requestBody);
    when(routingContext.body().asJsonObject()).thenReturn(jsonObject);
    when(routingContext.pathParams()).thenReturn(hashMap);
    validationHandler = new ValidationHandler(vertx, requestType);
    validationHandler.handle(routingContext);
    verify(routingContext, times(2)).request();
    verify(httpServerRequest).params();
    verify(routingContext, times(2)).body();
    verify(routingContext).pathParams();
    testContext.completeNow();
  }
}
