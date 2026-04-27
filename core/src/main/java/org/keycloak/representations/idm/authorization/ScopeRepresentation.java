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

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * <p>A bounded extent of access that is possible to perform on a resource set. In authorization policy terminology,
 * a scope is one of the potentially many "verbs" that can logically apply to a resource set ("object").
 *
 * <p>For more details, <a href="https://docs.kantarainitiative.org/uma/draft-oauth-resource-reg.html#rfc.section.2.1">OAuth-resource-reg</a>.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopeRepresentation {

    private String id;
    private String name;
    private String iconUri;
    private List<PolicyRepresentation> policies;
    private List<ResourceRepresentation> resources;
    private String displayName;

    /**
     * Creates an instance.
     *
     * @param name the a human-readable string describing some scope (extent) of access
     * @param iconUri a {@link URI} for a graphic icon representing the scope
     */
    public ScopeRepresentation(String name, String iconUri) {
        this.name = name;
        this.iconUri = iconUri;
    }

    /**
     * Creates an instance.
     *
     * @param name the a human-readable string describing some scope (extent) of access
     */
    public ScopeRepresentation(String name) {
        this(name, null);
    }

    /**
     * Creates an instance.
     */
    public ScopeRepresentation() {
        this(null, null);
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconUri() {
        return this.iconUri;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    public List<PolicyRepresentation> getPolicies() {
        return this.policies;
    }

    public void setPolicies(List<PolicyRepresentation> policies) {
        this.policies = policies;
    }

    public List<ResourceRepresentation> getResources() {
        return this.resources;
    }

    public void setResources(List<ResourceRepresentation> resources) {
        this.resources = resources;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScopeRepresentation scope = (ScopeRepresentation) o;
        return Objects.equals(getName(), scope.getName());
    }

    public int hashCode() {
        return Objects.hash(getName());
    }
}