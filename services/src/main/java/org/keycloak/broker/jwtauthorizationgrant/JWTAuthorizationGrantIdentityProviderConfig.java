package org.keycloak.broker.jwtauthorizationgrant;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;

import static org.keycloak.broker.oidc.OIDCIdentityProviderConfig.JWKS_URL;
import static org.keycloak.common.util.UriUtils.checkUrl;

public class JWTAuthorizationGrantIdentityProviderConfig extends IdentityProviderModel implements JWTAuthorizationGrantConfig {

    public JWTAuthorizationGrantIdentityProviderConfig() {
    }

    public JWTAuthorizationGrantIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    @Override
    public void validate(RealmModel realm) {
        checkUrl(realm.getSslRequired(), getIssuer(), ISSUER);
        checkUrl(realm.getSslRequired(), getJwksUrl(), JWKS_URL);
    }
}
