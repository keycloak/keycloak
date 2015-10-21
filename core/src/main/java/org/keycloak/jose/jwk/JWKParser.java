package org.keycloak.jose.jwk;

import org.codehaus.jackson.type.TypeReference;
import org.keycloak.common.util.Base64Url;
import org.keycloak.util.JsonSerialization;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JWKParser {

    private static TypeReference<Map<String,String>> typeRef = new TypeReference<Map<String,String>>() {};

    private JWK jwk;

    private JWKParser() {
    }

    public JWKParser(JWK jwk) {
        this.jwk = jwk;
    }

    public static JWKParser create() {
        return new JWKParser();
    }

    public static JWKParser create(JWK jwk) {
        return new JWKParser(jwk);
    }

    public JWKParser parse(String jwk) {
        try {
            this.jwk = JsonSerialization.mapper.readValue(jwk, JWK.class);
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JWK getJwk() {
        return jwk;
    }

    public PublicKey toPublicKey() {
        String algorithm = jwk.getKeyType();
        if (RSAPublicJWK.RSA.equals(algorithm)) {
            BigInteger modulus = new BigInteger(1, Base64Url.decode(jwk.getOtherClaims().get(RSAPublicJWK.MODULUS).toString()));
            BigInteger publicExponent = new BigInteger(1, Base64Url.decode(jwk.getOtherClaims().get(RSAPublicJWK.PUBLIC_EXPONENT).toString()));

            try {
                return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Unsupported algorithm " + algorithm);
        }
    }

}
