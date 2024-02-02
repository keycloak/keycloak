package org.keycloak.cookie;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultCookieProviderFactory implements CookieProviderFactory {

    private boolean legacyCookies;

    @Override
    public CookieProvider create(KeycloakSession session) {
        return new DefaultCookieProvider(session.getContext(), legacyCookies);
    }

    @Override
    public void init(Config.Scope config) {
        legacyCookies = config.getBoolean("legacyCookies", true);
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

}
