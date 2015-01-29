package org.keycloak.testsuite.broker;

import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testutils.KeycloakServer;

/**
 * @author pedroigor
 */
public class SAMLKeyCloakServerBrokerWithSignatureTest extends AbstractIdentityProviderTest {

    @ClassRule
    public static AbstractKeycloakRule samlServerRule = new AbstractKeycloakRule() {

        @Override
        protected void configureServer(KeycloakServer server) {
            server.getConfig().setPort(8082);
        }

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-saml-with-signature.json"));
        }
    };

    @Test
    public void testSuccessfulAuthentication() {
        assertSuccessfulAuthentication("kc-saml-signed-idp");
    }
}
