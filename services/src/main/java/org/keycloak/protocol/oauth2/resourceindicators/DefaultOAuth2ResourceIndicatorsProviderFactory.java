package org.keycloak.protocol.oauth2.resourceindicators;

import org.keycloak.models.KeycloakSession;

public class DefaultOAuth2ResourceIndicatorsProviderFactory implements OAuth2ResourceIndicatorsProviderFactory {

    private static final DefaultOAuth2ResourceIndicatorsProvider INSTANCE = new DefaultOAuth2ResourceIndicatorsProvider();

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public OAuth2ResourceIndicatorsProvider create(KeycloakSession session) {
        return INSTANCE;
    }
}
