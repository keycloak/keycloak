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
package org.keycloak.forms.login.freemarker.model;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;

import javax.ws.rs.core.MultivaluedMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthGrantBean {

    private final String accessRequestMessage;
    private List<RoleModel> realmRolesRequested;
    private MultivaluedMap<String, ClientRoleEntry> resourceRolesRequested;
    private String code;
    private ClientModel client;
    private List<String> claimsRequested;

    public OAuthGrantBean(String code, ClientSessionModel clientSession, ClientModel client, List<RoleModel> realmRolesRequested, MultivaluedMap<String, RoleModel> resourceRolesRequested,
                          List<ProtocolMapperModel> protocolMappersRequested, String accessRequestMessage) {
        this.code = code;
        this.client = client;
        this.realmRolesRequested = realmRolesRequested;
        if (resourceRolesRequested != null) {
            this.resourceRolesRequested = new MultivaluedMapImpl<String, ClientRoleEntry>();
            for (List<RoleModel> clientRoles : resourceRolesRequested.values()) {
                for (RoleModel role : clientRoles) {
                    ClientModel currentClient = (ClientModel) role.getContainer();
                    ClientRoleEntry roleEntry = new ClientRoleEntry(currentClient.getClientId(), currentClient.getName(), role.getName(), role.getDescription());
                    this.resourceRolesRequested.add(currentClient.getClientId(), roleEntry);
                }
            }
        }

        this.accessRequestMessage = accessRequestMessage;

        List<String> claims = new LinkedList<String>();
        if (protocolMappersRequested != null) {
            for (ProtocolMapperModel model : protocolMappersRequested) {
                claims.add(model.getConsentText());
            }
        }
        if (claims.size() > 0) this.claimsRequested = claims;
    }

    public String getCode() {
        return code;
    }

    public MultivaluedMap<String, ClientRoleEntry> getResourceRolesRequested() {
        return resourceRolesRequested;
    }

    public List<RoleModel> getRealmRolesRequested() {
        return realmRolesRequested;
    }

    public String getClient() {
        return client.getClientId();
    }

    public List<String> getClaimsRequested() {
        return claimsRequested;
    }

    public String getAccessRequestMessage() {
        return this.accessRequestMessage;
    }

    // Same class used in ConsentBean in account as well. Maybe should be merged into common-freemarker...
    public static class ClientRoleEntry {

        private final String clientId;
        private final String clientName;
        private final String roleName;
        private final String roleDescription;

        public ClientRoleEntry(String clientId, String clientName, String roleName, String roleDescription) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.roleName = roleName;
            this.roleDescription = roleDescription;
        }

        public String getClientId() {
            return clientId;
        }

        public String getClientName() {
            return clientName;
        }

        public String getRoleName() {
            return roleName;
        }

        public String getRoleDescription() {
            return roleDescription;
        }
    }
}
