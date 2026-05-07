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


import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.exceptions.TokenSignatureInvalidException;
import org.keycloak.exceptions.TokenVerificationException;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.jose.jws.Algorithm;
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
import org.keycloak.saml.RandomSecret;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Strings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

import static org.keycloak.OAuth2Constants.CLIENT_ID;
import static org.keycloak.OAuthErrorException.INVALID_CLIENT_ATTESTATION;


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
     *       "kid": "openid-abca-attester-key",
     *       "use": "sig",
     *       "alg": "PS256",
     *       "n": "uVd8mEqXMp...aaVZNQ",
     *       "e": "AQAB"
     *     }
     * ]
     */
    public static final String OAUTH_CLIENT_ATTESTATION_CONFIG_ATTESTER_JWKS = "attester_jwks";

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

        logger.debugf(OAUTH_CLIENT_ATTESTATION_HEADER + ": " + attestationValue);
        logger.debugf(OAUTH_CLIENT_ATTESTATION_POP_HEADER + ": " + attestationPoPValue);

        context.attempted();

        try {
            ClientAttestationJwt attesterJwt = validateClientAttestationJwt(context);
            validateClientAttestationPoPJwt(context, attesterJwt);

            context.success();

        } catch (Exception ex) {
            ServicesLogger.LOGGER.errorValidatingAssertion(ex);
            Response response = ClientAuthUtil.errorResponse(BAD_REQUEST.getStatusCode(), INVALID_CLIENT_ATTESTATION, ex.getMessage());
            context.failure(AuthenticationFlowError.INVALID_CLIENT_ATTESTATION, response);
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

        public ClientAttestationJwt confirmation(JWK jwk) {
            cnf = new Confirmation().setJwk(jwk);
            return this;
        }

        @Override
        public ClientAttestationJwt id(String id) {
            return (ClientAttestationJwt) super.id(id);
        }

        @Override
        public ClientAttestationJwt issuer(String issuer) {
            return (ClientAttestationJwt) super.issuer(issuer);
        }

        @Override
        public ClientAttestationJwt subject(String subject) {
            return (ClientAttestationJwt) super.subject(subject);
        }

        @Override
        public ClientAttestationJwt issuedNowWithTTL(int ttl) {
            return (ClientAttestationJwt) super.issuedNowWithTTL(ttl);
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

        public Confirmation setJwk(JWK jwk) {
            this.jwk = jwk;
            return this;
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

        public ClientAttestationPoPJwt challenge(String challenge) {
            this.challenge = challenge;
            return this;
        }

        public ClientAttestationPoPJwt randomId() {
            id = Base64Url.encode(RandomSecret.createRandomSecret(64));
            return this;
        }

        @Override
        public ClientAttestationPoPJwt id(String id) {
            Base64Url.encode(RandomSecret.createRandomSecret(64));
            return (ClientAttestationPoPJwt) super.id(id);
        }

        @Override
        public ClientAttestationPoPJwt audience(String... audience) {
            return (ClientAttestationPoPJwt) super.audience(audience);
        }

        @Override
        public ClientAttestationPoPJwt issuer(String issuer) {
            return (ClientAttestationPoPJwt) super.issuer(issuer);
        }

        @Override
        public ClientAttestationPoPJwt subject(String subject) {
            return (ClientAttestationPoPJwt) super.subject(subject);
        }

        @Override
        public ClientAttestationPoPJwt issuedNowWithTTL(int ttl) {
            return (ClientAttestationPoPJwt) super.issuedNowWithTTL(ttl);
        }
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private KeyWrapper findAttesterKey(ClientAuthenticationFlowContext context, String kid) {

        if (Strings.isEmpty(kid))
            throw new IllegalArgumentException("Invalid attester kid: " + kid);

        AuthenticatorConfigModel configModel = context.getRealm().getAuthenticatorConfigByAlias(PROVIDER_ID);
        if (configModel == null)
            throw new IllegalStateException("No config for client authenticator: " + PROVIDER_ID);

        String configValue = Optional.ofNullable(configModel.getConfig()).orElse(Map.of())
                .get(OAUTH_CLIENT_ATTESTATION_CONFIG_ATTESTER_JWKS);
        if (configValue == null)
            throw new IllegalStateException("Cannot load attester keys: " + OAUTH_CLIENT_ATTESTATION_CONFIG_ATTESTER_JWKS);

        ABCAConfig attesterKeys = JsonSerialization.valueFromString(configValue, ABCAConfig.class);
        JWK jwk = attesterKeys.getKeys().stream()
                .filter(k -> kid.equals(k.getKeyId()))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No matching key found for kid: " + kid));

        return toPublicKeyWrapper(jwk);
    }

    private KeyWrapper toPublicKeyWrapper(JWK jwk) {
        PublicKey publicKey = new JWKParser(jwk).toPublicKey();
        KeyWrapper kw = new KeyWrapper();
        kw.setPublicKey(publicKey);
        kw.setUse(KeyUse.SIG);
        kw.setType(jwk.getKeyType());
        kw.setAlgorithm(jwk.getAlgorithm());
        return kw;
    }

    // Validate the Client Attestation JWT
    // https://www.ietf.org/archive/id/draft-ietf-oauth-attestation-based-client-auth-07.html#section-5.1
    private ClientAttestationJwt validateClientAttestationJwt(ClientAuthenticationFlowContext context) throws Exception {

        KeycloakSession session = context.getSession();
        RealmModel realmModel = session.getContext().getRealm();

        HttpHeaders headers = context.getHttpRequest().getHttpHeaders();
        String headerValue = Optional.ofNullable(headers.getHeaderString(OAUTH_CLIENT_ATTESTATION_HEADER))
                .orElseThrow(() -> new IllegalArgumentException("Required header " + OAUTH_CLIENT_ATTESTATION_HEADER + " is missing"));

        MultivaluedMap<String, String> formParams = context.getHttpRequest().getDecodedFormParameters();

        JWSInput jws = new JWSInput(headerValue);
        String jwsType = jws.getHeader().getType();
        if (!OAUTH_CLIENT_ATTESTATION_JWT_TYPE.equals(jwsType))
            throw new IllegalArgumentException("The JWS type MUST be " + OAUTH_CLIENT_ATTESTATION_JWT_TYPE + " instead of " + jwsType);

        // Get the client model from the JWT subject
        ClientAttestationJwt attestationJwt = jws.readJsonContent(ClientAttestationJwt.class);
        ClientModel clientModel = Optional.ofNullable(attestationJwt.getSubject())
                .map(realmModel::getClientByClientId)
                .orElseThrow(() -> new TokenVerificationException(attestationJwt, "The sub (subject) claim MUST identify a known client_id"));

        // Set the target client in the context before we attempt signature verification
        context.setClient(clientModel);

        // Define a few Client Attestation JWT checks
        //

        TokenVerifier.Predicate<JsonWebToken> subCheck = (t) -> {
            String clientIdParam = formParams.getFirst(CLIENT_ID);
            if (clientIdParam != null && !clientIdParam.equals(t.getSubject()))
                throw new TokenVerificationException(t, "The sub claim (subject) MUST match the client_id parameter");
            return true;
        };

        TokenVerifier.Predicate<JsonWebToken> issCheck = (t) -> {
            if (Strings.isEmpty(t.getIssuer()))
                throw new TokenVerificationException(t, "The iss (issuer) claim MUST contain a unique identifier for the entity that issued the JWT");
            return true;
        };

        TokenVerifier.Predicate<JsonWebToken> cnfCheck = (t) -> {
            var jwt = (ClientAttestationJwt) t;
            if (jwt.getConfirmation() == null || jwt.getConfirmation().getJwk() == null)
                throw new TokenVerificationException(t, "The cnf (confirmation) claim MUST specify a key that is used by the Client Instance to generate the Client Attestation PoP JWT");
            return true;
        };

        // The signature of the Client Attestation JWT verifies with the public key of a known and trusted Attester
        //
        KeyWrapper attesterKey = findAttesterKey(context, jws.getHeader().getKeyId());

        // Client Attestation JWT verification without signature check
        //
        TokenVerifier.createWithoutSignature(attestationJwt)
                .withChecks(subCheck, issCheck, cnfCheck, TokenVerifier.IS_ACTIVE)
                .verify();

        // Client Attestation JWT signature check
        //
        Algorithm algorithm = jws.getHeader().getAlgorithm();
        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, algorithm.name());
        if (signatureProvider == null) {
            throw new TokenVerificationException(attestationJwt, "Signature provider not found for algorithm: " + algorithm);
        }
        byte[] data = jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8);
        if (!signatureProvider.verifier(attesterKey).verify(data, jws.getSignature())) {
            throw new TokenSignatureInvalidException(attestationJwt, "Invalid token signature");
        }

        // [TODO] The alg JOSE Header Parameter for both JWTs indicates a registered asymmetric digital signature algorithm
        // [TODO] The key contained in the cnf claim of the Client Attestation JWT is not a private key

        return attestationJwt;
    }

    // Validate the Client Attestation PoP JWT
    // https://www.ietf.org/archive/id/draft-ietf-oauth-attestation-based-client-auth-07.html#section-5.2
    private ClientAttestationPoPJwt validateClientAttestationPoPJwt(ClientAuthenticationFlowContext context, ClientAttestationJwt attesterJwt) throws Exception {

        KeycloakSession session = context.getSession();

        HttpHeaders headers = context.getHttpRequest().getHttpHeaders();
        String headerValue = Optional.ofNullable(headers.getHeaderString(OAUTH_CLIENT_ATTESTATION_POP_HEADER))
                .orElseThrow(() -> new IllegalArgumentException("Required header " + OAUTH_CLIENT_ATTESTATION_POP_HEADER + " is missing"));

        JWSInput jws = new JWSInput(headerValue);
        String jwsType = jws.getHeader().getType();
        if (!OAUTH_CLIENT_ATTESTATION_POP_JWT_TYPE.equals(jwsType))
            throw new IllegalArgumentException("The JWS type MUST be " + OAUTH_CLIENT_ATTESTATION_POP_JWT_TYPE + " instead of " + jwsType);

        ClientAttestationPoPJwt attestationPoPJwt = jws.readJsonContent(ClientAttestationPoPJwt.class);

        // Define a few Client Attestation JWT checks
        //

        TokenVerifier.Predicate<JsonWebToken> jtiCheck = (t) -> {
            if (Strings.isEmpty(t.getId()))
                throw new TokenVerificationException(t, "The jti (JWT identifier) claim MUST specify a unique identifier for the Client Attestation PoP.");
            return true;
        };

        TokenVerifier.Predicate<JsonWebToken> iatCheck = (t) -> {
            if (t.getIat() == 0)
                throw new TokenVerificationException(t, "The iat (issued at) claim MUST specify the time at which the Client Attestation PoP was issued.");
            return true;
        };

        TokenVerifier.Predicate<JsonWebToken> issCheck = (t) -> {
            if (Strings.isEmpty(t.getIssuer()) || !t.getIssuer().equals(attesterJwt.getSubject()))
                throw new TokenVerificationException(t, "The value of the iss (issuer) claim, representing the client_id MUST match the value of the sub (subject) claim in the Client Attestation");
            return true;
        };

        TokenVerifier.Predicate<JsonWebToken> audCheck = (t) -> {
            if (t.getAudience() == null || t.getAudience().length == 0)
                throw new TokenVerificationException(t, "The aud (audience) claim MUST specify a value that identifies the authorization server as an intended audience.");
            return true;
        };

        // The public key used to verify the ClientAttestationPoP JWT MUST be the key located in the "cnf" claim of the corresponding ClientAttestation JWT
        //
        JWK jwk = attesterJwt.getConfirmation().getJwk();
        KeyWrapper clientKey = toPublicKeyWrapper(jwk);

        // Client Attestation PoP JWT verification without signature check
        //
        TokenVerifier.createWithoutSignature(attestationPoPJwt)
                .withChecks(jtiCheck, iatCheck, issCheck, audCheck)
                .verify();

        // Client Attestation PoP JWT signature check
        //
        Algorithm algorithm = jws.getHeader().getAlgorithm();
        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, algorithm.name());
        if (signatureProvider == null) {
            throw new TokenVerificationException(attestationPoPJwt, "Signature provider not found for algorithm: " + algorithm);
        }
        byte[] data = jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8);
        if (!signatureProvider.verifier(clientKey).verify(data, jws.getSignature())) {
            throw new TokenSignatureInvalidException(attestationPoPJwt, "Invalid token signature");
        }

        // [TODO] The aud (audience) claim MUST specify a value that identifies the authorization server as an intended audience
        // [TODO] The authorization server can utilize the jti value for replay attack detection
        // [TODO] The authorization server may reject JWTs with an "iat" claim value that is unreasonably far in the past

        // [TODO] If the server provided a challenge value to the client, the challenge claim is present in the Client Attestation PoP JWT and matches the server-provided challenge value.
        // [TODO] Additional checks to guarantee replay protection for the Client Attestation PoP JWT might need to be applied

        return attestationPoPJwt;
    }

    // Error Message specifically related to the use of client attestations
    // [TODO] use_attestation_challenge MUST be used when the Client Attestation PoP JWT is not using an expected server-provided challenge.
    // [TODO] use_fresh_attestation MUST be used when the Client Attestation JWT is deemed to be not fresh enough to be acceptable by the server.
    // [TODO] invalid_client_attestation MAY be used in addition to the more general invalid_client error code as defined in [RFC6749] if the attestation or its proof of possession could not be successfully verified

    /**
     * The AttestationBasedClientAuthenticator config
     */
    public static class ABCAConfig {

        @JsonProperty
        private List<JWK> keys;

        public List<JWK> getKeys() {
            return keys;
        }

        public ABCAConfig setKeys(List<JWK> keys) {
            this.keys = keys;
            return this;
        }
    }
}
