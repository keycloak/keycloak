package org.keycloak.testsuite.broker;

import org.junit.ClassRule;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testutils.KeycloakServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    @Override
    protected String getProviderId() {
        return "kc-saml-signed-idp";
    }

    @Override
    protected void doAssertFederatedUser(UserModel federatedUser) {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        if (identityProviderModel.isUpdateProfileFirstLogin()) {
            super.doAssertFederatedUser(federatedUser);
        } else {
            assertEquals("test-user@localhost", federatedUser.getEmail());
            assertNull(federatedUser.getFirstName());
            assertNull(federatedUser.getLastName());
        }
    }
}
