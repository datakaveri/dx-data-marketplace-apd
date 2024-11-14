package iudx.data.marketplace.apiserver.validation;

import static iudx.data.marketplace.apiserver.util.Constants.*;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import iudx.data.marketplace.apiserver.util.RequestType;
import iudx.data.marketplace.apiserver.validation.types.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
      case DELETE_PRODUCT_VARIANT:
        validator = getDeleteProductVariantValidators(parameters);
        break;
      case LIST_PRODUCT_VARIANT:
        validator = listProductVariantValidators(parameters);
        break;
      case RESOURCE:
        validator = getResourceIdValidators(parameters);
        break;
      case PROVIDER:
        validator = getProviderIdValidators(parameters);
        break;
      case POLICY:
        validator = getPolicyValidators(body);
        break;
      case VERIFY:
        validator = getVerifyPolicyValidator(body);
        break;
      case ORDER:
        validator = getOrderValidator(parameters);
        break;
      case VERIFY_PAYMENT:
        validator = getVerfiyPaymentValidator(body);
        break;
      case POST_ACCOUNT:
        validator = getPostLinkedAccountValidator(body, requestType);
        break;
      case PUT_ACCOUNT:
        validator = getPutLinkedAccountValidator(body, requestType);
        break;
      case ORDER_PAID_WEBHOOK:
        validator = getPaymentWebhookValidator(body);
        break;
      case PURCHASE:
        validator = getPurchaseValidator(parameters);
        break;
      case CONSUMER_PRODUCT_VARIANT:
        validator = getConsumerProductVariantValidator(parameters);
        break;
      case CHECK_POLICY:
        validator = getCheckPolicyValidator(parameters);
        break;
      default:
        break;
    }
    return validator;
  }

  private List<Validator> getCheckPolicyValidator(MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new UuidTypeValidator(parameters.get(PRODUCT_VARIANT_ID), true));
    return validators;
  }

  private List<Validator> getPaymentWebhookValidator(JsonObject body) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new JsonSchemaTypeValidator(body, RequestType.ORDER_PAID_WEBHOOK));
    return validators;
  }

  private List<Validator> getConsumerProductVariantValidator(MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new ProductIdTypeValidator(parameters.get("productId"), true));
    return validators;
  }

  private List<Validator> getPurchaseValidator(MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new UuidTypeValidator(parameters.get("resourceId"), false));
    validators.add(new ProductIdTypeValidator(parameters.get("productId"), false));
    validators.add(new OrderIdTypeValidator(parameters.get("orderId"), false));
    return validators;
  }

  private List<Validator> getVerfiyPaymentValidator(JsonObject body) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new JsonSchemaTypeValidator(body, RequestType.VERIFY_PAYMENT));
    return validators;
  }

  private List<Validator> getPostLinkedAccountValidator(JsonObject body, RequestType requestType) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new JsonSchemaTypeValidator(body, requestType));
    return validators;
  }

  private List<Validator> getPutLinkedAccountValidator(JsonObject body, RequestType requestType) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new JsonSchemaTypeValidator(body, requestType));
    return validators;
  }

  private List<Validator> getOrderValidator(MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new UuidTypeValidator(parameters.get(PRODUCT_VARIANT_ID), true));
    return validators;
  }

  private List<Validator> getVerifyPolicyValidator(JsonObject body) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new JsonSchemaTypeValidator(body, RequestType.VERIFY));
    return validators;
  }

  private List<Validator> getPolicyValidators(final JsonObject body) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new UuidTypeValidator(body.getString(POLICY_ID), true));
    return validators;
  }

  private List<Validator> getResourceIdValidators(final MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new UuidTypeValidator(parameters.get("resourceId"), false));
    validators.add(new UuidTypeValidator(parameters.get(PROVIDER_ID), false));
    return validators;
  }

  private List<Validator> getProviderIdValidators(final MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new UuidTypeValidator(parameters.get(PROVIDER_ID), false));
    return validators;
  }

  private List<Validator> getProductValidators(
      final MultiMap parameters, final JsonObject body, final RequestType requestType) {
    List<Validator> validators = new ArrayList<>();

    if (body == null || body.isEmpty()) {
      validators.add(new ProductIdTypeValidator(parameters.get(PRODUCT_ID), true));
    } else {
      validators.add(new JsonSchemaTypeValidator(body, requestType));
    }

    return validators;
  }

  private List<Validator> getProductVariantValidators(
      final MultiMap parameters, final JsonObject body, final RequestType requestType) {
    List<Validator> validators = new ArrayList<>();

    if (body == null || body.isEmpty()) {
      validators.add(new UuidTypeValidator(parameters.get(PRODUCT_VARIANT_ID), false));
    } else {
      validators.add(new JsonSchemaTypeValidator(body, requestType));
    }

    return validators;
  }

  private List<Validator> getDeleteProductVariantValidators(final MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new UuidTypeValidator(parameters.get(PRODUCT_VARIANT_ID), true));
    return validators;
  }

  private List<Validator> listProductVariantValidators(final MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new ProductIdTypeValidator(parameters.get(PRODUCT_ID), true));
    return validators;
  }
}
