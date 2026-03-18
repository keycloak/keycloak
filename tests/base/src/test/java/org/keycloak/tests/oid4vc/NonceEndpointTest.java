package org.keycloak.tests.oid4vc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.keybinding.CNonceHandler;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtCNonceHandler;
import org.keycloak.protocol.oid4vc.model.NonceResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcNonceResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class NonceEndpointTest extends OID4VCIssuerTestBase {

    @InjectRunOnServer
    private RunOnServerClient runOnServer;

    @Test
    public void testGetCNonce() throws Exception {
        // Clear events before nonce request
        events.clear();

        Oid4vcNonceResponse response = oauth.oid4vc().nonceRequest().send();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode(),
                "Nonce endpoint should return 200 OK");
        String cNonce = response.getNonce();

        // Verify CREDENTIAL_NONCE_REQUEST event was fired (unauthenticated endpoint)
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.VERIFIABLE_CREDENTIAL_NONCE_REQUEST)
                .clientId((String) null)
                .userId((String) null)
                .sessionId((String) null);

        String nonceUrl = oauth.getEndpoints().getOid4vcNonce();

        Assertions.assertNotNull(cNonce);
        // verify nonce content
        {
            TokenVerifier<JsonWebToken> verifier = TokenVerifier.create(cNonce, JsonWebToken.class);

            JWSHeader jwsHeader = verifier.getHeader();
            Assertions.assertEquals(Algorithm.ES256, jwsHeader.getAlgorithm().name());
            Assertions.assertNotNull(jwsHeader.getKeyId());

            JsonWebToken nonce = verifier.getToken();
            String credentialsUrl = oauth.getEndpoints().getOid4vcCredential();
            Assertions.assertEquals(List.of(credentialsUrl), Arrays.asList(nonce.getAudience()));
            Assertions.assertEquals(testRealm.getBaseUrl(), nonce.getIssuer());
            Assertions.assertEquals(nonceUrl, nonce.getOtherClaims().get(JwtCNonceHandler.SOURCE_ENDPOINT));
            Assertions.assertNotNull(nonce.getOtherClaims().get("salt"));
        }

        // do internal nonce verification by using cNonceHandler
        runOnServer.run(session -> {
            CNonceHandler cNonceHandler = session.getProvider(CNonceHandler.class);
            var keycloakContext = session.getContext();
            cNonceHandler.verifyCNonce(cNonce,
                    List.of(OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(keycloakContext)),
                    Map.of(JwtCNonceHandler.SOURCE_ENDPOINT,
                            OID4VCIssuerWellKnownProvider.getNonceEndpoint(keycloakContext)));
        });
    }

    @Test
    public void testDPoPNonceHeaderPresent() throws Exception {
        // Clear events before nonce request
        events.clear();

        Oid4vcNonceResponse response = oauth.oid4vc().nonceRequest().send();

        // Verify CREDENTIAL_NONCE_REQUEST event was fired (unauthenticated endpoint)
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.VERIFIABLE_CREDENTIAL_NONCE_REQUEST)
                .clientId((String) null)
                .userId((String) null)
                .sessionId((String) null);

        // Verify successful response
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode(),
                "Nonce endpoint should return 200 OK");

        // Verify DPoP-Nonce header is present and non-empty
        String dpopNonceHeader = response.getHeader(OAuth2Constants.DPOP_NONCE_HEADER);
        Assertions.assertNotNull(dpopNonceHeader, "DPoP-Nonce header must be present");
        Assertions.assertFalse(dpopNonceHeader.trim().isEmpty(), "DPoP-Nonce header must not be empty");

        // Verify that DPoP nonce is different from body nonce (separate generation)
        NonceResponse nonceResponse = response.getNonceResponse();
        Assertions.assertNotNull(nonceResponse, "Response body should contain valid nonce response");
        Assertions.assertNotNull(nonceResponse.getNonce(), "c_nonce in response body should not be null");

        Assertions.assertNotEquals(nonceResponse.getNonce(), dpopNonceHeader,
                "DPoP-Nonce header should be different from c_nonce in body");
    }
}
