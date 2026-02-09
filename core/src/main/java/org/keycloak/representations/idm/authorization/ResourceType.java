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
package org.keycloak.representations.idm.authorization;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ResourceType {

    private final String type;
    private final Set<String> scopes;
    private final Map<String, Set<String>> scopeAliases;
    private final String groupType;

    @JsonCreator
    public ResourceType(@JsonProperty("type") String type, @JsonProperty("scopes") Set<String> scopes) {
        this(type, scopes, Collections.emptyMap());
    }

    public ResourceType(String type, Set<String> scopes, Map<String, Set<String>> scopeAliases) {
        this(type, scopes, scopeAliases, null);
    }

    public ResourceType(String type, Set<String> scopes, Map<String, Set<String>> scopeAliases, String groupType) {
        this.type = type;
        this.scopes = Collections.unmodifiableSet(scopes);
        this.scopeAliases = scopeAliases;
        this.groupType = groupType;
    }

    public String getType() {
        return type;
    }

    public Set<String> getScopes() {
        return Collections.unmodifiableSet(scopes);
    }

    public Map<String, Set<String>> getScopeAliases() {
        return Collections.unmodifiableMap(scopeAliases);
    }

    public String getGroupType() {
        return groupType;
    }
}
