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

package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Representation of an organization role.
 */
public class OrganizationRoleRepresentation {

    protected String id;
    protected String name;
    protected String description;
    protected String organizationId;
    protected boolean composite;
    protected Composites composites;
    protected Map<String, List<String>> attributes;

    public static class Composites {
        protected Set<String> realm;
        protected Set<String> organization;
        protected Map<String, List<String>> client;

        public Set<String> getRealm() {
            return realm;
        }

        public void setRealm(Set<String> realm) {
            this.realm = realm;
        }

        public Set<String> getOrganization() {
            return organization;
        }

        public void setOrganization(Set<String> organization) {
            this.organization = organization;
        }

        public Map<String, List<String>> getClient() {
            return client;
        }

        public void setClient(Map<String, List<String>> client) {
            this.client = client;
        }
    }

    public OrganizationRoleRepresentation() {
    }

    public OrganizationRoleRepresentation(String name, String description) {
        this.name = name;
        this.description = description;
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

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public boolean isComposite() {
        return composite;
    }

    public void setComposite(boolean composite) {
        this.composite = composite;
    }

    public Composites getComposites() {
        return composites;
    }

    public void setComposites(Composites composites) {
        this.composites = composites;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return name;
    }
}
