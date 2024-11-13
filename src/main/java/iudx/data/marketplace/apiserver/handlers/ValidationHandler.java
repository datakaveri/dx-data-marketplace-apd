package iudx.data.marketplace.apiserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.apiserver.validation.ValidationHandlerFactory;
import iudx.data.marketplace.apiserver.validation.types.Validator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ValidationHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(ValidationHandler.class);

  private RequestType requestType;
  private Vertx vertx;

  public ValidationHandler(Vertx vertx, RequestType requestType) {
    this.vertx = vertx;
    this.requestType = requestType;
  }

  @Override
  public void handle(RoutingContext context) {
    ValidationHandlerFactory validationFactory = new ValidationHandlerFactory();
    MultiMap parameters = context.request().params();
    JsonObject body = context.body().asJsonObject();
    Map<String, String> pathParams = context.pathParams();
    parameters.addAll(pathParams);

    List<Validator> validations = validationFactory.build(requestType, parameters, body);
    for (Validator validator : Optional.ofNullable(validations).orElse(Collections.emptyList())) {
      LOGGER.debug("validator : " + validator.getClass().getName());
      validator.isValid();
    }
    context.next();
  }
}
