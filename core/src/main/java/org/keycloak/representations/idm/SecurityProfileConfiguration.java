/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.idm;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Default configuration for security profile. For the moment just a name and pointers
 * to default global client profiles and policies.
 *
 * @author rmartinc
 */
public class SecurityProfileConfiguration {

    private String name;
    @JsonProperty("client-profiles")
    private String clientProfiles;
    @JsonProperty("client-policies")
    private String clientPolicies;
    @JsonIgnore
    private List<ClientProfileRepresentation> defaultClientProfiles;
    @JsonIgnore
    private List<ClientPolicyRepresentation> defaultClientPolicies;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientProfiles() {
        return clientProfiles;
    }

    public void setClientProfiles(String clientProfiles) {
        this.clientProfiles = clientProfiles;
    }

    public String getClientPolicies() {
        return clientPolicies;
    }

    public void setClientPolicies(String clientPolicies) {
        this.clientPolicies = clientPolicies;
    }

    public List<ClientProfileRepresentation> getDefaultClientProfiles() {
        return defaultClientProfiles;
    }

    public void setDefaultClientProfiles(List<ClientProfileRepresentation> defaultClientProfiles) {
        this.defaultClientProfiles = defaultClientProfiles;
    }

    public List<ClientPolicyRepresentation> getDefaultClientPolicies() {
        return defaultClientPolicies;
    }

    public void setDefaultClientPolicies(List<ClientPolicyRepresentation> defaultClientPolicies) {
        this.defaultClientPolicies = defaultClientPolicies;
    }
}
