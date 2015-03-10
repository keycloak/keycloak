package org.keycloak.testsuite.broker;

import org.junit.ClassRule;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testutils.KeycloakServer;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.keycloak.representations.idm.RealmRepresentation;

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
            //server.importRealm(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-kc-oidc.json"));
            RealmRepresentation realmWithOIDC = KeycloakServer.loadJson(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-kc-oidc.json"), RealmRepresentation.class);
            manager.importRealm(realmWithOIDC);
        }
    };

    @WebResource
    private OAuthGrantPage grantPage;

    @Override
    protected void doAfterProviderAuthentication() {
        // grant access to broker-app
        grantPage.assertCurrent();
        grantPage.accept();
    }

    @Override
    protected void doAssertTokenRetrieval(String pageSource) {
        try {
            AccessTokenResponse accessTokenResponse = JsonSerialization.readValue(pageSource, AccessTokenResponse.class);

            assertNotNull(accessTokenResponse.getToken());
            assertNotNull(accessTokenResponse.getIdToken());
        } catch (IOException e) {
            fail("Could not parse token.");
        }
    }

    @Override
    protected String getProviderId() {
        return "kc-oidc-idp";
    }
}
