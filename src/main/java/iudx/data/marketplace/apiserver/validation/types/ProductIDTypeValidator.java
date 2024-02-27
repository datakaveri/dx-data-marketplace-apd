package iudx.data.marketplace.apiserver.validation.types;

import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.HttpStatusCode;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.ResponseUrn.INVALID_ID_VALUE_URN;

public class ProductIDTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(ProductIDTypeValidator.class);

  private final String value;
  private final boolean required;

  public ProductIDTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }

  public boolean isValidID(final String value) {
    String[] variable = value.split(":");
    try {
      boolean isValidUrn =
          StringUtils.isNotBlank(variable[0]) && variable[0].equals(STRING_URN);
      boolean isValidDomain =
              StringUtils.isNotBlank(variable[1]) && variable[1].equals(DOMAIN);
      boolean isValidProvider =
          StringUtils.isNotBlank(variable[2]) && new UUIDTypeValidator(variable[2], true).isValid();
      boolean isValidProductName =
          StringUtils.isNotBlank(variable[3])
              && VALIDATION_PRODUCT_ID_REGEX.matcher(variable[3]).matches();
      return isValidProductName && isValidProvider && isValidUrn && isValidDomain;

    } catch (ArrayIndexOutOfBoundsException e) {
      throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
    }
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("validation error: null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error: blank value passed");
        throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
      }
    }
    if (value.length() > VALIDATION_PRODUCT_ID_MAXLEN) {
      LOGGER.error("Validation error : Value exceed max character limit.");
      throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
    }
    if (!isValidID(value)) {
      LOGGER.error("Validation error : Invalid Name");
      throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
    }
    return true;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_ID_VALUE_URN.getMessage();
  }
}
