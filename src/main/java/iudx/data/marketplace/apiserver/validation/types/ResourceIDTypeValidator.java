package iudx.data.marketplace.apiserver.validation.types;

import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.apiserver.util.Constants.*;
import static iudx.data.marketplace.common.ResponseUrn.INVALID_RESOURCE_URN;

public class ResourceIDTypeValidator implements Validator {

  public static final Logger LOGGER = LogManager.getLogger(ResourceIDTypeValidator.class);

  private final String value;
  private final boolean required;

  public ResourceIDTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }

  public boolean isValidID(final String value) {
    return VALIDATION_IUDX_ID_REGEX.matcher(value).matches();
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "");
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("validation error: null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_RESOURCE_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error: blank value passed");
        throw new DxRuntimeException(failureCode(), INVALID_RESOURCE_URN, failureMessage(value));
      }
    }
    if (value.length() != VALIDATION_ID_LENGTH) {
      LOGGER.error("Validation error : Value mismatch character limit.");
      throw new DxRuntimeException(failureCode(), INVALID_RESOURCE_URN, failureMessage(value));
    }
    if (!isValidID(value)) {
      LOGGER.error("Validation error : Invalid Resource ID");
      throw new DxRuntimeException(failureCode(), INVALID_RESOURCE_URN, failureMessage(value));
    }
    return true;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_RESOURCE_URN.getMessage();
  }
}
