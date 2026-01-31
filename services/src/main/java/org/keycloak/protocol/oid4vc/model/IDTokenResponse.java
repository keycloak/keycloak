/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.model;

import java.security.interfaces.ECPublicKey;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.keycloak.util.DIDUtils.decodeDidKey;

/**
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class IDTokenResponse {

    public final JWSInput jwsInput;
    public final JsonWebToken tokenJwt;
    public final String clientId;
    public final String idToken;

    public IDTokenResponse(String encodedJwt) {
        try {
            idToken = encodedJwt;
            jwsInput = new JWSInput(idToken);
            tokenJwt = jwsInput.readJsonContent(JsonWebToken.class);
            clientId = tokenJwt.getSubject();
        } catch (JWSInputException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void verify(KeycloakSession session) throws VerificationException {

        if (!clientId.startsWith("did:key:z"))
            throw new IllegalArgumentException("Unsupported IDToken subject: " + clientId);

        ECPublicKey publicKey = decodeDidKey(clientId);
        KeyWrapper keyWrapper = new KeyWrapper();
        keyWrapper.setType(KeyType.EC);
        keyWrapper.setUse(KeyUse.SIG);
        keyWrapper.setAlgorithm(Algorithm.ES256);
        keyWrapper.setPublicKey(publicKey);

        SignatureProvider provider = session.getProvider(SignatureProvider.class, Algorithm.ES256);
        SignatureVerifierContext verifier = provider.verifier(keyWrapper);

        // Verify signature
        byte[] signedData = jwsInput.getEncodedSignatureInput().getBytes(UTF_8);
        byte[] signature = jwsInput.getSignature();

        if (!verifier.verify(signedData, signature))
            throw new VerificationException("Invalid signature");
    }
}
