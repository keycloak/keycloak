/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.util.JsonSerialization;

/**
 * Client Profiles' (the set of all Client Profile) external representation class
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientProfilesRepresentation {

    private List<ClientProfileRepresentation> profiles = new ArrayList<>();

    // Global profiles, which are builtin in Keycloak.
    @JsonProperty("globalProfiles")
    private List<ClientProfileRepresentation> globalProfiles;

    public List<ClientProfileRepresentation> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<ClientProfileRepresentation> profiles) {
        this.profiles = profiles;
    }

    public List<ClientProfileRepresentation> getGlobalProfiles() {
        return globalProfiles;
    }

    public void setGlobalProfiles(List<ClientProfileRepresentation> globalProfiles) {
        this.globalProfiles = globalProfiles;
    }

    @Override
    public int hashCode() {
        return JsonSerialization.mapper.convertValue(this, JsonNode.class).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClientProfilesRepresentation)) return false;
        JsonNode jsonNode = JsonSerialization.mapper.convertValue(this, JsonNode.class);
        JsonNode jsonNodeThat = JsonSerialization.mapper.convertValue(obj, JsonNode.class);
        return jsonNode.equals(jsonNodeThat);
    }
}
