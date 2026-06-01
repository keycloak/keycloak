package org.keycloak.tests.oid4vc.issuance.signing;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.tests.oid4vc.OID4VCBasicWallet.AuthorizationEndpointRequest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.tests.oid4vc.OID4VCTestContext.AttachmentKey;
import org.keycloak.testsuite.util.oauth.AccessTokenRequest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.Strings;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_IDENTIFIER;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class for authorization_details tests in scope + authorization_code grant flow.
 */
public abstract class OID4VCAuthorizationDetailsFlowTestBase extends OID4VCIssuerTestBase {

    protected abstract CredentialScopeRepresentation getCredentialScope();

    protected abstract void verifyCredentialStructure(Object credentialObj);

    static final AttachmentKey<Boolean> ON_AUTH_REQUEST_ATTACHMENT_KEY = new AttachmentKey<>("onAuthRequest", Boolean.class);
    static final AttachmentKey<Boolean> ON_TOKEN_REQUEST_ATTACHMENT_KEY = new AttachmentKey<>("onTokenRequest", Boolean.class);

    @Test
    public void testNoAuthorizationDetails() {
        var ctx = new OID4VCTestContext(client, getCredentialScope());
        String credIdentifier = ctx.getCredentialScope().getCredentialIdentifier();
        runAuthorizationDetailsTest(ctx, false, false, credIdentifier);
    }

    @Test
    public void testAuthorizationDetails_OnAuthRequest() {
        var ctx = new OID4VCTestContext(client, getCredentialScope());
        String credIdentifier = ctx.getCredentialScope().getCredentialIdentifier();
        runAuthorizationDetailsTest(ctx, true, false, credIdentifier);
    }

    @Test
    public void testAuthorizationDetails_OnTokenRequest() {
        var ctx = new OID4VCTestContext(client, getCredentialScope());
        String credIdentifier = ctx.getCredentialScope().getCredentialIdentifier();
        runAuthorizationDetailsTest(ctx, false, true, credIdentifier);
    }

    @Test
    public void testAuthorizationDetails_OnAuthRequest_AndTokenRequest() {
        var ctx = new OID4VCTestContext(client, getCredentialScope());
        String credIdentifier = ctx.getCredentialScope().getCredentialIdentifier();
        runAuthorizationDetailsTest(ctx, true, true, credIdentifier);
    }

    @Test
    public void testNoAuthorizationDetails_NoIdentifier() {
        var ctx = new OID4VCTestContext(client, getCredentialScope());
        runAuthorizationDetailsTest(ctx, false, false, "");
    }

    @Test
    public void testAuthorizationDetails_OnAuthRequest_NoIdentifier() {
        var ctx = new OID4VCTestContext(client, getCredentialScope());
        runAuthorizationDetailsTest(ctx, true, false, "");
    }

    @Test
    public void testAuthorizationDetails_OnTokenRequest_NoIdentifier() {
        var ctx = new OID4VCTestContext(client, getCredentialScope());
        runAuthorizationDetailsTest(ctx, false, true, "");
    }

    @Test
    public void testAuthorizationDetails_OnAuthRequest_AndTokenRequest_NoIdentifier() {
        var ctx = new OID4VCTestContext(client, getCredentialScope());
        runAuthorizationDetailsTest(ctx, true, true, "");
    }

    @Test
    public void testAuthorizationDetails_WithInvalidCredentialIdentifiers() {

        var ctx = new OID4VCTestContext(client, getCredentialScope());
        var credIdentifier = ctx.getCredentialScope().getCredentialIdentifier();
        var issuerMetadata = wallet.getIssuerMetadata(ctx);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(ctx.getCredentialConfigurationId());
        authDetail.setCredentialIdentifiers(List.of("credential_identifiers_not_allowed_here"));
        authDetail.setLocations(List.of(issuerMetadata.getCredentialIssuer()));

        ctx.putAttachment(ON_AUTH_REQUEST_ATTACHMENT_KEY, true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                runAuthorizationDetailsTest(ctx, credIdentifier, () -> authDetail, null, null, null));
        assertTrue(ex.getMessage().contains("Invalid authorization_details: credential_identifiers not allowed"), ex.getMessage());
    }

    @Test
    public void testCredentialRequestWithCredentialConfigurationId() {

        var ctx = new OID4VCTestContext(client, getCredentialScope());
        var credIdentifier = ctx.getCredentialScope().getCredentialIdentifier();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runAuthorizationDetailsTest(ctx, credIdentifier, null, null, null,
                () -> new CredentialRequest().setCredentialConfigurationId(ctx.getCredentialConfigurationId())));
        assertTrue(ex.getMessage().contains("Credential must be requested by credential identifier from authorization_details"), ex.getMessage());
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private void runAuthorizationDetailsTest(OID4VCTestContext ctx, boolean onAuthRequest, boolean onTokenRequest, String credIdentifier) {

        ctx.putAttachment(ON_AUTH_REQUEST_ATTACHMENT_KEY, onAuthRequest);
        ctx.putAttachment(ON_TOKEN_REQUEST_ATTACHMENT_KEY, onTokenRequest);

        runAuthorizationDetailsTest(ctx, credIdentifier, null, null, null, null);
    }

