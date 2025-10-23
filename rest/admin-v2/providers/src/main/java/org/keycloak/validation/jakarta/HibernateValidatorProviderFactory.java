package org.keycloak.validation.jakarta;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.validation.Validator;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class HibernateValidatorProviderFactory implements JakartaValidatorProviderFactory {
    public static final String PROVIDER_ID = "default";
    private static HibernateValidatorProvider SINGLETON;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public JakartaValidatorProvider create(KeycloakSession session) {
        if (SINGLETON == null) {
            SINGLETON = new HibernateValidatorProvider(CDI.current().select(Validator.class).get());
        }
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }
}
