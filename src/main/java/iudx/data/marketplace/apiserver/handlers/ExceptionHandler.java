package iudx.data.marketplace.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.RespBuilder;
import iudx.data.marketplace.common.ResponseUrn;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.apiserver.util.Constants.*;

public class ExceptionHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(ExceptionHandler.class);

  @Override
  public void handle(RoutingContext routingContext) {

    Throwable failure = routingContext.failure();

    if (failure instanceof DxRuntimeException) {
      DxRuntimeException exception = (DxRuntimeException) failure;
      LOGGER.error(exception.getUrn().getUrn() + " : " + exception.getMessage());
      HttpStatusCode code = HttpStatusCode.getByValue(exception.getStatusCode());

      String response =
          new RespBuilder()
              .withType(exception.getUrn().getUrn())
              .withTitle(code.getDescription())
              .withDetail(exception.getMessage())
              .getResponse();

      routingContext
          .response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(exception.getStatusCode())
          .end(response);
    } else if (failure instanceof DecodeException) {
      handleDecodeException(routingContext);
      return;
    } else if (failure instanceof ClassCastException) {
      handleClassCastException(routingContext);
      return;
    } else if (failure instanceof RuntimeException) {
      routingContext
          .response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(HttpStatus.SC_BAD_REQUEST)
          .end(validationFailureReponse(MSG_BAD_QUERY).toString());

    } else {
      String INTERNAL_ERROR_RESP =
        new RespBuilder()
            .withType(ResponseUrn.INTERNAL_SERVER_ERR_URN.getUrn())
            .withTitle(ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage())
            .getResponse();

      routingContext
          .response()
          .setStatusCode(500)
          .putHeader(HEADER_CONTENT_TYPE, APPLICATION_JSON)
          .end(INTERNAL_ERROR_RESP);

      routingContext.next();

    }
    routingContext.next();
  }

  private JsonObject validationFailureReponse(String message) {
    return new JsonObject()
        .put(JSON_TYPE, HttpStatus.SC_BAD_REQUEST)
        .put(JSON_TITLE, "Bad Request")
        .put(JSON_DETAIL, message);
  }

  /**
   * Handles the JsonDecode Exception.
   *
   * @param routingContext
   */
  public void handleDecodeException(RoutingContext routingContext) {

    LOGGER.error("Error: Invalid Json payload: " + routingContext.failure().getLocalizedMessage());
    String response = "";

    if (routingContext.request().uri().startsWith(PROVIDER_BASE_PATH)) {

      response =
          new RespBuilder()
              .withType(ResponseUrn.INVALID_SCHEMA_URN.getUrn())
              .withTitle(ResponseUrn.INVALID_SCHEMA_URN.getMessage())
              .withDetail("Invalid Json payload")
              .getResponse();

      routingContext
          .response()
          .setStatusCode(400)
          .putHeader(HEADER_CONTENT_TYPE, APPLICATION_JSON)
          .end(response);

      routingContext.next();
      return;
    }


  }

  /**
   * Handles the exception from casting a object to different object.
   *
   * @param routingContext
   */
  public void handleClassCastException(RoutingContext routingContext) {

    LOGGER.error(
        "Error: Invalid request payload; " + routingContext.failure().getLocalizedMessage());

    routingContext
        .response()
        .setStatusCode(400)
        .putHeader(HEADER_CONTENT_TYPE, APPLICATION_JSON)
        .end(
            new JsonObject().put(JSON_TYPE, TYPE_FAIL).put(JSON_TITLE, "Invalid payload").encode());

    routingContext.next();
  }
}
