package org.keycloak.quarkus.runtime.validation;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.validator.ValidatorFactoryCustomizer;
import org.hibernate.validator.BaseHibernateValidatorConfiguration;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

@ApplicationScoped
public class HibernateValidatorFactoryCustomizer implements ValidatorFactoryCustomizer {

    @Override
    public void customize(BaseHibernateValidatorConfiguration<?> configuration) {
        // we do not need any 3rd party dependency for the Expression language handling (like expressly) as we do not use it
        configuration.messageInterpolator(new ParameterMessageInterpolator());
    }
}
