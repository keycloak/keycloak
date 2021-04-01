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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ApplicationsBean {

    private List<ApplicationEntry> applications = new LinkedList<>();

    public ApplicationsBean(KeycloakSession session, RealmModel realm, UserModel user) {
        Set<ClientModel> offlineClients = new UserSessionManager(session).findClientsWithOfflineToken(realm, user);

        this.applications = this.getApplications(session, realm, user)
                .filter(client -> !isAdminClient(client) || AdminPermissions.realms(session, realm, user).isAdmin())
                .map(client -> toApplicationEntry(session, realm, user, client, offlineClients))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static boolean isAdminClient(ClientModel client) {
        return client.getClientId().equals(Constants.ADMIN_CLI_CLIENT_ID)
          || client.getClientId().equals(Constants.ADMIN_CONSOLE_CLIENT_ID);
    }

    private Stream<ClientModel> getApplications(KeycloakSession session, RealmModel realm, UserModel user) {
        Predicate<ClientModel> bearerOnly = ClientModel::isBearerOnly;
        Stream<ClientModel> clients = realm.getClientsStream().filter(bearerOnly.negate());

        Predicate<ClientModel> isLocal = client -> new StorageId(client.getId()).isLocal();
        return Stream.concat(clients, session.users().getConsentsStream(realm, user.getId())
                    .map(UserConsentModel::getClient)
                    .filter(isLocal.negate())).distinct();
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

    /**
     * Constructs a {@link ApplicationEntry} from the specified parameters.
     *
     * @param session a reference to the {@code Keycloak} session.
     * @param realm a reference to the realm.
     * @param user a reference to the user.
     * @param client a reference to the client that contains the applications.
     * @param offlineClients a {@link Set} containing the offline clients.
     * @return the constructed {@link ApplicationEntry} instance or {@code null} if the user can't access the applications
     * in the specified client.
     */
    private ApplicationEntry toApplicationEntry(final KeycloakSession session, final RealmModel realm, final UserModel user,
                                                final ClientModel client, final Set<ClientModel> offlineClients) {

        // Construct scope parameter with all optional scopes to see all potentially available roles
        Stream<ClientScopeModel> allClientScopes = Stream.concat(
                client.getClientScopes(true).values().stream(),
                client.getClientScopes(false).values().stream());
        allClientScopes = Stream.concat(allClientScopes, Stream.of(client)).distinct();

        Set<RoleModel> availableRoles = TokenManager.getAccess(user, client, allClientScopes);

        // Don't show applications, which user doesn't have access into (any available roles)
        // unless this is can be changed by approving/revoking consent
        if (! isAdminClient(client) && availableRoles.isEmpty() && ! client.isConsentRequired()) {
            return null;
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
        return new ApplicationEntry(session, realmRolesAvailable, resourceRolesAvailable, client, clientScopesGranted, additionalGrants);
    }
}
