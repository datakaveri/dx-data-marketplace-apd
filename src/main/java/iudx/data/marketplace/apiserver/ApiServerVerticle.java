package iudx.data.marketplace.apiserver;

import static iudx.data.marketplace.apiserver.response.ResponseUtil.generateResponse;
import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.Constants.*;
import static iudx.data.marketplace.common.HttpStatusCode.BAD_REQUEST;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.*;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import iudx.data.marketplace.apiserver.handlers.AuthHandler;
import iudx.data.marketplace.apiserver.handlers.ExceptionHandler;
import iudx.data.marketplace.apiserver.handlers.ValidationHandler;
import iudx.data.marketplace.apiserver.provider.linkedAccount.LinkedAccountService;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.auditing.AuditingService;
import iudx.data.marketplace.authenticator.AuthClient;
import iudx.data.marketplace.authenticator.AuthenticationService;
import iudx.data.marketplace.common.*;
import iudx.data.marketplace.policies.PolicyService;
import iudx.data.marketplace.policies.User;
import iudx.data.marketplace.postgres.PostgresService;
import iudx.data.marketplace.razorpay.RazorPayService;
import iudx.data.marketplace.webhook.WebhookService;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Data Marketplace API Verticle.
 *
 * <h1>Data Marketplace APi Verticle</h1>
 *
 * <p>The API Server verticle implements the IUDX Data Marketplace APIs. It handles the API requests
 * from the clients and interacts with the associated service to respond.
 *
 * @version 1.0
 * @since 2022-08-04
 */
