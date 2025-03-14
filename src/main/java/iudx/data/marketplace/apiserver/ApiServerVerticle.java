package iudx.data.marketplace.apiserver;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.*;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import iudx.data.marketplace.dmpAuth.aaaService.AuthClient;
import iudx.data.marketplace.apiserver.provider.controller.ProviderApis;
import iudx.data.marketplace.apiserver.provider.linkedaccount.controller.LinkedAccountController;
import iudx.data.marketplace.authenticator.service.AuthenticationService;
import iudx.data.marketplace.common.*;
import iudx.data.marketplace.consumer.controller.ConsumerApis;
import iudx.data.marketplace.consumer.controller.PaymentVerificationController;
import iudx.data.marketplace.policies.controller.PolicyController;
import iudx.data.marketplace.postgres.service.PostgresService;
import iudx.data.marketplace.webhook.controller.WebhookController;
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
  private HttpServer server;
  private Router router;
  private int port;
  private PostgresService postgresService;
  private AuthClient authClient;
  private WebClient webClient;
  private WebClientOptions webClientOptions;
  private AuthenticationService authenticationService;

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
    allowedHeaders.add(HEADER_BEARER_AUTHORIZATION);

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);

    webClientOptions = new WebClientOptions();
    webClientOptions.setTrustAll(false).setVerifyHost(true).setSsl(true);
    webClient = WebClient.create(vertx, webClientOptions);

    /* Initialize service proxy */
    postgresService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);

    authClient = new AuthClient(config(), webClient);
    authenticationService = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
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
                                .getResponse());
                  });
            });

    router.route().handler(BodyHandler.create().setHandleFileUploads(false));
    router.route().handler(TimeoutHandler.create(30000, 408));

    HttpServerOptions serverOptions = new HttpServerOptions();
    setServerOptions(serverOptions);
    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    server = vertx.createHttpServer(serverOptions);
    server.requestHandler(router).listen(port);
    Api api = Api.getInstance(config().getString("dxApiBasePath"));
    String audience = config().getString("audience");
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
    router
        .route(POLICIES_API + "/*")
        .subRouter(new PolicyController(router, api, vertx, audience, authClient).policyHandler());
    router
        .route(CHECK_POLICY_PATH + "/*")
        .subRouter(
            new PolicyController(router, api, vertx, audience, authClient).checkPolicyHandler());
    router
        .route(VERIFY_PATH + "/*")
        .subRouter(new PolicyController(router, api, vertx, audience, authClient).verifyHandler());

    router
        .route(ACCOUNTS_API + "/*")
        .subRouter(new LinkedAccountController(router, api, vertx, authClient).init());
    router
        .route(PAYMENT_AUTHORIZED_PATH + "/*")
        .subRouter(new WebhookController(router, vertx).paymentAuthorizedHandler());
    router
        .route(ORDER_PAID_WEBHOOK_PATH + "/*")
        .subRouter(new WebhookController(router, vertx).orderPaidHandler());
    router
        .route(PAYMENTS_FAILED_PATH + "/*")
        .subRouter(new WebhookController(router, vertx).paymentFailedHandler());

    router
        .route(VERIFY_PAYMENTS_PATH + "/*")
        .subRouter(new PaymentVerificationController(router, api, vertx, authClient).init());

    //    router
    //        .post(api.getProductUserMapsPath())
    //        .handler(this::mapUserToProduct)
    //        .failureHandler(exceptionHandler);

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
    LOGGER.info("API server deployed on: {}", port);
  }

  /**
   * starts an HTTP server with the specified HTTP port. If the HTTP port is not specified in the
   * configuration, default ports (8080 for HTTP) will be used.
   *
   * @param serverOptions The server options to be configured.
   */
  private void setServerOptions(HttpServerOptions serverOptions) {
    LOGGER.debug("Info: Starting HTTP server");
    serverOptions.setSsl(false);
    port = config().getInteger("httpPort") == null ? 8080 : config().getInteger("httpPort");
  }

  private void printDeployedEndpoints(Router router) {
    for (Route route : router.getRoutes()) {
      if (route.getPath() != null) {
        LOGGER.debug("API Endpoints deployed : {} : {}",  route.methods(), route.getPath());
      }
    }
  }

  //  private void mapUserToProduct(RoutingContext routingContext) {}
}
