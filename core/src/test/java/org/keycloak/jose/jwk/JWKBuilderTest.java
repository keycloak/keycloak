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
