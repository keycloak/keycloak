/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.signing;

import org.jboss.logging.Logger;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link VerifiableCredentialsSigningService} implementing the JWT_VC format. It returns a string, containing the
 * Signed JWT-Credential
 * {@see https://identity.foundation/jwt-vc-presentation-profile/}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class JwtSigningService extends SigningService<String> {

    private static final Logger LOGGER = Logger.getLogger(JwtSigningService.class);

    private static final String ID_TEMPLATE = "urn:uuid:%s";
    private static final String VC_CLAIM_KEY = "vc";
    private static final String ID_CLAIM_KEY = "id";


    private final SignatureSignerContext signatureSignerContext;
    private final TimeProvider timeProvider;
    private final String tokenType;
    protected final String issuerDid;

    public JwtSigningService(KeycloakSession keycloakSession, String keyId, String algorithmType, String tokenType, String issuerDid, TimeProvider timeProvider) {
        super(keycloakSession, keyId, algorithmType);
        this.issuerDid = issuerDid;
        this.timeProvider = timeProvider;
        this.tokenType = tokenType;
        KeyWrapper signingKey = getKey(keyId, algorithmType);
        if (signingKey == null) {
            throw new SigningServiceException(String.format("No key for id %s and algorithm %s available.", keyId, algorithmType));
        }
        SignatureProvider signatureProvider = keycloakSession.getProvider(SignatureProvider.class, algorithmType);
        signatureSignerContext = signatureProvider.signer(signingKey);

        LOGGER.debugf("Successfully initiated the JWT Signing Service with algorithm %s.", algorithmType);
    }

    @Override
    public String signCredential(VerifiableCredential verifiableCredential) {
        LOGGER.debugf("Sign credentials to jwt-vc format.");

        // Get the issuance date from the credential. Since nbf is mandatory, we set it to the current time if not
        // provided
        long iat = Optional.ofNullable(verifiableCredential.getIssuanceDate())
                .map(issuanceDate -> issuanceDate.toInstant().getEpochSecond())
                .orElse((long) timeProvider.currentTimeSeconds());

        // set mandatory fields
        JsonWebToken jsonWebToken = new JsonWebToken()
                .issuer(verifiableCredential.getIssuer().toString())
                .nbf(iat)
                .id(createCredentialId(verifiableCredential));
        jsonWebToken.setOtherClaims(VC_CLAIM_KEY, verifiableCredential);

        // expiry is optional
        Optional.ofNullable(verifiableCredential.getExpirationDate())
                .ifPresent(d -> jsonWebToken.exp(d.toInstant().getEpochSecond()));

        // subject id should only be set if the credential subject has an id.
        Optional.ofNullable(
                        verifiableCredential
                                .getCredentialSubject()
                                .getClaims()
                                .get(ID_CLAIM_KEY))
                .map(Object::toString)
                .ifPresent(jsonWebToken::subject);

        return new JWSBuilder()
                .type(tokenType)
                .jsonContent(jsonWebToken)
                .sign(signatureSignerContext);
    }

    // retrieve the credential id from the given VC or generate one.
    static String createCredentialId(VerifiableCredential verifiableCredential) {
        return Optional.ofNullable(
                        verifiableCredential.getId())
                .orElse(URI.create(String.format(ID_TEMPLATE, UUID.randomUUID())))
                .toString();
    }
}