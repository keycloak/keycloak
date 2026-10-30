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
package org.keycloak.protocol.oidc.utils;

import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.AKPPublicJWK;
import org.keycloak.jose.jwk.AKPSamples;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.util.JsonSerialization;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JWKSServerUtilsTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void convertsAkpRealmKeyToPublicJwk() throws Exception {
        AKPPublicJWK source = JsonSerialization.readValue(AKPSamples.ML_DSA_44, AKPPublicJWK.class);
        KeyWrapper key = new KeyWrapper();
        key.setKid(source.getKeyId());
        key.setAlgorithm(source.getAlgorithm());
        key.setType(KeyType.AKP);
        key.setUse(KeyUse.SIG);
        key.setPublicKey(JWKParser.create(source).toPublicKey());

        JWK result = JWKSServerUtils.toJwk(key);

        assertNotNull(result);
        assertEquals(KeyType.AKP, result.getKeyType());
        assertEquals(source.getKeyId(), result.getKeyId());
        assertEquals(source.getAlgorithm(), result.getAlgorithm());
        assertEquals(source.getPub(), result.getOtherClaim(AKPPublicJWK.PUB, String.class));
        assertNull(result.getOtherClaim("priv", String.class));
    }
}
