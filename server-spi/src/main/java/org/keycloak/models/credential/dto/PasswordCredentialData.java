package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.common.util.MultivaluedHashMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PasswordCredentialData {
    private final int hashIterations;
    private final String algorithm;

    private MultivaluedHashMap<String, String> additionalParameters;

    /**
     * Creator for standard algorithms (no algorithm tuning beyond hash iterations)
     * @param hashIterations iterations
     * @param algorithm algorithm id
     */
    public PasswordCredentialData(int hashIterations, String algorithm) {
        this(hashIterations, algorithm, null);
    }

    /**
     * Creator for custom algorithms (algorithm with tuning parameters beyond simple has iterations)
     * @param hashIterations iterations
     * @param algorithm algorithm id
     * @param additionalParameters additional tuning parameters
     */
    @JsonCreator
    public PasswordCredentialData(@JsonProperty("hashIterations") int hashIterations, @JsonProperty("algorithm") String algorithm, @JsonProperty("algorithmData") Map<String, List<String>> additionalParameters) {
        this.hashIterations = hashIterations;
        this.algorithm = algorithm;
        this.additionalParameters = additionalParameters != null ?  new MultivaluedHashMap<>(additionalParameters) : null;
    }



    public int getHashIterations() {
        return hashIterations;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns a map of algorithm-specific settings. These settings may include additional
     * parameters such as Bcrypt memory-tuning parameters. It should be used immutably.
     * @return algorithm data
     */
    public MultivaluedHashMap<String, String> getAdditionalParameters() {
        if (additionalParameters == null) {
            additionalParameters = new MultivaluedHashMap<>();
        }
        return additionalParameters;
    }
}
