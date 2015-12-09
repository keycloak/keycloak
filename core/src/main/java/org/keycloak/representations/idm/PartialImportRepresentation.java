/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.representations.idm;

import java.util.List;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Used for partial import of users, clients, and identity providers.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class PartialImportRepresentation {
    public enum Policy { SKIP, OVERWRITE, FAIL };

    protected Policy policy = Policy.FAIL;
    protected String ifResourceExists = "";
    protected List<UserRepresentation> users;
    protected List<ClientRepresentation> clients;
    protected List<IdentityProviderRepresentation> identityProviders;
    protected RolesRepresentation roles;

    public boolean hasUsers() {
        return (users != null) && !users.isEmpty();
    }

    public boolean hasClients() {
        return (clients != null) && !clients.isEmpty();
    }

    public boolean hasIdps() {
        return (identityProviders != null) && !identityProviders.isEmpty();
    }

    public String getIfResourceExists() {
        return ifResourceExists;
    }

    public void setIfResourceExists(String ifResourceExists) {
        this.ifResourceExists = ifResourceExists;
        this.policy = Policy.valueOf(ifResourceExists);
    }

    public Policy getPolicy() {
        return this.policy;
    }

    public List<UserRepresentation> getUsers() {
        return users;
    }

    public void setUsers(List<UserRepresentation> users) {
        this.users = users;
    }

    public List<ClientRepresentation> getClients() {
        return clients;
    }

    public void setClients(List<ClientRepresentation> clients) {
        this.clients = clients;
    }

    public List<IdentityProviderRepresentation> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<IdentityProviderRepresentation> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public RolesRepresentation getRoles() {
        return roles;
    }

    public void setRoles(RolesRepresentation roles) {
        this.roles = roles;
    }
}
