package org.keycloak.validate.builtin;

import org.keycloak.validate.CompactValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;

import java.util.Map;

public class LengthValidator implements CompactValidator {

    public static final LengthValidator INSTANCE = new LengthValidator();
    public static final String ID = "length";
    public static final String ERROR_INVALID_LENGTH = "error-invalid-length";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, Map<String, Object> config) {

        if (input == null) {
            context.addError(new ValidationError(ID, inputHint, ERROR_INVALID_LENGTH, input));
            return context;
        }

        if (!(input instanceof String)) {
            return context;
        }

        // TODO make config value extraction more robust

        String string = (String) input;
        int min = config.containsKey("min") ? Integer.parseInt(String.valueOf(config.get("min"))) : 0;
        int max = config.containsKey("max") ? Integer.parseInt(String.valueOf(config.get("max"))) : Integer.MAX_VALUE;

        int length = string.length();

        if (length < min) {
            context.addError(new ValidationError(ID, inputHint, ERROR_INVALID_LENGTH, string));
        }

        if (length > max) {
            context.addError(new ValidationError(ID, inputHint, ERROR_INVALID_LENGTH, string));
        }

        return context;
    }
}
