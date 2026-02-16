package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.util.List;
import java.util.function.Consumer;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testsuite.Assert;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.forms.PassThroughClientAuthenticator.clientId;
import static org.keycloak.testsuite.forms.PassThroughClientAuthenticator.namedClientId;

import static org.junit.Assert.assertEquals;

/**
 * Tests for OID4VCIssuerEndpoint with OID4VCI disabled for SD-JWT format
 */
public class OID4VCSdJwtIssuingEndpointDisabledTest extends OID4VCIssuerEndpointTest {

    @Override
    protected boolean shouldEnableOid4vci() {
        return false;
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        testRealm.setVerifiableCredentialsEnabled(false);
    }

    /**
     * Override setup to skip creating oid4vc client scopes when verifiable credentials is disabled.
     * The parent setup() tries to create client scopes with oid4vc protocol, which will fail
     * with the new validation that prevents creating oid4vc scopes when VC is disabled.
     */
    @Override
    @Before
    public void setup() {
        CryptoIntegration.init(this.getClass().getClassLoader());
        httpClient = HttpClientBuilder.create().build();
        client = testRealm().clients().findByClientId(clientId).get(0);
        namedClient = testRealm().clients().findByClientId(namedClientId).get(0);

        // Skip creating oid4vc client scopes when VC is disabled - they cannot be created
        // and are not needed for these tests which verify that endpoints reject calls

        List.of(client, namedClient).forEach(client -> {
            String clientId = client.getClientId();
            // Enable OID4VCI for the client by default, but allow tests to override
            setClientOid4vciEnabled(clientId, shouldEnableOid4vci());
        });
    }

    @Test
    public void testClientNotEnabled() {
        testWithBearerToken(token -> testingClient.server(TEST_REALM_NAME).run((session) -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Test getCredentialOfferURI
            CorsErrorResponseException offerUriException = Assert.assertThrows(CorsErrorResponseException.class, () ->
                    issuerEndpoint.getCredentialOfferURI("test-credential")
            );
            assertEquals("Should fail with 403 Forbidden when client is not OID4VCI-enabled",
                    Response.Status.FORBIDDEN.getStatusCode(), offerUriException.getResponse().getStatus());

            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialConfigurationId(sdJwtTypeCredentialConfigurationIdName);
            String requestPayload;
            try {
                requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
            } catch (JsonProcessingException e) {
                Assert.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }
            CorsErrorResponseException requestException = Assert.assertThrows(CorsErrorResponseException.class, () ->
                    issuerEndpoint.requestCredential(requestPayload)
            );
            assertEquals("Should fail with 403 Forbidden when client is not OID4VCI-enabled",
                    Response.Status.FORBIDDEN.getStatusCode(), requestException.getResponse().getStatus());
        }));
    }

    private void testWithBearerToken(Consumer<String> testLogic) {
        String token = getBearerToken(oauth);
        testLogic.accept(token);
    }
}
