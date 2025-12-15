package org.keycloak.testsuite.federation.testConnection;

import java.net.URL;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.user.UserStorageConnectionTest;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;

public class TestConnectionStorageProviderFactory extends DummyUserFederationProviderFactory implements UserStorageConnectionTest {

    public static final String PROVIDER_ID = "test-connection-storage";
    public static final String CONFIG_URL = "url";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property().name(CONFIG_URL)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add().build();
    }

    @Override
    public Result testConnection(KeycloakSession session, RealmModel realm, ComponentModel model) throws Exception {
        String urlStr = model.get(CONFIG_URL);
        if (urlStr == null) {
            return Result.failure("url is null");
        }

        //Dummy connection check: is the URL valid
        URL url = new URL(urlStr);
        return Result.success();
    }
}
