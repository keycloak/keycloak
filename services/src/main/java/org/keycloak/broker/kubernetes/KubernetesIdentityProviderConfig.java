package org.keycloak.broker.kubernetes;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class KubernetesIdentityProviderConfig extends IdentityProviderModel {

    public static final String ISSUER = OIDCIdentityProviderConfig.ISSUER;

    public static final String JWKS_URL = OIDCIdentityProviderConfig.JWKS_URL;

    public KubernetesIdentityProviderConfig() {
    }

    public KubernetesIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public String getIssuer() {
        return getConfig().get(ISSUER);
    }

    public String getJwksUrl() {
        return getConfig().get(JWKS_URL);
    }

    public int getAllowedClockSkew() {
        String allowedClockSkew = getConfig().get(ALLOWED_CLOCK_SKEW);
        if (allowedClockSkew == null || allowedClockSkew.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(getConfig().get(ALLOWED_CLOCK_SKEW));
        } catch (NumberFormatException e) {
            // ignore it and use default
            return 0;
        }
    }

    @Override
    public Boolean isHideOnLogin() {
        return true;
    }

}
