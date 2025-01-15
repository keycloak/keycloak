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

package org.keycloak.quarkus.runtime.compatibility;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerInfo {

    @JsonProperty("epoch")
    private int epoch;
    @JsonProperty("versions")
    private Map<String, String> versions = new HashMap<>();

    public int getEpoch() {
        return epoch;
    }

    public void setEpoch(int epoch) {
        this.epoch = epoch;
    }

    public Map<String, String> getVersions() {
        return versions;
    }

    public void setVersions(Map<String, String> versions) {
        this.versions = versions == null ? new HashMap<>() : versions;
    }

    public void addVersion(String key, String version) {
        if (versions == null) {
            versions = new HashMap<>();
        }
        versions.put(key, version);
    }
}
