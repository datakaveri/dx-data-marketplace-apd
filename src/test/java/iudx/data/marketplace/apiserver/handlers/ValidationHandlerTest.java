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
  static MultiMap headerMap;
  static int numberOfInvocations;
  JsonObject jsonObject;
  ValidationHandler validationHandler;

  @BeforeAll
  public static void setup(VertxTestContext testContext) {
    routingContext = mock(RoutingContext.class);
    httpServerRequest = mock(HttpServerRequest.class);
    requestBody = mock(RequestBody.class);
    headerMap = mock(MultiMap.class);
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
        Arguments.of(RequestType.CHECK_POLICY, map, new JsonObject()),
        Arguments.of(
            RequestType.VERIFY,
            map,
            new JsonObject()
                .put(
                    "user",
                    new JsonObject()
                        .put("id", "ad47186b-3497-4248-ac1e-082e4d37a66c")
                        .put(
                            "name",
                            new JsonObject()
                                .put("firstName", "DMP-APD")
                                .put("lastName", "Unit-test-Consumer"))
                        .put("email", "dummy-consumer@datakaveri.org"))
                .put(
                    "owner",
                    new JsonObject()
                        .put("id", "685e222b-3fae-4325-9db0-3e29d01c4fc1")
                        .put(
                            "name",
                            new JsonObject()
                                .put("firstName", "Provider")
                                .put("lastName", "DMP-APD-Unit-Test-Provider"))
                        .put("email", "dummy@email.com"))
                .put(
                    "item",
                    new JsonObject()
                        .put("itemId", "695e222b-3fae-4325-8db0-3e29d01c4fc0")
                        .put("itemType", "RESOURCE"))
                .put("context", new JsonObject().put("orderId", "order_UDPminEt6aA0M2"))),
        Arguments.of(RequestType.CONSUMER_PRODUCT_VARIANT, map, new JsonObject()),
        Arguments.of(RequestType.PURCHASE, map, new JsonObject()),
        Arguments.of(
            RequestType.ORDER_PAID_WEBHOOK,
            map,
            new JsonObject()
                .put("entity", "event")
                .put("account_id", "acc_dummy123")
                .put("event", "order.paid")
                .put("contains", new JsonArray().add("order").add("payment"))
                .put(
                    "payload",
                    new JsonObject()
                        .put(
                            "payment",
                            new JsonObject()
                                .put(
                                    "entity",
                                    new JsonObject()
                                        .put("id", "pay_dummy456")
                                        .put("entity", "payment")
                                        .put("amount", 1000)
                                        .put("currency", "INR")
                                        .put("status", "successful")
                                        .put("order_id", "order_dummy789")
                                        .put("invoice_id", null)
                                        .put("international", false)
                                        .put("method", "credit_card")
                                        .put("amount_refunded", null)
                                        .put("refund_status", null)
                                        .put("captured", true)
                                        .put("description", null)
                                        .put("card_id", null)
                                        .put("bank", null)
                                        .put("wallet", null)
                                        .put("vpa", null)
                                        .put("email", "dummy@example.com")
                                        .put("contact", "+911234567890")
                                        .put(
                                            "notes",
                                            new JsonObject()
                                                .put("note1", "First note")
                                                .put("note2", "Second note"))
                                        .put("fee", null)
                                        .put("tax", null)
                                        .put("error_code", null)
                                        .put("error_description", null)
                                        .put("error_source", null)
                                        .put("error_step", null)
                                        .put("error_reason", null)
                                        .put("created_at", 1633036800)
                                        .put("reward", null)
                                        .put("upi", new JsonObject().put("vpa", null))))
                        .put(
                            "order",
                            new JsonObject()
                                .put(
                                    "entity",
                                    new JsonObject()
                                        .put("id", "order_dummy789")
                                        .put("entity", "order")
                                        .put("amount", 1000)
                                        .put("amount_paid", 1000)
                                        .put("amount_due", 0)
                                        .put("currency", "INR")
                                        .put("receipt", null)
                                        .put("offer_id", null)
                                        .put("status", "completed")
                                        .put("attempts", null)
                                        .put("notes", null)
                                        .put("created_at", 1633036800))))
                .put("created_at", 1633036800)),
        Arguments.of(RequestType.PUT_ACCOUNT, map, new JsonObject()
            .put("phone", "9876543210")
            .put("legalBusinessName", "Tech Innovations Pvt Ltd")
            .put("customerFacingBusinessName", "Tech Innovations")
            .put("contactName", "John Smith")
            .put("profile", new JsonObject()
                .put("category", "technology")
                .put("subcategory", "software_development")
                .put("addresses", new JsonObject()
                    .put("registered", new JsonObject()
                        .put("street1", "101 Silicon Valley Road")
                        .put("street2", "Block B, Floor 5")
                        .put("city", "Bengaluru")
                        .put("state", "KARNATAKA")
                        .put("postalCode", "560001")
                        .put("country", "IN")
                    )
                )
            )
            .put("legalInfo", new JsonObject()
                .put("pan", "ABCDE1234F")
                .put("gst", "18AABCU9603R1ZM")
            )),
        Arguments.of(RequestType.POST_ACCOUNT, map, new JsonObject()
            .put("phone", "9988776655")
            .put("legalBusinessName", "Apex Consulting & Solutions LLP")
            .put("customerFacingBusinessName", "Apex Solutions")
            .put("businessType", "llp")
            .put("contactName", "Sarah Chen")
            .put("profile", new JsonObject()
                .put("category", "consulting")
                .put("subcategory", "business_management")
                .put("addresses", new JsonObject()
                    .put("registered", new JsonObject()
                        .put("street1", "45, Electronic City")
                        .put("street2", "Phase 1, Hosur Road")
                        .put("city", "Bengaluru")
                        .put("state", "KARNATAKA")
                        .put("postalCode", "560100")
                        .put("country", "IN")
                    )
                )
            )
            .put("legalInfo", new JsonObject()
                .put("pan", "FGHJK6789L")
                .put("gst", "29XYZAB1234C1Z5")
            )),
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
    when(routingContext.request().headers()).thenReturn(headerMap);
    when(headerMap.get("Authorization")).thenReturn("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");

    when(routingContext.body()).thenReturn(requestBody);
    when(routingContext.body().asJsonObject()).thenReturn(req);
    when(routingContext.pathParams()).thenReturn(hashMap);
    validationHandler = new ValidationHandler(requestType);
    validationHandler.handle(routingContext);
    verify(routingContext, times(numberOfInvocations*2)).request();
    verify(routingContext, times(numberOfInvocations)).body();
    testContext.completeNow();
  }
}
