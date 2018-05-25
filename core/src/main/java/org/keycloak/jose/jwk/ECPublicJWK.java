package org.keycloak.jose.jwk;

import com.fasterxml.jackson.annotation.JsonProperty;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public class ECPublicJWK extends JWK {

    // https://tools.ietf.org/html/rfc7518#section-6.2
    public static final String EC = "EC";
    public static final String ES256 = "ES256";
    public static final String P256 = "P-256";

    // https://tools.ietf.org/html/rfc7518#section-6.2.1.2
    public static final String CURVE = "crv";
    // https://tools.ietf.org/html/rfc7518#section-6.2.1.2
    public static final String X_COORDINATE = "x";
    // https://tools.ietf.org/html/rfc7518#section-6.2.1.3
    public static final String Y_COORDINATE = "y";

    @JsonProperty(CURVE)
    private String curve;

    @JsonProperty(X_COORDINATE)
    private String x;

    @JsonProperty(Y_COORDINATE)
    private String y;

    public String getCurve() {
        return curve;
    }

    public void setCurve(String curve) {
        this.curve = curve;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }
}