    private void runAuthorizationDetailsTest(
            OID4VCTestContext ctx,
            String credIdentifier,
            Supplier<OID4VCAuthorizationDetail> authDetailSupplier,
            Supplier<AuthorizationEndpointRequest> authRequestSupplier,
            Function<String, AccessTokenRequest> tokenRequestSupplier,
            Supplier<CredentialRequest> credentialRequestSupplier) {

        String credConfigId = ctx.getCredentialConfigurationId();
        String expCredentialIdentifier = !Strings.isEmpty(credIdentifier) ? credIdentifier : credConfigId + "_0000";

        if (authDetailSupplier == null) {
            authDetailSupplier = () -> {
                var issuerMetadata = wallet.getIssuerMetadata(ctx);
                OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
                authDetail.setType(OPENID_CREDENTIAL);
                authDetail.setCredentialConfigurationId(credConfigId);
                authDetail.setLocations(List.of(issuerMetadata.getCredentialIssuer()));
                return authDetail;
            };
        }
        OID4VCAuthorizationDetail authDetail = authDetailSupplier.get();

        if (authRequestSupplier == null) {
            authRequestSupplier = () -> {
                boolean onAuthRequest = ctx.getAttachment(ON_AUTH_REQUEST_ATTACHMENT_KEY, false);
                AuthorizationEndpointRequest authRequest = wallet.authorizationRequest().scope(ctx.getScope());
                if (onAuthRequest)
                    authRequest.authorizationDetails(authDetail);
                return authRequest;
            };
        }

        if (tokenRequestSupplier == null) {
            tokenRequestSupplier = (authCode) -> {
                boolean onTokenRequest = ctx.getAttachment(ON_TOKEN_REQUEST_ATTACHMENT_KEY, false);
                AccessTokenRequest tokenRequest = oauth.accessTokenRequest(authCode);
                if (onTokenRequest)
                    tokenRequest.authorizationDetails(List.of(authDetail));
                return tokenRequest;
            };
        }

        if (credentialRequestSupplier == null) {
            credentialRequestSupplier = () -> new CredentialRequest()
                    .setCredentialIdentifier(expCredentialIdentifier)
                    .setProofs(wallet.generateJwtProof(ctx));
        }

        // Update the vc.credential_identifier attribute
        //
        String wasCredentialIdentifier = ctx.getCredentialScope().getCredentialIdentifier();
        if (!wasCredentialIdentifier.equals(credIdentifier)) {
            setCredentialScopeAttributes(ctx.getCredentialScope(), Map.of(VC_IDENTIFIER, credIdentifier));
        }

        try {
            AuthorizationEndpointRequest authRequest = authRequestSupplier.get();
            if (authRequest.openLoginForm()) {
                authRequest.fillLoginForm(ctx.getHolder(), TEST_PASSWORD);
            }
            AuthorizationEndpointResponse authResponse = authRequest.parseLoginResponse();
            if (authResponse.getError() != null)
                throw new IllegalStateException(authResponse.getErrorDescription());

            String authCode = authResponse.getCode();
            assertNotNull(authCode, "No authorization code");

            AccessTokenResponse tokenResponse = tokenRequestSupplier.apply(authCode).send();
            assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());
            String accessToken = tokenResponse.getAccessToken();

            // TokenResponse requires credential_identifiers in authorization_details
            // https://github.com/keycloak/keycloak/issues/47386

            List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
            assertNotNull(authDetailsResponse, "authorization_details should be present");
            assertEquals(1, authDetailsResponse.size(), "Should have authorization_details");

            OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(0);
            assertEquals(ctx.getCredentialConfigurationId(), authDetailResponse.getCredentialConfigurationId());

            assertNotNull(authDetailResponse.getCredentialIdentifiers(), "Credential identifiers should be present");
            assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());
            assertEquals(expCredentialIdentifier, authDetailResponse.getCredentialIdentifiers().get(0));

            CredentialRequest credRequest = credentialRequestSupplier.get();
            CredentialResponse credResponse = oauth.oid4vc()
                    .credentialRequest(credRequest)
                    .bearerToken(accessToken)
                    .send().getCredentialResponse();

            assertNotNull(credResponse, "Credential response should not be null");
            assertNotNull(credResponse.getCredentials(), "Credentials should be present");
            assertEquals(1, credResponse.getCredentials().size(), "Should have exactly one credential");

            CredentialResponse.Credential credentialWrapper = credResponse.getCredentials().get(0);
            assertNotNull(credentialWrapper, "Credential wrapper should not be null");

            Object credentialObj = credentialWrapper.getCredential();
            assertNotNull(credentialObj, "Credential object should not be null");

            verifyCredentialStructure(credentialObj);

        } finally {
            // Needed, because we don't go through wallet.authorizationRequest().send()
            wallet.logout(ctx.getHolder());

            // Restore the vc.credential_identifier attribute value
            if (!wasCredentialIdentifier.equals(credIdentifier)) {
                setCredentialScopeAttributes(ctx.getCredentialScope(), Map.of(VC_IDENTIFIER, wasCredentialIdentifier));
            }
        }
    }
}
