package org.keycloak.jose.jwk;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RSAPublicJWK extends JWK {

    public static final String RSA = "RSA";
    public static final String RS256 = "RS256";

    public static final String MODULUS = "n";
    public static final String PUBLIC_EXPONENT = "e";

    @JsonProperty(MODULUS)
    private String modulus;

    @JsonProperty("e")
    private String publicExponent;

    public String getModulus() {
        return modulus;
    }

    public void setModulus(String modulus) {
        this.modulus = modulus;
    }

    public String getPublicExponent() {
        return publicExponent;
    }

    public void setPublicExponent(String publicExponent) {
        this.publicExponent = publicExponent;
    }

}
