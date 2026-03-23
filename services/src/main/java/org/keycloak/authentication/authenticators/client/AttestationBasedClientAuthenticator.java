/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authentication.authenticators.client;


import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.http.HttpRequest;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Strings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import static org.keycloak.OAuth2Constants.CLIENT_ID;


/**
 * Attestation-Based Client Authentication based on Client Attestation JWT and PoP.
 * See <a href="https://datatracker.ietf.org/doc/draft-ietf-oauth-attestation-based-client-auth">specs</a> for more details.
 *
 * The current implementation aligns with <a href="https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0-final.html">HAIP Profile 1.0</a>
 * specifically <a href="https://www.ietf.org/archive/id/draft-ietf-oauth-attestation-based-client-auth-07.html">Attestation-Based Client Authentication - Draft07</a>
 * 
 * @author <a href="mailto:tdiesler@proton.me">Thomas Diesler</a>
 */
public class AttestationBasedClientAuthenticator extends AbstractClientAuthenticator implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "attestation-based";
    public static final String OAUTH_CLIENT_ATTESTATION_HEADER = "OAuth-Client-Attestation";
    public static final String OAUTH_CLIENT_ATTESTATION_POP_HEADER = "OAuth-Client-Attestation-PoP";

    public static final String OAUTH_CLIENT_ATTESTATION_JWT_TYPE = "oauth-client-attestation+jwt";
    public static final String OAUTH_CLIENT_ATTESTATION_POP_JWT_TYPE = "oauth-client-attestation-pop+jwt";

    /**
     * The ClientAuthenticator needs to be aware of the public keys from the various Attesters it can trust.
     *
     * [
     *     {
     *       "kty": "RSA",
     *       "kid": "keycloak-abca-sig-rsa",
     *       "use": "sig",
     *       "alg": "PS256",
     *       "n": "uVd8mEqXMp...aaVZNQ",
     *       "e": "AQAB"
     *     }
     * ]
     */
    public static final String OAUTH_CLIENT_ATTESTATION_CONFIG_ATTESTER_JWKS = "attester.jwks";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {

        HttpHeaders headers = context.getHttpRequest().getHttpHeaders();
        String attestationValue = headers.getHeaderString(OAUTH_CLIENT_ATTESTATION_HEADER);
        String attestationPoPValue = headers.getHeaderString(OAUTH_CLIENT_ATTESTATION_POP_HEADER);

        // At least one of the header must be present
        //
        if (attestationValue == null && attestationPoPValue == null) {
            return;
        }

        context.attempted();

        try {
            validateClientAttestationJwt(context);
            validateClientAttestationPoPJwt(context);

            context.success();

        } catch (Exception ex) {
            ServicesLogger.LOGGER.errorValidatingAssertion(ex);
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), OAuthErrorException.INVALID_CLIENT_ATTESTATION, ex.getMessage());
            context.failure(AuthenticationFlowError.INVALID_CLIENT_ATTESTATION, challengeResponse);
        }
    }

    @Override
    public String getDisplayType() {
        return "Attestation-Based";
    }

    @Override
    public String getHelpText() {
        return "Validates client based on a Client Attestation JWT and a PoP JWT which proves possession of the private key";
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty jwks = new ProviderConfigProperty();
        jwks.setName(OAUTH_CLIENT_ATTESTATION_CONFIG_ATTESTER_JWKS);
        jwks.setLabel("Attester JWKS");
        jwks.setType(ProviderConfigProperty.TEXT_TYPE);
        jwks.setHelpText("JWKS containing trusted attester public keys");
        return List.of(jwks);
    }

    @Override
    public List<ProviderConfigProperty> getConfigPropertiesPerClient() {
        return List.of();
    }

    @Override
    public Map<String, Object> getAdapterConfiguration(KeycloakSession session, ClientModel client) {
        return Map.of();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_AUTH_ABCA);
    }

    @Override
    public Set<String> getProtocolAuthenticatorMethods(String loginProtocol) {
        if (loginProtocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            return Set.of(OIDCLoginProtocol.ATTEST_JWT_CLIENT_AUTH);
        } else {
            return Set.of();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClientAttestationJwt extends JsonWebToken {

        @JsonProperty("cnf")
        private Confirmation cnf;

        public Confirmation getConfirmation() {
            return cnf;
        }

        public void setConfirmation(Confirmation cnf) {
            this.cnf = cnf;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Confirmation {

        @JsonProperty("jwk")
        private JWK jwk;

        public JWK getJwk() {
            return jwk;
        }

        public void setJwk(JWK jwk) {
            this.jwk = jwk;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClientAttestationPoPJwt extends JsonWebToken {

        @JsonProperty("challenge")
        private String challenge;

        public String getChallenge() {
            return challenge;
        }

        public void setChallenge(String challenge) {
            this.challenge = challenge;
        }
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private PublicKey loadAttesterPublicKey(ClientAuthenticationFlowContext context, String kid) throws GeneralSecurityException {

        String configName = OAUTH_CLIENT_ATTESTATION_CONFIG_ATTESTER_JWKS;
        String attesterKeysConfig = Optional.ofNullable(context.getAuthenticatorConfig())
                .map(AuthenticatorConfigModel::getConfig).orElse(Map.of()).get(configName);
        if (attesterKeysConfig == null)
            throw new IllegalStateException("Cannot load Attester public keys from: " + configName);

        JsonNode attesterKeys = JsonSerialization.valueFromString(attesterKeysConfig, JsonNode.class);
        if (attesterKeys == null)
            throw new IllegalStateException("Cannot load Attester public keys");

        for (JsonNode key : attesterKeys) {
            String currentKid = key.get("kid").asText();

            if (kid == null || kid.equals(currentKid)) {
                String kty = key.get("kty").asText();

                if (!"RSA".equals(kty)) {
                    throw new IllegalStateException("Unsupported key type: " + kty);
                }

                String n = key.get("n").asText();
                String e = key.get("e").asText();

                BigInteger modulus = new BigInteger(1, Base64Url.decode(n));
                BigInteger exponent = new BigInteger(1, Base64Url.decode(e));

                RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                return KeyFactory.getInstance("RSA").generatePublic(spec);
            }
        }

        throw new IllegalStateException("No matching key found for kid: " + kid);
    }

    // Validate the Client Attestation JWT
    // https://www.ietf.org/archive/id/draft-ietf-oauth-attestation-based-client-auth-07.html#section-5.1
    private void validateClientAttestationJwt(ClientAuthenticationFlowContext context) throws Exception {

        HttpRequest httpRequest = context.getHttpRequest();
        MultivaluedMap<String, String> formParams = httpRequest.getDecodedFormParameters();

        HttpHeaders headers = httpRequest.getHttpHeaders();
        String headerValue = headers.getHeaderString(OAUTH_CLIENT_ATTESTATION_HEADER);
        if (headerValue == null)
            throw new IllegalStateException("Required header " + OAUTH_CLIENT_ATTESTATION_HEADER + " for is missing");

        // Parse the Client Attestation JWT without signature verification
        //
        TokenVerifier<ClientAttestationJwt> tokenVerifier = TokenVerifier.create(headerValue, ClientAttestationJwt.class);
        ClientAttestationJwt jwt = tokenVerifier
                .withChecks(TokenVerifier.IS_ACTIVE)
                .getToken();

        // [TODO] Do these checks with predicates

        if (OAUTH_CLIENT_ATTESTATION_JWT_TYPE.equals(jwt.getType()))
            throw new IllegalStateException("The JWT type MUST be " + OAUTH_CLIENT_ATTESTATION_JWT_TYPE + " instead of " + jwt.getType());
        if (Strings.isEmpty(jwt.getIssuer()))
            throw new IllegalStateException("The iss (issuer) claim MUST contains a unique identifier for the entity that issued the JWT");
        if (Strings.isEmpty(jwt.getSubject()))
            throw new IllegalStateException("The sub (subject) claim MUST specify client_id value of the OAuth Client");
        if (jwt.getExp() == 0)
            throw new IllegalStateException("The exp (expiration time) claim MUST specify the time at which the Client Attestation is considered expired by its issuer.");
        if (jwt.getConfirmation() == null || jwt.getConfirmation().getJwk() == null)
            throw new IllegalStateException("The cnf (confirmation) claim MUST specify a key that is used by the Client Instance to generate the Client Attestation PoP JWT");

        // The Authorization Server MUST verify that the value of client_id parameter is the same as the client_id value in the sub claim
        //
        String clientIdParam = formParams.getFirst(CLIENT_ID);
        if (clientIdParam != null && !clientIdParam.equals(jwt.getSubject()))
            throw new IllegalStateException("The client attestation subject does not match the client_id parameter");

        // We set the target client in the context before we attempt signature verification
        //
        RealmModel realmModel = context.getSession().getContext().getRealm();
        ClientModel clientModel = realmModel.getClientByClientId(jwt.getSubject());
        context.setClient(clientModel);

        // Verification and Processing

        // [TODO] The alg JOSE Header Parameter for both JWTs indicates a registered asymmetric digital signature algorithm
        // [TODO] The key contained in the cnf claim of the Client Attestation JWT is not a private key
    }

    // Validate the Client Attestation PoP JWT
    // https://www.ietf.org/archive/id/draft-ietf-oauth-attestation-based-client-auth-07.html#section-5.2
    private void validateClientAttestationPoPJwt(ClientAuthenticationFlowContext context) throws Exception {

        HttpHeaders headers = context.getHttpRequest().getHttpHeaders();
        String headerValue = headers.getHeaderString(OAUTH_CLIENT_ATTESTATION_POP_HEADER);
        if (headerValue == null)
            throw new IllegalStateException("Required header " + OAUTH_CLIENT_ATTESTATION_POP_HEADER + " for is missing");

        JWSInput jws = new JWSInput(headerValue);
        ClientAttestationPoPJwt jwt = jws.readJsonContent(ClientAttestationPoPJwt.class);

        if (OAUTH_CLIENT_ATTESTATION_POP_JWT_TYPE.equals(jwt.getType()))
            throw new IllegalStateException("The JWT type MUST be " + OAUTH_CLIENT_ATTESTATION_POP_JWT_TYPE + " instead of " + jwt.getType());
        if (Strings.isEmpty(jwt.getIssuer()))
            throw new IllegalStateException("The iss (issuer) claim MUST specify client_id value of the OAuth Client");
        if (jwt.getAudience() == null || jwt.getAudience().length == 0)
            throw new IllegalStateException("The aud (audience) claim MUST specify a value that identifies the authorization server as an intended audience.");
        if (Strings.isEmpty(jwt.getId()))
            throw new IllegalStateException("The jti (JWT identifier) claim MUST specify a unique identifier for the Client Attestation PoP.");
        if (jwt.getIat() == 0)
            throw new IllegalStateException("The iat (issued at) claim MUST specify the time at which the Client Attestation PoP was issued.");

        // [TODO] The aud (audience) claim MUST specify a value that identifies the authorization server as an intended audience
        // [TODO] The authorization server can utilize the jti value for replay attack detection
        // [TODO] The authorization server may reject JWTs with an "iat" claim value that is unreasonably far in the past

        // [TODO] The authorization server MUST reject JWTs with an invalid signature.
        // [TODO] The value of the iss claim, representing the client_id MUST match the value of the sub claim in the corresponding Client Attestation JWT

//        if (!clientModel.getClientId().equals(clientAttestationPoPJwt.getIssuer())) {
//            throw new IllegalStateException("The client attestation PoP issuer does not match the authorized client_id");
//        }


        // Verification and Processing

        // [TODO] The signature of the Client Attestation PoP JWT verifies with the public key contained in the cnf claim of the Client Attestation JWT.
        // [TODO] If the server provided a challenge value to the client, the challenge claim is present in the Client Attestation PoP JWT and matches the server-provided challenge value.
        // [TODO] Additional checks to guarantee replay protection for the Client Attestation PoP JWT might need to be applied

    }

    // Error Message specifically related to the use of client attestations
    // [TODO] use_attestation_challenge MUST be used when the Client Attestation PoP JWT is not using an expected server-provided challenge.
    // [TODO] use_fresh_attestation MUST be used when the Client Attestation JWT is deemed to be not fresh enough to be acceptable by the server.
    // [TODO] invalid_client_attestation MAY be used in addition to the more general invalid_client error code as defined in [RFC6749] if the attestation or its proof of possession could not be successfully verified

}
