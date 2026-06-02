package org.keycloak.testframework.oauth;

public class OAuthIdentityProviderConfigBuilder {

    private Mode mode = Mode.DEFAULT;
    private boolean jwkUse = true;
    private String issuer;
    private String discoveryPath = "/idp";

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

    public OAuthIdentityProviderConfigBuilder discoveryPath(String discoveryPath) {
        this.discoveryPath = discoveryPath;
        return this;
    }

    public OAuthIdentityProviderConfiguration build() {
        return new OAuthIdentityProviderConfiguration(mode, jwkUse, issuer, discoveryPath);
    }

    public record OAuthIdentityProviderConfiguration(Mode mode, boolean jwkUse, String issuer, String discoveryPath) {
    }

    public enum Mode {
        DEFAULT,
        SPIFFE,
        KUBERNETES
    }

}
