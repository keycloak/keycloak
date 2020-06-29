package org.keycloak.models.credential.dto;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;

public class PasswordSecretData {

    public static final Logger logger = Logger.getLogger(PasswordSecretData.class);

    private final String value;
    private final byte[] salt;
    private final Map<String, String> algorithmData;

    /**
     * Creator with the option to provide customized secret data (multiple salt values, chiefly)
     * @param value hash value
     * @param salt salt value
     * @param algorithmData additional data required by the algorithm
     * @throws IOException invalid base64 in salt value
     */
    @JsonCreator
    public PasswordSecretData(@JsonProperty("value") String value, @JsonProperty("salt") String salt, @JsonProperty("algorithmData") Map<String, String> algorithmData) throws IOException {
        this.algorithmData = algorithmData == null ? Collections.emptyMap() : Collections.unmodifiableMap(algorithmData);

        if (salt == null || "__SALT__".equals(salt)) {
            this.value = value;
            this.salt = null;
        }
        else {
            this.value = value;
            this.salt = Base64.decode(salt);
        }
    }

    /**
     * Default creator (Secret consists only of a value and a single salt)
     * @param value hash value
     * @param salt salt
     */
    public PasswordSecretData(String value, byte[] salt) {
        this.value = value;
        this.salt = salt;
        this.algorithmData = Collections.emptyMap();
    }

    public String getValue() {
        return value;
    }

    public byte[] getSalt() {
        return salt;
    }

    public Map<String, String> getAlgorithmData() {
        return algorithmData;
    }
}
