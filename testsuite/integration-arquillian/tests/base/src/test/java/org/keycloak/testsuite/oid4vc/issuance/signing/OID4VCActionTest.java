package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.io.IOException;
import java.util.List;

import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oid4vc.issuance.requiredactions.VerifiableCredentialOfferAction;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.OID4VCCredentialOfferPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OpenIDProviderConfigurationResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialIssuerMetadataResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;

import org.apache.http.HttpStatus;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.constants.OID4VCIConstants.VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID;
import static org.keycloak.events.Details.CREDENTIAL_TYPE;
import static org.keycloak.events.Details.CUSTOM_REQUIRED_ACTION;
import static org.keycloak.events.Details.GRANT_TYPE;
import static org.keycloak.events.Details.REASON;
import static org.keycloak.events.Errors.INVALID_REQUEST;
import static org.keycloak.events.Errors.REJECTED_BY_USER;
import static org.keycloak.protocol.oid4vc.model.ErrorType.MISSING_CREDENTIAL_CONFIG;
import static org.keycloak.protocol.oid4vc.model.ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class OID4VCActionTest extends OID4VCIssuerEndpointTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    private OID4VCCredentialOfferPage credentialOfferPage;

    private Oid4vcTestContext ctx;

    protected static class Oid4vcTestContext {
        CredentialIssuer credentialIssuer;
        OIDCConfigurationRepresentation openidConfig;
    }

    @Before
    public void prepareOid4vcTestContext() throws Exception {
        Oid4vcTestContext ctx = new Oid4vcTestContext();
        CredentialIssuerMetadataResponse issuerMetadataResponse = oauth.oid4vc().issuerMetadataRequest()
                .endpoint(getRealmMetadataPath(TEST_REALM_NAME))
                .send();
        assertEquals(HttpStatus.SC_OK, issuerMetadataResponse.getStatusCode());
        ctx.credentialIssuer = issuerMetadataResponse.getMetadata();

        OpenIDProviderConfigurationResponse openIDProviderConfigurationResponse = oauth.wellknownRequest()
                .url(ctx.credentialIssuer.getAuthorizationServers().get(0))
                .send();
        assertEquals(HttpStatus.SC_OK, openIDProviderConfigurationResponse.getStatusCode());
        ctx.openidConfig = openIDProviderConfigurationResponse.getOidcConfiguration();

        this.ctx = ctx;
    }

    private String getKcActionParameter(String credentialConfigId) {
        try {
            VerifiableCredentialOfferAction.CredentialOfferActionConfig cfg = new VerifiableCredentialOfferAction.CredentialOfferActionConfig();
            cfg.setCredentialConfigurationId(credentialConfigId);
            cfg.setClientId(client.getClientId());
            cfg.setPreAuthorized(true);
            String cfgAsString = cfg.asEncodedParameter();
            return VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID + ":" + cfgAsString;
        } catch (IOException ioe) {
            Assert.fail("Failed to encode parameter: " + ioe.getMessage());
            return null;
        }
    }


    // Test successful scenario. Client redirects to Keycloak OIDC authentication request with "kc_action" parameter pointing to the
    // credential-offer, which should be created on Keycloak side. Upon successful authentication of the user, the credential-offer page is displayed
    // from where user can scan QR-code to his wallet and retrieve OID4 VC
    @Test
    public void testCredentialOfferAIASuccess() throws Exception {
        // Login as user. Check required-action displayed
        oauth.client(client.getClientId(), "password");
        oauth.loginForm()
                .kcAction(getKcActionParameter("sd-jwt-credential-config-id"))
                .doLogin("john", "password");

        credentialOfferPage.assertCurrent();
        String credentialOfferUri = credentialOfferPage.getCredentialOfferUri();

        events.expect(EventType.VERIFIABLE_CREDENTIAL_CREATE_OFFER)
                .user(AssertEvents.isUUID())
                .session((String) null)
                .detail(Details.CREDENTIAL_TYPE, "sd-jwt-credential-config-id")
                .detail(Details.VERIFIABLE_CREDENTIAL_TARGET_USER_ID, AssertEvents.isUUID())
                .detail(Details.VERIFIABLE_CREDENTIAL_TARGET_CLIENT_ID, "test-app")
                .assertEvent();

        // Refresh screen. Should be still same credential-offer as before and test that there are not new events
        driver.navigate().refresh();
        credentialOfferPage.assertCurrent();
        Assert.assertEquals(credentialOfferUri, credentialOfferPage.getCredentialOfferUri());
        events.assertEmpty();

        String credentialOfferNonce = getNonceFromCredentialOfferUri(credentialOfferUri);

        // Pre-authorized code flow with credential exchange
        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc().credentialOfferRequest(credentialOfferNonce)
                .send();
        assertEquals(HttpStatus.SC_OK, credentialOfferResponse.getStatusCode());
        CredentialsOffer credOffer = credentialOfferResponse.getCredentialsOffer();

        events.expect(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST)
                .user(AssertEvents.isUUID())
                .session((String) null)
                .detail(Details.CREDENTIAL_TYPE, "sd-jwt-credential-config-id")
                .assertEvent();

        PreAuthorizedCode preAuthorizedCode = credOffer.getGrants().getPreAuthorizedCode();
        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(preAuthorizedCode.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .send();

        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertEquals(1, authDetailsResponse.size());
        assertNotNull("Credential identifiers should be present", authDetailsResponse.get(0).getCredentialIdentifiers());
        assertEquals(1, authDetailsResponse.get(0).getCredentialIdentifiers().size());
        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);

        events.expect(EventType.CODE_TO_TOKEN)
                .user(AssertEvents.isUUID())
                .client("test-app")
                .detail(GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE)
                .assertEvent();

        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .endpoint(ctx.credentialIssuer.getCredentialEndpoint())
                .bearerToken(tokenResponse.getAccessToken())
                .credentialIdentifier(credentialIdentifier)
                .send();

        assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusCode());

        // Parse the credential response
        CredentialResponse parsedResponse = credentialResponse.getCredentialResponse();
        assertNotNull("Credential response should not be null", parsedResponse);
        assertNotNull("Credentials should be present", parsedResponse.getCredentials());
        assertEquals("Should have exactly one credential", 1, parsedResponse.getCredentials().size());

        // Verify CREDENTIAL_REQUEST event was fired
        events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST)
                .client(client.getClientId())
                .user(AssertEvents.isUUID())
                .session(AssertEvents.isSessionId())
                .detail(Details.USERNAME, "john")
                .detail(Details.CREDENTIAL_TYPE, "sd-jwt-credential-config-id")
                .assertEvent();

        // Continue browser login. Should be fine
        credentialOfferPage.clickContinueButton();
        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);
    }

    // Test scenario when user press "cancel" on the page with credential-offer. Credential offer was rejected by user and removed from credentialOfferStorage
    @Test
    public void testCredentialOfferAIACancel() throws Exception {
        // Login as user. Check required-action displayed
        oauth.client(client.getClientId(), "password");
        oauth.loginForm()
                .kcAction(getKcActionParameter("sd-jwt-credential-config-id"))
                .doLogin("john", "password");

        credentialOfferPage.assertCurrent();
        String credentialOfferUri = credentialOfferPage.getCredentialOfferUri();
        String credentialOfferNonce = getNonceFromCredentialOfferUri(credentialOfferUri);

        events.clear();

        // Cancel AIA
        credentialOfferPage.cancel();
        events.expect(EventType.CUSTOM_REQUIRED_ACTION_ERROR)
                .user(AssertEvents.isUUID())
                .session((String) null)
                .detail(CUSTOM_REQUIRED_ACTION, VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID)
                .error(REJECTED_BY_USER)
                .assertEvent();
        events.clear();

        // Should not be possible to retrieve credential-offer as credential-offer was removed by user clicking "cancel"
        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc().credentialOfferRequest(credentialOfferNonce)
                .send();
        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialOfferResponse.getStatusCode());

        events.expect(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST_ERROR)
                .user((String) null)
                .client((String)null)
                .session((String) null)
                .detail(REASON, "Credential offer not found or already consumed")
                .error(INVALID_REQUEST)
                .assertEvent();
    }

    private String getNonceFromCredentialOfferUri(String credentialOfferUri) {
        return credentialOfferUri.substring(credentialOfferUri.lastIndexOf("/") + 1);
    }

    // Test for some error scenarios (incorrect values of "kc_action" referencing non-existent client scope etc).
    @Test
    public void testCredentialOfferErrors() throws Exception {
        oauth.client(client.getClientId(), "password");

        // Test missing kc_action_parameter
        oauth.loginForm()
                .kcAction(VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID)
                .doLogin("john", "password");
        assertFalse(credentialOfferPage.isCurrent());

        events.expect(EventType.VERIFIABLE_CREDENTIAL_CREATE_OFFER_ERROR)
                .user(AssertEvents.isUUID())
                .client("test-app")
                .session((String) null)
                .error(MISSING_CREDENTIAL_CONFIG.getValue())
                .assertEvent();
        events.clear();

        // Test kc_action_parameter referencing incorrect credentialConfig
        oauth.loginForm()
                .kcAction(getKcActionParameter("unknown-config-id"))
                .open();
        assertFalse(credentialOfferPage.isCurrent());

        events.expect(EventType.VERIFIABLE_CREDENTIAL_CREATE_OFFER_ERROR)
                .user(AssertEvents.isUUID())
                .client("test-app")
                .session((String) null)
                .detail(CREDENTIAL_TYPE, "unknown-config-id")
                .detail(REASON, "Client scope was not found for credential configuration ID: unknown-config-id")
                .error(UNKNOWN_CREDENTIAL_CONFIGURATION.getValue())
                .assertEvent();
        events.clear();
    }

}
