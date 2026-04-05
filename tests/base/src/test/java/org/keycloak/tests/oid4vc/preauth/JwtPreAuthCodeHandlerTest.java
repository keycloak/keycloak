package org.keycloak.tests.oid4vc.preauth;

import java.util.List;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.preauth.JwtPreAuthCodeHandler;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.preauth.PreAuthCodeHandler;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.JwtPreAuthCode;
import org.keycloak.protocol.oid4vc.model.PreAuthCodeCtx;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.DEFAULT_CODE_LIFESPAN_S;
import static org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.VCTestServerWithPreAuthCodeEnabled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = VCTestServerWithPreAuthCodeEnabled.class)
public class JwtPreAuthCodeHandlerTest extends OID4VCIssuerTestBase {

    OID4VCTestContext testCtx;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    void beforeEach() {
        testCtx = new OID4VCTestContext(client, jwtTypeCredentialScope);
    }

    @Test
    public void shouldGenerateValidJwtPreAuthCodes() {
        // Request a pre-auth code. This will create an offer state in the server
        // and return a pre-auth code linked to that state.
        String preAuthCode = createPreAuthOffer();
        assertValidPreAuthCodeJwt(preAuthCode);

        // Verify pre-auth code and recover associated context
        PreAuthCodeCtx preAuthCodeCtx = runOnServer.fetch((session) -> {
            try {
                JwtPreAuthCodeHandler handler = new JwtPreAuthCodeHandler(session);
                return handler.verifyPreAuthCode(preAuthCode);
            } catch (VerificationException e) {
                throw new RuntimeException(e);
            }
        }, PreAuthCodeCtx.class);

        // Validate the context embedded in the pre-auth code
        assertPreAuthCodeCtx(preAuthCodeCtx);

        // Ensure that the pre-auth code can be exchanged for an access token
        AccessTokenResponse resp = wallet.accessTokenRequestPreAuth(testCtx, preAuthCode).send();
        assertEquals(HttpStatus.SC_OK, resp.getStatusCode());
        assertNotNull(resp.getAccessToken(), "Access token must not be null");
    }

    @Test
    public void mustRejectNonPreAuthCodeJwts() {
        // Get a valid but not pre-auth code JWT
        String imposterPreAuthCode = runOnServer.fetch((session) -> {
            // Build a random JWT
            JsonWebToken jwt = new JsonWebToken()
                    .issuer("issuer")
                    .addAudience("audience")
                    .issuedNow()
                    .exp((long) (Time.currentTime() + 60));

            // Sign to yield a valid JWT
            RealmModel realm = session.getContext().getRealm();
            KeyWrapper keyWrapper = session.keys().getActiveKey(realm, KeyUse.SIG, Algorithm.ES256);
            ECDSASignatureSignerContext signer = new ECDSASignatureSignerContext(keyWrapper);
            return new JWSBuilder().jsonContent(jwt).sign(signer);
        }, String.class);

        // Ensure that it fails handler verification
        runOnServer.run((session) -> {
            JwtPreAuthCodeHandler handler = new JwtPreAuthCodeHandler(session);
            VerificationException exception = assertThrows(VerificationException.class,
                    () -> handler.verifyPreAuthCode(imposterPreAuthCode));

            assertEquals("Not a jwt pre-auth code: no pre-auth code context found",
                    exception.getMessage());
        });

        // Ensure that it cannot be exchanged for an access token
        AccessTokenResponse resp = wallet.accessTokenRequestPreAuth(testCtx, imposterPreAuthCode).send();
        assertEquals(HttpStatus.SC_BAD_REQUEST, resp.getStatusCode());
        assertEquals("Pre-authorized code failed handler verification (invalid_code)",
                resp.getErrorDescription());
    }

