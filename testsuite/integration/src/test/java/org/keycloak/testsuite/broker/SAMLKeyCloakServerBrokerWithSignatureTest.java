package org.keycloak.testsuite.broker;

import org.junit.ClassRule;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testutils.KeycloakServer;
import org.picketlink.identity.federation.api.saml.v2.request.SAML2Request;
import org.picketlink.identity.federation.saml.v2.protocol.ResponseType;
import org.picketlink.identity.federation.web.util.PostBindingUtil;

import java.net.URLDecoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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
    protected void doAssertFederatedUser(UserModel federatedUser, IdentityProviderModel identityProviderModel, String expectedEmail) {
        if (identityProviderModel.isUpdateProfileFirstLogin()) {
            super.doAssertFederatedUser(federatedUser, identityProviderModel, expectedEmail);
        } else {
            if (expectedEmail == null)
                expectedEmail = "";
            assertEquals(expectedEmail, federatedUser.getEmail());
            assertNull(federatedUser.getFirstName());
            assertNull(federatedUser.getLastName());
        }
    }

    @Override
    protected void doAssertFederatedUserNoEmail(UserModel federatedUser) {
        assertEquals("", federatedUser.getUsername());
        assertEquals("", federatedUser.getEmail());
        assertEquals(null, federatedUser.getFirstName());
        assertEquals(null, federatedUser.getLastName());
    }

    @Override
    protected void doAssertTokenRetrieval(String pageSource) {
        try {
            SAML2Request saml2Request = new SAML2Request();
            ResponseType responseType = (ResponseType) saml2Request
                        .getSAML2ObjectFromStream(PostBindingUtil.base64DecodeAsStream(pageSource));

            assertNotNull(responseType);
            assertFalse(responseType.getAssertions().isEmpty());
        } catch (Exception e) {
            fail("Could not parse token.");
        }
    }
}
