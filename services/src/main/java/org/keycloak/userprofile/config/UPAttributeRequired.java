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
package org.keycloak.userprofile.config;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Config of the rules when attribute is required.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UPAttributeRequired {

    private Set<String> roles;
    private Set<String> scopes;

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

}
