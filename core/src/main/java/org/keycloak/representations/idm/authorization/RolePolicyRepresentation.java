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

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RolePolicyRepresentation extends AbstractPolicyRepresentation {

    private Set<RoleDefinition> roles;
    private Boolean fetchRoles;

    @Override
    public String getType() {
        return "role";
    }

    public Set<RoleDefinition> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleDefinition> roles) {
        this.roles = roles;
    }

    public void addRole(String name, Boolean required) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(new RoleDefinition(name, required));
    }

    public void addRole(String name) {
        addRole(name, false);
    }

    public void addClientRole(String clientId, String name) {
        addRole(clientId + "/" +name, false);
    }

    public void addClientRole(String clientId, String name, boolean required) {
        addRole(clientId + "/" + name, required);
    }

    public Boolean isFetchRoles() {
        return fetchRoles;
    }

    public void setFetchRoles(Boolean fetchRoles) {
        this.fetchRoles = fetchRoles;
    }

    public static class RoleDefinition implements Comparable<RoleDefinition> {

        private String id;
        private Boolean required;

        public RoleDefinition() {
            this(null, false);
        }

        public RoleDefinition(String id, Boolean required) {
            this.id = id;
            this.required = required;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Boolean isRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }

        @Override
        public int compareTo(RoleDefinition o) {
            if (id == null || o.id == null) {
                return 1;
            }
            return id.compareTo(o.id);
        }
    }
}
