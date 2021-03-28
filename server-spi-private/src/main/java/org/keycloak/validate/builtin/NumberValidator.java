package org.keycloak.validate.builtin;

import org.keycloak.validate.CompactValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

public class NumberValidator implements CompactValidator {

    public static final String ID = "number";

    public static final String MESSAGE_INVALID_NUMBER = "error-invalid-number";

    public static final NumberValidator INSTANCE = new NumberValidator();

    private NumberValidator() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        if (input instanceof Number) {
            return context;
        }

        if (input instanceof String) {

            // try to parse the string into a number
            String string = (String) input;
            try {
                Integer.parseInt(string);
                // okay we have an integer
            } catch (NumberFormatException nfe) {
                try {
                    Double.parseDouble(string);
                    // okay we have a double
                } catch (NumberFormatException nfe2) {
                    context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_NUMBER, input));
                }
            }

            return context;
        }

        context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_NUMBER, input));

        return context;
    }
}
