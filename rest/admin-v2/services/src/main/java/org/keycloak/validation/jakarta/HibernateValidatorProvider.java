package org.keycloak.validation.jakarta;

import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidatorFactory;

/**
 * Hibernate Validator implementation of {@link JakartaValidatorProvider} that supports
 * passing a {@link ValidationContext} to custom constraint validators.
 * <p>
 * The {@link ValidationContext} containing the {@link org.keycloak.models.KeycloakSession}
 * and {@link org.keycloak.models.RealmModel} is passed as a constraint validator payload,
 * allowing custom validators to access Keycloak's runtime context.
 *
 * @see ValidationContext
 */
public class HibernateValidatorProvider implements JakartaValidatorProvider {

    private final Validator validator;

    /**
     * Creates a validator provider with a {@link ValidationContext} that will be
     * available to custom constraint validators.
     *
     * @param context the validation context containing session and realm
     */
    public HibernateValidatorProvider(ValidationContext context) {
        ValidatorFactory factory = CDI.current().select(ValidatorFactory.class).get();
        this.validator = factory.unwrap(HibernateValidatorFactory.class)
                .usingContext()
                .constraintValidatorPayload(context)
                .getValidator();
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
