package org.keycloak.testframework.oauth;

public class DefaultOAuthIdentityProviderConfig implements OAuthIdentityProviderConfig {
    @Override
    public OAuthIdentityProviderConfigBuilder configure(OAuthIdentityProviderConfigBuilder config) {
        return config;
    }
}
