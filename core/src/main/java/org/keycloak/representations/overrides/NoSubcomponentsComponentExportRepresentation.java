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

package org.keycloak.representations.overrides;

import org.keycloak.common.util.MultivaluedHashMap;
import io.fabric8.crd.generator.annotation.SchemaFrom;

public class NoSubcomponentsComponentExportRepresentation {

    private String id;
    private String name;
    private String providerId;
    private String subType;
    // TODO: eventually generate code for Nth levels of depth
    // private MultivaluedHashMap<String, ComponentExportRepresentation> subComponents = new MultivaluedHashMap<>();
    @SchemaFrom(type = org.keycloak.representations.overrides.MultivaluedStringStringHashMap.class)
    private MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public MultivaluedHashMap<String, String> getConfig() {
        return config;
    }

    public void setConfig(MultivaluedHashMap<String, String> config) {
        this.config = config;
    }

}
