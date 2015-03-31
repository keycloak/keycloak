package org.keycloak.jose.jwk;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JWK {

    public static final String KEY_ID = "kid";

    public static final String KEY_TYPE = "kty";

    public static final String ALGORITHM = "alg";

    public static final String PUBLIC_KEY_USE = "use";

    public static final String SIG_USE = "sig";
    public static final String ENCRYPTION_USE = "enc";

    @JsonProperty(KEY_ID)
    private String keyId;

    @JsonProperty(KEY_TYPE)
    private String keyType;

    @JsonProperty(ALGORITHM)
    private String algorithm;

    @JsonProperty(PUBLIC_KEY_USE)
    private String publicKeyUse;

    protected Map<String, Object> otherClaims = new HashMap<String, Object>();


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

    @JsonAnyGetter
    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(String name, Object value) {
        otherClaims.put(name, value);
    }


}
