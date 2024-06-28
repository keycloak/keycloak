/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.representations.userprofile.config;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Config of the rules when attribute is required.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UPAttributeRequired implements Cloneable {

    private Set<String> roles;
    private Set<String> scopes;

    public UPAttributeRequired() {
        // for reflection
    }

    public UPAttributeRequired(Set<String> roles, Set<String> scopes) {
        this.roles = roles;
        this.scopes = scopes;
    }

    /**
     * Check if this config means that the attribute is ALWAYS required.
     * 
     * @return true if the attribute is always required
     */
    @JsonIgnore
    public boolean isAlways() {
        return (roles == null || roles.isEmpty()) && (scopes == null || scopes.isEmpty());
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }


    @Override
    public String toString() {
        return "UPAttributeRequired [isAlways=" + isAlways() + ", roles=" + roles + ", scopes=" + scopes + "]";
    }

    @Override
    protected UPAttributeRequired clone() {
        Set<String> scopes = this.scopes == null ? null : new HashSet<>(this.scopes);
        Set<String> roles = this.roles == null ? null : new HashSet<>(this.roles);
        return new UPAttributeRequired(roles, scopes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roles, scopes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final UPAttributeRequired other = (UPAttributeRequired) obj;
        return Objects.equals(this.roles, other.roles)
                && Objects.equals(this.scopes, other.scopes);
    }
}
