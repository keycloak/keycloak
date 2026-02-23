package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.util.function.Consumer;

import jakarta.ws.rs.core.Response;

import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testsuite.Assert;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for OID4VCIssuerEndpoint with OID4VCI disabled.
 */
public class OID4VCJWTIssuerEndpointDisabledTest extends OID4VCIssuerEndpointTest {

    @Override
    protected boolean shouldEnableOid4vci(RealmRepresentation testRealm) {
        return false;
    }

    @Override
    protected boolean shouldEnableOid4vci(ClientRepresentation testClient) {
        return false;
    }

    /**
     * When verifiable credentials are disabled at the realm level, OID4VCI endpoints
     * must reject calls regardless of the client configuration.
     */
    @Test
    public void testRealmDisabledEndpoints() {
        testWithBearerToken(token -> testingClient.server(TEST_REALM_NAME).run((session) -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Nonce endpoint should be forbidden when OID4VCI is disabled for the realm
            CorsErrorResponseException nonceException = Assert.assertThrows(CorsErrorResponseException.class, issuerEndpoint::getCNonce);
            assertEquals("Realm-disabled OID4VCI should return 403 for nonce endpoint",
                    Response.Status.FORBIDDEN.getStatusCode(), nonceException.getResponse().getStatus());
        }));
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

            // Test requestCredential
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier(jwtTypeCredentialScopeName);
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
