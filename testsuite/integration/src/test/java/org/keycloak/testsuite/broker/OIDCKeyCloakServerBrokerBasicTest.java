package org.keycloak.testsuite.broker;

import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.NoSuchElementException;

import java.io.IOException;

import javax.ws.rs.core.UriBuilder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author pedroigor
 */
public class OIDCKeyCloakServerBrokerBasicTest extends AbstractIdentityProviderTest {

    private static final int PORT = 8082;

    @ClassRule
    public static AbstractKeycloakRule samlServerRule = new AbstractKeycloakRule() {

        @Override
        protected void configureServer(KeycloakServer server) {
            server.getConfig().setPort(PORT);
        }

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-kc-oidc.json"));
        }

        @Override
        protected String[] getTestRealms() {
            return new String[] { "realm-with-oidc-identity-provider" };
        }
    };

    @WebResource
    private OAuthGrantPage grantPage;

    @WebResource
    protected AccountApplicationsPage accountApplicationsPage;

    @Override
    protected void revokeGrant() {
        String currentUrl = driver.getCurrentUrl();

        String accountAccessPath = Urls.accountApplicationsPage(UriBuilder.fromUri(Constants.AUTH_SERVER_ROOT).port(PORT).build(), "realm-with-oidc-identity-provider").toString();
        accountApplicationsPage.setPath(accountAccessPath);
        accountApplicationsPage.open();
        try {
            accountApplicationsPage.revokeGrant("broker-app");
        } catch (NoSuchElementException e) {
            System.err.println("Couldn't revoke broker-app application, maybe because it wasn't granted or user not logged");
        }

        driver.navigate().to(currentUrl);
    }

    @Override
    protected void doAfterProviderAuthentication() {
        // grant access to broker-app
        //grantPage.assertCurrent();
        //grantPage.accept();
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

    @Test
    public void testSuccessfulAuthentication() {
        super.testSuccessfulAuthentication();
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile() {
        super.testSuccessfulAuthenticationWithoutUpdateProfile();
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_emailNotProvided_emailVerifyEnabled() {
        super.testSuccessfulAuthenticationWithoutUpdateProfile_emailNotProvided_emailVerifyEnabled();
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_newUser_emailAsUsername() {
        super.testSuccessfulAuthenticationWithoutUpdateProfile_newUser_emailAsUsername();
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_newUser_emailAsUsername_emailNotProvided() {
        super.testSuccessfulAuthenticationWithoutUpdateProfile_newUser_emailAsUsername_emailNotProvided();
    }

    @Test
    public void testTokenStorageAndRetrievalByApplication() {
        super.testTokenStorageAndRetrievalByApplication();
    }

    @Test
    public void testAccountManagementLinkIdentity() {
        super.testAccountManagementLinkIdentity();
    }
}
