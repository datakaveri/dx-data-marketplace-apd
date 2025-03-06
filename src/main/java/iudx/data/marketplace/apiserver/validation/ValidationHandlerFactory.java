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

/*TODO: set required to true for Bearer Authorization header later */

public class ValidationHandlerFactory {
  private static final Logger LOGGER = LogManager.getLogger(ValidationHandlerFactory.class);

  public List<Validator> build(
      final RequestType requestType,
      final MultiMap parameters,
      final JsonObject body,
      MultiMap header) {
    LOGGER.debug("getValidation4Context() started for : " + requestType);
    LOGGER.debug("type : " + requestType);
    List<Validator> validator = null;

    switch (requestType) {
      case PRODUCT:
        validator = getProductValidators(parameters, body, requestType, header);
        break;
      case PRODUCT_VARIANT:
        validator = getProductVariantValidators(parameters, body, requestType, header);
        break;
      case DELETE_PRODUCT_VARIANT:
        validator = getDeleteProductVariantValidators(parameters, header);
        break;
      case LIST_PRODUCT_VARIANT:
        validator = listProductVariantValidators(parameters, header);
        break;
      case RESOURCE:
        validator = getResourceIdValidators(parameters, header);
        break;
      case PROVIDER:
        validator = getProviderIdValidators(parameters, header);
        break;
      case POLICY:
        validator = getPolicyValidators(body, parameters, header);
        break;
      case VERIFY:
        validator = getVerifyPolicyValidator(body, header);
        break;
      case ORDER:
        validator = getOrderValidator(parameters, header);
        break;
      case VERIFY_PAYMENT:
        validator = getVerfiyPaymentValidator(body, parameters, header);
        break;
      case POST_ACCOUNT:
        validator = getPostLinkedAccountValidator(body, requestType, parameters, header);
        break;
      case PUT_ACCOUNT:
        validator = getPutLinkedAccountValidator(body, requestType, parameters, header);
        break;
      case ORDER_PAID_WEBHOOK:
        validator = getPaymentWebhookValidator(body, parameters, header);
        break;
      case PURCHASE:
        validator = getPurchaseValidator(parameters, header);
        break;
      case CONSUMER_PRODUCT_VARIANT:
        validator = getConsumerProductVariantValidator(parameters, header);
        break;
      case CHECK_POLICY:
        validator = getCheckPolicyValidator(parameters, header);
        break;
      default:
        break;
    }
    return validator;
  }

  private List<Validator> getCheckPolicyValidator(MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new UuidTypeValidator(parameters.get(PRODUCT_VARIANT_ID), true));
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));

    return validators;
  }

  private List<Validator> getPaymentWebhookValidator(
      JsonObject body, MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));
    validators.add(new JsonSchemaTypeValidator(body, RequestType.ORDER_PAID_WEBHOOK));
    return validators;
  }

  private List<Validator> getConsumerProductVariantValidator(MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));

    validators.add(new ProductIdTypeValidator(parameters.get("productId"), true));
    return validators;
  }

  private List<Validator> getPurchaseValidator(MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));

    validators.add(new UuidTypeValidator(parameters.get("resourceId"), false));
    validators.add(new ProductIdTypeValidator(parameters.get("productId"), false));
    validators.add(new OrderIdTypeValidator(parameters.get("orderId"), false));
    return validators;
  }

  private List<Validator> getVerfiyPaymentValidator(
      JsonObject body, MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));

    validators.add(new JsonSchemaTypeValidator(body, RequestType.VERIFY_PAYMENT));
    return validators;
  }

  private List<Validator> getPostLinkedAccountValidator(
      JsonObject body, RequestType requestType, MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));
    validators.add(new JsonSchemaTypeValidator(body, requestType));
    return validators;
  }

  private List<Validator> getPutLinkedAccountValidator(
      JsonObject body, RequestType requestType, MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));

    validators.add(new JsonSchemaTypeValidator(body, requestType));
    return validators;
  }

  private List<Validator> getOrderValidator(MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));
    validators.add(new UuidTypeValidator(parameters.get(PRODUCT_VARIANT_ID), true));
    return validators;
  }

  private List<Validator> getVerifyPolicyValidator(JsonObject body, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new JsonSchemaTypeValidator(body, RequestType.VERIFY));
    return validators;
  }

  private List<Validator> getPolicyValidators(
      final JsonObject body, MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));
    validators.add(new UuidTypeValidator(body.getString(POLICY_ID), true));
    return validators;
  }

  private List<Validator> getResourceIdValidators(final MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));

    validators.add(new UuidTypeValidator(parameters.get("resourceId"), false));
    validators.add(new UuidTypeValidator(parameters.get(PROVIDER_ID), false));
    return validators;
  }

  private List<Validator> getProviderIdValidators(final MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));

    validators.add(new UuidTypeValidator(parameters.get(PROVIDER_ID), false));
    return validators;
  }

  private List<Validator> getProductValidators(
      final MultiMap parameters,
      final JsonObject body,
      final RequestType requestType,
      MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));

    if (body == null || body.isEmpty()) {
      validators.add(new ProductIdTypeValidator(parameters.get(PRODUCT_ID), true));
    } else {
      validators.add(new JsonSchemaTypeValidator(body, requestType));
    }

    return validators;
  }

  private List<Validator> getProductVariantValidators(
      final MultiMap parameters,
      final JsonObject body,
      final RequestType requestType,
      MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));

    if (body == null || body.isEmpty()) {
      validators.add(new UuidTypeValidator(parameters.get(PRODUCT_VARIANT_ID), false));
    } else {
      validators.add(new JsonSchemaTypeValidator(body, requestType));
    }

    return validators;
  }

  private List<Validator> getDeleteProductVariantValidators(
      final MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));
    validators.add(new UuidTypeValidator(parameters.get(PRODUCT_VARIANT_ID), true));
    return validators;
  }

  private List<Validator> listProductVariantValidators(final MultiMap parameters, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(HEADER_BEARER_AUTHORIZATION), false));
    validators.add(new ProductIdTypeValidator(parameters.get(PRODUCT_ID), true));
    return validators;
  }
}
