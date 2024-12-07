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
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.JwtCredentialBody;
import org.keycloak.protocol.oid4vc.model.Format;

/**
 * {@link VerifiableCredentialsSigningService} implementing the JWT_VC format. It returns a string, containing the
 * Signed JWT-Credential
 * {@see https://identity.foundation/jwt-vc-presentation-profile/}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class JwtSigningService extends SigningService<String> {

    private static final Logger LOGGER = Logger.getLogger(JwtSigningService.class);


    private final SignatureSignerContext signatureSignerContext;


    public JwtSigningService(KeycloakSession keycloakSession, String keyId, String algorithmType) {
        super(keycloakSession, keyId, Format.JWT_VC, algorithmType);

        KeyWrapper signingKey = getKey(keyId, algorithmType);
        if (signingKey == null) {
            throw new SigningServiceException(String.format("No key for id %s and algorithm %s available.", keyId, algorithmType));
        }
        SignatureProvider signatureProvider = keycloakSession.getProvider(SignatureProvider.class, algorithmType);
        signatureSignerContext = signatureProvider.signer(signingKey);

        LOGGER.debugf("Successfully initiated the JWT Signing Service with algorithm %s.", algorithmType);
    }

    @Override
    public String signCredential(VCIssuanceContext vcIssuanceContext) {
        LOGGER.debugf("Sign credentials to jwt-vc format.");

        CredentialBody credentialBody = vcIssuanceContext.getCredentialBody();
        if (!(credentialBody instanceof JwtCredentialBody jwtCredentialBody)) {
            throw new VCIssuerException("Credential body unexpectedly not of type JwtCredentialBody");
        }

        return jwtCredentialBody.sign(signatureSignerContext);
    }
}
