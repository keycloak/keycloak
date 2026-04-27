package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RecoveryAuthnCodesCredentialData {

    private final Integer hashIterations;
    private final String algorithm;

    private int totalCodes;
    private int remainingCodes;

    @JsonCreator
    public RecoveryAuthnCodesCredentialData(@JsonProperty("hashIterations") Integer hashIterations,
            @JsonProperty("algorithm") String algorithm, @JsonProperty("remaining") int remainingCodes,
                                            @JsonProperty("total") int totalCodes) {
        this.hashIterations = hashIterations;
        this.algorithm = algorithm;
        this.remainingCodes = remainingCodes;
        this.totalCodes = totalCodes;
    }

    public Integer getHashIterations() {
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

    public int getTotalCodes() {
        return totalCodes;
    }

    public void setTotalCodes(int totalCodes) {
        this.totalCodes = totalCodes;
    }


}
