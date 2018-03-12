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
package org.keycloak.representations.idm.authorization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Permission {

    @JsonProperty("rsid")
    private String resourceId;

    @JsonProperty("rsname")
    private final String resourceName;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<String> scopes;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Map<String, Set<String>> claims;

    public Permission() {
        this(null, null, null, null);
    }

    public Permission(final String resourceId, String resourceName, final Set<String> scopes, Map<String, Set<String>> claims) {
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.scopes = scopes;
        this.claims = claims;
    }

    public String getResourceId() {
        return this.resourceId;
    }

    public String getResourceName() {
        return this.resourceName;
    }

    public Set<String> getScopes() {
        if (this.scopes == null) {
            this.scopes = new HashSet<>();
        }

        return this.scopes;
    }

    public Map<String, Set<String>> getClaims() {
        return claims;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Permission {").append("id=").append(resourceId).append(", name=").append(resourceName)
                .append(", scopes=").append(scopes).append("}");

        return builder.toString();
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }
}
