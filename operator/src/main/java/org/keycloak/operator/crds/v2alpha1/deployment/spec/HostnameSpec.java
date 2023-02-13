/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.crds.v2alpha1.deployment.spec;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.sundr.builder.annotations.Buildable;

import java.io.Serializable;

@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class HostnameSpec implements Serializable {

    @JsonPropertyDescription("Hostname for the Keycloak server.")
    private String hostname;

    @JsonPropertyDescription("The hostname for accessing the administration console.")
    private String admin;

    @JsonPropertyDescription("Set the base URL for accessing the administration console, including scheme, host, port and path")
    private String adminUrl;

    @JsonPropertyDescription("Disables dynamically resolving the hostname from request headers.")
    private Boolean strict;

    @JsonPropertyDescription("By default backchannel URLs are dynamically resolved from request headers to allow internal and external applications.")
    private Boolean strictBackchannel;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    public Boolean isStrict() {
        return strict;
    }

    public void setStrict(Boolean strict) {
        this.strict = strict;
    }

    public Boolean isStrictBackchannel() {
        return strictBackchannel;
    }

    public void setStrictBackchannel(Boolean strictBackchannel) {
        this.strictBackchannel = strictBackchannel;
    }
}