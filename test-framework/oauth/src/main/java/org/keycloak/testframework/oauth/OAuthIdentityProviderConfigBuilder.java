package org.keycloak.testframework.oauth;

public class OAuthIdentityProviderConfigBuilder {

    private Mode mode = Mode.DEFAULT;
    private boolean jwkUse = true;

    public OAuthIdentityProviderConfigBuilder spiffe() {
        mode = Mode.SPIFFE;
        return this;
    }

    public OAuthIdentityProviderConfigBuilder kubernetes() {
        mode = Mode.KUBERNETES;
        return this;
    }

    public OAuthIdentityProviderConfigBuilder jwkUse(boolean jwkUse) {
        this.jwkUse = jwkUse;
        return this;
    }

    public OAuthIdentityProviderConfiguration build() {
        return new OAuthIdentityProviderConfiguration(mode, jwkUse);
    }

    public record OAuthIdentityProviderConfiguration(Mode mode, boolean jwkUse) {
    }

    public enum Mode {
        DEFAULT,
        SPIFFE,
        KUBERNETES
    }

}
