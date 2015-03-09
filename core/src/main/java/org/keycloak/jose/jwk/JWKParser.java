package org.keycloak.jose.jwk;

import org.codehaus.jackson.type.TypeReference;
import org.keycloak.util.Base64Url;
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

    private Map<String, String> values;

    private JWKParser() {
    }

    public static JWKParser create() {
        return new JWKParser();
    }

    public JWKParser parse(String jwk) {
        try {
            this.values = JsonSerialization.mapper.readValue(jwk, typeRef);
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PublicKey toPublicKey() {
        String algorithm = values.get(JWK.KEY_TYPE);
        if (RSAPublicJWK.RSA.equals(algorithm)) {
            BigInteger modulus = new BigInteger(1, Base64Url.decode(values.get(RSAPublicJWK.MODULUS)));
            BigInteger publicExponent = new BigInteger(1, Base64Url.decode(values.get(RSAPublicJWK.PUBLIC_EXPONENT)));

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
