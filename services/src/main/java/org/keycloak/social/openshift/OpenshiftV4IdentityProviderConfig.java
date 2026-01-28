package org.keycloak.social.openshift;

import java.util.List;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

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

    public OpenshiftV4IdentityProviderConfig() {
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

    public static List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property().name(BASE_URL)
                .label("Base URL")
                .helpText("Override the default Base URL for this identity provider.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add().build();
    }
}
