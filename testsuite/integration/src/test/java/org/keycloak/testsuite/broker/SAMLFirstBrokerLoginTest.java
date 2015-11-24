package org.keycloak.testsuite.broker;

import org.junit.ClassRule;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SAMLFirstBrokerLoginTest extends AbstractFirstBrokerLoginTest {

    private static final int PORT = 8082;

    @ClassRule
    public static AbstractKeycloakRule samlServerRule = new AbstractKeycloakRule() {

        @Override
        protected void configureServer(KeycloakServer server) {
            server.getConfig().setPort(PORT);
        }

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-saml.json"));
        }

        @Override
        protected String[] getTestRealms() {
            return new String[] { "realm-with-saml-idp-basic" };
        }
    };

    @Override
    protected String getProviderId() {
        return "kc-saml-idp-basic";
    }
}
