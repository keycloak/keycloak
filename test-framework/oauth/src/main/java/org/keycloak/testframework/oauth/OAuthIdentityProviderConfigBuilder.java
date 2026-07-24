package org.keycloak.testframework.oauth;

public class OAuthIdentityProviderConfigBuilder {

    private Mode mode = Mode.DEFAULT;
    private boolean jwkUse = true;
    private String issuer;

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

    public OAuthIdentityProviderConfigBuilder issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public OAuthIdentityProviderConfiguration build() {
        return new OAuthIdentityProviderConfiguration(mode, jwkUse, issuer);
    }

    public record OAuthIdentityProviderConfiguration(Mode mode, boolean jwkUse, String issuer) {
    }

    public enum Mode {
        DEFAULT,
        SPIFFE,
        KUBERNETES
    }

}
