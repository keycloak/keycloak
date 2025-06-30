package org.keycloak.cookie;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class DefaultCookieProviderFactory implements CookieProviderFactory {

    private static final String SAME_SITE_LEGACY_KEY = "sameSiteLegacy";
    private boolean sameSiteLegacyEnabled;

    @Override
    public CookieProvider create(KeycloakSession session) {
        return new DefaultCookieProvider(session.getContext(), sameSiteLegacyEnabled);
    }

    @Override
    public void init(Config.Scope config) {
        sameSiteLegacyEnabled = config.getBoolean(SAME_SITE_LEGACY_KEY, true);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(SAME_SITE_LEGACY_KEY)
                .type("boolean")
                .helpText("Adds legacy cookies without SameSite parameter")
                .defaultValue(true)
                .add()
                .build();
    }
}
