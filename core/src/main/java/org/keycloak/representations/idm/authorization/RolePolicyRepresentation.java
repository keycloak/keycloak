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

    public void addRole(String name, boolean required) {
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

    public static class RoleDefinition {

        private String id;
        private boolean required;

        public RoleDefinition() {
            this(null, false);
        }

        public RoleDefinition(String id, boolean required) {
            this.id = id;
            this.required = required;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }
}
