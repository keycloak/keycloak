/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.representations.account.DeviceRepresentation;

/**
 * @author Norbert Kelemen
 * @version $Revision: 1 $
 */
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
