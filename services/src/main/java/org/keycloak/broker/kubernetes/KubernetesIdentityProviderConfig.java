package org.keycloak.broker.kubernetes;


import com.google.common.base.Strings;

import org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantConfig;
import org.keycloak.broker.oidc.IssuerValidation;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderType;
import org.keycloak.models.RealmModel;

import static org.keycloak.broker.oidc.OIDCIdentityProviderConfig.USE_JWKS_URL;


public class KubernetesIdentityProviderConfig extends IdentityProviderModel implements IssuerValidation, JWTAuthorizationGrantConfig {

    public KubernetesIdentityProviderConfig() {
    }

    public KubernetesIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public String getIssuer() {
        return getConfig().get(ISSUER);
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

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
        validateIssuer(realm, IdentityProviderType.CLIENT_ASSERTION);
    }

    @Override
    public boolean isUseJwksUrl() {
        return !Strings.isNullOrEmpty(getJwksUrl());
    }
}
