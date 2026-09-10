package org.keycloak.tests.oauth;

import org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

@KeycloakIntegrationTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class JWTAuthorizationGrantTest extends AbstractJWTAuthorizationGrantTest {

    @InjectRealm(config = JWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig.class)
    protected ManagedRealm realm;

    public static class JWTAuthorizationGrantRealmConfig extends AbstractJWTAuthorizationGrantTest.JWTAuthorizationGrantRealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            super.configure(realm);
            realm.identityProviders(IdentityProviderBuilder.create()
                    .providerId(JWTAuthorizationGrantIdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .attribute(IdentityProviderModel.ISSUER, IDP_ISSUER)
                    .attribute(OIDCIdentityProviderConfig.USE_JWKS_URL, Boolean.TRUE.toString())
                    .attribute(OIDCIdentityProviderConfig.JWKS_URL, "http://127.0.0.1:8500/idp/jwks")
                    .build());
            return realm;
        }
    }
}
