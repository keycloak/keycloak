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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.sundr.builder.annotations.Buildable;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class HostnameSpec implements Serializable {

    @JsonPropertyDescription("Hostname for the Keycloak server. Applicable for Hostname v1 and v2.")
    private String hostname;

    @JsonPropertyDescription("The hostname for accessing the administration console. Applicable for Hostname v1 and v2.")
    private String admin;

    @JsonPropertyDescription("DEPRECATED. Sets the base URL for accessing the administration console, including scheme, host, port and path. Applicable for Hostname v1.")
    private String adminUrl;

    @JsonPropertyDescription("Disables dynamically resolving the hostname from request headers. Applicable for Hostname v1 and v2.")
    private Boolean strict;

    @JsonPropertyDescription("DEPRECATED. By default backchannel URLs are dynamically resolved from request headers to allow internal and external applications. Applicable for Hostname v1.")
    private Boolean strictBackchannel;

    @JsonPropertyDescription("Enables dynamic resolving of backchannel URLs, including hostname, scheme, port and context path. Set to true if your application accesses Keycloak via a private network. Applicable for Hostname v2.")
    private Boolean backchannelDynamic;

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

    public Boolean isBackchannelDynamic() {
        return backchannelDynamic;
    }

    public void setBackchannelDynamic(Boolean backchannelDynamic) {
        this.backchannelDynamic = backchannelDynamic;
    }
}