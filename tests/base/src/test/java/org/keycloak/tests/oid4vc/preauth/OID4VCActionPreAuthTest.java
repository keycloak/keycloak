package org.keycloak.tests.oid4vc.preauth;


import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.OID4VCCredentialOfferPage;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.events.Details.GRANT_TYPE;
import static org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE;
import static org.keycloak.tests.oid4vc.OID4VCActionTest.getKcActionParameter;
import static org.keycloak.tests.oid4vc.OID4VCActionTest.getNonceFromCredentialOfferUri;
import static org.keycloak.tests.oid4vc.OID4VCActionTest.verifyVCActionCredentialResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerWithPreAuthCodeEnabled.class)
public class OID4VCActionPreAuthTest extends OID4VCIssuerTestBase {

    @InjectPage
    OID4VCCredentialOfferPage credentialOfferPage;

    @InjectUser(config = TestRealmUserConfig.class)
    ManagedUser user;

    OID4VCTestContext ctx;

    @BeforeEach
    void beforeEach() {
        ctx = new OID4VCTestContext(client, minimalJwtTypeCredentialScope);
        user.admin().logout();
    }

    // Test successful scenario. Client redirects to Keycloak OIDC authentication request with "kc_action" parameter pointing to the
    // credential-offer, which should be created on Keycloak side. Upon successful authentication of the user, the credential-offer page is displayed
    // from where user can scan QR-code to his wallet and retrieve OID4 VC. Wallet uses "pre-authorization code"
    @Test
    public void testCredentialOfferAIASuccess_preAuthorizedCode() throws Exception {

        var ctx = new OID4VCTestContext(client, minimalJwtTypeCredentialScope);

        // Login as user. Check required-action displayed
        oauth.client(client.getClientId(), "test-secret");
        oauth.loginForm()
                .kcAction(getKcActionParameter(client.getClientId(), minimalJwtTypeCredentialConfigurationIdName, true))
                .open();
        oauth.fillLoginForm(user.getUsername(), "password");

        credentialOfferPage.assertCurrent();
        String credentialOfferUri = credentialOfferPage.getCredentialOfferUri();

        EventAssertion.assertSuccess(events.poll())
                .userId(user.getId())
                .details(Details.CREDENTIAL_TYPE, minimalJwtTypeCredentialConfigurationIdName)
                .details(Details.VERIFIABLE_CREDENTIAL_PRE_AUTHORIZED, String.valueOf(true))
                .details(Details.VERIFIABLE_CREDENTIAL_TARGET_USER_ID, user.getId())
                .details(Details.VERIFIABLE_CREDENTIAL_TARGET_CLIENT_ID, client.getClientId())
                .type(EventType.VERIFIABLE_CREDENTIAL_CREATE_OFFER);

        // Refresh screen. Should be still same credential-offer as before and test that there are not new events
        driver.navigate().refresh();
        credentialOfferPage.assertCurrent();
        assertEquals(credentialOfferUri, credentialOfferPage.getCredentialOfferUri());

        String credentialOfferNonce = getNonceFromCredentialOfferUri(credentialOfferUri);

        // Pre-authorized code flow with credential exchange
        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc()
                .credentialOfferRequest(credentialOfferNonce)
                .send();
        assertEquals(HttpStatus.SC_OK, credentialOfferResponse.getStatusCode());
        CredentialsOffer credOffer = credentialOfferResponse.getCredentialsOffer();

        EventAssertion.assertSuccess(events.poll())
                .userId(user.getId())
                .details(Details.CREDENTIAL_TYPE, minimalJwtTypeCredentialConfigurationIdName)
                .type(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST);

        String preAuthCode = credOffer.getPreAuthorizedCode();
        assertNotNull(preAuthCode);
        assertNull(credOffer.getIssuerState());

        // Redeem Pre-Authorized Code for AccessToken
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequestPreAuth(ctx, preAuthCode).send();
        assertTrue(tokenResponse.isSuccess(), tokenResponse.getErrorDescription());

        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken,"No accessToken");

        assertNull(ctx.getAuthorizedCredentialIdentifier(),"Not expected to have credential identifier");

        String credentialConfigId = ctx.getAuthorizedCredentialConfigurationId();
        assertEquals(minimalJwtTypeCredentialConfigurationIdName, credentialConfigId);

        EventAssertion.assertSuccess(events.poll())
                .userId(user.getId())
                .clientId(client.getClientId())
                .details(GRANT_TYPE, PRE_AUTH_GRANT_TYPE)
                .type(EventType.VERIFIABLE_CREDENTIAL_PRE_AUTHORIZED_GRANT);

        // Credential request
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialConfigurationId(credentialConfigId)
                .send().getCredentialResponse();

        EventAssertion.assertSuccess(events.poll())
                .userId(user.getId())
                .clientId(client.getClientId())
                .details(Details.CREDENTIAL_TYPE, minimalJwtTypeCredentialConfigurationIdName)
                .type(EventType.VERIFIABLE_CREDENTIAL_REQUEST);

        verifyVCActionCredentialResponse(credResponse);

        // Continue browser login inside the browser
        credentialOfferPage.clickContinueButton();
        String code = oauth.parseLoginResponse().getCode();
        assertNotNull(code, "Authorization code should not be null");
    }
}