    @Test
    public void mustRejectNonPreAuthCodeJwts_MalformedJwt() {
        runOnServer.run((session) -> {
            JwtPreAuthCodeHandler handler = new JwtPreAuthCodeHandler(session);
            VerificationException exception = assertThrows(VerificationException.class,
                    () -> handler.verifyPreAuthCode("not-jwt-pre-auth-code"));

            assertEquals("Pre-auth code decoding/verification failed",
                    exception.getMessage());
        });
    }

    @Test
    public void mustRejectExpiredPreAuthCodeJwts() {
        // Request a pre-auth code. This will create an offer state in the server
        // and return a pre-auth code linked to that state.
        String preAuthCode = createPreAuthOffer();
        assertValidPreAuthCodeJwt(preAuthCode);

        // Assert that an expired pre-auth code fails verification
        runOnServer.run((session) -> {
            try {
                // Move time forward to ensure code is expired
                Time.setOffset(2 * DEFAULT_CODE_LIFESPAN_S);

                JwtPreAuthCodeHandler handler = new JwtPreAuthCodeHandler(session);
                VerificationException exception = assertThrows(VerificationException.class,
                        () -> handler.verifyPreAuthCode(preAuthCode));

                assertTrue(exception.getMessage().startsWith("Jwt pre-auth code not valid:"));
            } finally {
                Time.setOffset(0);
            }
        });
    }

    @Test
    public void mustRejectReplayedPreAuthCodeJwts() {
        // Request a pre-auth code. This will create an offer state in the server
        // and return a pre-auth code linked to that state.
        String preAuthCode = createPreAuthOffer();
        assertValidPreAuthCodeJwt(preAuthCode);

        // First use: the pre-auth code can be exchanged for an access token
        AccessTokenResponse resp = wallet.accessTokenRequestPreAuth(testCtx, preAuthCode).send();
        assertEquals(HttpStatus.SC_OK, resp.getStatusCode());

        // Second use: the same pre-auth code must be rejected as replayed
        AccessTokenResponse replayResp = wallet.accessTokenRequestPreAuth(testCtx, preAuthCode).send();
        assertEquals(HttpStatus.SC_BAD_REQUEST, replayResp.getStatusCode());
        assertEquals("Pre-authorized code has already been used", replayResp.getErrorDescription());
    }

    private String createPreAuthOffer() {
        runOnServer.run(session -> {
            PreAuthCodeHandler handler = session.getProvider(PreAuthCodeHandler.class);
            assertInstanceOf(JwtPreAuthCodeHandler.class, handler,
                    "This testsuite expects JwtPreAuthCodeHandler as PreAuthCodeHandler provider");
        });

        try {
            CredentialsOffer offer = wallet.createCredentialOfferPreAuth(testCtx, testCtx.getHolder());
            return offer.getPreAuthorizedCode();
        } catch (Exception e) {
            throw new AssertionError("Should not fail to create pre-auth code", e);
        }
    }

    public static void assertValidPreAuthCodeJwt(String jwt) {
        JWSInput jws;
        JwtPreAuthCode payload;

        try {
            jws = new JWSInput(jwt);
            payload = jws.readJsonContent(JwtPreAuthCode.class);
        } catch (JWSInputException e) {
            throw new RuntimeException(e);
        }

        assertEquals(Algorithm.HS512, jws.getHeader().getAlgorithm().toString(),
                "Must use expected signing algorithm");

        assertNotNull(payload.getContext(), "Must embed an associated context");
        assertNotNull(payload.getSalt(), "Must be salted");
    }

    private void assertPreAuthCodeCtx(PreAuthCodeCtx preAuthCodeCtx) {
        assertEquals(client.getClientId(), preAuthCodeCtx.getTargetClientId());
        assertEquals(getExistingUser(testCtx.getHolder()).getId(), preAuthCodeCtx.getTargetUserId());
        assertEquals(List.of(testCtx.getCredentialScope().getCredentialConfigurationId()),
                preAuthCodeCtx.getCredentialConfigurationIds());
    }
}
