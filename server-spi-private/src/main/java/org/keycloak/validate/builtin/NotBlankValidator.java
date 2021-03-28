package org.keycloak.validate.builtin;

import org.keycloak.validate.CompactValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

public class NotBlankValidator implements CompactValidator {

    public static final String ID = "blank";

    public static final String MESSAGE_BLANK = "error-invalid-blank";

    public static final NotBlankValidator INSTANCE = new NotBlankValidator();

    private NotBlankValidator() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        if (!(input instanceof String)) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_VALUE, input));
            return context;
        }

        String string = (String) input;
        if (string.trim().length() == 0) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_BLANK, input));
        }

        return context;
    }
}
