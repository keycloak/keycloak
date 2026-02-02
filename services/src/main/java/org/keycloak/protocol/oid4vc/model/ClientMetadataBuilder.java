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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class ClientMetadataBuilder {

    private String type = "openid_credential";  // default as per spec
    private String credentialConfigurationId;

    private String format;
    private final List<String> types = new ArrayList<>();
    private final List<String> locations = new ArrayList<>();


    // =========================================================================
    // Fluent builder API
    // =========================================================================

    public ClientMetadataBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public ClientMetadataBuilder withCredentialConfigurationId(String id) {
        this.credentialConfigurationId = id;
        return this;
    }

    public ClientMetadataBuilder withFormat(String format) {
        this.format = format;
        return this;
    }

    public ClientMetadataBuilder withTypes(String... t) {
        this.types.addAll(Arrays.asList(t));
        return this;
    }

    public ClientMetadataBuilder withTypes(List<String> t) {
        this.types.addAll(t);
        return this;
    }

    public ClientMetadataBuilder withLocations(String... locs) {
        this.locations.addAll(Arrays.asList(locs));
        return this;
    }

    public ClientMetadataBuilder withLocations(List<String> locs) {
        this.locations.addAll(locs);
        return this;
    }

    public ClientMetadata build() {

        if (credentialConfigurationId == null || credentialConfigurationId.isBlank()) {
            throw new IllegalArgumentException("Missing credential_configuration_id");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Missing type");
        }

        var cmd = new ClientMetadata();
        cmd.setCredentialConfigurationId(credentialConfigurationId);
        cmd.setType(type);

        if (format != null)
            cmd.setFormat(format);

        if (!types.isEmpty())
            cmd.setTypes(List.copyOf(types));

        if (!locations.isEmpty())
            cmd.setLocations(List.copyOf(locations));

        return cmd;
    }
}
