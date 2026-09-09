package org.keycloak.tests.providers.policy;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PasswordPolicyProviderFactory;
import org.keycloak.policy.PolicyError;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * Custom password policy provider implementing {@link ConfiguredProvider}, used to verify that the
 * provider-supplied help text is exposed through the server info endpoint.
 */
public class TestConfiguredPasswordPolicyProviderFactory implements PasswordPolicyProviderFactory, ConfiguredProvider {

    public static final String ID = "test-configured-policy";

    public static final String HELP_TEXT = "Test policy help text provided by the factory.";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public PasswordPolicyProvider create(KeycloakSession session) {
        return new TestConfiguredPasswordPolicyProvider();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getDisplayName() {
        return "Test Configured Policy";
    }

    @Override
    public String getConfigType() {
        return PasswordPolicyProvider.STRING_CONFIG_TYPE;
    }

    @Override
    public String getDefaultConfigValue() {
        return "default-value";
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("value")
                .label("Value")
                .helpText("Value of the test policy.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("default-value")
                .add()
                .build();
    }

    @Override
    public void close() {
    }

    private static class TestConfiguredPasswordPolicyProvider implements PasswordPolicyProvider {

        @Override
        public PolicyError validate(RealmModel realm, UserModel user, String password) {
            return null;
        }

        @Override
        public PolicyError validate(String user, String password) {
            return null;
        }

        @Override
        public Object parseConfig(String value) {
            return value;
        }

        @Override
        public void close() {
        }
    }
}
