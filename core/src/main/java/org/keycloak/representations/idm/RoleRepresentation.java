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

package org.keycloak.representations.idm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleRepresentation {
    protected String id;
    protected String name;
    protected String description;
    @Deprecated
    protected Boolean scopeParamRequired;
    protected boolean composite;
    protected Composites composites;
    private Boolean clientRole;
    private String containerId;
    protected Map<String, List<String>> attributes;

    public static class Composites {
        protected Set<String> realm;
        protected Map<String, List<String>> client;
        @Deprecated
        protected Map<String, List<String>> application;

        public Set<String> getRealm() {
            return realm;
        }

        public void setRealm(Set<String> realm) {
            this.realm = realm;
        }

        public Map<String, List<String>> getClient() {
            return client;
        }

        public void setClient(Map<String, List<String>> client) {
            this.client = client;
        }

        @Deprecated
        public Map<String, List<String>> getApplication() {
            return application;
        }
    }

    public RoleRepresentation() {
    }

    public RoleRepresentation(String name, String description, boolean scopeParamRequired) {
        this.name = name;
        this.description = description;
        this.scopeParamRequired = scopeParamRequired;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Deprecated
    public Boolean isScopeParamRequired() {
        return scopeParamRequired;
    }

    public Composites getComposites() {
        return composites;
    }

    public void setComposites(Composites composites) {
        this.composites = composites;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isComposite() {
        return composite;
    }

    public void setComposite(boolean composite) {
        this.composite = composite;
    }

    public Boolean getClientRole() {
        return clientRole;
    }

    public void setClientRole(Boolean clientRole) {
        this.clientRole = clientRole;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    public RoleRepresentation singleAttribute(String name, String value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        attributes.put(name, Arrays.asList(value));
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.id);
        hash = 29 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || (!(obj instanceof RoleRepresentation))) {
            return false;
        }
        final RoleRepresentation other = (RoleRepresentation) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.name, other.name);
    }
}
