package org.keycloak.testsuite.ssl;

import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.AUTH_SERVER_SSL_REQUIRED;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;

/**
 * This test checks if TLS can be explicitly switched off.
 *
 * Note, it should run only if TLS is enabled by default.
 */
public class TLSTest extends AbstractTestRealmKeycloakTest {

    public static final String AUTH_SERVER_ROOT_WITHOUT_TLS = "http://localhost:" + System.getProperty("auth.server.http.port", "8180") + "/auth";

    @BeforeClass
    public static void checkIfTLSIsTurnedOn() {
        Assume.assumeTrue(AUTH_SERVER_SSL_REQUIRED);
    }

    @Override
    protected boolean modifyRealmForSSL() {
        return false;
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setSslRequired(SslRequired.NONE.toString());
    }

    @Test
    public void testTurningTLSOn() throws Exception {
        //given
        oauth.baseUrl(AUTH_SERVER_ROOT_WITHOUT_TLS);

        //when
        OIDCConfigurationRepresentation config = oauth.doWellKnownRequest("test");

        //then
        Assert.assertTrue(config.getAuthorizationEndpoint().startsWith(AUTH_SERVER_ROOT_WITHOUT_TLS));
    }

}
