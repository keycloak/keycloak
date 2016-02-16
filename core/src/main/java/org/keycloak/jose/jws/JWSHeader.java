package org.keycloak.jose.jws;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JWSHeader implements Serializable {
    @com.fasterxml.jackson.annotation.JsonProperty("alg")
    @JsonProperty("alg")
    private Algorithm algorithm;

    @com.fasterxml.jackson.annotation.JsonProperty("typ")
    @JsonProperty("typ")
    private String type;

    @com.fasterxml.jackson.annotation.JsonProperty("cty")
    @JsonProperty("cty")
    private String contentType;

    @com.fasterxml.jackson.annotation.JsonProperty("kid")
    @JsonProperty("kid")
    private String keyId;

    public JWSHeader() {
    }

    public JWSHeader(Algorithm algorithm, String type, String contentType) {
        this.algorithm = algorithm;
        this.type = type;
        this.contentType = contentType;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public String getType() {
        return type;
    }

    public String getContentType() {
        return contentType;
    }

    public String getKeyId() {
        return keyId;
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);

    }

    public String toString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
