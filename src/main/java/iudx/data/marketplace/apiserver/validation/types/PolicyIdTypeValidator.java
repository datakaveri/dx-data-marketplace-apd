package iudx.data.marketplace.apiserver.validation.types;

import iudx.data.marketplace.apiserver.exceptions.DxRuntimeException;
import iudx.data.marketplace.common.HttpStatusCode;
import iudx.data.marketplace.common.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

import static iudx.data.marketplace.apiserver.util.Constants.POLICY_ID_PATTERN;

public class PolicyIdTypeValidator implements Validator {
  private static final Logger LOGGER = LogManager.getLogger(PolicyIdTypeValidator.class);
  private final String value;
  private final boolean required;

  public PolicyIdTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;

  }

  public boolean isValidPolicyId(final String value)
  {
    return POLICY_ID_PATTERN.matcher(value).matches();
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if(required && (value == null || value.isBlank()))
    {
      LOGGER.error("Validation error : null or empty policy id when it is a required mandatory field");
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ID_VALUE_URN, failureMessage());
    }
    else
    {
      if(value == null)
      {
        return true;
      }
      if(value.isBlank())
      {
        LOGGER.error("Validation error : blank value for policy id passed");
        throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ID_VALUE_URN, failureMessage());
      }
    }
    if(value.length() > 36)
    {
      LOGGER.error("Validation error : Value exceeded max character limit");
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ID_VALUE_URN, failureMessage());
    }
    if(!isValidPolicyId(value))
    {
      LOGGER.error("Validation error : invalid id");
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ID_VALUE_URN, failureMessage());
    }

    return true;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return ResponseUrn.INVALID_ID_VALUE_URN.getMessage();
  }
}
