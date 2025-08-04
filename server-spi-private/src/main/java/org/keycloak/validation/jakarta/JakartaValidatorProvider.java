package org.keycloak.validation.jakarta;

import jakarta.validation.Validator;
import org.keycloak.provider.Provider;

public interface JakartaValidatorProvider extends Provider {

    Validator getValidator();
}
