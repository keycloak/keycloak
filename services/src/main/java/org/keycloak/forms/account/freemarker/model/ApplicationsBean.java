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

package org.keycloak.forms.account.freemarker.model;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrderedModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.storage.StorageId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ApplicationsBean {

    private List<ApplicationEntry> applications = new LinkedList<>();

    public ApplicationsBean(KeycloakSession session, RealmModel realm, UserModel user) {
        Set<ClientModel> offlineClients = new UserSessionManager(session).findClientsWithOfflineToken(realm, user);

        for (ClientModel client : getApplications(session, realm, user)) {
            if (isAdminClient(client) && ! AdminPermissions.realms(session, realm, user).isAdmin()) {
                continue;
            }

            // Construct scope parameter with all optional scopes to see all potentially available roles
            Set<ClientScopeModel> allClientScopes = new HashSet<>(client.getClientScopes(true, true).values());
            allClientScopes.addAll(client.getClientScopes(false, true).values());
            allClientScopes.add(client);

            Set<RoleModel> availableRoles = TokenManager.getAccess(user, client, allClientScopes);

            // Don't show applications, which user doesn't have access into (any available roles)
            // unless this is can be changed by approving/revoking consent
            if (! isAdminClient(client) && availableRoles.isEmpty() && ! client.isConsentRequired()) {
                continue;
            }

            List<RoleModel> realmRolesAvailable = new LinkedList<>();
            MultivaluedHashMap<String, ClientRoleEntry> resourceRolesAvailable = new MultivaluedHashMap<>();
            processRoles(availableRoles, realmRolesAvailable, resourceRolesAvailable);

            List<ClientScopeModel> orderedScopes = new LinkedList<>();
            if (client.isConsentRequired()) {
                UserConsentModel consent = session.users().getConsentByClient(realm, user.getId(), client.getId());

                if (consent != null) {
                    orderedScopes.addAll(consent.getGrantedClientScopes());
                }
            }
            List<String> clientScopesGranted = orderedScopes.stream()
                    .sorted(OrderedModel.OrderedModelComparator.getInstance())
                    .map(ClientScopeModel::getConsentScreenText)
                    .collect(Collectors.toList());

            List<String> additionalGrants = new ArrayList<>();
            if (offlineClients.contains(client)) {
                additionalGrants.add("${offlineToken}");
            }

            applications.add(new ApplicationEntry(session, realmRolesAvailable, resourceRolesAvailable, client, clientScopesGranted, additionalGrants));
        }
    }

    public static boolean isAdminClient(ClientModel client) {
        return client.getClientId().equals(Constants.ADMIN_CLI_CLIENT_ID)
          || client.getClientId().equals(Constants.ADMIN_CONSOLE_CLIENT_ID);
    }

    private Set<ClientModel> getApplications(KeycloakSession session, RealmModel realm, UserModel user) {
        Set<ClientModel> clients = new HashSet<>();

        for (ClientModel client : realm.getClients()) {
            // Don't show bearerOnly clients
            if (client.isBearerOnly()) {
                continue;
            }

            clients.add(client);
        }

        List<UserConsentModel> consents = session.users().getConsents(realm, user.getId());

        for (UserConsentModel consent : consents) {
            ClientModel client = consent.getClient();

            if (!new StorageId(client.getId()).isLocal()) {
                clients.add(client);
            }
        }
        return clients;
    }

    private void processRoles(Set<RoleModel> inputRoles, List<RoleModel> realmRoles, MultivaluedHashMap<String, ClientRoleEntry> clientRoles) {
        for (RoleModel role : inputRoles) {
            if (role.getContainer() instanceof RealmModel) {
                realmRoles.add(role);
            } else {
                ClientModel currentClient = (ClientModel) role.getContainer();
                ClientRoleEntry clientRole = new ClientRoleEntry(currentClient.getClientId(), currentClient.getName(),
                        role.getName(), role.getDescription());
                clientRoles.add(currentClient.getClientId(), clientRole);
            }
        }
    }

    public List<ApplicationEntry> getApplications() {
        return applications;
    }

    public static class ApplicationEntry {

        private KeycloakSession session;
        private final List<RoleModel> realmRolesAvailable;
        private final MultivaluedHashMap<String, ClientRoleEntry> resourceRolesAvailable;
        private final ClientModel client;
        private final List<String> clientScopesGranted;
        private final List<String> additionalGrants;

        public ApplicationEntry(KeycloakSession session, List<RoleModel> realmRolesAvailable, MultivaluedHashMap<String, ClientRoleEntry> resourceRolesAvailable,
                                ClientModel client, List<String> clientScopesGranted, List<String> additionalGrants) {
            this.session = session;
            this.realmRolesAvailable = realmRolesAvailable;
            this.resourceRolesAvailable = resourceRolesAvailable;
            this.client = client;
            this.clientScopesGranted = clientScopesGranted;
            this.additionalGrants = additionalGrants;
        }

        public List<RoleModel> getRealmRolesAvailable() {
            return realmRolesAvailable;
        }

        public MultivaluedHashMap<String, ClientRoleEntry> getResourceRolesAvailable() {
            return resourceRolesAvailable;
        }

        public List<String> getClientScopesGranted() {
            return clientScopesGranted;
        }

        public String getEffectiveUrl() {
            return ResolveRelative.resolveRelativeUri(session, getClient().getRootUrl(), getClient().getBaseUrl());
        }
        
        public ClientModel getClient() {
            return client;
        }

        public List<String> getAdditionalGrants() {
            return additionalGrants;
        }
    }

    // Same class used in OAuthGrantBean as well. Maybe should be merged into common-freemarker...
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
