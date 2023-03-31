package iudx.data.marketplace.apiserver.validation;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.apiserver.validation.types.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static iudx.data.marketplace.apiserver.util.Constants.PRODUCT_ID;
import static iudx.data.marketplace.apiserver.util.Constants.PRODUCT_VARIANT_NAME;
import static iudx.data.marketplace.apiserver.util.Constants.DATASET_ID;

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
      case DATASET:
        validator = getDatasetIDValidators(parameters, requestType);
    }
    return validator;
  }

  private List<Validator> getDatasetIDValidators(final MultiMap parameters, final RequestType requestType) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new DatasetIDTypeValidator(parameters.get(DATASET_ID), true));
    return validators;
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
