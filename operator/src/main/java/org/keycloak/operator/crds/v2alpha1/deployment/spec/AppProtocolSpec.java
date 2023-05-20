package org.keycloak.operator.crds.v2alpha1.deployment.spec;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class AppProtocolSpec {
    @JsonProperty("discoveryServiceAppProtocal")
    @JsonPropertyDescription("The appProtocal of Keycloak Discovery service")
    private String appProtocalKD;
    @JsonProperty("serviceAppProtocal")
    @JsonPropertyDescription("The appProtocal of Keycloak  service")
    private String appProtocalKS;

    public String getAppProtocalKD() {
        return appProtocalKD;
    }

    public void setAppProtocalKD(String appProtocalKD) {
        this.appProtocalKD = appProtocalKD;
    }

    public String getAppProtocalKS() {
        return appProtocalKS;
    }

    public void setAppProtocalKS(String appProtocalKS) {
        this.appProtocalKS = appProtocalKS;
    }
}
