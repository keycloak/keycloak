package org.keycloak.testframework.oauth;

public class OAuthIdentityProviderConfigBuilder {

    private boolean spiffe;
    private boolean jwkUse = true;

    public OAuthIdentityProviderConfigBuilder spiffe() {
        spiffe = true;
        return this;
    }

    public OAuthIdentityProviderConfigBuilder jwkUse(boolean jwkUse) {
        this.jwkUse = jwkUse;
        return this;
    }

    public OAuthIdentityProviderConfiguration build() {
        return new OAuthIdentityProviderConfiguration(spiffe, jwkUse);
    }

    public record OAuthIdentityProviderConfiguration(boolean spiffe, boolean jwkUse) {
    }

}
