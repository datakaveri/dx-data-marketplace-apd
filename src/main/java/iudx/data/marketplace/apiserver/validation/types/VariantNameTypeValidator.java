package iudx.data.marketplace.apiserver.validation.types;

import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.apiserver.util.Constants.VALIDATION_VARIANT_NAME_MAX_LEN;
import static iudx.data.marketplace.apiserver.util.Constants.VALIDATION_VARIANT_NAME_REGEX;
import static iudx.data.marketplace.common.ResponseUrn.INVALID_NAME_URN;

public class VariantNameTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(VariantNameTypeValidator.class);

  private final String value;
  private final boolean required;

  public VariantNameTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }

  public boolean isValidName(final String value) {
    return VALIDATION_VARIANT_NAME_REGEX.matcher(value).matches();
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("validation error: null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_NAME_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error: blank value passed");
        throw new DxRuntimeException(failureCode(), INVALID_NAME_URN, failureMessage(value));
      }
    }
    if (value.length() > VALIDATION_VARIANT_NAME_MAX_LEN) {
      LOGGER.error("Validation error : Value exceed max character limit.");
      throw new DxRuntimeException(failureCode(), INVALID_NAME_URN, failureMessage(value));
    }
    if (!isValidName(value)) {
      LOGGER.error("Validation error : Invalid Name");
      throw new DxRuntimeException(failureCode(), INVALID_NAME_URN, failureMessage(value));
    }
    return true;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_NAME_URN.getMessage();
  }
}
