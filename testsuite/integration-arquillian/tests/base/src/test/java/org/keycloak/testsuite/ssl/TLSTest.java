package org.keycloak.testsuite.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.util.OAuthClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
