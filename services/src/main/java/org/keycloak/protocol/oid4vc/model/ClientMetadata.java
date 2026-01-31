/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientMetadata {

    /** Must be "openid_credential" */
    private String type;

    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;

    private String format;
    private List<String> types;
    private List<String> locations;

    // ===== Getters and Setters =====

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public String getCredentialConfigurationId() { return credentialConfigurationId; }

    public void setCredentialConfigurationId(String credentialConfigurationId) {
        this.credentialConfigurationId = credentialConfigurationId;
    }

    public String getFormat() { return format; }

    public void setFormat(String format) { this.format = format; }

    public List<String> getTypes() { return types; }

    public void setTypes(List<String> types) { this.types = types; }

    public List<String> getLocations() { return locations; }

    public void setLocations(List<String> locations) { this.locations = locations; }
}
