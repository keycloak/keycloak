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

/**
 * Config of the rules when attribute is selected.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UPAttributeSelector implements Cloneable {

    private Set<String> scopes;

    public UPAttributeSelector() {
        // for reflection
    }

    public UPAttributeSelector(Set<String> scopes) {
        this.scopes = scopes;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    @Override
    public String toString() {
        return "UPAttributeSelector [scopes=" + scopes + "]";
    }

    @Override
    protected UPAttributeSelector clone() {
        return new UPAttributeSelector(scopes == null ? null : new HashSet<>(scopes));
    }

    @Override
    public int hashCode() {
        return Objects.hash(scopes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final UPAttributeSelector other = (UPAttributeSelector) obj;
        return Objects.equals(this.scopes, other.scopes);
    }
}
