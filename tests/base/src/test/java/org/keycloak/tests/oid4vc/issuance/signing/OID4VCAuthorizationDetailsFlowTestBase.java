package org.keycloak.tests.oid4vc.issuance.signing;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;

import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCProofTestUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialIssuerMetadataResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class for authorization_details tests in scope + authorization_code grant flow.
 */
public abstract class OID4VCAuthorizationDetailsFlowTestBase extends OID4VCIssuerTestBase {

    private static final class Oid4vcTestContext {
        CredentialIssuer credentialIssuer;
    }

    protected abstract ClientScopeRepresentation getCredentialClientScope();

    protected abstract void verifyCredentialStructure(Object credentialObj);

    private void clearLoginState() {
        try {
            wallet.logout("john");
        } catch (Exception e) {
            log.warn("Failed to logout test user before authorization-details flow", e);
        }

        if (driver != null && driver.driver() != null) {
            try {
                driver.cookies().deleteAll();
                driver.open("about:blank");
            } catch (Exception e) {
                log.warn("Failed to cleanup browser state before authorization-details flow", e);
            }
        }
    }

    @Test
    public void testAuthorizationCodeFlowWithAuthorizationDetails() throws Exception {
        Oid4vcTestContext ctx = new Oid4vcTestContext();

        clearLoginState();

        CredentialIssuerMetadataResponse issuerMetadataResponse = oauth.oid4vc().issuerMetadataRequest().send();
        assertEquals(HttpStatus.SC_OK, issuerMetadataResponse.getStatusCode());
        ctx.credentialIssuer = issuerMetadataResponse.getMetadata();

        ClientScopeRepresentation credClientScope = getCredentialClientScope();
        String credConfigId = credClientScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(ctx.credentialIssuer.getCredentialIssuer()));

        String authDetailsJson = JsonSerialization.valueAsString(List.of(authDetail));
        String authDetailsEncoded = URLEncoder.encode(authDetailsJson, Charset.defaultCharset());

        // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
        AuthorizationEndpointResponse authEndpointResponse = oauth.loginForm()
                .scope(credClientScope.getName())
                .param("authorization_details", authDetailsEncoded)
                .doLogin("john", "password");

        String authCode = authEndpointResponse.getCode();
        assertNotNull(authCode, "No authorization code");

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authCode)
                .authorizationDetails(List.of(authDetail))
                .send();
        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        String accessToken = tokenResponse.getAccessToken();

        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
        assertEquals(1, authDetailsResponse.size(),
                "Should have authorization_details for each credential configuration in the offer");

        OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(0);
        assertNotNull(authDetailResponse.getCredentialIdentifiers(), "Credential identifiers should be present");
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());

        String credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        assertNotNull(credentialIdentifier, "Credential identifier should not be null");

        String credentialConfigurationId = authDetailResponse.getCredentialConfigurationId();
        assertNotNull(credentialConfigurationId, "Credential configuration id should not be null");

        String cNonce = oauth.oid4vc().nonceRequest().send().getNonce();
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .proofs(OID4VCProofTestUtils.jwtProofs(ctx.credentialIssuer.getCredentialIssuer(), cNonce))
                .bearerToken(accessToken)
                .send();

        CredentialResponse parsedResponse = credentialResponse.getCredentialResponse();
        assertNotNull(parsedResponse, "Credential response should not be null");
        assertNotNull(parsedResponse.getCredentials(), "Credentials should be present");
        assertEquals(1, parsedResponse.getCredentials().size(), "Should have exactly one credential");

        CredentialResponse.Credential credentialWrapper = parsedResponse.getCredentials().get(0);
        assertNotNull(credentialWrapper, "Credential wrapper should not be null");

        Object credentialObj = credentialWrapper.getCredential();
        assertNotNull(credentialObj, "Credential object should not be null");

        verifyCredentialStructure(credentialObj);
    }

    @Test
    public void testAuthorizationCodeFlowWithCredentialIdentifier() throws Exception {
        Oid4vcTestContext ctx = new Oid4vcTestContext();

        CredentialIssuerMetadataResponse issuerMetadataResponse = oauth.oid4vc().issuerMetadataRequest().send();
        assertEquals(HttpStatus.SC_OK, issuerMetadataResponse.getStatusCode());
        ctx.credentialIssuer = issuerMetadataResponse.getMetadata();

        ClientScopeRepresentation credClientScope = getCredentialClientScope();

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialIdentifiers(List.of("credential_identifiers_not_allowed_here"));
        authDetail.setLocations(List.of(ctx.credentialIssuer.getCredentialIssuer()));

        String authDetailsJson = JsonSerialization.valueAsString(List.of(authDetail));
        String authDetailsEncoded = URLEncoder.encode(authDetailsJson, Charset.defaultCharset());

        // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
        AuthorizationEndpointResponse authEndpointResponse = oauth.loginForm()
                .scope(credClientScope.getName())
                .param("authorization_details", authDetailsEncoded)
                .doLogin("john", "password");

        String authCode = authEndpointResponse.getCode();
        assertNotNull(authCode, "No authorization code");

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authCode)
                .authorizationDetails(List.of(authDetail))
                .send();
        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        assertTrue(tokenResponse.getErrorDescription().contains("credential_identifiers not allowed"));
    }
}
