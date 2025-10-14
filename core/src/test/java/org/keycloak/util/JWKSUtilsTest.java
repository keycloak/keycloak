/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.util;

import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.jose.jwk.ECPublicJWK;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.rule.CryptoInitRule;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;


public abstract class JWKSUtilsTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void publicEcMatches() throws Exception {
        String keyA = "{" +
                      "   \"kty\": \"EC\"," +
                      "   \"use\": \"sig\"," +
                      "   \"crv\": \"P-384\"," +
                      "   \"kid\": \"key-a\"," +
                      "   \"x\": \"KVZ5h_W0-8fXmUrxmyRpO_9vwwI7urXfyxGdxm1hpEuhPj2hhDxivnb2BhNvtC6O\"," +
                      "   \"y\": \"1J3JVw_zR3uB3biAE7fs3V_4tJy2M1JinzWj9a4je5GSoW6zgGV4bk85OcuyUAhj\"," +
                      "   \"alg\": \"ES384\"" +
                      "  }";

        ECPublicJWK ecPublicKey = JsonSerialization.readValue(keyA, ECPublicJWK.class);
        JWK publicKey = JsonSerialization.readValue(keyA, JWK.class);

        assertEquals(JWKSUtils.computeThumbprint(publicKey), JWKSUtils.computeThumbprint(ecPublicKey));
    }

    @Test
    public void unsupportedKeyType() throws Exception {
        String keyA = "{" +
                      "   \"kty\": \"OCT\"," +
                      "   \"use\": \"sig\"" +
                      "  }";

        JWK publicKey = JsonSerialization.readValue(keyA, JWK.class);
        assertThrows(UnsupportedOperationException.class, () -> JWKSUtils.computeThumbprint(publicKey));
    }

    @Test
    public void publicRs256() throws Exception {

        String kidRsa1 = "key1";
        String kidRsa2 = "key2";
        String kidInvalidKey = "ignored";
        String kidEC1 = "key3";
        String kidEC2 = "key4";
        String jwksJson = "{" +
                "\"keys\": [" +
                "  {" +
                "   \"kty\": \"RSA\"," +
                "   \"alg\": \"RS256\"," +
                "   \"use\": \"sig\"," +
                "   \"kid\": \"" + kidRsa1 + "\"," +
                "   \"n\": \"soFDjoZ5mQ8XAA7reQAFg90inKAHk0DXMTizo4JuOsgzUbhcplIeZ7ks83hsEjm8mP8lUVaHMPMAHEIp3gu6Xxsg-s73ofx1dtt_Fo7aj8j383MFQGl8-FvixTVobNeGeC0XBBQjN8lEl-lIwOa4ZoERNAShplTej0ntDp7TQm0=\"," +
                "   \"e\": \"AQAB\"" +
                "  }" +
                "  ,{" +
                "   \"kty\": \"RSA\"," +
                "   \"use\": \"sig\"," +
                "   \"kid\": \"" + kidRsa2 + "\"," +
                "   \"n\": \"soFDjoZ5mQ8XAA7reQAFg90inKAHk0DXMTizo4JuOsgzUbhcplIeZ7ks83hsEjm8mP8lUVaHMPMAHEIp3gu6Xxsg-s73ofx1dtt_Fo7aj8j383MFQGl8-FvixTVobNeGeC0XBBQjN8lEl-lIwOa4ZoERNAShplTej0ntDp7TQm0=\"," +
                "   \"e\": \"AQAB\"" +
                "  }," +
                "  {" +
                "   \"kty\": \"RSA\"," +
                "   \"alg\": \"RS512\"," +
                "   \"use\": \"sig\"," +
                "   \"kid\": \"" + kidRsa1 + "\"," +
                "   \"n\": \"soFDjoZ5mQ8XAA7reQAFg90inKAHk0DXMTizo4JuOsgzUbhcplIeZ7ks83hsEjm8mP8lUVaHMPMAHEIp3gu6Xxsg-s73ofx1dtt_Fo7aj8j383MFQGl8-FvixTVobNeGeC0XBBQjN8lEl-lIwOa4ZoERNAShplTej0ntDp7TQm0=\"," +
                "   \"e\": \"AQAB\"" +
                "  }," +
                "  {" +
                "   \"kty\": \"RSA\"," +
                "   \"kid\": \"" + kidInvalidKey + "\"," +
                "   \"n\": \"soFDjoZ5mQ8XAA7reQAFg90inKAHk0DXMTizo4JuOsgzUbhcplIeZ7ks83hsEjm8mP8lUVaHMPMAHEIp3gu6Xxsg-s73ofx1dtt_Fo7aj8j383MFQGl8-FvixTVobNeGeC0XBBQjN8lEl-lIwOa4ZoERNAShplTej0ntDp7TQm0=\"," +
                "   \"e\": \"AQAB\"" +
                "  }," +
                "  {" +
                "   \"kty\": \"EC\"," +
                "   \"use\": \"sig\"," +
                "   \"crv\": \"P-384\"," +
                "   \"kid\": \"" + kidEC1 + "\"," +
                "   \"x\": \"KVZ5h_W0-8fXmUrxmyRpO_9vwwI7urXfyxGdxm1hpEuhPj2hhDxivnb2BhNvtC6O\"," +
                "   \"y\": \"1J3JVw_zR3uB3biAE7fs3V_4tJy2M1JinzWj9a4je5GSoW6zgGV4bk85OcuyUAhj\"," +
                "   \"alg\": \"ES384\"" +
                "  }," +
                "  {" +
                "   \"kty\": \"EC\"," +
                "   \"use\": \"sig\"," +
                "   \"crv\": \"P-384\"," +
                "   \"kid\": \"" + kidEC2 + "\"," +
                "   \"x\": \"KVZ5h_W0-8fXmUrxmyRpO_9vwwI7urXfyxGdxm1hpEuhPj2hhDxivnb2BhNvtC6O\"," +
                "   \"y\": \"1J3JVw_zR3uB3biAE7fs3V_4tJy2M1JinzWj9a4je5GSoW6zgGV4bk85OcuyUAhj\"" +
                "  }" +
                "] }";
        JSONWebKeySet jsonWebKeySet = JsonSerialization.readValue(jwksJson, JSONWebKeySet.class);
        PublicKeysWrapper keyWrappersForUse = JWKSUtils.getKeyWrappersForUse(jsonWebKeySet, JWK.Use.SIG);
        assertEquals(5, keyWrappersForUse.getKeys().size());

        // get by both kid and alg
        KeyWrapper key = keyWrappersForUse.getKeyByKidAndAlg(kidRsa1, "RS256");
        assertNotNull(key);
        assertEquals("RS256", key.getAlgorithmOrDefault());
        assertEquals(KeyUse.SIG, key.getUse());
        assertEquals(kidRsa1, key.getKid());
        assertEquals("RSA", key.getType());

        // get by both kid and alg with RS512. It is same 'kid' as the previous, but should choose "RS512" key now
        key = keyWrappersForUse.getKeyByKidAndAlg(kidRsa1, "RS512");
        assertNotNull(key);
        assertEquals("RS512", key.getAlgorithmOrDefault());
        assertEquals(KeyUse.SIG, key.getUse());
        assertEquals(kidRsa1, key.getKid());
        assertEquals("RSA", key.getType());

        // Get by kid only. Should choose default algorithm, so RS256
        key = keyWrappersForUse.getKeyByKidAndAlg(kidRsa1, null);
        assertNotNull(key);
        assertEquals("RS256", key.getAlgorithmOrDefault());
        assertEquals(KeyUse.SIG, key.getUse());
        assertEquals(kidRsa1, key.getKid());
        assertEquals("RSA", key.getType());

        key = keyWrappersForUse.getKeyByKidAndAlg(kidRsa2, null);
        assertNotNull(key);
        assertEquals("RS256", key.getAlgorithmOrDefault());
        assertEquals(KeyUse.SIG, key.getUse());
        assertEquals(kidRsa2, key.getKid());
        assertEquals("RSA", key.getType());

        key = keyWrappersForUse.getKeyByKidAndAlg(kidEC1, null);
        assertNotNull(key);
        assertEquals("ES384", key.getAlgorithmOrDefault());
        assertEquals(KeyUse.SIG, key.getUse());
        assertEquals(kidEC1, key.getKid());
        assertEquals("EC", key.getType());

        key = keyWrappersForUse.getKeyByKidAndAlg(kidEC2, null);
        assertNotNull(key);
        assertEquals("ES384", key.getAlgorithmOrDefault());
        assertEquals(KeyUse.SIG, key.getUse());
        assertEquals(kidEC2, key.getKid());
        assertEquals("EC", key.getType());

        // Search by alg only
        key = keyWrappersForUse.getKeyByKidAndAlg(null, "ES384");
        assertNotNull(key);
        assertEquals("ES384", key.getAlgorithmOrDefault());
        assertEquals(KeyUse.SIG, key.getUse());
        assertEquals(kidEC1, key.getKid());
        assertEquals("EC", key.getType());
    }


}
