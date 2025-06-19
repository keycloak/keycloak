package org.keycloak.operator.crds.v2alpha1.deployment.spec;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sundr.builder.annotations.Buildable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class ProbeSpec {

    @JsonProperty("periodSeconds")
    private int probePeriodSeconds;

    @JsonProperty("failureThreshold")
    private int probeFailureThreshold;

    public int getProbeFailureThreshold() {return probeFailureThreshold;}
    public void setProbeFailureThreshold(int probeFailureThreshold) {this.probeFailureThreshold = probeFailureThreshold;}
    public int getProbePeriodSeconds() {return probePeriodSeconds;}
    public void setProbePeriodSeconds(int probePeriodSeconds) {this.probePeriodSeconds = probePeriodSeconds;}
}
