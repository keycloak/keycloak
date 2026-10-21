/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.representations.idm.authorization;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:federico@martel-innovate.com">Federico M. Facca</a>
 */
public class UmaPermissionRepresentation extends AbstractPolicyRepresentation {
    
    private Set<String> roles;
    private Set<String> groups;
    private Set<String> clients;
    private Set<String> users;
    private String condition;

    @Override
    public String getType() {
        return "uma";
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public void addRole(String... role) {
        if (roles == null) {
            roles = new HashSet<>();
        }

        roles.addAll(Arrays.asList(role));
    }

    public void addClientRole(String clientId, String roleName) {
        addRole(clientId + "/" + roleName);
    }

    public void removeRole(String role) {
        if (roles != null) {
            roles.remove(role);
        }
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public void addGroup(String... group) {
        if (groups == null) {
            groups = new HashSet<>();
        }

        groups.addAll(Arrays.asList(group));
    }

    public void removeGroup(String group) {
        if (groups != null) {
            groups.remove(group);
        }
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setClients(Set<String> clients) {
        this.clients = clients;
    }

    public void addClient(String... client) {
        if (clients == null) {
            clients = new HashSet<>();
        }

        clients.addAll(Arrays.asList(client));
    }

    public void removeClient(String client) {
        if (clients != null) {
            clients.remove(client);
        }
    }

    public Set<String> getClients() {
        return clients;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }

    public void addUser(String... user) {
        if (this.users == null) {
            this.users = new HashSet<>();
        }
        this.users.addAll(Arrays.asList(user));
    }

    public void removeUser(String user) {
        if (this.users != null) {
            this.users.remove(user);
        }
    }

    public Set<String> getUsers() {
        return this.users;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }
}
