package org.keycloak.admin.providers.validation;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.validation.jakarta.JakartaValidator;
import org.keycloak.validation.jakarta.JakartaValidatorFactory;

public class HibernateValidatorFactory implements JakartaValidatorFactory {
    public static final String PROVIDER_ID = "default";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public JakartaValidator create(KeycloakSession session) {
        return new HibernateValidatorProvider();
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
