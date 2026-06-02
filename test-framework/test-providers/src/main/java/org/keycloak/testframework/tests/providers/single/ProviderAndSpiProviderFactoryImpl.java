package org.keycloak.testframework.tests.providers.single;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;

public class ProviderAndSpiProviderFactoryImpl implements ProviderAndSpiProviderFactory {

    final static String ID = "provider-and-spi";

    private final List<ProviderConfigProperty> config = ProviderConfigurationBuilder.create()
            .property("secret", "Secret", "A secret value", STRING_TYPE, null, null, true)
            .property("number", "Number", "A number value", STRING_TYPE, null, null, false)
            .property("required", "Required", "A required value", STRING_TYPE, null, null, false)
            .property("val1", "Value 1", "Some more values", STRING_TYPE, null, null, false)
            .property("val2", "Value 2", "Some more values", STRING_TYPE, null, null, false)
            .property("val3", "Value 3", "Some more values", STRING_TYPE, null, null, false)
            .build();

    @Override
    public ProviderAndSpiProviderImpl create(KeycloakSession session, ComponentModel model) {
        return new ProviderAndSpiProviderImpl(model);
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        ConfigurationValidationHelper.check(model)
                .checkRequired("required", "Required")
                .checkInt("number", "Number", false);
    }

    @Override
    public String getHelpText() {
        return "Provider to test component storage";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return config;
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

    @Override
    public String getId() {
        return ID;
    }

    public static class ProviderAndSpiProviderImpl implements ProviderAndSpiProvider {

        private ComponentModel model;

        public ProviderAndSpiProviderImpl(ComponentModel model) {
            this.model = model;
        }

        @Override
        public void close() {
        }

    }

}
