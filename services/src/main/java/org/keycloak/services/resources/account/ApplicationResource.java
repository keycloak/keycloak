/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.resources.account;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.forms.account.freemarker.model.ApplicationsBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.managers.Auth;

public class ApplicationResource {

    private final KeycloakSession session;
    private final UserModel user;

    public ApplicationResource(KeycloakSession session, UserModel user, Auth auth, HttpRequest request) {
        this.session = session;
        this.user = user;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response getApplications() {
        return Response.ok()
                .entity(new ApplicationsBean(session, session.getContext().getRealm(), user).getApplications().stream().map(
                        ApplicationRepresentation::new).collect(Collectors.toList())).build();
    }

    public static class ApplicationRepresentation extends ClientRepresentation {

        private boolean internal;
        private List<String> clientScopesGranted;
        private List<String> additionalGrants;
        private Map<String, List<ClientRoleRepresentation>> clientRoles;

        public ApplicationRepresentation() {
        }

        public ApplicationRepresentation(ApplicationsBean.ApplicationEntry entry) {
            setClientId(entry.getClient().getClientId());
            setName(entry.getClient().getName());
            setBaseUrl(entry.getEffectiveUrl());
            setDescription(entry.getClient().getDescription());
            this.clientScopesGranted = entry.getClientScopesGranted();
            this.additionalGrants = entry.getAdditionalGrants();
            this.clientRoles = new HashMap<>();
            for (Map.Entry<String, List<ApplicationsBean.ClientRoleEntry>> roleEntry : entry.getResourceRolesAvailable()
                    .entrySet()) {
                clientRoles.put(roleEntry.getKey(), roleEntry.getValue().stream().map(
                        ClientRoleRepresentation::new).collect(Collectors.toList()));
            }
        }

        public boolean isInternal() {
            return internal;
        }

        public List<String> getClientScopesGranted() {
            return clientScopesGranted;
        }

        public List<String> getAdditionalGrants() {
            return additionalGrants;
        }

        public Map<String, List<ClientRoleRepresentation>> getClientRoles() {
            return clientRoles;
        }
    }

    public static class ClientRoleRepresentation {

        private ClientRepresentation client;
        private String role;
        private String description;

        public ClientRoleRepresentation() {

        }

        public ClientRoleRepresentation(ApplicationsBean.ClientRoleEntry entry) {
            this.client = new ClientRepresentation();
            this.client.setClientId(entry.getClientId());
            this.client.setName(entry.getClientName());
            this.role = entry.getRoleName();
            this.description = entry.getRoleDescription();
        }

        public ClientRepresentation getClient() {
            return client;
        }

        public String getRole() {
            return role;
        }

        public String getDescription() {
            return description;
        }
    }
}
