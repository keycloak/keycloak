/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

public class IdentityProviderReloadWellKnownRepresentation {

    private String metadataDescriptorUrl;
    private String includedWellKnownFields;
    private Boolean reloadEnabled;

    public String getMetadataDescriptorUrl() {
        return metadataDescriptorUrl;
    }

    public void setMetadataDescriptorUrl(String metadataDescriptorUrl) {
        this.metadataDescriptorUrl = metadataDescriptorUrl;
    }

    public String getIncludedWellKnownFields() {
        return includedWellKnownFields;
    }

    public void setIncludedWellKnownFields(String includedWellKnownFields) {
        this.includedWellKnownFields = includedWellKnownFields;
    }

    public Boolean getReloadEnabled() {
        return reloadEnabled;
    }

    public void setReloadEnabled(Boolean reloadEnabled) {
        this.reloadEnabled = reloadEnabled;
    }
}
