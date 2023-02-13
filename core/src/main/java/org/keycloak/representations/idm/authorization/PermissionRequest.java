/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.keycloak.json.StringListMapDeserializer;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionRequest {

    private String resourceId;
    private Set<String> scopes;
    private String resourceServerId;

    @JsonDeserialize(using = StringListMapDeserializer.class)
    private Map<String, List<String>> claims;

    public PermissionRequest(String resourceId, String... scopes) {
        this.resourceId = resourceId;
        if (scopes != null) {
            this.scopes = new HashSet<>(Arrays.asList(scopes));
        }
    }

    public PermissionRequest() {
        this(null, null);
    }

    public String getResourceId() {
        return resourceId;
    }

    @JsonProperty("resource_id")
    public void setResourceId(String resourceSetId) {
        this.resourceId = resourceSetId;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    @JsonProperty("resource_scopes")
    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    @JsonProperty("resource_server_id")
    public void setResourceServerId(String resourceServerId) {
        this.resourceServerId = resourceServerId;
    }

    public String getResourceServerId() {
        return resourceServerId;
    }

    public Map<String, List<String>> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, List<String>> claims) {
        this.claims = claims;
    }

    public void setClaim(String name, String... value) {
        if (claims == null) {
            claims = new HashMap<>();
        }

        claims.put(name, Arrays.asList(value));
    }

    public void addScope(String... name) {
        if (scopes == null) {
            scopes = new HashSet<>();
        }

        scopes.addAll(Arrays.asList(name));
    }
}
