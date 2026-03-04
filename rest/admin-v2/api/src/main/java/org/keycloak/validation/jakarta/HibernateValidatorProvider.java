package org.keycloak.validation.jakarta;

import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

/**
 * Hibernate Validator-based implementation of {@link JakartaValidatorProvider}.
 * <p>
 * This implementation supports both simple validation and context-aware validation
 * for custom validators that need access to Keycloak session, realm, or models.
 * <p>
 * For context-aware validation, this provider:
 * <ol>
 *   <li>Sets the context in the request-scoped {@link ValidationContextHolder}</li>
 *   <li>Executes validation (custom validators can inject the holder)</li>
 *   <li>Clears the context after validation completes</li>
 * </ol>
 *
 * @see ValidationContextHolder
 * @see ValidationContextData
 */
public class HibernateValidatorProvider implements JakartaValidatorProvider {

    private final Validator validator;
    private final ValidationContextHolder contextHolder;

    public HibernateValidatorProvider() {
        this.validator = CDI.current().select(Validator.class).get();
        this.contextHolder = CDI.current().select(ValidationContextHolder.class).get();
    }

    /**
     * Constructor for testing or manual instantiation.
     *
     * @param validator the Jakarta Validator instance
     * @param contextHolder the validation context holder
     */
    public HibernateValidatorProvider(Validator validator, ValidationContextHolder contextHolder) {
        this.validator = validator;
        this.contextHolder = contextHolder;
    }

    @Override
    public <T> void validate(T object, Class<?>... groups) throws ConstraintViolationException {
        var errors = validator.validate(object, groups);
        if (!errors.isEmpty()) {
            throw new ConstraintViolationException(errors);
        }
    }

    @Override
    public <T> void validate(T object, ValidationContextData context, Class<?>... groups)
            throws ConstraintViolationException {
        contextHolder.setContext(context);
        try {
            validate(object, groups);
        } finally {
            contextHolder.clear();
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
