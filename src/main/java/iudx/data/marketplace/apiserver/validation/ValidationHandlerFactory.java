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

  /*TODO: set required to true for Bearer Authorization header later */
  public List<Validator> build(
      final RequestType requestType, final MultiMap parameters, final JsonObject body, MultiMap header) {
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
        validator = getPolicyValidators(body, header);
        break;
      case VERIFY:
        validator = getVerifyPolicyValidator(body, header);
        break;
      case ORDER:
        validator = getOrderValidator(parameters, header);
        break;
      case VERIFY_PAYMENT:
        validator = getVerfiyPaymentValidator(body, header);
        break;
      case POST_ACCOUNT:
        validator = getPostLinkedAccountValidator(body, requestType, header);
        break;
      case PUT_ACCOUNT:
        validator = getPutLinkedAccountValidator(body, requestType, header);
        break;
      case ORDER_PAID_WEBHOOK:
        validator = getPaymentWebhookValidator(body, header);
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

  private List<Validator> getCheckPolicyValidator(MultiMap parameters, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new UuidTypeValidator(parameters.get(PRODUCT_VARIANT_ID), true));
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getPaymentWebhookValidator(JsonObject body, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new JsonSchemaTypeValidator(body, RequestType.ORDER_PAID_WEBHOOK));
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getConsumerProductVariantValidator(MultiMap parameters, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new ProductIdTypeValidator(parameters.get("productId"), true));
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getPurchaseValidator(MultiMap parameters, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new UuidTypeValidator(parameters.get("resourceId"), false));
    validators.add(new ProductIdTypeValidator(parameters.get("productId"), false));
    validators.add(new OrderIdTypeValidator(parameters.get("orderId"), false));
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getVerfiyPaymentValidator(JsonObject body, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new JsonSchemaTypeValidator(body, RequestType.VERIFY_PAYMENT));
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getPostLinkedAccountValidator(JsonObject body, RequestType requestType, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new JsonSchemaTypeValidator(body, requestType));
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getPutLinkedAccountValidator(JsonObject body, RequestType requestType, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new JsonSchemaTypeValidator(body, requestType));
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getOrderValidator(MultiMap parameters, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new UuidTypeValidator(parameters.get(PRODUCT_VARIANT_ID), true));
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getVerifyPolicyValidator(JsonObject body, MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new JsonSchemaTypeValidator(body, RequestType.VERIFY));
    validators.add(new VerifyTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getPolicyValidators(final JsonObject body, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new UuidTypeValidator(body.getString(POLICY_ID), true));
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getResourceIdValidators(final MultiMap parameters, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new UuidTypeValidator(parameters.get("resourceId"), false));
    validators.add(new UuidTypeValidator(parameters.get(PROVIDER_ID), false));
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getProviderIdValidators(final MultiMap parameters, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    validators.add(new UuidTypeValidator(parameters.get(PROVIDER_ID), false));
    return validators;
  }

  private List<Validator> getProductValidators(
      final MultiMap parameters, final JsonObject body, final RequestType requestType, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();

    if (body == null || body.isEmpty()) {
      validators.add(new ProductIdTypeValidator(parameters.get(PRODUCT_ID), true));
    } else {
      validators.add(new JsonSchemaTypeValidator(body, requestType));
    }
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getProductVariantValidators(
      final MultiMap parameters, final JsonObject body, final RequestType requestType, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();

    if (body == null || body.isEmpty()) {
      validators.add(new UuidTypeValidator(parameters.get(PRODUCT_VARIANT_ID), false));
    } else {
      validators.add(new JsonSchemaTypeValidator(body, requestType));
    }
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));

    return validators;
  }

  private List<Validator> getDeleteProductVariantValidators(final MultiMap parameters, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new UuidTypeValidator(parameters.get(PRODUCT_VARIANT_ID), true));
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));
    return validators;
  }

  private List<Validator> listProductVariantValidators(final MultiMap parameters, final MultiMap header) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new ProductIdTypeValidator(parameters.get(PRODUCT_ID), true));
    validators.add(new BearerTokenTypeValidator(header.get(AUTHORIZATION_KEY), false));
    return validators;
  }
}
