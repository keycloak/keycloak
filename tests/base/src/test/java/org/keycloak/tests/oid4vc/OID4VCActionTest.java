package org.keycloak.tests.oid4vc;

import java.io.IOException;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oid4vc.issuance.requiredactions.VerifiableCredentialOfferAction;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.OID4VCCredentialOfferPage;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.tests.utils.Assert;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_CODE;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;
import static org.keycloak.constants.OID4VCIConstants.VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID;
import static org.keycloak.events.Details.CREDENTIAL_TYPE;
import static org.keycloak.events.Details.CUSTOM_REQUIRED_ACTION;
import static org.keycloak.events.Details.GRANT_TYPE;
import static org.keycloak.events.Details.REASON;
import static org.keycloak.events.Errors.REJECTED_BY_USER;
import static org.keycloak.protocol.oid4vc.model.ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST;
import static org.keycloak.protocol.oid4vc.model.ErrorType.MISSING_CREDENTIAL_CONFIG;
import static org.keycloak.protocol.oid4vc.model.ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCActionTest extends OID4VCIssuerTestBase {

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

    public static String getKcActionParameter(String clientId, String credentialConfigId, boolean preAuthorized) {
        try {
            VerifiableCredentialOfferAction.CredentialOfferActionConfig cfg = new VerifiableCredentialOfferAction.CredentialOfferActionConfig();
            cfg.setCredentialConfigurationId(credentialConfigId);
            cfg.setPreAuthorized(preAuthorized);
            cfg.setClientId(clientId);
            String cfgAsString = cfg.asEncodedParameter();
            return VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID + ":" + cfgAsString;
        } catch (IOException ioe) {
            Assert.fail("Failed to encode parameter: " + ioe.getMessage());
            return null;
        }
    }

    public static String getNonceFromCredentialOfferUri(String credentialOfferUri) {
        return credentialOfferUri.substring(credentialOfferUri.lastIndexOf("/") + 1);
    }

    public static void verifyVCActionCredentialResponse(CredentialResponse credResponse) {
        CredentialResponse.Credential credentialObj = credResponse.getCredentials().get(0);
        assertNotNull(credentialObj, "The first credential in the array should not be null");
        IssuerSignedJWT issuerSignedJWT = SdJwtVP.of(credentialObj.getCredential().toString()).getIssuerSignedJWT();
        assertEquals(minimalJwtTypeCredentialScopeName, issuerSignedJWT.getPayload().get(CLAIM_NAME_VCT).asText());
    }

    // Test successful scenario with wallet using "authorization code" grant
    @Test
    public void testCredentialOfferAIASuccess_authorizationCodeFlow() throws Exception {
        // Login as user. Check required-action displayed
        oauth.client(client.getClientId(), "test-secret");
        oauth.loginForm()
                .kcAction(getKcActionParameter(client.getClientId(), minimalJwtTypeCredentialConfigurationIdName, false))
                .open();
        oauth.fillLoginForm(user.getUsername(), "password");

        credentialOfferPage.assertCurrent();
        String credentialOfferUri = credentialOfferPage.getCredentialOfferUri();

        EventAssertion.assertSuccess(events.poll())
                .userId(user.getId())
                .details(Details.CREDENTIAL_TYPE, minimalJwtTypeCredentialConfigurationIdName)
                .details(Details.VERIFIABLE_CREDENTIAL_PRE_AUTHORIZED, String.valueOf(false))
                .details(Details.VERIFIABLE_CREDENTIAL_TARGET_USER_ID, user.getId())
                .details(Details.VERIFIABLE_CREDENTIAL_TARGET_CLIENT_ID, client.getClientId())
                .type(EventType.VERIFIABLE_CREDENTIAL_CREATE_OFFER);

        // Refresh screen. Should be still same credential-offer as before and test that there are not new events
        driver.navigate().refresh();
        credentialOfferPage.assertCurrent();
        assertEquals(credentialOfferUri, credentialOfferPage.getCredentialOfferUri());

        String credentialOfferNonce = getNonceFromCredentialOfferUri(credentialOfferUri);

        // Obtain credential offer
        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc().credentialOfferRequest(credentialOfferNonce)
                .send();
        assertEquals(HttpStatus.SC_OK, credentialOfferResponse.getStatusCode());
        CredentialsOffer credOffer = credentialOfferResponse.getCredentialsOffer();

        EventAssertion.assertSuccess(events.poll())
                .userId(user.getId())
                .details(Details.CREDENTIAL_TYPE, minimalJwtTypeCredentialConfigurationIdName)
                .type(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST);

        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState);
        assertNull(credOffer.getPreAuthorizedCode());

        // Send AuthorizationRequest
        //
        AuthorizationEndpointResponse authResponse = wallet
                .authorizationRequest()
                .scope(ctx.getScope())
                .issuerState(issuerState)
                .send(user.getUsername(), "password");
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No authCode");

        // Build and send AccessTokenRequest
        //
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        assertNull(ctx.getAuthorizedCredentialIdentifier(), "Not expected to have credential identifier");

        String credentialConfigId = ctx.getAuthorizedCredentialConfigurationId();
        assertEquals(minimalJwtTypeCredentialConfigurationIdName, credentialConfigId);

        EventAssertion.assertSuccess(events.poll())
                .userId(user.getId())
                .clientId(client.getClientId())
                .type(EventType.LOGIN);
        EventAssertion.assertSuccess(events.poll())
                .userId(user.getId())
                .clientId(client.getClientId())
                .details(GRANT_TYPE, AUTHORIZATION_CODE)
                .type(EventType.CODE_TO_TOKEN);

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
    }


    // Test scenario when user press "cancel" on the page with credential-offer. Credential offer was rejected by user and removed from credentialOfferStorage
    @Test
    public void testCredentialOfferAIACancel() {
        // Login as user. Check required-action displayed
        oauth.client(client.getClientId(), "test-secret");
        oauth.loginForm()
                .kcAction(getKcActionParameter(client.getClientId(), minimalJwtTypeCredentialConfigurationIdName, false))
                .open();
        oauth.fillLoginForm(user.getUsername(), "password");

        credentialOfferPage.assertCurrent();
        String credentialOfferUri = credentialOfferPage.getCredentialOfferUri();
        String credentialOfferNonce = getNonceFromCredentialOfferUri(credentialOfferUri);
        events.clear();

        // Cancel AIA
        credentialOfferPage.cancel();
        EventAssertion.assertError(events.poll())
                .userId(user.getId())
                .clientId(client.getClientId())
                .details(CUSTOM_REQUIRED_ACTION, VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID)
                .error(REJECTED_BY_USER)
                .type(EventType.CUSTOM_REQUIRED_ACTION_ERROR);
        events.clear();

        // Should not be possible to retrieve credential-offer as credential-offer was removed by user clicking "cancel"
        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc().credentialOfferRequest(credentialOfferNonce)
                .send();
        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialOfferResponse.getStatusCode());

        EventAssertion.assertError(events.poll())
                .details(REASON, "Credential offer not found or already consumed")
                .error(INVALID_CREDENTIAL_OFFER_REQUEST.getValue())
                .type(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST_ERROR);
    }

    // Test for some error scenarios (incorrect values of "kc_action" referencing non-existent client scope etc).
    @Test
    public void testCredentialOfferErrors() {
        oauth.client(client.getClientId(), "test-secret");

        // Test missing kc_action_parameter
        oauth.client(client.getClientId(), "password");
        AuthorizationEndpointResponse authzCodeResponse = oauth.loginForm()
                .kcAction(VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID)
                .doLogin(user.getUsername(), "password");
        assertNotNull(authzCodeResponse.getCode());
        assertEquals(RequiredActionContext.KcActionStatus.ERROR.name().toLowerCase(), authzCodeResponse.getKcActionStatus());

        EventAssertion.assertError(events.poll())
                .userId(user.getId())
                .clientId(client.getClientId())
                .error(MISSING_CREDENTIAL_CONFIG.getValue())
                .type(EventType.VERIFIABLE_CREDENTIAL_CREATE_OFFER_ERROR);
        events.clear();

        // Test kc_action_parameter referencing incorrect credentialConfig
        oauth.loginForm()
                .kcAction(getKcActionParameter(client.getClientId(), "unknown-config-id", false))
                .open();
        authzCodeResponse = new AuthorizationEndpointResponse(oauth);
        assertNotNull(authzCodeResponse.getCode());
        assertEquals(RequiredActionContext.KcActionStatus.ERROR.name().toLowerCase(), authzCodeResponse.getKcActionStatus());

        EventAssertion.assertError(events.poll())
                .userId(user.getId())
                .clientId(client.getClientId())
                .error(UNKNOWN_CREDENTIAL_CONFIGURATION.getValue())
                .details(CREDENTIAL_TYPE, "unknown-config-id")
                .details(REASON, "Client scope was not found for credential configuration ID: unknown-config-id")
                .type(EventType.VERIFIABLE_CREDENTIAL_CREATE_OFFER_ERROR);
        events.clear();
    }

}
