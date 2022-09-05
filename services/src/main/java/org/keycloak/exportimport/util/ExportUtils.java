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

package org.keycloak.exportimport.util;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.representations.idm.UserConsentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportUtils {

    public static RealmRepresentation exportRealm(KeycloakSession session, RealmModel realm, boolean includeUsers, boolean internal) {
        ExportOptions opts = new ExportOptions(includeUsers, true, true, false);
        return exportRealm(session, realm, opts, internal);
    }

    public static RealmRepresentation exportRealm(KeycloakSession session, RealmModel realm, ExportOptions options, boolean internal) {
        RealmRepresentation rep = ModelToRepresentation.toRepresentation(session, realm, internal);
        ModelToRepresentation.exportAuthenticationFlows(realm, rep);
        ModelToRepresentation.exportRequiredActions(realm, rep);

        // Project/product version
        rep.setKeycloakVersion(Version.VERSION_KEYCLOAK);

        // Client Scopes
        rep.setClientScopes(realm.getClientScopesStream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList()));
        rep.setDefaultDefaultClientScopes(realm.getDefaultClientScopesStream(true)
                .map(ClientScopeModel::getName).collect(Collectors.toList()));
        rep.setDefaultOptionalClientScopes(realm.getDefaultClientScopesStream(false)
                .map(ClientScopeModel::getName).collect(Collectors.toList()));

        // Clients
        List<ClientModel> clients = new LinkedList<>();

        if (options.isClientsIncluded()) {
            // we iterate over all clients in the stream.
            // only those client models that can be translated into a valid client representation will be added to the client list
            // that is later used to retrieve related information about groups and roles
            List<ClientRepresentation> clientReps = ModelToRepresentation.filterValidRepresentations(realm.getClientsStream(), app -> {
                ClientRepresentation clientRepresentation = exportClient(session, app);
                clients.add(app);
                return clientRepresentation;
            }).collect(Collectors.toList());
            rep.setClients(clientReps);
        }

        // Groups and Roles
        if (options.isGroupsAndRolesIncluded()) {
            ModelToRepresentation.exportGroups(realm, rep);

            Map<String, List<RoleRepresentation>> clientRolesReps = new HashMap<>();

            List<RoleRepresentation> realmRoleReps = exportRoles(realm.getRolesStream());

            RolesRepresentation rolesRep = new RolesRepresentation();
            if (!realmRoleReps.isEmpty()) {
                rolesRep.setRealm(realmRoleReps);
            }

            if (options.isClientsIncluded()) {
                for (ClientModel client : clients) {
                    Stream<RoleModel> currentAppRoles = client.getRolesStream();
                    List<RoleRepresentation> currentAppRoleReps = exportRoles(currentAppRoles);
                    clientRolesReps.put(client.getClientId(), currentAppRoleReps);
                }
                if (clientRolesReps.size() > 0) {
                    rolesRep.setClient(clientRolesReps);
                }
            }
            rep.setRoles(rolesRep);
        }

        // Scopes
        Map<String, List<ScopeMappingRepresentation>> clientScopeReps = new HashMap<>();

        if (options.isClientsIncluded()) {
            List<ClientModel> allClients = new ArrayList<>(clients);

            // Scopes of clients
            for (ClientModel client : allClients) {
                Set<RoleModel> clientScopes = client.getScopeMappingsStream().collect(Collectors.toSet());
                ScopeMappingRepresentation scopeMappingRep = null;
                for (RoleModel scope : clientScopes) {
                    if (scope.getContainer() instanceof RealmModel) {
                        if (scopeMappingRep == null) {
                            scopeMappingRep = rep.clientScopeMapping(client.getClientId());
                        }
                        scopeMappingRep.role(scope.getName());
                    } else {
                        ClientModel app = (ClientModel) scope.getContainer();
                        String appName = app.getClientId();
                        List<ScopeMappingRepresentation> currentAppScopes = clientScopeReps.get(appName);
                        if (currentAppScopes == null) {
                            currentAppScopes = new ArrayList<>();
                            clientScopeReps.put(appName, currentAppScopes);
                        }

                        ScopeMappingRepresentation currentClientScope = null;
                        for (ScopeMappingRepresentation scopeMapping : currentAppScopes) {
                            if (client.getClientId().equals(scopeMapping.getClient())) {
                                currentClientScope = scopeMapping;
                                break;
                            }
                        }
                        if (currentClientScope == null) {
                            currentClientScope = new ScopeMappingRepresentation();
                            currentClientScope.setClient(client.getClientId());
                            currentAppScopes.add(currentClientScope);
                        }
                        currentClientScope.role(scope.getName());
                    }
                }
            }
        }

        // Scopes of client scopes
        realm.getClientScopesStream().forEach(clientScope -> {
            Set<RoleModel> clientScopes = clientScope.getScopeMappingsStream().collect(Collectors.toSet());
            ScopeMappingRepresentation scopeMappingRep = null;
            for (RoleModel scope : clientScopes) {
                if (scope.getContainer() instanceof RealmModel) {
                    if (scopeMappingRep == null) {
                        scopeMappingRep = rep.clientScopeScopeMapping(clientScope.getName());
                    }
                    scopeMappingRep.role(scope.getName());
                } else {
                    ClientModel app = (ClientModel)scope.getContainer();
                    String appName = app.getClientId();
                    List<ScopeMappingRepresentation> currentAppScopes = clientScopeReps.get(appName);
                    if (currentAppScopes == null) {
                        currentAppScopes = new ArrayList<>();
                        clientScopeReps.put(appName, currentAppScopes);
                    }

                    ScopeMappingRepresentation currentClientTemplateScope = null;
                    for (ScopeMappingRepresentation scopeMapping : currentAppScopes) {
                        if (clientScope.getName().equals(scopeMapping.getClientScope())) {
                            currentClientTemplateScope = scopeMapping;
                            break;
                        }
                    }
                    if (currentClientTemplateScope == null) {
                        currentClientTemplateScope = new ScopeMappingRepresentation();
                        currentClientTemplateScope.setClientScope(clientScope.getName());
                        currentAppScopes.add(currentClientTemplateScope);
                    }
                    currentClientTemplateScope.role(scope.getName());
                }
            }
        });

        if (clientScopeReps.size() > 0) {
            rep.setClientScopeMappings(clientScopeReps);
        }

        // Finally users if needed
        if (options.isUsersIncluded()) {
            List<UserRepresentation> users = session.users().getUsersStream(realm, true)
                    .map(user -> exportUser(session, realm, user, options, internal))
                    .collect(Collectors.toList());

            if (users.size() > 0) {
                rep.setUsers(users);
            }

            UserFederatedStorageProvider userFederatedStorageProvider = userFederatedStorage(session);
            if (userFederatedStorageProvider != null) {
                List<UserRepresentation> federatedUsers = userFederatedStorage(session).getStoredUsersStream(realm, 0, -1)
                        .map(user -> exportFederatedUser(session, realm, user, options)).collect(Collectors.toList());
                if (federatedUsers.size() > 0) {
                    rep.setFederatedUsers(federatedUsers);
                }
            }

        } else if (options.isClientsIncluded() && options.isOnlyServiceAccountsIncluded()) {
            List<UserRepresentation> users = new LinkedList<>();
            for (ClientModel app : clients) {
                if (app.isServiceAccountsEnabled() && !app.isPublicClient() && !app.isBearerOnly()) {
                    UserModel user = session.users().getServiceAccount(app);
                    if (user != null) {
                        UserRepresentation userRep = exportUser(session, realm, user, options, internal);
                        users.add(userRep);
                    }
                }
            }

            if (users.size() > 0) {
                rep.setUsers(users);
            }
        }

        // components
        MultivaluedHashMap<String, ComponentExportRepresentation> components = exportComponents(realm, realm.getId());
        rep.setComponents(components);

        return rep;
    }

    public static MultivaluedHashMap<String, ComponentExportRepresentation> exportComponents(RealmModel realm, String parentId) {
        MultivaluedHashMap<String, ComponentExportRepresentation> components = new MultivaluedHashMap<>();
        realm.getComponentsStream(parentId).forEach(component -> {
            ComponentExportRepresentation compRep = new ComponentExportRepresentation();
            compRep.setId(component.getId());
            compRep.setProviderId(component.getProviderId());
            compRep.setConfig(component.getConfig());
            compRep.setName(component.getName());
            compRep.setSubType(component.getSubType());
            compRep.setSubComponents(exportComponents(realm, component.getId()));
            components.add(component.getProviderType(), compRep);
        });
        return components;
    }

    /**
     * Full export of application including claims and secret
     * @param client
     * @return full ApplicationRepresentation
     */
    public static ClientRepresentation exportClient(KeycloakSession session, ClientModel client) {
        ClientRepresentation clientRep = ModelToRepresentation.toRepresentation(client, session);
        clientRep.setSecret(client.getSecret());
        if (Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION)) {
            clientRep.setAuthorizationSettings(exportAuthorizationSettings(session, client));
        }
        return clientRep;
    }

    public static ResourceServerRepresentation exportAuthorizationSettings(KeycloakSession session, ClientModel client) {
        AuthorizationProviderFactory providerFactory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(AuthorizationProvider.class);
        AuthorizationProvider authorization = providerFactory.create(session, client.getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer settingsModel = authorization.getStoreFactory().getResourceServerStore().findByClient(client);

        if (settingsModel == null) {
            return null;
        }

        ResourceServerRepresentation representation = toRepresentation(settingsModel, client);

        representation.setId(null);
        representation.setName(null);
        representation.setClientId(null);

        List<ResourceRepresentation> resources = storeFactory.getResourceStore().findByResourceServer(settingsModel)
                .stream().map(resource -> {
                    ResourceRepresentation rep = toRepresentation(resource, settingsModel, authorization);

                    if (rep.getOwner().getId().equals(settingsModel.getClientId())) {
                        rep.setOwner((ResourceOwnerRepresentation) null);
                    } else {
                        rep.getOwner().setId(null);
                    }
                    rep.getScopes().forEach(scopeRepresentation -> {
                        scopeRepresentation.setId(null);
                        scopeRepresentation.setIconUri(null);
                    });

                    return rep;
                }).collect(Collectors.toList());

        representation.setResources(resources);

        List<PolicyRepresentation> policies = new ArrayList<>();
        PolicyStore policyStore = storeFactory.getPolicyStore();

        policies.addAll(policyStore.findByResourceServer(settingsModel)
                .stream().filter(policy -> !policy.getType().equals("resource") && !policy.getType().equals("scope") && policy.getOwner() == null)
                .map(policy -> createPolicyRepresentation(authorization, policy)).collect(Collectors.toList()));
        policies.addAll(policyStore.findByResourceServer(settingsModel)
                .stream().filter(policy -> (policy.getType().equals("resource") || policy.getType().equals("scope") && policy.getOwner() == null))
                .map(policy -> createPolicyRepresentation(authorization, policy)).collect(Collectors.toList()));

        representation.setPolicies(policies);

        List<ScopeRepresentation> scopes = storeFactory.getScopeStore().findByResourceServer(settingsModel).stream().map(scope -> {
            ScopeRepresentation rep = toRepresentation(scope);

            rep.setPolicies(null);
            rep.setResources(null);

            return rep;
        }).collect(Collectors.toList());

        representation.setScopes(scopes);

        return representation;
    }

    private static PolicyRepresentation createPolicyRepresentation(AuthorizationProvider authorizationProvider, Policy policy) {
        try {
            PolicyRepresentation rep = toRepresentation(policy, authorizationProvider, true, true);

            Map<String, String> config = new HashMap<>(rep.getConfig());

            rep.setConfig(config);

            Set<Scope> scopes = policy.getScopes();

            if (!scopes.isEmpty()) {
                List<String> scopeNames = scopes.stream().map(Scope::getName).collect(Collectors.toList());
                config.put("scopes", JsonSerialization.writeValueAsString(scopeNames));
            }

            Set<Resource> policyResources = policy.getResources();

            if (!policyResources.isEmpty()) {
                List<String> resourceNames = policyResources.stream().map(Resource::getName).collect(Collectors.toList());
                config.put("resources", JsonSerialization.writeValueAsString(resourceNames));
            }

            Set<Policy> associatedPolicies = policy.getAssociatedPolicies();

            if (!associatedPolicies.isEmpty()) {
                config.put("applyPolicies", JsonSerialization.writeValueAsString(associatedPolicies.stream().map(associated -> associated.getName()).collect(Collectors.toList())));
            }

            return rep;
        } catch (Exception e) {
            throw new RuntimeException("Error while exporting policy [" + policy.getName() + "].", e);
        }
    }

    public static List<RoleRepresentation> exportRoles(Stream<RoleModel> roles) {
        return roles.map(ExportUtils::exportRole).collect(Collectors.toList());
    }

    public static List<String> getRoleNames(Collection<RoleModel> roles) {
        List<String> roleNames = new ArrayList<>();
        for (RoleModel role : roles) {
            roleNames.add(role.getName());
        }
        return roleNames;
    }

    /**
     * Full export of role including composite roles
     * @param role
     * @return RoleRepresentation with all stuff filled (including composite roles)
     */
    public static RoleRepresentation exportRole(RoleModel role) {
        RoleRepresentation roleRep = ModelToRepresentation.toRepresentation(role);

        Set<RoleModel> composites = role.getCompositesStream().collect(Collectors.toSet());
        if (composites != null && composites.size() > 0) {
            Set<String> compositeRealmRoles = null;
            Map<String, List<String>> compositeClientRoles = null;

            for (RoleModel composite : composites) {
                RoleContainerModel crContainer = composite.getContainer();
                if (crContainer instanceof RealmModel) {

                    if (compositeRealmRoles == null) {
                        compositeRealmRoles = new HashSet<>();
                    }
                    compositeRealmRoles.add(composite.getName());
                } else {
                    if (compositeClientRoles == null) {
                        compositeClientRoles = new HashMap<>();
                    }

                    ClientModel app = (ClientModel)crContainer;
                    String appName = app.getClientId();
                    List<String> currentAppComposites = compositeClientRoles.get(appName);
                    if (currentAppComposites == null) {
                        currentAppComposites = new ArrayList<>();
                        compositeClientRoles.put(appName, currentAppComposites);
                    }
                    currentAppComposites.add(composite.getName());
                }
            }

            RoleRepresentation.Composites compRep = new RoleRepresentation.Composites();
            if (compositeRealmRoles != null) {
                compRep.setRealm(compositeRealmRoles);
            }
            if (compositeClientRoles != null) {
                compRep.setClient(compositeClientRoles);
            }

            roleRep.setComposites(compRep);
        }

        return roleRep;
    }

    /**
     * Full export of user (including role mappings and credentials)
     *
     * @param user
     * @return fully exported user representation
     */
    public static UserRepresentation exportUser(KeycloakSession session, RealmModel realm, UserModel user, ExportOptions options, boolean internal) {
        UserRepresentation userRep = ModelToRepresentation.toRepresentation(session, realm, user);

        // Social links
        List<FederatedIdentityRepresentation> socialLinkReps = session.users().getFederatedIdentitiesStream(realm, user)
                .map(ExportUtils::exportSocialLink).collect(Collectors.toList());
        if (socialLinkReps.size() > 0) {
            userRep.setFederatedIdentities(socialLinkReps);
        }

        // Role mappings
        if (options.isGroupsAndRolesIncluded()) {
            Set<RoleModel> roles = user.getRoleMappingsStream().collect(Collectors.toSet());
            List<String> realmRoleNames = new ArrayList<>();
            Map<String, List<String>> clientRoleNames = new HashMap<>();
            for (RoleModel role : roles) {
                if (role.getContainer() instanceof RealmModel) {
                    realmRoleNames.add(role.getName());
                } else {
                    ClientModel client = (ClientModel)role.getContainer();
                    String clientId = client.getClientId();
                    List<String> currentClientRoles = clientRoleNames.get(clientId);
                    if (currentClientRoles == null) {
                        currentClientRoles = new ArrayList<>();
                        clientRoleNames.put(clientId, currentClientRoles);
                    }

                    currentClientRoles.add(role.getName());
                }
            }

            if (realmRoleNames.size() > 0) {
                userRep.setRealmRoles(realmRoleNames);
            }
            if (clientRoleNames.size() > 0) {
                userRep.setClientRoles(clientRoleNames);
            }
        }

        // Credentials - extra security, do not export credentials if service accounts
        if (internal) {
            List<CredentialRepresentation> credReps = user.credentialManager().getStoredCredentialsStream()
                    .map(ExportUtils::exportCredential).collect(Collectors.toList());
            userRep.setCredentials(credReps);
        }

        userRep.setFederationLink(user.getFederationLink());

        // Grants
        List<UserConsentRepresentation> consentReps = session.users().getConsentsStream(realm, user.getId())
                .map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        if (consentReps.size() > 0) {
            userRep.setClientConsents(consentReps);
        }

        // Not Before
        int notBefore = session.users().getNotBeforeOfUser(realm, user);
        userRep.setNotBefore(notBefore);

        // Service account
        if (user.getServiceAccountClientLink() != null) {
            String clientInternalId = user.getServiceAccountClientLink();
            ClientModel client = realm.getClientById(clientInternalId);
            if (client != null) {
                userRep.setServiceAccountClientId(client.getClientId());
            }
        }

        if (options.isGroupsAndRolesIncluded()) {
            List<String> groups = user.getGroupsStream().map(ModelToRepresentation::buildGroupPath).collect(Collectors.toList());
            userRep.setGroups(groups);
        }
        return userRep;
    }

    public static FederatedIdentityRepresentation exportSocialLink(FederatedIdentityModel socialLink) {
        FederatedIdentityRepresentation socialLinkRep = new FederatedIdentityRepresentation();
        socialLinkRep.setIdentityProvider(socialLink.getIdentityProvider());
        socialLinkRep.setUserId(socialLink.getUserId());
        socialLinkRep.setUserName(socialLink.getUserName());
        return socialLinkRep;
    }

    public static CredentialRepresentation exportCredential(CredentialModel userCred) {
        return ModelToRepresentation.toRepresentation(userCred);
    }

    // Streaming API

    public static void exportUsersToStream(KeycloakSession session, RealmModel realm, List<UserModel> usersToExport, ObjectMapper mapper, OutputStream os) throws IOException {
        exportUsersToStream(session, realm, usersToExport, mapper, os, new ExportOptions());
    }

    public static void exportUsersToStream(KeycloakSession session, RealmModel realm, List<UserModel> usersToExport, ObjectMapper mapper, OutputStream os, ExportOptions options) throws IOException {
        JsonFactory factory = mapper.getFactory();
        JsonGenerator generator = factory.createGenerator(os, JsonEncoding.UTF8);
        try {
            if (mapper.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
                generator.useDefaultPrettyPrinter();
            }
            generator.writeStartObject();
            generator.writeStringField("realm", realm.getName());
            // generator.writeStringField("strategy", strategy.toString());
            generator.writeFieldName("users");
            generator.writeStartArray();

            for (UserModel user : usersToExport) {
                UserRepresentation userRep = ExportUtils.exportUser(session, realm, user, options, true);
                generator.writeObject(userRep);
            }

            generator.writeEndArray();
            generator.writeEndObject();
        } finally {
            generator.close();
        }
    }

    public static void exportFederatedUsersToStream(KeycloakSession session, RealmModel realm, List<String> usersToExport, ObjectMapper mapper, OutputStream os) throws IOException {
        exportFederatedUsersToStream(session, realm, usersToExport, mapper, os, new ExportOptions());
    }

    public static void exportFederatedUsersToStream(KeycloakSession session, RealmModel realm, List<String> usersToExport, ObjectMapper mapper, OutputStream os, ExportOptions options) throws IOException {
        JsonFactory factory = mapper.getFactory();
        JsonGenerator generator = factory.createGenerator(os, JsonEncoding.UTF8);
        try {
            if (mapper.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
                generator.useDefaultPrettyPrinter();
            }
            generator.writeStartObject();
            generator.writeStringField("realm", realm.getName());
            // generator.writeStringField("strategy", strategy.toString());
            generator.writeFieldName("federatedUsers");
            generator.writeStartArray();

            for (String userId : usersToExport) {
                UserRepresentation userRep = ExportUtils.exportFederatedUser(session, realm, userId, options);
                generator.writeObject(userRep);
            }

            generator.writeEndArray();
            generator.writeEndObject();
        } finally {
            generator.close();
        }
    }

    /**
     * Full export of user data stored in federated storage (including role mappings and credentials)
     *
     * @param id
     * @return fully exported user representation
     */
    public static UserRepresentation exportFederatedUser(KeycloakSession session, RealmModel realm, String id, ExportOptions options) {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setId(id);
        MultivaluedHashMap<String, String> attributes = userFederatedStorage(session).getAttributes(realm, id);
        if (attributes.size() > 0) {
            Map<String, List<String>> attrs = new HashMap<>();
            attrs.putAll(attributes);
            userRep.setAttributes(attrs);
        }

        List<String> requiredActions = userFederatedStorage(session).getRequiredActionsStream(realm, id).collect(Collectors.toList());
        if (requiredActions.size() > 0) {
            userRep.setRequiredActions(requiredActions);
        }

        // Social links
        List<FederatedIdentityRepresentation> socialLinkReps = userFederatedStorage(session).getFederatedIdentitiesStream(id, realm)
                .map(ExportUtils::exportSocialLink).collect(Collectors.toList());

        if (socialLinkReps.size() > 0) {
            userRep.setFederatedIdentities(socialLinkReps);
        }

        // Role mappings
        if (options.isGroupsAndRolesIncluded()) {
            Set<RoleModel> roles = userFederatedStorage(session).getRoleMappingsStream(realm, id).collect(Collectors.toSet());
            List<String> realmRoleNames = new ArrayList<>();
            Map<String, List<String>> clientRoleNames = new HashMap<>();
            for (RoleModel role : roles) {
                if (role.getContainer() instanceof RealmModel) {
                    realmRoleNames.add(role.getName());
                } else {
                    ClientModel client = (ClientModel) role.getContainer();
                    String clientId = client.getClientId();
                    List<String> currentClientRoles = clientRoleNames.get(clientId);
                    if (currentClientRoles == null) {
                        currentClientRoles = new ArrayList<>();
                        clientRoleNames.put(clientId, currentClientRoles);
                    }

                    currentClientRoles.add(role.getName());
                }
            }

            if (realmRoleNames.size() > 0) {
                userRep.setRealmRoles(realmRoleNames);
            }
            if (clientRoleNames.size() > 0) {
                userRep.setClientRoles(clientRoleNames);
            }
        }

        // Credentials
        List<CredentialRepresentation> credReps = userFederatedStorage(session).getStoredCredentialsStream(realm, id)
                .map(ExportUtils::exportCredential).collect(Collectors.toList());
        userRep.setCredentials(credReps);

        // Grants
        List<UserConsentRepresentation> consentReps = session.users().getConsentsStream(realm, id)
                .map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        if (consentReps.size() > 0) {
            userRep.setClientConsents(consentReps);
        }

        // Not Before
        int notBefore = userFederatedStorage(session).getNotBeforeOfUser(realm, userRep.getId());
        userRep.setNotBefore(notBefore);

        if (options.isGroupsAndRolesIncluded()) {
            List<String> groups = userFederatedStorage(session).getGroupsStream(realm, id)
                    .map(ModelToRepresentation::buildGroupPath).collect(Collectors.toList());
            userRep.setGroups(groups);
        }
        return userRep;
    }
    
    private static UserFederatedStorageProvider userFederatedStorage(KeycloakSession session) {
        return session.getProvider(UserFederatedStorageProvider.class);
    }

}
