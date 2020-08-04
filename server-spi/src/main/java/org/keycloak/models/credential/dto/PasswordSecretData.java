package org.keycloak.models.credential.dto;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.MultivaluedHashMap;

public class PasswordSecretData {

    public static final Logger logger = Logger.getLogger(PasswordSecretData.class);

    private final String value;
    private final byte[] salt;
    private final MultivaluedHashMap<String, String> additionalParameters;

    /**
     * Creator with the option to provide customized secret data (multiple salt values, chiefly)
     * @param value hash value
     * @param salt salt value
     * @param additionalParameters additional data required by the algorithm
     * @throws IOException invalid base64 in salt value
     */
    @JsonCreator
    public PasswordSecretData(@JsonProperty("value") String value, @JsonProperty("salt") String salt, @JsonProperty("algorithmData") Map<String, List<String>> additionalParameters) throws IOException {
        this.additionalParameters = new MultivaluedHashMap<>(additionalParameters == null ? Collections.emptyMap() : additionalParameters);

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
        this.additionalParameters = new MultivaluedHashMap<>();
    }

    public String getValue() {
        return value;
    }

    public byte[] getSalt() {
        return salt;
    }

    public MultivaluedHashMap<String, String> getAdditionalParameters() {
        return additionalParameters;
    }
}
