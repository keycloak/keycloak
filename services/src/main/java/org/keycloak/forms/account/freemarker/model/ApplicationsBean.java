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
import org.keycloak.forms.login.freemarker.model.OAuthGrantBean;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrderedModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ApplicationsBean {

    private List<ApplicationEntry> applications = new LinkedList<ApplicationEntry>();

    public ApplicationsBean(KeycloakSession session, RealmModel realm, UserModel user) {

        Set<ClientModel> offlineClients = new UserSessionManager(session).findClientsWithOfflineToken(realm, user);

        List<ClientModel> realmClients = realm.getClients();
        for (ClientModel client : realmClients) {
            // Don't show bearerOnly clients
            if (client.isBearerOnly()) {
                continue;
            }

            Set<RoleModel> availableRoles = new HashSet<>();
            if (client.getClientId().equals(Constants.ADMIN_CLI_CLIENT_ID)
                    || client.getClientId().equals(Constants.ADMIN_CONSOLE_CLIENT_ID)) {
                if (!AdminPermissions.realms(session, realm, user).isAdmin()) continue;

            } else {
                // Construct scope parameter with all optional scopes to see all potentially available roles
                Set<ClientScopeModel> allClientScopes = new HashSet<>(client.getClientScopes(true, true).values());
                allClientScopes.addAll(client.getClientScopes(false, true).values());
                allClientScopes.add(client);

                availableRoles = TokenManager.getAccess(user, client, allClientScopes);
            }
            List<RoleModel> realmRolesAvailable = new LinkedList<RoleModel>();
            MultivaluedHashMap<String, ClientRoleEntry> resourceRolesAvailable = new MultivaluedHashMap<String, ClientRoleEntry>();
            processRoles(availableRoles, realmRolesAvailable, resourceRolesAvailable);

            List<ClientScopeModel> orderedScopes = new ArrayList<>();
            if (client.isConsentRequired()) {
                UserConsentModel consent = session.users().getConsentByClient(realm, user.getId(), client.getId());

                if (consent != null) {
                    orderedScopes.addAll(consent.getGrantedClientScopes());
                    orderedScopes.sort(new OrderedModel.OrderedModelComparator<>());
                }
            }
            List<String> clientScopesGranted = orderedScopes.stream()
                    .map(ClientScopeModel::getConsentScreenText)
                    .collect(Collectors.toList());

            List<String> additionalGrants = new ArrayList<>();
            if (offlineClients.contains(client)) {
                additionalGrants.add("${offlineToken}");
            }

            ApplicationEntry appEntry = new ApplicationEntry(realmRolesAvailable, resourceRolesAvailable, client,
                    clientScopesGranted, additionalGrants);
            applications.add(appEntry);
        }
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

        private final List<RoleModel> realmRolesAvailable;
        private final MultivaluedHashMap<String, ClientRoleEntry> resourceRolesAvailable;
        private final ClientModel client;
        private final List<String> clientScopesGranted;
        private final List<String> additionalGrants;

        public ApplicationEntry(List<RoleModel> realmRolesAvailable, MultivaluedHashMap<String, ClientRoleEntry> resourceRolesAvailable,
                                ClientModel client, List<String> clientScopesGranted, List<String> additionalGrants) {
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
            String rootUrl = getClient().getRootUrl();
            String baseUrl = getClient().getBaseUrl();
            
            if (rootUrl == null) rootUrl = "";
            if (baseUrl == null) baseUrl = "";
            
            if (rootUrl.equals("") && baseUrl.equals("")) {
                return "";
            }
            
            if (rootUrl.equals("") && !baseUrl.equals("")) {
                return baseUrl;
            }
            
            if (!rootUrl.equals("") && baseUrl.equals("")) {
                return rootUrl;
            }
            
            if (isBaseUrlRelative() && !rootUrl.equals("")) {
                return concatUrls(rootUrl, baseUrl);
            }
            
            return baseUrl;
        }
        
        private String concatUrls(String u1, String u2) {
            if (u1.endsWith("/")) u1 = u1.substring(0, u1.length() - 1);
            if (u2.startsWith("/")) u2 = u2.substring(1);
            return u1 + "/" + u2;
        }
        
        private boolean isBaseUrlRelative() {
            String baseUrl = getClient().getBaseUrl();
            if (baseUrl.equals("")) return false;
            if (baseUrl.startsWith("/")) return true;
            if (baseUrl.startsWith("./")) return true;
            if (baseUrl.startsWith("../")) return true;
            return false;
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
