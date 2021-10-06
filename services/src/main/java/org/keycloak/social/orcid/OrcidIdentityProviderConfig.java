package org.keycloak.social.orcid;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class OrcidIdentityProviderConfig extends OAuth2IdentityProviderConfig {
    private static final String BASE_URL = "baseUrl";
    private static final String DEFAULT_BASE_URL = "https://orcid.org/oauth";
    public static final String DEFAULT_PROFILE_URL = "https://pub.orcid.org/v3.0";

    public OrcidIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public OrcidIdentityProviderConfig() {
    }

    public String getBaseUrl() {
        return getConfig().get(BASE_URL) != null ? getConfig().get(BASE_URL) : DEFAULT_BASE_URL;
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

    @Override
    public String getUserInfoUrl() {
        return getConfig().get("userInfoUrl") != null ?  getConfig().get("userInfoUrl") : DEFAULT_PROFILE_URL;
    }
}