package org.keycloak.tests.ssl;

import java.net.MalformedURLException;
import java.net.URL;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testsuite.util.oauth.OpenIDProviderConfigurationResponse;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
class TlsSslRequiredTest {

    @InjectCertificates(config = TlsEnabledConfig.class)
    ManagedCertificates managedCertificates;

    @InjectRealm(config = SslNoneRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @Test
    void testHttpAccessAllowedWhenSslNotRequired() {
        assertThat("TLS must be enabled for this test", managedCertificates.isTlsEnabled(), is(true));

        String httpBaseUrl = getHttpBaseUrl();
        oauth.baseUrl(httpBaseUrl);

        OIDCConfigurationRepresentation config = oauth.doWellKnownRequest();

        assertThat("Authorization endpoint should use HTTP when ssl-required is NONE",
                config.getAuthorizationEndpoint(), startsWith(httpBaseUrl));
    }

    @Test
    void testHttpAccessRejectedWhenSslAlwaysRequired() {
        realm.updateWithCleanup(r -> r.sslRequired(SslRequired.ALL.toString()));
        oauth.baseUrl(getHttpBaseUrl());

        OpenIDProviderConfigurationResponse response = oauth.wellknownRequest().send();
        assertThat("Well-known request over HTTP should fail when ssl-required is ALL",
                response.isSuccess(), is(false));
        assertThat("Error should indicate HTTPS is required",
                response.getErrorDescription(), is("HTTPS required"));

        assertThrows(RuntimeException.class, () -> oauth.keys().getRealmKeys(),
                "Fetching realm keys over HTTP should fail when ssl-required is ALL");
    }

    // TODO replace hardcoded port with server API once https://github.com/keycloak/keycloak/issues/48089 is resolved
    private String getHttpBaseUrl() {
        try {
            URL realmUrl = new URL(realm.getBaseUrl());
            return new URL("http", realmUrl.getHost(), 8080, "").toExternalForm();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    static class SslNoneRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.sslRequired(SslRequired.NONE.toString());
        }
    }

    static class TlsEnabledConfig implements CertificatesConfig {
        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }
}
