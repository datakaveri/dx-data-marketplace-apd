package iudx.data.marketplace.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.*;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import iudx.data.marketplace.apiserver.handlers.AuthHandler;
import iudx.data.marketplace.apiserver.handlers.ExceptionHandler;
import iudx.data.marketplace.common.Api;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import iudx.data.marketplace.policies.PolicyService;
import iudx.data.marketplace.policies.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static iudx.data.marketplace.apiserver.response.ResponseUtil.generateResponse;
import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.Constants.POLICY_SERVICE_ADDRESS;
import static iudx.data.marketplace.common.HttpStatusCode.BAD_REQUEST;

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
  private String detail;

  private int port;
  private boolean isSSL;
  private String keystore;
  private String keystorePassword;
  private PolicyService policyService;

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

    Api api = Api.getInstance(config().getString("dxApiBasePath"));
    policyService = PolicyService.createProxy(vertx, POLICY_SERVICE_ADDRESS);

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
    router.route().handler(TimeoutHandler.create(10000, 408));

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

    router.route(PROVIDER_BASE_PATH + "/*").subRouter(new ProviderApis(vertx, router).init());
    router.route(CONSUMER_BASE_PATH + "/*").subRouter(new ConsumerApis(vertx, router).init());

    ExceptionHandler exceptionHandler = new ExceptionHandler();


    router
            .put(api.getPoliciesUrl())
            .handler(AuthHandler.create(vertx))
            .handler(this::postPoliciesHandler)
            .failureHandler(exceptionHandler);

    router
            .get(api.getPoliciesUrl())
            .handler(AuthHandler.create(vertx))
            .handler(this::getPoliciesHandler)
            .failureHandler(exceptionHandler);

    router
            .delete(api.getPoliciesUrl())
            .handler(AuthHandler.create(vertx))
            .handler(this::deletePoliciesHandler)
            .failureHandler(exceptionHandler);

    router
        .post(USERMAPS_PATH)
        .handler(this::mapUserToProduct)
        .failureHandler(exceptionHandler);

    router
        .post(VERIFY_PATH)
        .handler(this::handleVerify)
        .failureHandler(exceptionHandler);

    //  Documentation routes

    /* Static Resource Handler */
    /* Get openapiv3 spec */
    router.get(ROUTE_STATIC_SPEC)
            .produces(MIME_APPLICATION_JSON)
            .handler(routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/openapi.yaml");
            });
    /* Get redoc */
    router.get(ROUTE_DOC)
            .produces(MIME_TEXT_HTML)
            .handler(routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/apidoc.html");
            });

    printDeployedEndpoints(router);
    /* Print the deployed endpoints */
    LOGGER.info("API server deployed on: " + port);
  }

  private void printDeployedEndpoints(Router router) {
    for (Route route : router.getRoutes()) {
      if (route.getPath() != null) {
        LOGGER.debug("API Endpoints deployed : " + route.methods() + " : " + route.getPath());
      }
    }
  }

  private void postPoliciesHandler(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();
    User user = routingContext.get("user");
    policyService
            .createPolicy(requestBody, user)
            .onComplete(
                    handler -> {
                      if (handler.succeeded()) {
                        LOGGER.info("Policy created successfully ");
                        handleSuccessResponse(
                                response, HttpStatusCode.SUCCESS.getValue(), handler.result().toString());
                      } else {
                        LOGGER.error("Policy could not be created");
                        handleFailureResponse(routingContext, handler.cause().getMessage());
                      }
                    });
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
            .getPolicy(user)
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
  private void mapUserToProduct(RoutingContext routingContext) {

  }

  private void handleVerify(RoutingContext routingContext) {

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
