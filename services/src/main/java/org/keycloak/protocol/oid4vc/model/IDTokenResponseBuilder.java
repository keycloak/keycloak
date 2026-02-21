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

import java.security.KeyPair;
import java.util.Optional;
import java.util.UUID;

import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;

/**
 /**
 * Build an IDTokenResponse as a response to an IDTokenRequest
 * <p>
 * https://hub.ebsi.eu/conformance/build-solutions/issue-to-holder-functional-flows#in-time-issuance
 * <p>
 * POST into https://my-issuer.rocks/auth/direct_post
 *   Content-Type: application/x-www-form-urlencoded
 *   id_token=eyJ0eXAiOiJKV1Q...bjQAYPGOjQJbGlz0pCwAqBX
 * <p>
 * JWT Header: {
 *   typ: 'JWT',
 *   alg: 'ES256',
 *   kid: 'did:key:z2dmzD81c...7U4KTk66xsA5r'
 * }
 * JWT Payload: {
 *   iss: 'did:key:z2dmzD81c...7U4KTk66xsA5r',
 *   sub: 'did:key:z2dmzD81c...7U4KTk66xsA5r',
 *   aud: 'https://my-issuer.rocks/auth',
 *   exp: 1589699360,
 *   iat: 1589699260,
 *   nonce: 'n-0S6_WzA2Mj'
 * }
 * <p>
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class IDTokenResponseBuilder {

    private String request;
    private JsonWebToken jwt;

    public IDTokenResponseBuilder withJwtAudience(String aud) {
        getOrCreateJWT().audience(aud);
        return this;
    }

    public IDTokenResponseBuilder withJwtIssuer(String iss) {
        getOrCreateJWT().issuer(iss);
        return this;
    }

    public IDTokenResponseBuilder withJwtNonce(String nonce) {
        getOrCreateJWT().getOtherClaims().put("nonce", nonce);
        return this;
    }

    public IDTokenResponseBuilder withJwtSubject(String sub) {
        getOrCreateJWT().subject(sub);
        return this;
    }

    private JsonWebToken getOrCreateJWT() {
        jwt = Optional.ofNullable(jwt).orElse(new JsonWebToken());
        return jwt;
    }

    public IDTokenResponseBuilder sign(KeyPair keyPair) {

        if (jwt == null)
            throw new IllegalStateException("JWT not initialized");

        jwt.exp((long) Time.currentTime() + 300); // 5min
        jwt.id(String.format("urn:uuid:%s", UUID.randomUUID()));

        KeyWrapper walletKey = new KeyWrapper();
        walletKey.setKid(jwt.getSubject());     // DID-based kid or thumbprint
        walletKey.setAlgorithm(Algorithm.ES256);
        walletKey.setType(KeyType.EC);
        walletKey.setPrivateKey(keyPair.getPrivate());
        walletKey.setPublicKey(keyPair.getPublic());

        SignatureSignerContext signer = new ECDSASignatureSignerContext(walletKey);
        request = new JWSBuilder()
                .kid(walletKey.getKid())
                .jsonContent(jwt)
                .sign(signer);

        return this;
    }

    public IDTokenResponse build() {
        if (request == null)
            throw new IllegalStateException("Not signed");
        return new IDTokenResponse(request);
    }
}
