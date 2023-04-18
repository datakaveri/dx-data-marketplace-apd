package iudx.data.marketplace.apiserver.validation.types;

import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.data.marketplace.apiserver.util.Constants.VALIDATION_DATASET_ID_REGEX;
import static iudx.data.marketplace.apiserver.util.Constants.VALIDATION_DATASET_ID_MAXLEN;
import static iudx.data.marketplace.common.ResponseUrn.INVALID_DATASET_URN;

public class DatasetIDTypeValidator implements  Validator {

  public static final Logger LOGGER = LogManager.getLogger(DatasetIDTypeValidator.class);

  private final String value;
  private final boolean required;

  public DatasetIDTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }

  public boolean isValidID(final String value) {
    return VALIDATION_DATASET_ID_REGEX.matcher(value).matches();
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "");
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("validation error: null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_DATASET_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error: blank value passed");
        throw new DxRuntimeException(failureCode(), INVALID_DATASET_URN, failureMessage(value));
      }
    }
    if (value.length() > VALIDATION_DATASET_ID_MAXLEN) {
      LOGGER.error("Validation error : Value exceed max character limit.");
      throw new DxRuntimeException(failureCode(), INVALID_DATASET_URN, failureMessage(value));
    }
    if (!isValidID(value)) {
      LOGGER.error("Validation error : Invalid Dataset ID");
      throw new DxRuntimeException(failureCode(), INVALID_DATASET_URN, failureMessage(value));
    }
    return true;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_DATASET_URN.getMessage();
  }
}
