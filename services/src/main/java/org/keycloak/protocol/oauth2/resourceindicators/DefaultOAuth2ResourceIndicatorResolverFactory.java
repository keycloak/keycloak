package org.keycloak.protocol.oauth2.resourceindicators;

import org.keycloak.models.KeycloakSession;

public class DefaultOAuth2ResourceIndicatorResolverFactory implements OAuth2ResourceIndicatorResolverFactory {

    private static final DefaultOAuth2ResourceIndicatorResolver INSTANCE = new DefaultOAuth2ResourceIndicatorResolver();

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public OAuth2ResourceIndicatorResolver create(KeycloakSession session) {
        return INSTANCE;
    }
}
