package org.keycloak.tests.oid4vc;

import java.io.IOException;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.BadRequestException;

import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;
import org.keycloak.representations.idm.oid4vc.VerifiableCredentialOfferActionConfig;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.OID4VCCredentialOfferPage;
import org.keycloak.testframework.ui.page.ProceedPage;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.oid4vc.OID4VCActionTest.getNonceFromCredentialOfferUri;
import static org.keycloak.tests.oid4vc.OID4VCActionTest.verifyVCActionCredentialResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

//Test for sending credential-offer by the administrator to the user
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCAdminActionTest extends OID4VCIssuerTestBase {

    @InjectPage
    OID4VCCredentialOfferPage credentialOfferPage;

    @InjectPage
    InfoPage infoPage;

    @InjectPage
    ProceedPage proceedPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectUser(config = OID4VCActionTest.OID4VCTestUserConfig.class)
    ManagedUser user;

    @InjectMailServer
    MailServer mailServer;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectClient(ref = "oidc-client", config = OIDCConfidentialClient.class)
    ManagedClient oidcClient;

    OID4VCTestContext ctx;

    @BeforeEach
    void beforeEach() {
        ctx = new OID4VCTestContext(client, minimalJwtTypeCredentialScope);
        user.admin().logout();
        adminEvents.clear();
    }

    @Test
    public void testAdminCredentialOfferEmailSuccess() throws Exception {
        String link = sendEmailAndGetLink(null, null, null, "This link will expire within 12 hours");

        driver.open(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Claim your vc-with-minimal-config-id"));
        proceedPage.clickProceedLink();

        EventAssertion.assertSuccess(events.poll())
                .userId(user.getId())
                .clientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)
                .details(Details.CREDENTIAL_TYPE, minimalJwtTypeCredentialConfigurationIdName)
                .details(Details.VERIFIABLE_CREDENTIAL_PRE_AUTHORIZED, String.valueOf(false))
                .details(Details.VERIFIABLE_CREDENTIAL_TARGET_USER_ID, user.getId())
                .withoutDetails(Details.VERIFIABLE_CREDENTIAL_TARGET_CLIENT_ID)
                .type(EventType.VERIFIABLE_CREDENTIAL_CREATE_OFFER);

        credentialOfferPage.assertCurrent();
        assertFalse(credentialOfferPage.isCancelDisplayed());
        String credentialOfferUri = credentialOfferPage.getCredentialOfferUri();
        assertNotNull(credentialOfferUri);

        // Continue OID4VCI flow and make sure it is successful
        loginSuccessForAuthorizationCodeCredentialOffer(credentialOfferUri);
    }


    @Test
    public void testAdminCredentialOfferReply() throws Exception {
        String link = sendEmailAndGetLink(null, null, null, "This link will expire within 12 hours");

        driver.open(link);

        proceedPage.assertCurrent();
        assertThat(proceedPage.getInfo(), Matchers.containsString("Claim your vc-with-minimal-config-id"));
        proceedPage.clickProceedLink();

        credentialOfferPage.assertCurrent();
        credentialOfferPage.clickContinueButton();

        infoPage.assertCurrent();
        assertEquals("Your account has been updated.", infoPage.getInfo());

        // Try to reply action
        driver.open(link);
        errorPage.assertCurrent();
        assertEquals("Action expired. Please continue with login now.", errorPage.getError());
    }

    @Test
    public void testAdminCredentialOfferClientId() throws Exception {
        String link = sendEmailAndGetLink("oidc-client", null, 7200, "This link will expire within 2 hours");

        driver.open(link);
        proceedPage.clickProceedLink();
        credentialOfferPage.clickContinueButton();

        infoPage.assertCurrent();
        assertEquals("Your account has been updated.", infoPage.getInfo());
        infoPage.clickBackToApplicationLink();

        assertEquals("http://localhost:8080/test-app", driver.driver().getCurrentUrl());
    }

    @Test
    public void testAdminCredentialOfferClientIdAndRedirectUri() throws Exception {
        String link = sendEmailAndGetLink("oidc-client", "http://localhost:8080/test-app/callback", 7200, "This link will expire within 2 hours");

        driver.open(link);
        proceedPage.clickProceedLink();
        credentialOfferPage.clickContinueButton();

        infoPage.assertCurrent();
        assertEquals("Your account has been updated.", infoPage.getInfo());
        infoPage.clickBackToApplicationLink();

        // Straight redirect after click "Continue". Info page not shown
        assertEquals("http://localhost:8080/test-app/callback", driver.driver().getCurrentUrl());
    }

    @Test
    public void testAdminCredentialOfferErrorInvalidRedirectUri() {
        VerifiableCredentialOfferActionConfig actionConfig = getActionConfig(minimalJwtTypeCredentialConfigurationIdName, null, false);
        try {
            user.admin().verifiableCredentials().sendCredentialOffer("oidc-client", "http://localhost:8080/invalid", null, actionConfig);
            fail("Not expected to successfully send the credential offer");
        } catch (BadRequestException bre) {
            ErrorRepresentation error = bre.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Invalid redirect uri.", error.getErrorMessage());
        }
    }

    @Test
    public void testAdminCredentialOfferErrorInvalidAction() {
        VerifiableCredentialOfferActionConfig actionConfig = getActionConfig("unknown", null, false);
        try {
            user.admin().verifiableCredentials().sendCredentialOffer(null, null, null, actionConfig);
            fail("Not expected to successfully send the credential offer");
        } catch (BadRequestException bre) {
            ErrorRepresentation error = bre.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("Client scope was not found for specified credential configuration ID", error.getErrorMessage());
        }
    }

    @Test
    public void testAdminCredentialOfferErrorActionMissingForUser() {
        VerifiableCredentialOfferActionConfig actionConfig = getActionConfig(sdJwtTypeNaturalPersonScopeName, null, false);
        try {
            user.admin().verifiableCredentials().sendCredentialOffer(null, null, null, actionConfig);
            fail("Not expected to successfully send the credential offer");
        } catch (BadRequestException bre) {
            ErrorRepresentation error = bre.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("User does not have requested credential scope", error.getErrorMessage());
        }
    }

    // Test for the scenario when user has VC at the time of sending email, but then credential is removed from him. When reading email, he does not have credential anymore
    @Test
    public void testAdminCredentialOfferErrorActionRemovedFromUser() throws Exception {
        String link = sendEmailAndGetLink(null, null, null, "This link will expire within 12 hours");

        user.admin().verifiableCredentials().revokeCredential(minimalJwtTypeCredentialScopeName);

        try {
            driver.open(link);
            errorPage.assertCurrent();
            assertEquals("Invalid Request", errorPage.getError());
        } finally {
            UserVerifiableCredentialRepresentation credRep = new UserVerifiableCredentialRepresentation();
            credRep.setCredentialScopeName(minimalJwtTypeCredentialScopeName);
            user.admin().verifiableCredentials().createCredential(credRep);
        }
    }

    private String sendEmailAndGetLink(String clientId, String redirectUri, Integer lifespan, String expectedLifespanMessage) throws IOException {
        VerifiableCredentialOfferActionConfig actionConfig = getActionConfig(minimalJwtTypeCredentialConfigurationIdName, null, false);
        user.admin().verifiableCredentials().sendCredentialOffer(clientId, redirectUri, lifespan, actionConfig);

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.ACTION, AdminEventPaths.userVerifiableCredentialsPath(user.getId()) + "/send-credential-offer", null, ResourceType.USER);

        Assertions.assertEquals(1, mailServer.getReceivedMessages().length);

        MimeMessage message = mailServer.getReceivedMessages()[0];

        MailUtils.EmailBody body = MailUtils.getBody(message);

        assertTrue(body.getText().contains("Your administrator has just informed you that in your Test account you can claim verifiable credential"));
        assertTrue(body.getText().contains("vc-with-minimal-config-id"));
        assertTrue(body.getText().contains(expectedLifespanMessage));

        return MailUtils.getPasswordResetEmailLink(body);
    }

    private VerifiableCredentialOfferActionConfig getActionConfig(String credentialConfigId, String clientId, boolean preAuthorized) {
        VerifiableCredentialOfferActionConfig cfg = new VerifiableCredentialOfferActionConfig();
        cfg.setCredentialConfigurationId(credentialConfigId);
        cfg.setPreAuthorized(preAuthorized);
        cfg.setClientId(clientId);
        return cfg;
    }

    private void loginSuccessForAuthorizationCodeCredentialOffer(String credentialOfferUri) {
        String credentialOfferNonce = getNonceFromCredentialOfferUri(credentialOfferUri);

        // Obtain credential offer
        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc().credentialOfferRequest(credentialOfferNonce)
                .send();
        assertEquals(HttpStatus.SC_OK, credentialOfferResponse.getStatusCode());
        CredentialsOffer credOffer = credentialOfferResponse.getCredentialsOffer();

        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState);
        assertNull(credOffer.getPreAuthorizedCode());

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .scope(ctx.getScope())
                .issuerState(issuerState)
                .send(user.getUsername(), TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(credentialIdentifier, "Expected to have credential identifier");

        String credentialConfigId = ctx.getAuthorizedCredentialConfigurationId();
        assertEquals(minimalJwtTypeCredentialConfigurationIdName, credentialConfigId);

        // Credential request
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();

        verifyVCActionCredentialResponse(credResponse);
    }


    public static class OIDCConfidentialClient implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            client.clientId("oidc-client")
                    .serviceAccountsEnabled(false)
                    .directAccessGrantsEnabled(false)
                    .defaultClientScopes("basic", "profile", "roles")
                    .baseUrl("http://localhost:8080/test-app")
                    .redirectUris("http://localhost:8080/test-app", "http://localhost:8080/test-app/callback")
                    .secret("test-secret");
            return client;
        }
    }
}
