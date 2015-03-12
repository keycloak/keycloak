package org.keycloak.jose.jwk;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JWK {

    public static final String KEY_ID = "kid";

    public static final String KEY_TYPE = "kty";

    public static final String ALGORITHM = "alg";

    public static final String PUBLIC_KEY_USE = "use";

    @JsonProperty(KEY_ID)
    private String keyId;

    @JsonProperty(KEY_TYPE)
    private String keyType;

    @JsonProperty(ALGORITHM)
    private String algorithm;

    @JsonProperty(PUBLIC_KEY_USE)
    private String publicKeyUse;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getPublicKeyUse() {
        return publicKeyUse;
    }

    public void setPublicKeyUse(String publicKeyUse) {
        this.publicKeyUse = publicKeyUse;
    }

}
