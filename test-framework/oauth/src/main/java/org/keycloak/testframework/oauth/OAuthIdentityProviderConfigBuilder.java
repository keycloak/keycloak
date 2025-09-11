package org.keycloak.testframework.oauth;

public class OAuthIdentityProviderConfigBuilder {

    private boolean spiffe;

    public OAuthIdentityProviderConfigBuilder spiffe() {
        spiffe = true;
        return this;
    }

    public OAuthIdentityProviderConfiguration build() {
        return new OAuthIdentityProviderConfiguration(spiffe);
    }

    public record OAuthIdentityProviderConfiguration(boolean spiffe) {
    }

}
