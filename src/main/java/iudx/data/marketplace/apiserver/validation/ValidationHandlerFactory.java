package iudx.data.marketplace.apiserver.validation;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.apiserver.validation.types.JsonSchemaTypeValidator;
import iudx.data.marketplace.apiserver.validation.types.ProductIDTypeValidator;
import iudx.data.marketplace.apiserver.validation.types.Validator;
import iudx.data.marketplace.apiserver.validation.types.VariantNameTypeValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static iudx.data.marketplace.apiserver.util.Constants.PRODUCT_ID;
import static iudx.data.marketplace.apiserver.util.Constants.PRODUCT_VARIANT_NAME;

public class ValidationHandlerFactory {
  private static final Logger LOGGER = LogManager.getLogger(ValidationHandlerFactory.class);

  public List<Validator> build(
      final RequestType requestType, final MultiMap parameters, final JsonObject body) {
    LOGGER.debug("getValidation4Context() started for : " + requestType);
    LOGGER.debug("type : " + requestType);
    List<Validator> validator = null;

    switch (requestType) {
      case PRODUCT:
        validator = getProductValidators(parameters, body, requestType);
        break;
      case PRODUCT_VARIANT:
        validator = getProductVariantValidators(parameters, body, requestType);
    }
    return validator;
  }

  private List<Validator> getProductValidators(
      final MultiMap parameters, final JsonObject body, final RequestType requestType) {
    List<Validator> validators = new ArrayList<>();

    if (body == null || body.isEmpty()) {
      validators.add(new ProductIDTypeValidator(parameters.get(PRODUCT_ID), true));
    } else {
      validators.add(new JsonSchemaTypeValidator(body, requestType));
    }

    return validators;
  }

  private List<Validator> getProductVariantValidators(
      final MultiMap parameters, final JsonObject body, final RequestType requestType) {
    List<Validator> validators = new ArrayList<>();

    if (body == null || body.isEmpty()) {
      validators.add(new ProductIDTypeValidator(parameters.get(PRODUCT_ID), true));
      validators.add(new VariantNameTypeValidator(parameters.get(PRODUCT_VARIANT_NAME), true));
    } else {
      validators.add(new JsonSchemaTypeValidator(body, requestType));
    }

    return validators;
  }
}
