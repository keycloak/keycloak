package org.keycloak.validation.jakarta;

import jakarta.validation.Validator;

public class HibernateValidatorProvider implements JakartaValidatorProvider {
    private final Validator validator;

    public HibernateValidatorProvider(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

    @Override
    public void close() {

    }
}
