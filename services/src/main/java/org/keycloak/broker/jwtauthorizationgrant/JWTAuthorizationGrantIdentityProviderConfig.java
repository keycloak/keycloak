package org.keycloak.broker.jwtauthorizationgrant;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;

import java.util.Map;

import static org.keycloak.broker.oidc.OIDCIdentityProviderConfig.JWKS_URL;
import static org.keycloak.common.util.UriUtils.checkUrl;

public class JWTAuthorizationGrantIdentityProviderConfig extends IdentityProviderModel implements JWTAuthorizationGrantConfig {

    public JWTAuthorizationGrantIdentityProviderConfig() {
    }

    public JWTAuthorizationGrantIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public JWTAuthorizationGrantIdentityProviderConfig(Map<String, String> config) {
        this.setConfig(config);
    }

    @Override
    public void validate(RealmModel realm) {
        checkUrl(realm.getSslRequired(), getIssuer(), ISSUER);
        checkUrl(realm.getSslRequired(), getJwksUrl(), JWKS_URL);
    }

    public boolean getJWTAuthorizationGrantEnabled() {
        return Boolean.parseBoolean(getConfig().getOrDefault(JWT_AUTHORIZATION_GRANT_ENABLED, "false"));
    }

    public boolean getJWTAuthorizationGrantAssertionReuseAllowed() {
        return Boolean.parseBoolean(getConfig().getOrDefault(JWT_AUTHORIZATION_GRANT_ASSERTION_REUSE_ALLOWED, "false"));
    }

    public int getJWTAuthorizationGrantMaxAllowedAssertionExpiration() {
        return Integer.parseInt(getConfig().getOrDefault(JWT_AUTHORIZATION_GRANT_MAX_ALLOWED_ASSERTION_EXPIRATION, "300"));
    }

    public String getJWTAuthorizationGrantAssertionSignatureAlg() {
        return getConfig().get(JWT_AUTHORIZATION_GRANT_ASSERTION_SIGNATURE_ALG);
    }

    public int getJWTAuthorizationGrantAllowedClockSkewAllowedClockSkew() {
        String allowedClockSkew = getConfig().get(JWT_AUTHORIZATION_GRANT_ALLOWED_CLOCK_SKEW);
        if (allowedClockSkew == null || allowedClockSkew.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(getConfig().get(JWT_AUTHORIZATION_GRANT_ALLOWED_CLOCK_SKEW));
        } catch (NumberFormatException e) {
            // ignore it and use default
            return 0;
        }
    }

    @Override
    public String getIssuer() {
        return getConfig().get(ISSUER);
    }

    @Override
    public String getJwksUrl() {
        return getConfig().get(JWKS_URL);
    }
}
