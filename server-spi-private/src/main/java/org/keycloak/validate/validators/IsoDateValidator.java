package org.keycloak.validate.validators;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.validate.AbstractStringValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;


/**
 * A date validator that only takes into account the format associated with the current locale.
 */
public class IsoDateValidator extends AbstractStringValidator implements ConfiguredProvider {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public static final String MESSAGE_INVALID_DATE = "error-invalid-date";

    public static final IsoDateValidator INSTANCE = new IsoDateValidator();

    public static final String ID = "iso-date";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
        try {
            FORMATTER.parse(value);
        } catch (DateTimeParseException e) {
            context.addError(new ValidationError(getId(), inputHint, MESSAGE_INVALID_DATE, value));
        }
    }

    @Override
    public String getHelpText() {
        return "Validates date in rfc3339/iso8601 format, as provided by the html5-date input.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    protected boolean isIgnoreEmptyValuesConfigured(ValidatorConfig config) {
        return true;
    }
}
