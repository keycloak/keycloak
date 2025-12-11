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
package org.keycloak.representations;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The JSON representation of a Rich Authorization Request's "authorization_details" object.
 *
 * @author <a href="mailto:dgozalob@redhat.com">Daniel Gozalo</a>
 * @see {@link <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-rar#section-2">Request parameter "authorization_details"</a>}
 */
public class AuthorizationDetailsJSONRepresentation implements Serializable {

    // The internal Keycloak's type for static scopes as a RAR request object
    public static final String STATIC_SCOPE_RAR_TYPE = "https://keycloak.org/auth-type/static-oauth2-scope";

    // The internal Keycloak's type for dynamic scopes as a RAR request object
    public static final String DYNAMIC_SCOPE_RAR_TYPE = "https://keycloak.org/auth-type/dynamic-oauth2-scope";

    @JsonProperty("type")
    private String type;
    @JsonProperty("locations")
    private List<String> locations;
    @JsonProperty("actions")
    private List<String> actions;
    @JsonProperty("datatypes")
    private List<String> datatypes;
    @JsonProperty("identifier")
    private String identifier;
    @JsonProperty("privileges")
    private List<String> privileges;

    private final Map<String, Object> customData = new HashMap<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public List<String> getDatatypes() {
        return datatypes;
    }

    public void setDatatypes(List<String> datatypes) {
        this.datatypes = datatypes;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<String> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(List<String> privileges) {
        this.privileges = privileges;
    }

    @JsonAnyGetter
    public Map<String, Object> getCustomData() {
        return customData;
    }

    @JsonAnySetter
    public void setCustomData(String key, Object value) {
        this.customData.put(key, value);
    }

    @Override
    public String toString() {
        return "AuthorizationDetailsJSONRepresentation{" +
                "type='" + type + '\'' +
                ", locations=" + locations +
                ", actions=" + actions +
                ", datatypes=" + datatypes +
                ", identifier='" + identifier + '\'' +
                ", privileges=" + privileges +
                ", customData=" + customData +
                '}';
    }

    public String getScopeNameFromCustomData() {
        if (this.getType().equalsIgnoreCase(DYNAMIC_SCOPE_RAR_TYPE) || this.getType().equalsIgnoreCase(STATIC_SCOPE_RAR_TYPE)) {
            List<String> accessList = (List<String>) this.customData.get("access");
            if (accessList.isEmpty()) {
                throw new RuntimeException("A RAR Scope representation should never have an empty access property");
            }
            return accessList.get(0);
        }
        return null;
    }

    public String getDynamicScopeParamFromCustomData() {
        if(this.getType().equalsIgnoreCase(DYNAMIC_SCOPE_RAR_TYPE)) {
            return (String) this.customData.get("scope_parameter");
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizationDetailsJSONRepresentation that = (AuthorizationDetailsJSONRepresentation) o;
        return Objects.equals(type, that.type) && Objects.equals(locations, that.locations) && Objects.equals(actions, that.actions) && Objects.equals(datatypes, that.datatypes) && Objects.equals(identifier, that.identifier) && Objects.equals(privileges, that.privileges) && Objects.equals(customData, that.customData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, locations, actions, datatypes, identifier, privileges, customData);
    }
}
