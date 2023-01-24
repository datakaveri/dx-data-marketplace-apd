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
    int count = StringUtils.countMatches(value, ":");
    return VALIDATION_PRODUCT_ID_REGEX.matcher(value).matches() && (count == VALIDATION_URN_DELIMITER_COUNT);
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
