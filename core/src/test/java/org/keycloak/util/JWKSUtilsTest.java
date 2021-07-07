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

import org.junit.Test;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.*;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Map;

import static org.junit.Assert.*;


public class JWKSUtilsTest {

    @Test
    public void publicRs256() throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

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
        Map<String, KeyWrapper> keyWrappersForUse = JWKSUtils.getKeyWrappersForUse(jsonWebKeySet, JWK.Use.SIG);
        assertEquals(4, keyWrappersForUse.size());

        KeyWrapper key = keyWrappersForUse.get(kidRsa1);
        assertNotNull(key);
        assertEquals("RS256", key.getAlgorithm());
        assertEquals(KeyUse.SIG, key.getUse());
        assertEquals(kidRsa1, key.getKid());
        assertEquals("RSA", key.getType());

         key = keyWrappersForUse.get(kidRsa2);
        assertNotNull(key);
        assertEquals("RS256", key.getAlgorithm());
        assertEquals(KeyUse.SIG, key.getUse());
        assertEquals(kidRsa2, key.getKid());
        assertEquals("RSA", key.getType());

        key = keyWrappersForUse.get(kidEC1);
        assertNotNull(key);
        assertEquals("ES384", key.getAlgorithm());
        assertEquals(KeyUse.SIG, key.getUse());
        assertEquals(kidEC1, key.getKid());
        assertEquals("EC", key.getType());

        key = keyWrappersForUse.get(kidEC2);
        assertNotNull(key);
        assertNull(key.getAlgorithm());
        assertEquals(KeyUse.SIG, key.getUse());
        assertEquals(kidEC2, key.getKid());
        assertEquals("EC", key.getType());
    }


}
