package org.keycloak.social.openshift;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class OpenshiftV3IdentityProviderConfig extends OAuth2IdentityProviderConfig {
    private static final String BASE_URL = "baseUrl";

    public OpenshiftV3IdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public OpenshiftV3IdentityProviderConfig() {
        
    }

    public String getBaseUrl() {
        return getConfig().get(BASE_URL);
    }

    public void setBaseUrl(String baseUrl) {
        getConfig().put(BASE_URL, trimTrailingSlash(baseUrl));
    }

    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
