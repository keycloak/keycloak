package org.keycloak.tests.oid4vc;

import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.TokenVerifier;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.providers.oid4vc.ClaimsReferenceMapper;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = OID4VCClaimsReferenceTest.ServerConfig.class)
public class OID4VCClaimsReferenceTest extends OID4VCIssuerTestBase {

    @ParameterizedTest
    @CsvSource({
            "true, booking/42?case=one & two, confirmed",
            "false, booking/42?case=one & two, confirmed",
            "true, booking-43, cancelled",
            "false, booking-43, cancelled",
            "true, , no-reference",
            "false, , no-reference"
    })
    public void shouldPassOfferReferenceToMapper(boolean preAuthorized, String claimsRef, String expectedValue) throws Exception {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtils.getProtocolMapper(
                "external-booking", ClaimsReferenceMapper.ID, Map.of("claim.name", "booking_status"));
        var mappers = testRealm.admin().clientScopes().get(jwtTypeCredentialScope.getId()).getProtocolMappers();
        String mapperId;
        try (Response response = mappers.createMapper(mapper)) {
            mapperId = ApiUtil.getCreatedId(response);
        }

        try {
            var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);
            CredentialsOffer offer = wallet.createCredentialOffer(ctx, request -> request
                    .preAuthorized(preAuthorized)
                    .targetUser(ctx.getHolder())
                    .claimsRef(claimsRef));
            String nonce = ctx.getCredentialsOfferUri().getNonce();
            runOnServer.run(session -> {
                var state = session.getProvider(CredentialOfferStorage.class).getOfferStateByNonce(nonce);
                assertNotNull(state);
                assertEquals(claimsRef, state.getClaimsRef());
            });
            assertFalse(JsonSerialization.valueAsString(offer).contains("claims_ref"));
            assertFalse(JsonSerialization.valueAsString(offer).contains("claimsRef"));

            var response = wallet.fetchCredentialByOffer(ctx, offer).getCredentialResponse();
            String encodedCredential = (String) response.getCredentials().get(0).getCredential();
            JsonWebToken token = TokenVerifier.create(encodedCredential, JsonWebToken.class).getToken();
            VerifiableCredential credential = JsonSerialization.mapper.convertValue(
                    token.getOtherClaims().get("vc"), VerifiableCredential.class);
            assertEquals(expectedValue, credential.getCredentialSubject().getClaims().get("booking_status"));
            // Existing user-model mappers continue to run through the backward-compatible overload.
            assertEquals(ctx.getHolder() + "@email.cz", credential.getCredentialSubject().getClaims().get("email"));
            if (claimsRef != null) {
                assertFalse(JsonSerialization.valueAsString(credential).contains(claimsRef));
            }
        } finally {
            mappers.delete(mapperId);
        }
    }

    public static class ServerConfig extends VCTestServerWithPreAuthCodeEnabled {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return super.configure(config).dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}
