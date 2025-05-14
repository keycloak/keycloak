package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.representations.account.DeviceRepresentation;

public class TrustedDeviceCredentialData {
    private final String os;
    private final String osVersion;
    private final String browser;


    @JsonCreator
    public TrustedDeviceCredentialData(@JsonProperty("os") String os, @JsonProperty("osVersion") String osVersion, @JsonProperty("browser") String browser) {
        this.os = os;
        this.osVersion = osVersion;
        this.browser = browser;
    }

    public TrustedDeviceCredentialData(DeviceRepresentation deviceRepresentation){
        this.os = deviceRepresentation.getOs();
        this.osVersion = deviceRepresentation.getOsVersion();
        this.browser = deviceRepresentation.getBrowser();
    }


    public String getOs() {
        return os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getBrowser() {
        return browser;
    }

}
