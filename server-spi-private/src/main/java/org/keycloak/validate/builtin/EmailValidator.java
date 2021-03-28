package org.keycloak.validate.builtin;

import org.keycloak.validate.CompactValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

import java.util.regex.Pattern;

public class EmailValidator implements CompactValidator {

    public static final String ID = "email";

    public static final EmailValidator INSTANCE = new EmailValidator();

    public static final String MESSAGE_INVALID_EMAIL = "error-invalid-email";

    // Actually allow same emails like angular. See ValidationTest.testEmailValidation()
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*");

    private EmailValidator() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        if (!(input instanceof String)) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_EMAIL, input));
            return context;
        }

        if (!EMAIL_PATTERN.matcher((String) input).matches()) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_EMAIL, input));
        }

        return context;
    }
}
