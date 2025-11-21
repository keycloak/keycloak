package org.keycloak.validation.jakarta;

import java.util.Set;
import java.util.function.Function;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

public interface JakartaValidatorProvider {

    <T> void validate(T object, Class<?>... groups) throws ConstraintViolationException;

    void validate(Function<Validator, Set<ConstraintViolation<?>>> validation) throws ConstraintViolationException;

    Validator getValidator();
}
