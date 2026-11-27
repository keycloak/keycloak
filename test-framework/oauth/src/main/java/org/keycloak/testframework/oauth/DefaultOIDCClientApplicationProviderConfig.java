package org.keycloak.testframework.oauth;

public class DefaultOIDCClientApplicationProviderConfig implements OIDCClientApplicationProviderConfig {

    @Override
    public OIDCClientApplicationProviderConfiguration build() {
        return new OIDCClientApplicationProviderConfiguration();
    }
}
