package org.keycloak.social.openshift;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

import java.util.Map;
import java.util.Optional;

/**
 * OpenShift 4 Identity Provider configuration class.
 *
 * @author David Festal and Sebastian Łaskawiec
 */
public class OpenshiftV4IdentityProviderConfig extends OAuth2IdentityProviderConfig {

    private static final String BASE_URL = "baseUrl";

    public OpenshiftV4IdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    public String getBaseUrl() {
        return getConfig().get(BASE_URL);
    }

    public void setBaseUrl(String baseUrl) {
        getConfig().put(BASE_URL, trimTrailingSlash(baseUrl));
    }
}