public class ApiServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);
  private PolicyService policyService;
  private HttpServer server;
  private Router router;
  private String detail;
  private int port;
  private boolean isSSL;
  private String keystore;
  private String keystorePassword;
  private PostgresService postgresService;
  private RazorPayService razorPayService;
  private AuthClient authClient;
  private WebClient webClient;
  private WebClientOptions webClientOptions;
  private AuthenticationService authenticationService;
  private LinkedAccountService linkedAccountService;
  private AuditingService auditingService;
  private WebhookService webhookService;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, reads the
   * configuration, obtains a proxy for the Event Bus services exposed through service discovery,
   * start an HTTPs server at port //TODO: port number
   *
   * @throws Exception which is a startup exception
   */
  @Override
  public void start() throws Exception {

    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add(HEADER_ACCEPT);
    allowedHeaders.add(HEADER_TOKEN);
    allowedHeaders.add(HEADER_CONTENT_LENGTH);
    allowedHeaders.add(HEADER_CONTENT_TYPE);
    allowedHeaders.add(HEADER_HOST);
    allowedHeaders.add(HEADER_ORIGIN);
    allowedHeaders.add(HEADER_REFERER);
    allowedHeaders.add(HEADER_ALLOW_ORIGIN);

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);

    webClientOptions = new WebClientOptions();
    webClientOptions.setTrustAll(false).setVerifyHost(true).setSsl(true);
    webClient = WebClient.create(vertx, webClientOptions);

    Api api = Api.getInstance(config().getString("dxApiBasePath"));

    /* Initialize service proxy */
    policyService = PolicyService.createProxy(vertx, POLICY_SERVICE_ADDRESS);
    postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    razorPayService = RazorPayService.createProxy(vertx, RAZORPAY_SERVICE_ADDRESS);

    authClient = new AuthClient(config(), webClient);
    authenticationService = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    linkedAccountService = LinkedAccountService.createProxy(vertx, LINKED_ACCOUNT_ADDRESS);
    webhookService = WebhookService.createProxy(vertx, WEBHOOK_SERVICE_ADDRESS);
    router = Router.router(vertx);

    router
        .route()
        .handler(
            CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));

    router
        .route()
        .handler(
            requestHandler -> {
              requestHandler
                  .response()
                  .putHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0")
                  .putHeader("Pragma", "no-cache")
                  .putHeader("Expires", "0")
                  .putHeader("X-Content-Type-Options", "nosniff");
              requestHandler.next();
            });

    // attach custom http error responses to router
    HttpStatusCode[] statusCodes = HttpStatusCode.values();
    Stream.of(statusCodes)
        .forEach(
            code -> {
              router.errorHandler(
                  code.getValue(),
                  errorHandler -> {
                    HttpServerResponse response = errorHandler.response();
                    if (response.headWritten()) {
                      try {
                        response.close();
                      } catch (RuntimeException e) {
                        LOGGER.error("Error : " + e);
                      }
                      return;
                    }
                    response
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(code.getValue())
                        .end(
                            new RespBuilder()
                                .withType(code.getUrn())
                                .withTitle(code.getDescription())
                                .withDetail(code.getDescription())
                                .toString());
                  });
            });

    router.route().handler(BodyHandler.create().setHandleFileUploads(false));
    router.route().handler(TimeoutHandler.create(30000, 408));
    isSSL = config().getBoolean("ssl");

    HttpServerOptions serverOptions = new HttpServerOptions();
    if (isSSL) {
      port = config().getInteger("httpPort") == null ? 8443 : config().getInteger("httpPort");
      keystore = config().getString("keystore");
      keystorePassword = config().getString("keystorePassword");

      serverOptions
          .setSsl(true)
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));
      LOGGER.info("Info: Starting HTTPs server at port " + port);
    } else {
      serverOptions.setSsl(false);
      port = config().getInteger("httpPort") == null ? 8080 : config().getInteger("httpPort");
      LOGGER.info("Info: Starting HTTP server at port " + port);
    }

    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    server = vertx.createHttpServer(serverOptions);
    server.requestHandler(router).listen(port);

    //  Documentation routes

    /* Static Resource Handler */
    /* Get openapiv3 spec */
    router
        .get(ROUTE_STATIC_SPEC)
        .produces(APPLICATION_JSON)
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/openapi.yaml");
            });
    /* Get redoc */
    router
        .get(ROUTE_DOC)
        .produces(MIME_TEXT_HTML)
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/apidoc.html");
            });

    router
        .route(PROVIDER_PATH + "/*")
        .subRouter(
            new ProviderApis(vertx, router, api, postgresService, authClient, authenticationService)
                .init());
    router
        .route(CONSUMER_PATH + "/*")
        .subRouter(
            new ConsumerApis(vertx, router, api, postgresService, authClient, authenticationService)
                .init());

    ExceptionHandler exceptionHandler = new ExceptionHandler();
    ValidationHandler policyValidationHandler = new ValidationHandler(vertx, RequestType.POLICY);
    ValidationHandler verifyValidationHandler = new ValidationHandler(vertx, RequestType.VERIFY);
    ValidationHandler postLinkedAccountHandler =
        new ValidationHandler(vertx, RequestType.POST_ACCOUNT);
    ValidationHandler putLinkedAccountHandler =
        new ValidationHandler(vertx, RequestType.PUT_ACCOUNT);

    router
        .get(api.getPoliciesUrl())
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::getPoliciesHandler)
        .failureHandler(exceptionHandler);
    router.post(api.getPoliciesUrl()).handler(this::createPolicy).failureHandler(exceptionHandler);

    router
        .delete(api.getPoliciesUrl())
        .handler(policyValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::deletePoliciesHandler)
        .failureHandler(exceptionHandler);

    router
        .post(api.getProductUserMapsPath())
        .handler(this::mapUserToProduct)
        .failureHandler(exceptionHandler);

    router
        .post(api.getVerifyUrl())
        .handler(verifyValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::handleVerify)
        .failureHandler(exceptionHandler);

    ValidationHandler verifyPaymentValidationHandler =
        new ValidationHandler(vertx, RequestType.VERIFY_PAYMENT);
    router
        .post(api.getVerifyPaymentApi())
        .handler(verifyPaymentValidationHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::handleVerifyPayment)
        .failureHandler(exceptionHandler);

    router
        .post(api.getLinkedAccountService())
        .handler(postLinkedAccountHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::handlePostLinkedAccount)
        .failureHandler(exceptionHandler);

    router
        .put(api.getLinkedAccountService())
        .handler(putLinkedAccountHandler)
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::handlePutLinkedAccount)
        .failureHandler(exceptionHandler);

    router
        .get(api.getLinkedAccountService())
        .handler(AuthHandler.create(authenticationService, api, postgresService, authClient))
        .handler(this::handleFetchLinkedAccount)
        .failureHandler(exceptionHandler);

    /*Webhook routes */

    ValidationHandler orderPaidRequestValidationHandler =
        new ValidationHandler(vertx, RequestType.ORDER_PAID_WEBHOOK);
    router
        .post("/order-paid-webhook")
        .handler(this::handleWebhookSignatureValidation)
        .handler(orderPaidRequestValidationHandler)
        .handler(this::orderPaidRequestHandler)
        .failureHandler(exceptionHandler);

    ValidationHandler paymentAuthorizedRequestValidationHandler =
        new ValidationHandler(vertx, RequestType.PAYMENT_AUTHORIZED_WEBHOOK);
    router
        .post("/payment-authorized")
        .handler(this::handleWebhookSignatureValidation)
        .handler(paymentAuthorizedRequestValidationHandler)
        .handler(this::paymentAuthorizedRequestHandler);

    ValidationHandler paymentFailedRequestValidationHandler =
        new ValidationHandler(vertx, RequestType.PAYMENT_FAILED_WEBHOOK);
    router
        .post("/payment-failed")
        .handler(this::handleWebhookSignatureValidation)
        .handler(paymentFailedRequestValidationHandler)
        .handler(this::paymentFailedRequestHandler);

    //  Documentation routes

    /* Static Resource Handler */
    /* Get openapiv3 spec */
    router
        .get(ROUTE_STATIC_SPEC)
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/openapi.yaml");
            });
    /* Get redoc */
    router
        .get(ROUTE_DOC)
        .produces(MIME_TEXT_HTML)
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/apidoc.html");
            });

    printDeployedEndpoints(router);
    /* Print the deployed endpoints */
    LOGGER.info("API server deployed on: " + port);
  }

  private void handleWebhookSignatureValidation(RoutingContext routingContext) {

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String xRazorpaySignature = request.headers().get(HEADER_X_RAZORPAY_SIGNATURE);

    razorPayService
        .webhookSignatureValidator(requestBody, xRazorpaySignature)
        .onSuccess(
            requestValidated -> {
              LOGGER.debug("Request Validated");
              routingContext.next();
            })
        .onFailure(
            requestInvalidated -> {
              LOGGER.error("Request Validation Failed");
              routingContext.next();
            });
  }

  private void paymentFailedRequestHandler(RoutingContext routingContext) {

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    LOGGER.debug(requestBody);
    String orderId =
        requestBody
            .getJsonObject(RAZORPAY_PAYLOAD)
            .getJsonObject(RAZORPAY_PAYMENT)
            .getJsonObject(RAZORPAY_ENTITY)
            .getString(RAZORPAY_ORDER_ID, "");
    webhookService
        .recordPaymentFailure(orderId)
        .onSuccess(
            statusUpdated -> {
              handleSuccessResponse(response, 200, "Payment status updated");
            })
        .onFailure(
            statusUpdateFailed -> {
              handleFailureResponse(routingContext, statusUpdateFailed.getMessage());
            });
  }

  private void paymentAuthorizedRequestHandler(RoutingContext routingContext) {

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    LOGGER.debug(requestBody);

    handleSuccessResponse(response, 200, requestBody.encode());
  }

  private void orderPaidRequestHandler(RoutingContext routingContext) {

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    String orderId =
        requestBody
            .getJsonObject(RAZORPAY_PAYLOAD)
            .getJsonObject(RAZORPAY_ORDER)
            .getJsonObject(RAZORPAY_ENTITY)
            .getString(RAZORPAY_ID, "");

    webhookService
        .recordOrderPaid(orderId)
        .onSuccess(
            policyCreated -> {
              handleSuccessResponse(response, 200, policyCreated.encode());
            })
        .onFailure(
            policyCreationFailed -> {
              handleFailureResponse(routingContext, policyCreationFailed.getMessage());
            });
  }

  private void handleVerifyPayment(RoutingContext routingContext) {

    LOGGER.debug("here 1");
    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();

    razorPayService
        .verifyPayment(requestBody)
        .onSuccess(
            paymentVerified -> {
              handleSuccessResponse(response, 200, paymentVerified.encode());
            })
        .onFailure(
            verifyFailed -> {
              handleFailureResponse(routingContext, verifyFailed.getMessage());
            });
  }

  private void createPolicy(RoutingContext routingContext) {
    JsonObject order = routingContext.body().asJsonObject();
    String orderId = order.getString("orderId");
    policyService
        .createPolicy(orderId)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Insertion Success");
              } else {
                handler.cause().printStackTrace();
                LOGGER.error("Failure : " + handler.cause().getMessage());
              }
            });
  }

  private void printDeployedEndpoints(Router router) {
    for (Route route : router.getRoutes()) {
      if (route.getPath() != null) {
        LOGGER.debug("API Endpoints deployed : " + route.methods() + " : " + route.getPath());
      }
    }
  }

  private void deletePoliciesHandler(RoutingContext routingContext) {
    JsonObject policy = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();

    User user = routingContext.get("user");
    policyService
        .deletePolicy(policy, user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Delete policy succeeded : {} ", handler.result().encode());
                JsonObject responseJson =
                    new JsonObject()
                        .put(TYPE, handler.result().getString(TYPE))
                        .put(TITLE, handler.result().getString(TITLE))
                        .put(DETAIL, handler.result().getValue(DETAIL));
                handleSuccessResponse(
                    response, handler.result().getInteger(STATUS_CODE), responseJson.toString());
              } else {
                LOGGER.error("Delete policy failed : {} ", handler.cause().getMessage());
                handleFailureResponse(routingContext, handler.cause().getMessage());
              }
            });
  }

  private void getPoliciesHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();

    User user = routingContext.get("user");
    policyService
        .getPolicies(user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                String result = handler.result().getJsonObject(RESULT).encode();
                handleSuccessResponse(response, handler.result().getInteger(STATUS_CODE), result);
              } else {
                handleFailureResponse(routingContext, handler.cause().getMessage());
              }
            });
  }

  private void mapUserToProduct(RoutingContext routingContext) {}

  private void handlePostLinkedAccount(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    User user = routingContext.get("user");
    HttpServerResponse response = routingContext.response();
    linkedAccountService
        .createLinkedAccount(requestBody, user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Linked account created successfully ");
                handleSuccessResponse(
                    response, HttpStatusCode.SUCCESS.getValue(), handler.result().toString());

              } else {
                LOGGER.error(
                    "Linked account could not be created {}", handler.cause().getMessage());
                handleFailureResponse(routingContext, handler.cause().getMessage());
              }
            });
  }

  private void handlePutLinkedAccount(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();
    User user = routingContext.get("user");

    linkedAccountService
        .updateLinkedAccount(requestBody, user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Linked account updated successfully ");
                handleSuccessResponse(
                    response, HttpStatusCode.SUCCESS.getValue(), handler.result().toString());
              } else {
                LOGGER.error(
                    "Linked account could not be updated {}", handler.cause().getMessage());
                handleFailureResponse(routingContext, handler.cause().getMessage());
              }
            });
  }

  private void handleFetchLinkedAccount(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    User user = routingContext.get("user");
    linkedAccountService
        .fetchLinkedAccount(user)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Linked account fetched successfully ");
                handleSuccessResponse(
                    response, HttpStatusCode.SUCCESS.getValue(), handler.result().toString());
              } else {
                LOGGER.error(
                    "Linked account could not be fetched {}", handler.cause().getMessage());
                handleFailureResponse(routingContext, handler.cause().getMessage());
              }
            });
  }

  private void handleVerify(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();
    policyService
        .verifyPolicy(requestBody)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.info("Policy verified successfully ");
                handleSuccessResponse(
                    response, HttpStatusCode.SUCCESS.getValue(), handler.result().toString());
              } else {
                LOGGER.error("Policy could not be verified {}", handler.cause().getMessage());
                handleFailureResponse(routingContext, handler.cause().getMessage());
              }
            });
  }

  /**
   * Handles HTTP Success response from the server
   *
   * @param response HttpServerResponse object
   * @param statusCode statusCode to respond with
   * @param result respective result returned from the service
   */
  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  /**
   * Handles Failed HTTP Response
   *
   * @param routingContext Routing context object
   * @param failureMessage Failure message for response
   */
  private void handleFailureResponse(RoutingContext routingContext, String failureMessage) {
    HttpServerResponse response = routingContext.response();
    LOGGER.debug("Failure Message : {} ", failureMessage);

    try {
      JsonObject jsonObject = new JsonObject(failureMessage);
      int type = jsonObject.getInteger(TYPE);
      String title = jsonObject.getString(TITLE);

      HttpStatusCode status = HttpStatusCode.getByValue(type);

      ResponseUrn urn;

      // get the urn by either type or title
      if (title != null) {
        urn = ResponseUrn.fromCode(title);
      } else {

        urn = ResponseUrn.fromCode(String.valueOf(type));
      }
      if (jsonObject.getString(DETAIL) != null) {
        detail = jsonObject.getString(DETAIL);
        response
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(type)
            .end(generateResponse(status, urn, detail).toString());
      } else {
        response
            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(type)
            .end(generateResponse(status, urn).toString());
      }

    } catch (DecodeException exception) {
      LOGGER.error("Error : Expecting JSON from backend service [ jsonFormattingException ] ");
      handleResponse(response, BAD_REQUEST, ResponseUrn.BACKING_SERVICE_FORMAT_URN);
    }
  }

  private void handleResponse(
      HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn) {
    handleResponse(response, statusCode, urn, statusCode.getDescription());
  }

  private void handleResponse(
      HttpServerResponse response,
      HttpStatusCode statusCode,
      ResponseUrn urn,
      String failureMessage) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, failureMessage).toString());
  }
}
