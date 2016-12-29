package org.keycloak.testsuite.oauth;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.util.*;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.OAuthClient.APP_ROOT;

/**
 * @author <a href="mailto:w@willsr.com">Will Russell</a>
 */
public class OAuthPKCETest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected ErrorPage errorPage;

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realmRepresentation);
    }

    @Before
    public void clientConfiguration() {
        oauth.clientId("test-app");
        oauth.responseType(OAuth2Constants.CODE);
        oauth.responseMode(null);
    }

    @Test
    public void invalidCodeChallengeMethod() throws IOException {
        oauth.codeChallenge("test");
        oauth.codeChallengeMethod("MD5");

        oauth.openLoginForm();
        assertTrue(errorPage.isCurrent());
        assertEquals("Invalid parameter: code_challenge_method", errorPage.getError());
    }

    @Test
    public void codeChallengePlain() throws Exception {
        oauth.codeChallenge("test");
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.codeVerifier("test");

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void codeChallengeSHA256() throws Exception {
        String codeChallenge = "test";
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        String codeChallengeHashed = Base64Url.encode(sha256.digest(codeChallenge.getBytes(StandardCharsets.US_ASCII)));
        oauth.codeChallenge(codeChallengeHashed);
        oauth.codeChallengeMethod("S256");

        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.codeVerifier(codeChallenge);

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void invalidCodeVerifierPlain() throws Exception {
        oauth.codeChallenge("test");
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.codeVerifier("nottest");

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
    }

    @Test
    public void invalidCodeVerifierSHA256() throws Exception {
        oauth.codeChallenge("test");
        oauth.codeChallengeMethod("S256");

        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.codeVerifier("test");

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
    }

    @Test
    public void missingCodeVerifier() throws Exception {
        oauth.codeChallenge("test");

        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
    }
}
