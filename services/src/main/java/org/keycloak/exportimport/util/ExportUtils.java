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
import org.keycloak.common.Version;
import org.keycloak.common.util.Base64;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportUtils {

    public static RealmRepresentation exportRealm(KeycloakSession session, RealmModel realm, boolean includeUsers) {
        RealmRepresentation rep = ModelToRepresentation.toRepresentation(realm, true);

        // Project/product version
        rep.setKeycloakVersion(Version.VERSION);

        // Client Templates
        List<ClientTemplateModel> templates = realm.getClientTemplates();
        List<ClientTemplateRepresentation> templateReps = new ArrayList<>();
        for (ClientTemplateModel app : templates) {
            ClientTemplateRepresentation clientRep = ModelToRepresentation.toRepresentation(app);
            templateReps.add(clientRep);
        }
        rep.setClientTemplates(templateReps);

        // Clients
        List<ClientModel> clients = realm.getClients();
        List<ClientRepresentation> clientReps = new ArrayList<>();
        for (ClientModel app : clients) {
            ClientRepresentation clientRep = exportClient(app);
            clientReps.add(clientRep);
        }
        rep.setClients(clientReps);

        // Roles
        List<RoleRepresentation> realmRoleReps = null;
        Map<String, List<RoleRepresentation>> clientRolesReps = new HashMap<>();

        Set<RoleModel> realmRoles = realm.getRoles();
        if (realmRoles != null && realmRoles.size() > 0) {
            realmRoleReps = exportRoles(realmRoles);
        }
        for (ClientModel client : clients) {
            Set<RoleModel> currentAppRoles = client.getRoles();
            List<RoleRepresentation> currentAppRoleReps = exportRoles(currentAppRoles);
            clientRolesReps.put(client.getClientId(), currentAppRoleReps);
        }

        RolesRepresentation rolesRep = new RolesRepresentation();
        if (realmRoleReps != null) {
            rolesRep.setRealm(realmRoleReps);
        }
        if (clientRolesReps.size() > 0) {
            rolesRep.setClient(clientRolesReps);
        }
        rep.setRoles(rolesRep);

        // Scopes
        List<ClientModel> allClients = new ArrayList<>(clients);
        Map<String, List<ScopeMappingRepresentation>> clientScopeReps = new HashMap<>();

        // Scopes of clients
        for (ClientModel client : allClients) {
            Set<RoleModel> clientScopes = client.getScopeMappings();
            ScopeMappingRepresentation scopeMappingRep = null;
            for (RoleModel scope : clientScopes) {
                if (scope.getContainer() instanceof RealmModel) {
                    if (scopeMappingRep == null) {
                        scopeMappingRep = rep.clientScopeMapping(client.getClientId());
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

        // Scopes of client templates
        for (ClientTemplateModel clientTemplate : realm.getClientTemplates()) {
            Set<RoleModel> clientScopes = clientTemplate.getScopeMappings();
            ScopeMappingRepresentation scopeMappingRep = null;
            for (RoleModel scope : clientScopes) {
                if (scope.getContainer() instanceof RealmModel) {
                    if (scopeMappingRep == null) {
                        scopeMappingRep = rep.clientTemplateScopeMapping(clientTemplate.getName());
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
                        if (clientTemplate.getName().equals(scopeMapping.getClientTemplate())) {
                            currentClientTemplateScope = scopeMapping;
                            break;
                        }
                    }
                    if (currentClientTemplateScope == null) {
                        currentClientTemplateScope = new ScopeMappingRepresentation();
                        currentClientTemplateScope.setClientTemplate(clientTemplate.getName());
                        currentAppScopes.add(currentClientTemplateScope);
                    }
                    currentClientTemplateScope.role(scope.getName());
                }
            }
        }

        if (clientScopeReps.size() > 0) {
            rep.setClientScopeMappings(clientScopeReps);
        }

        // Finally users if needed
        if (includeUsers) {
            List<UserModel> allUsers = session.users().getUsers(realm, true);
            List<UserRepresentation> users = new ArrayList<UserRepresentation>();
            for (UserModel user : allUsers) {
                UserRepresentation userRep = exportUser(session, realm, user);
                users.add(userRep);
            }

            if (users.size() > 0) {
                rep.setUsers(users);
            }
        }

        return rep;
    }

    /**
     * Full export of application including claims and secret
     * @param client
     * @return full ApplicationRepresentation
     */
    public static ClientRepresentation exportClient(ClientModel client) {
        ClientRepresentation clientRep = ModelToRepresentation.toRepresentation(client);
        clientRep.setSecret(client.getSecret());
        return clientRep;
    }

    public static List<RoleRepresentation> exportRoles(Collection<RoleModel> roles) {
        List<RoleRepresentation> roleReps = new ArrayList<RoleRepresentation>();

        for (RoleModel role : roles) {
            RoleRepresentation roleRep = exportRole(role);
            roleReps.add(roleRep);
        }
        return roleReps;
    }

    public static List<String> getRoleNames(Collection<RoleModel> roles) {
        List<String> roleNames = new ArrayList<String>();
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

        Set<RoleModel> composites = role.getComposites();
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
    public static UserRepresentation exportUser(KeycloakSession session, RealmModel realm, UserModel user) {
        UserRepresentation userRep = ModelToRepresentation.toRepresentation(user);

        // Social links
        Set<FederatedIdentityModel> socialLinks = session.users().getFederatedIdentities(user, realm);
        List<FederatedIdentityRepresentation> socialLinkReps = new ArrayList<FederatedIdentityRepresentation>();
        for (FederatedIdentityModel socialLink : socialLinks) {
            FederatedIdentityRepresentation socialLinkRep = exportSocialLink(socialLink);
            socialLinkReps.add(socialLinkRep);
        }
        if (socialLinkReps.size() > 0) {
            userRep.setFederatedIdentities(socialLinkReps);
        }

        // Role mappings
        Set<RoleModel> roles = user.getRoleMappings();
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

        // Credentials
        List<UserCredentialValueModel> creds = user.getCredentialsDirectly();
        List<CredentialRepresentation> credReps = new ArrayList<CredentialRepresentation>();
        for (UserCredentialValueModel cred : creds) {
            CredentialRepresentation credRep = exportCredential(cred);
            credReps.add(credRep);
        }
        userRep.setCredentials(credReps);
        userRep.setFederationLink(user.getFederationLink());

        // Grants
        List<UserConsentModel> consents = user.getConsents();
        LinkedList<UserConsentRepresentation> consentReps = new LinkedList<UserConsentRepresentation>();
        for (UserConsentModel consent : consents) {
            UserConsentRepresentation consentRep = ModelToRepresentation.toRepresentation(consent);
            consentReps.add(consentRep);
        }
        if (consentReps.size() > 0) {
            userRep.setClientConsents(consentReps);
        }

        // Service account
        if (user.getServiceAccountClientLink() != null) {
            String clientInternalId = user.getServiceAccountClientLink();
            ClientModel client = realm.getClientById(clientInternalId);
            if (client != null) {
                userRep.setServiceAccountClientId(client.getClientId());
            }
        }

        List<String> groups = new LinkedList<>();
        for (GroupModel group : user.getGroups()) {
            groups.add(ModelToRepresentation.buildGroupPath(group));
        }
        userRep.setGroups(groups);

        return userRep;
    }

    public static FederatedIdentityRepresentation exportSocialLink(FederatedIdentityModel socialLink) {
        FederatedIdentityRepresentation socialLinkRep = new FederatedIdentityRepresentation();
        socialLinkRep.setIdentityProvider(socialLink.getIdentityProvider());
        socialLinkRep.setUserId(socialLink.getUserId());
        socialLinkRep.setUserName(socialLink.getUserName());
        return socialLinkRep;
    }

    public static CredentialRepresentation exportCredential(UserCredentialValueModel userCred) {
        CredentialRepresentation credRep = new CredentialRepresentation();
        credRep.setType(userCred.getType());
        credRep.setDevice(userCred.getDevice());
        credRep.setHashedSaltedValue(userCred.getValue());
        if (userCred.getSalt() != null) credRep.setSalt(Base64.encodeBytes(userCred.getSalt()));
        credRep.setHashIterations(userCred.getHashIterations());
        credRep.setCounter(userCred.getCounter());
        credRep.setAlgorithm(userCred.getAlgorithm());
        credRep.setDigits(userCred.getDigits());
        credRep.setCreatedDate(userCred.getCreatedDate());
        return credRep;
    }

    // Streaming API

    public static void exportUsersToStream(KeycloakSession session, RealmModel realm, List<UserModel> usersToExport, ObjectMapper mapper, OutputStream os) throws IOException {
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
                UserRepresentation userRep = ExportUtils.exportUser(session, realm, user);
                generator.writeObject(userRep);
            }

            generator.writeEndArray();
            generator.writeEndObject();
        } finally {
            generator.close();
        }
    }
}
