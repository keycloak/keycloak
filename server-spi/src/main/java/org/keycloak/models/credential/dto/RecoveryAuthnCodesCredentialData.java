package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RecoveryAuthnCodesCredentialData {

    private final int hashIterations;
    private final String algorithm;

    private int remainingCodes;

    @JsonCreator
    public RecoveryAuthnCodesCredentialData(@JsonProperty("hashIterations") int hashIterations,
                                            @JsonProperty("algorithm") String algorithm, @JsonProperty("remaining") int remainingCodes) {
        this.hashIterations = hashIterations;
        this.algorithm = algorithm;
        this.remainingCodes = remainingCodes;
    }

    public int getHashIterations() {
        return hashIterations;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getRemainingCodes() {
        return remainingCodes;
    }

    public void setRemainingCodes(int remainingCodes) {
        this.remainingCodes = remainingCodes;
    }

}
