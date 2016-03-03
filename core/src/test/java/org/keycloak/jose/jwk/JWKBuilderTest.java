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

package org.keycloak.jose.jwk;

import org.junit.Test;
import org.keycloak.util.JsonSerialization;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JWKBuilderTest {

    @Test
    public void publicRs256() throws Exception {
        PublicKey publicKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();

        JWK jwk = JWKBuilder.create().rs256(publicKey);

        assertNotNull(jwk.getKeyId());
        assertEquals("RSA", jwk.getKeyType());
        assertEquals("RS256", jwk.getAlgorithm());
        assertEquals("sig", jwk.getPublicKeyUse());

        assertTrue(jwk instanceof RSAPublicJWK);
        assertNotNull(((RSAPublicJWK) jwk).getModulus());
        assertNotNull(((RSAPublicJWK) jwk).getPublicExponent());

        String jwkJson = JsonSerialization.writeValueAsString(jwk);

        // Parse
        assertArrayEquals(publicKey.getEncoded(), JWKParser.create().parse(jwkJson).toPublicKey().getEncoded());
    }

    @Test
    public void parse() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String jwkJson = "{" +
                "   \"kty\": \"RSA\"," +
                "   \"alg\": \"RS256\"," +
                "   \"use\": \"sig\"," +
                "   \"kid\": \"3121adaa80ace09f89d80899d4a5dc4ce33d0747\"," +
                "   \"n\": \"soFDjoZ5mQ8XAA7reQAFg90inKAHk0DXMTizo4JuOsgzUbhcplIeZ7ks83hsEjm8mP8lUVaHMPMAHEIp3gu6Xxsg-s73ofx1dtt_Fo7aj8j383MFQGl8-FvixTVobNeGeC0XBBQjN8lEl-lIwOa4ZoERNAShplTej0ntDp7TQm0=\"," +
                "   \"e\": \"AQAB\"" +
                "  }";

        PublicKey key = JWKParser.create().parse(jwkJson).toPublicKey();
        assertEquals("RSA", key.getAlgorithm());
        assertEquals("X.509", key.getFormat());
    }

}
