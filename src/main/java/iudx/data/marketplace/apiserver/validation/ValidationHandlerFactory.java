package iudx.data.marketplace.apiserver.validation;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.apiserver.validation.types.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static iudx.data.marketplace.apiserver.util.Constants.*;

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
        break;
      case RESOURCE:
        validator = getResourceIDValidators(parameters);
        break;
      case PROVIDER:
        validator = getProviderIDValidators(parameters);
        break;
      case POLICY:
        validator: getPolicyValidators(parameters);
        break;
      case VERIFY:
        validator: getVerifyPolicyValidator(parameters, body);
        break;

    }
    return validator;
  }

  private List<Validator> getVerifyPolicyValidator(MultiMap parameters, JsonObject body) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new JsonSchemaTypeValidator(body, RequestType.VERIFY));
    return validators;
  }

  private List<Validator> getPolicyValidators(final MultiMap parameters){
    List<Validator> validators = new ArrayList<>();
    validators.add(new PolicyIdTypeValidator(parameters.get(POLICY_ID), true));
    return validators;
  }
  private List<Validator> getResourceIDValidators(final MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new ResourceIDTypeValidator(parameters.get(RESOURCE_ID), false));
    validators.add(new ProviderIDTypeValidator(parameters.get(PROVIDER_ID), false));
    return validators;
  }

  private List<Validator> getProviderIDValidators(final MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new ProviderIDTypeValidator(parameters.get(PROVIDER_ID), false));
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
