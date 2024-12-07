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
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBody;
import org.keycloak.protocol.oid4vc.model.CredentialConfigId;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.VerifiableCredentialType;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * {@link VerifiableCredentialsSigningService} implementing the SD_JWT_VC format. It returns a String, containing
 * the signed SD-JWT
 * <p>
 * {@see https://drafts.oauth.net/oauth-sd-jwt-vc/draft-ietf-oauth-sd-jwt-vc.html}
 * {@see https://www.ietf.org/archive/id/draft-fett-oauth-selective-disclosure-jwt-02.html}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class SdJwtSigningService extends JwtProofBasedSigningService<String> {

    private static final Logger LOGGER = Logger.getLogger(SdJwtSigningService.class);

    private static final String JWK_CLAIM = "jwk";

    private final SignatureSignerContext signatureSignerContext;

    private final CredentialConfigId vcConfigId;

    // See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-request-6
    // vct sort of additional category for sd-jwt.
    private final VerifiableCredentialType vct;

    public SdJwtSigningService(KeycloakSession keycloakSession, String keyId, String algorithmType, Optional<String> kid, VerifiableCredentialType credentialType, CredentialConfigId vcConfigId) {
        super(keycloakSession, keyId, Format.SD_JWT_VC, algorithmType);
        this.vcConfigId = vcConfigId;
        this.vct = credentialType;

        // If a config id is defined, a vct must be defined.
        // Also validated in: org.keycloak.protocol.oid4vc.issuance.signing.SdJwtSigningServiceProviderFactory.validateSpecificConfiguration
        if (this.vcConfigId != null && this.vct == null) {
            throw new SigningServiceException(String.format("Missing vct for credential config id %s.", vcConfigId));
        }

        // Will return the active key if key id is null.
        KeyWrapper signingKey = getKey(keyId, algorithmType);
        if (signingKey == null) {
            throw new SigningServiceException(String.format("No key for id %s and algorithm %s available.", keyId, algorithmType));
        }
        // keyId header can be confusing if there is any key rotation, as key ids have to be immutable. It can lead
        // to different keys being exposed under the same id.
        // set the configured kid if present.
        if (kid.isPresent()) {
            // we need to clone the key first, to not change the kid of the original key so that the next request still can find it.
            signingKey = signingKey.cloneKey();
            signingKey.setKid(keyId);
        }

        SignatureProvider signatureProvider = keycloakSession.getProvider(SignatureProvider.class, algorithmType);
        signatureSignerContext = signatureProvider.signer(signingKey);

        LOGGER.debugf("Successfully initiated the SD-JWT Signing Service with algorithm %s.", algorithmType);
    }

    @Override
    public String signCredential(VCIssuanceContext vcIssuanceContext) throws VCIssuerException {

        CredentialBody credentialBody = vcIssuanceContext.getCredentialBody();
        if (!(credentialBody instanceof SdJwtCredentialBody sdJwtCredentialBody)) {
            throw new VCIssuerException("Credential body unexpectedly not of type SdJwtCredentialBody");
        }

        JWK jwk;
        try {
            // null returned is a valid result. Means no key binding will be included.
            jwk = validateProof(vcIssuanceContext);
        } catch (JWSInputException | VerificationException | IOException e) {
            throw new VCIssuerException("Can not verify proof", e);
        }

        // add the key binding if any
        if (jwk != null) {
            sdJwtCredentialBody.addCnfClaim(Map.of(JWK_CLAIM, jwk));
        }

        return sdJwtCredentialBody.sign(signatureSignerContext);
    }

    @Override
    public String locator() {
        return VerifiableCredentialsSigningService.locator(format, vct, vcConfigId);
    }
}
