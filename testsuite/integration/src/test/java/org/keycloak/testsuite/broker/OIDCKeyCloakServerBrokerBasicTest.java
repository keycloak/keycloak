package org.keycloak.testsuite.broker;

import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testutils.KeycloakServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author pedroigor
 */
public class OIDCKeyCloakServerBrokerBasicTest extends AbstractIdentityProviderTest {

    @ClassRule
    public static AbstractKeycloakRule samlServerRule = new AbstractKeycloakRule() {

        @Override
        protected void configureServer(KeycloakServer server) {
            server.getConfig().setPort(8082);
        }

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-oidc.json"));
        }
    };

    @WebResource
    private OAuthGrantPage grantPage;

    @Test
    public void testSuccessfulAuthentication() {
        assertSuccessfulAuthentication("kc-oidc-idp");
    }

    @Override
    protected void doAfterProviderAuthentication(String providerId) {
        // grant access to broker-app
        grantPage.assertCurrent();
        grantPage.accept();
    }

    @Override
    protected void doUpdateProfile(String providerId) {
    }

    @Override
    protected void doAssertFederatedUser(String providerId) {
        UserModel userModel = getFederatedUser();

        assertNotNull(userModel);
        assertEquals("test-user@localhost", userModel.getEmail());
        assertEquals("Test", userModel.getFirstName());
        assertEquals("User", userModel.getLastName());
    }
}
