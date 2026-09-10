package org.keycloak.tests.oid4vc;

import java.util.List;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_CODE;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;
import static org.keycloak.events.Details.GRANT_TYPE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for the scenarios where consent screen is required when issuing OID4VCI credentials.
 * Related specification section: <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-user-consent">OID4VCI 15.1</a>
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCConsentTest extends OID4VCIssuerTestBase {

    protected OID4VCTestContext ctx;

    @InjectUser(config = OID4VCConsentTestUserConfig.class)
    ManagedUser user;

    @InjectClient(ref = "oid4vci-consent-client", config = OID4VCConfidentialClientWithConsentRequired.class)
    protected ManagedClient managedConsentClient;

    @InjectPage
    protected OAuthGrantPage grantPage;

    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();
        CredentialScopeRepresentation sdJwtNaturalPerson = getCredentialScope(sdJwtTypeNaturalPersonScopeName);
        sdJwtNaturalPerson.setBindingRequired(false);
        testRealm.admin().clientScopes().get(sdJwtNaturalPerson.getId()).update(sdJwtNaturalPerson);
    }

    @BeforeEach
    void setUp() {
        // Clean up before starting
        user.admin().logout();
        try {
            user.admin().revokeConsent("oid4vci-consent-client");
        } catch (NotFoundException nfe) {
            // Can ignore the case when consent not present
        }
    }

    @Test
    public void loginUserWithConsentScreen() {
        ClientRepresentation consentClient = managedConsentClient.admin().toRepresentation();

        loginUserAndObtainVC(consentClient, true, true, Details.CONSENT_VALUE_CONSENT_GRANTED);

        // Consent screen not displayed again as consent was already granted
        loginUserAndObtainVC(consentClient, false, false, Details.CONSENT_VALUE_PERSISTED_CONSENT);
    }

    @Test
    public void loginUserWithRevokedConsent() {
        ClientRepresentation consentClient = managedConsentClient.admin().toRepresentation();

        // Login user and make sure consent screen displayed
        loginUserAndObtainVC(consentClient,  true, true, Details.CONSENT_VALUE_CONSENT_GRANTED);
        loginUserAndObtainVC(client, false, false, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);

        // Check that issued-credentials are available
        List<IssuedVerifiableCredentialRepresentation> issuedCreds = user.admin().verifiableCredentials().getIssuedCredentials();
        assertEquals(2, issuedCreds.size());
        assertEquals(1, countOfIssuedCredsByClient(issuedCreds, managedConsentClient.getId()));
        assertEquals(1, countOfIssuedCredsByClient(issuedCreds, managedClient.getId()));

        // Revoke consent
        user.admin().revokeConsent("oid4vci-consent-client");

        // Check that issued-credential were revoked as well
        issuedCreds = user.admin().verifiableCredentials().getIssuedCredentials();
        assertEquals(1, issuedCreds.size());
        assertEquals(0, countOfIssuedCredsByClient(issuedCreds, managedConsentClient.getId()));
        assertEquals(1, countOfIssuedCredsByClient(issuedCreds, managedClient.getId()));

        // Re-login. Will require consent again
        loginUserAndObtainVC(consentClient, false, true, Details.CONSENT_VALUE_CONSENT_GRANTED);

        issuedCreds = user.admin().verifiableCredentials().getIssuedCredentials();
        assertEquals(2, issuedCreds.size());
        assertEquals(1, countOfIssuedCredsByClient(issuedCreds, managedConsentClient.getId()));
        assertEquals(1, countOfIssuedCredsByClient(issuedCreds, managedClient.getId()));
    }

    private long countOfIssuedCredsByClient(List<IssuedVerifiableCredentialRepresentation> issuedCreds, String clientUUID) {
        return issuedCreds.stream()
                .filter(issuedCred -> clientUUID.equals(issuedCred.getClientId()))
                .count();
    }


    private void loginUserAndObtainVC(ClientRepresentation client, boolean expectLoginFormDisplayed, boolean expectConsentScreenDisplayed, String expectedConsentEventDetail) {
        oauth.client(client.getClientId(), client.getSecret());
        wallet = new OID4VCBasicWallet(keycloak, oauth);
        ctx = new OID4VCTestContext(client, getCredentialScope(sdJwtTypeNaturalPersonScopeName));

        OID4VCBasicWallet.AuthorizationEndpointRequest request = wallet
                .authorizationRequest()
                .scope(ctx.getScope());
        request.openLoginForm();

        if (expectLoginFormDisplayed) {
            request.fillLoginForm(user.getUsername(), TEST_PASSWORD);
        }

        if (expectConsentScreenDisplayed) {
            grantPage.assertCurrent();
            grantPage.assertGrants("User profile", "User roles", "Natural person verifiable credential");
            grantPage.accept();
        }

        EventAssertion.assertSuccess(events.poll())
                .userId(user.getId())
                .clientId(client.getClientId())
                .details(Details.CONSENT, expectedConsentEventDetail)
                .type(EventType.LOGIN);

        AuthorizationEndpointResponse authResponse = oauth.parseLoginResponse();
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
        assertEquals(sdJwtTypeNaturalPersonScopeName, credentialConfigId);

        EventAssertion.assertSuccess(events.poll())
                .userId(user.getId())
                .clientId(client.getClientId())
                .details(GRANT_TYPE, AUTHORIZATION_CODE)
                .type(EventType.CODE_TO_TOKEN);

        // Credential request
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(credentialIdentifier)
                .send().getCredentialResponse();

        EventAssertion.assertSuccess(events.poll())
                .userId(user.getId())
                .clientId(client.getClientId())
                .details(Details.CREDENTIAL_TYPE, sdJwtTypeNaturalPersonScopeName)
                .type(EventType.VERIFIABLE_CREDENTIAL_REQUEST);

        CredentialResponse.Credential credentialObj = credResponse.getCredentials().get(0);
        assertNotNull(credentialObj, "The first credential in the array should not be null");
        IssuerSignedJWT issuerSignedJWT = SdJwtVP.of(credentialObj.getCredential().toString()).getIssuerSignedJWT();
        assertEquals("oid4vc_natural_person", issuerSignedJWT.getPayload().get(CLAIM_NAME_VCT).asText());
    }

    public static class OID4VCConfidentialClientWithConsentRequired extends OID4VCConfidentialClient {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super.configure(client)
                    .clientId("oid4vci-consent-client")
                    .consentRequired(true);
        }
    }

    public static class OID4VCConsentTestUserConfig extends OID4VCActionTest.OID4VCTestUserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return super.configure(user)
                    .verifiableCredential(jwtTypeNaturalPersonScopeName)
                    .verifiableCredential(sdJwtTypeNaturalPersonScopeName);
        }
    }


}
