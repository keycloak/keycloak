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

package org.keycloak.broker.oid4vp;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;

import org.keycloak.broker.provider.TrustMaterialRequest;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.util.JsonSerialization;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class OID4VPIdentityProviderTest {

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(OID4VPIdentityProviderTest.class.getClassLoader());
    }

    @Test
    public void computesX509HashClientId() throws Exception {
        KeyPair keyPair = KeyUtils.generateEcKeyPair("secp256r1");
        X509Certificate cert = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "oid4vp-verifier");

        String clientId = ClientIdentifier.X509_HASH.forCertificate(cert);

        String expected = "x509_hash:"
                + Base64Url.encode(HashUtils.hash(JavaAlgorithm.SHA256, cert.getEncoded()));
        Assert.assertEquals(expected, clientId);
    }

    @Test
    public void resolveKeysFiltersByKid() throws Exception {
        JWK first = jwk();
        JWK second = jwk();
        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.setKeys(new JWK[] {first, second});

        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        config.setTrustedIssuerJwks(JsonSerialization.writeValueAsString(jwks));
        OID4VPIdentityProvider provider = new OID4VPIdentityProvider(null, config);

        List<JWK> matched = provider
                .resolveKeys(TrustMaterialRequest.builder().kid(second.getKeyId()).build())
                .toList();

        Assert.assertEquals(1, matched.size());
        Assert.assertEquals(second.getKeyId(), matched.get(0).getKeyId());
    }

    @Test
    public void resolveKeysReturnsEmptyWhenNoJwksConfigured() {
        OID4VPIdentityProvider provider = new OID4VPIdentityProvider(null, new OID4VPIdentityProviderConfig());
        Assert.assertEquals(0, provider.resolveKeys(TrustMaterialRequest.builder().build()).count());
    }

    private static JWK jwk() {
        KeyPair keyPair = KeyUtils.generateEcKeyPair("secp256r1");
        KeyWrapper keyWrapper = new KeyWrapper();
        keyWrapper.setKid(KeyUtils.createKeyId(keyPair.getPublic()));
        keyWrapper.setType(KeyType.EC);
        keyWrapper.setAlgorithm(Algorithm.ES256);
        keyWrapper.setUse(KeyUse.SIG);
        keyWrapper.setPublicKey(keyPair.getPublic());
        return JWKBuilder.create().kid(keyWrapper.getKid()).ec(keyWrapper.getPublicKey());
    }
}
