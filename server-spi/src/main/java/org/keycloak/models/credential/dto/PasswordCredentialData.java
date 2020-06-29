package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

public class PasswordCredentialData {
    private final int hashIterations;
    private final String algorithm;
    private final Map<String, String> algorithmData;

    /**
     * Creator for standard algorithms (no algorithm tuning beyond hash iterations)
     * @param hashIterations iterations
     * @param algorithm algorithm id
     */
    public PasswordCredentialData(int hashIterations, String algorithm) {
        this(hashIterations, algorithm, Collections.emptyMap());
    }

    /**
     * Creator for custom algorithms (algorithm with tuning parameters beyond simple has iterations)
     * @param hashIterations iterations
     * @param algorithm algorithm id
     * @param algorithmData additional tuning parameters
     */
    @JsonCreator
    public PasswordCredentialData(@JsonProperty("hashIterations") int hashIterations, @JsonProperty("algorithm") String algorithm, @JsonProperty("algorithmData") Map<String, String> algorithmData) {
        this.hashIterations = hashIterations;
        this.algorithm = algorithm;
        this.algorithmData = algorithmData == null ? Collections.emptyMap() : Collections.unmodifiableMap(algorithmData);
    }



    public int getHashIterations() {
        return hashIterations;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns an immutable map of algorithm-specific settings. These settings may include additional
     * parameters such as Bcrypt memory-tuning parameters
     * @return algorithm data
     */
    public Map<String, String> getAlgorithmData() {
        return algorithmData;
    }
}
