package org.keycloak.validation.jakarta;

import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

public class HibernateValidatorProvider implements JakartaValidatorProvider {

    private final Validator validator = CDI.current().select(Validator.class).get();

    public HibernateValidatorProvider() {
    }

    @Override
    public <T> void validate(T object, Class<?>... groups) throws ConstraintViolationException {
        var errors = validator.validate(object, groups);
        if (!errors.isEmpty()) {
            throw new ConstraintViolationException(errors);
        }
    }

    @Override
    public void validate(Function<Validator, Set<ConstraintViolation<?>>> validation) throws ConstraintViolationException {
        var errors = validation.apply(getValidator());
        if (!errors.isEmpty()) {
            throw new ConstraintViolationException(errors);
        }
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

}
