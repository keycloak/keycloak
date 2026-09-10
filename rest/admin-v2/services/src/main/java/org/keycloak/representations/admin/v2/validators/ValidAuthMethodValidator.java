package org.keycloak.representations.admin.v2.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.representations.admin.v2.validation.ValidAuthMethod;
import org.keycloak.validation.jakarta.ValidationContext;

public class ValidAuthMethodValidator implements ConstraintValidator<ValidAuthMethod, String> {

    @Override
    public boolean isValid(String authMethod, ConstraintValidatorContext context) {
        if (authMethod == null || authMethod.isBlank()) {
            return true;
        }
        ValidationContext validationContext = ValidationContext.unwrap(context);
        return validationContext.session().getKeycloakSessionFactory()
                .getProviderFactory(ClientAuthenticator.class, authMethod) != null;
    }
}
