package org.keycloak.tests.oid4vc.issuance.signing;

import java.util.function.Consumer;

import jakarta.ws.rs.core.Response;

import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.oid4vc.OID4VCIssuerEndpointTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for OID4VCIssuerEndpoint with OID4VCI disabled.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCJWTIssuerEndpointDisabledTest extends OID4VCIssuerEndpointTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    void disableVerifiedCredentialsOnTestRealm() {
        testRealm.updateWithCleanup(realm -> realm.verifiableCredentialsEnabled(false));
    }


    @AfterEach
    void logout() {
        AccountHelper.logout(testRealm.admin(), "john");
    }

    /**
     * When verifiable credentials are disabled at the realm level, OID4VCI endpoints
     * must reject calls regardless of the client configuration.
     */
    @Test
    public void testRealmDisabledEndpoints() {
        testWithBearerToken(token -> runOnServer.run((session) -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Nonce endpoint should be forbidden when OID4VCI is disabled for the realm
            CorsErrorResponseException nonceException = Assertions.assertThrows(CorsErrorResponseException.class, issuerEndpoint::getCNonce);
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), nonceException.getResponse().getStatus(), "Realm-disabled OID4VCI should return 403 for nonce endpoint");
        }));
    }

    @Test
    public void testClientNotEnabled() {
        testWithBearerToken(token -> runOnServer.run((session) -> {
            AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            // Test getCredentialOfferURI
            CorsErrorResponseException offerUriException = Assertions.assertThrows(CorsErrorResponseException.class, () ->
                    issuerEndpoint.createCredentialOffer("test-credential")
            );
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), offerUriException.getResponse().getStatus(), "Should fail with 403 Forbidden when client is not OID4VCI-enabled");

            // Test requestCredential
            CredentialRequest credentialRequest = new CredentialRequest()
                    .setCredentialIdentifier(jwtTypeCredentialScopeName);
            String requestPayload;
            try {
                requestPayload = JsonSerialization.writeValueAsString(credentialRequest);
            } catch (JsonProcessingException e) {
                Assertions.fail("Failed to serialize CredentialRequest: " + e.getMessage());
                return;
            }
            CorsErrorResponseException requestException = Assertions.assertThrows(CorsErrorResponseException.class, () ->
                    issuerEndpoint.requestCredential(requestPayload)
            );
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), requestException.getResponse().getStatus(), "Should fail with 403 Forbidden when client is not OID4VCI-enabled");
        }));
    }

    private void testWithBearerToken(Consumer<String> testLogic) {
        String token = getBearerToken(oauth);
        testLogic.accept(token);
    }
}
