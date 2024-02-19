package iudx.data.marketplace.apiserver.validation.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AccountIdTypeValidator implements Validator{
    private static final Logger LOGGER = LogManager.getLogger(AccountIdTypeValidator.class);
    private final String value;
    private final boolean required;
    public AccountIdTypeValidator(final String value, final boolean required) {
        this.value = value;
        this.required = required;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public int failureCode() {
        return 0;
    }

    @Override
    public String failureMessage() {
        return null;
    }
}
