package org.keycloak.tests.keys;

import java.util.List;

import org.keycloak.crypto.Algorithm;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.tests.utils.Assert;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = FallbackKeyProviderDisabledTest.FallbackKeysDisabledConfig.class)
public class FallbackKeyProviderDisabledTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD, config = BasicRealmWithUserConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    public void noFallbackKeyWhenDisabled() {
        AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest();
        Assertions.assertTrue(response.isSuccess());

        RealmRepresentation rep = realm.admin().toRepresentation();
        rep.setDefaultSignatureAlgorithm(Algorithm.ES256);
        realm.admin().update(rep);

        response = oauth.doClientCredentialsGrantAccessTokenRequest();
        Assertions.assertFalse(response.isSuccess());

        List<ComponentRepresentation> providers = realm.admin().components().query(realm.getId(), KeyProvider.class.getName());
        Assert.assertNames(providers, "rsa-generated", "rsa-enc-generated", "hmac-generated-hs512", "aes-generated");
    }

    public static class FallbackKeysDisabledConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("spi-keys--generate-fallback-keys", "false");
        }
    }
}
