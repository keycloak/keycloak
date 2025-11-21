/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.utils;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.client.clienttype.ClientType;
import org.keycloak.client.clienttype.ClientTypeException;
import org.keycloak.client.clienttype.ClientTypeManager;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.common.util.UriUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.deployment.DeployedConfigurationsManager;
import org.keycloak.migration.migrators.MigrationUtils;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.OTPCredentialData;
import org.keycloak.models.credential.dto.OTPSecretData;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.policy.PasswordPolicyNotMetException;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserConsentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import static java.util.Optional.ofNullable;

import static org.keycloak.models.OrganizationDomainModel.ANY_DOMAIN;
import static org.keycloak.protocol.saml.util.ArtifactBindingUtils.computeArtifactBindingIdentifierString;

public class RepresentationToModel {

    private static Logger logger = Logger.getLogger(RepresentationToModel.class);
    public static final String OIDC = "openid-connect";


    public static void importRealm(KeycloakSession session, RealmRepresentation rep, RealmModel newRealm, Runnable userImport) {
        session.getProvider(DatastoreProvider.class).getExportImportManager().importRealm(rep, newRealm, userImport);
    }

    public static void importRoles(RolesRepresentation realmRoles, RealmModel realm) {
        if (realmRoles == null) return;

        if (realmRoles.getRealm() != null) { // realm roles
            for (RoleRepresentation roleRep : realmRoles.getRealm()) {
                if (! realm.getDefaultRole().getName().equals(roleRep.getName())) { // default role was already imported
                    createRole(realm, roleRep);
                }
            }
        }
        if (realmRoles.getClient() != null) {
            for (Map.Entry<String, List<RoleRepresentation>> entry : realmRoles.getClient().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new RuntimeException("App doesn't exist in role definitions: " + entry.getKey());
                }
                for (RoleRepresentation roleRep : entry.getValue()) {
                    // Application role may already exists (for example if it is defaultRole)
                    RoleModel role = roleRep.getId() != null ? client.addRole(roleRep.getId(), roleRep.getName()) : client.addRole(roleRep.getName());
                    role.setDescription(roleRep.getDescription());
                    if (roleRep.getAttributes() != null) {
                        roleRep.getAttributes().forEach((key, value) -> role.setAttribute(key, value));
                    }
                }
            }
        }
        // now that all roles are created, re-iterate and set up composites
        if (realmRoles.getRealm() != null) { // realm roles
            for (RoleRepresentation roleRep : realmRoles.getRealm()) {
                RoleModel role = realm.getRole(roleRep.getName());
                addComposites(role, roleRep, realm);
            }
        }
        if (realmRoles.getClient() != null) {
            for (Map.Entry<String, List<RoleRepresentation>> entry : realmRoles.getClient().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new RuntimeException("App doesn't exist in role definitions: " + entry.getKey());
                }
                for (RoleRepresentation roleRep : entry.getValue()) {
                    RoleModel role = client.getRole(roleRep.getName());
                    addComposites(role, roleRep, realm);
                }
            }
        }
    }


    public static void importGroup(RealmModel realm, GroupModel parent, GroupRepresentation group) {
        GroupModel newGroup = realm.createGroup(group.getId(), group.getName(), parent);
        if (group.getDescription() != null) {
            newGroup.setDescription(group.getDescription());
        }
        if (group.getAttributes() != null) {
            for (Map.Entry<String, List<String>> attr : group.getAttributes().entrySet()) {
                newGroup.setAttribute(attr.getKey(), attr.getValue());
            }
        }

        if (group.getRealmRoles() != null) {
            for (String roleString : group.getRealmRoles()) {
                RoleModel role = realm.getRole(roleString.trim());
                if (role == null) {
                    role = realm.addRole(roleString.trim());
                }
                newGroup.grantRole(role);
            }
        }
        if (group.getClientRoles() != null) {
            for (Map.Entry<String, List<String>> entry : group.getClientRoles().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new RuntimeException("Unable to find client role mappings for client: " + entry.getKey());
                }
                List<String> roleNames = entry.getValue();
                for (String roleName : roleNames) {
                    RoleModel role = client.getRole(roleName.trim());
                    if (role == null) {
                        role = client.addRole(roleName.trim());
                    }
                    newGroup.grantRole(role);

                }
            }
        }
        if (group.getSubGroups() != null) {
            for (GroupRepresentation subGroup : group.getSubGroups()) {
                importGroup(realm, newGroup, subGroup);
            }
        }
    }


    private static void convertDeprecatedCredentialsFormat(UserRepresentation user) {
        if (user.getCredentials() != null) {
            for (CredentialRepresentation cred : user.getCredentials()) {
                try {
                    if ((cred.getCredentialData() == null || cred.getSecretData() == null) && cred.getValue() == null) {
                        logger.warnf("Using deprecated 'credentials' format in JSON representation for user '%s'. It will be removed in future versions", user.getUsername());

                        if (PasswordCredentialModel.TYPE.equals(cred.getType()) || PasswordCredentialModel.PASSWORD_HISTORY.equals(cred.getType())) {
                            PasswordCredentialData credentialData = new PasswordCredentialData(cred.getHashIterations(), cred.getAlgorithm());
                            cred.setCredentialData(JsonSerialization.writeValueAsString(credentialData));
                            // Created this manually to avoid conversion from Base64 and back
                            cred.setSecretData("{\"value\":\"" + cred.getHashedSaltedValue() + "\",\"salt\":\"" + cred.getSalt() + "\"}");
                            cred.setPriority(10);
                        } else if (OTPCredentialModel.TOTP.equals(cred.getType()) || OTPCredentialModel.HOTP.equals(cred.getType())) {
                            OTPCredentialData credentialData = new OTPCredentialData(cred.getType(), cred.getDigits(), cred.getCounter(), cred.getPeriod(), cred.getAlgorithm(), null);
                            OTPSecretData secretData = new OTPSecretData(cred.getHashedSaltedValue());
                            cred.setCredentialData(JsonSerialization.writeValueAsString(credentialData));
                            cred.setSecretData(JsonSerialization.writeValueAsString(secretData));
                            cred.setPriority(20);
                            cred.setType(OTPCredentialModel.TYPE);
                        }
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
    }




    public static void updateRealm(RealmRepresentation rep, RealmModel realm, KeycloakSession session) {
        session.getProvider(DatastoreProvider.class).getExportImportManager().updateRealm(rep, realm);

    }

    // Basic realm stuff

    // Roles

    public static RoleModel createRole(RealmModel newRealm, RoleRepresentation roleRep) {
        RoleModel role = roleRep.getId() != null ? newRealm.addRole(roleRep.getId(), roleRep.getName()) : newRealm.addRole(roleRep.getName());
        if (roleRep.getDescription() != null) role.setDescription(roleRep.getDescription());
        if (roleRep.getAttributes() != null) {
            for (Map.Entry<String, List<String>> attribute : roleRep.getAttributes().entrySet()) {
                role.setAttribute(attribute.getKey(), attribute.getValue());
            }
        }
        return role;
    }

    private static void addComposites(RoleModel role, RoleRepresentation roleRep, RealmModel realm) {
        if (roleRep.getComposites() == null) return;
        if (roleRep.getComposites().getRealm() != null) {
            for (String roleStr : roleRep.getComposites().getRealm()) {
                RoleModel realmRole = realm.getRole(roleStr);
                if (realmRole == null) throw new RuntimeException("Unable to find composite realm role: " + roleStr);
                role.addCompositeRole(realmRole);
            }
        }
        if (roleRep.getComposites().getClient() != null) {
            for (Map.Entry<String, List<String>> entry : roleRep.getComposites().getClient().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new RuntimeException("App doesn't exist in role definitions: " + roleRep.getName());
                }
                for (String roleStr : entry.getValue()) {
                    RoleModel clientRole = client.getRole(roleStr);
                    if (clientRole == null)
                        throw new RuntimeException("Unable to find composite client role: " + roleStr);
                    role.addCompositeRole(clientRole);
                }
            }

        }

    }

    // CLIENTS

    /**
     * Does not create scope or role mappings!
     *
     * @param realm
     * @param resourceRep
     * @return
     */
    public static ClientModel createClient(KeycloakSession session, RealmModel realm, ClientRepresentation resourceRep) {
        return createClient(session, realm, resourceRep, null);
    }

    public static ClientModel createClient(KeycloakSession session, RealmModel realm, ClientRepresentation resourceRep, Map<String, String> mappedFlows) {
        logger.debugv("Create client: {0}", resourceRep.getClientId());

        ClientModel client = resourceRep.getId() != null ? realm.addClient(resourceRep.getId(), resourceRep.getClientId()) : realm.addClient(resourceRep.getClientId());

        if (Profile.isFeatureEnabled(Profile.Feature.CLIENT_TYPES) && resourceRep.getType() != null) {
            ClientTypeManager mgr = session.getProvider(ClientTypeManager.class);
            ClientType clientType = mgr.getClientType(realm, resourceRep.getType());
            client = clientType.augment(client);
        }

        updateClientProperties(client, resourceRep, true);

        // Backwards compatibility only
        if (resourceRep.isDirectGrantsOnly() != null) {
            logger.warn("Using deprecated 'directGrantsOnly' configuration in JSON representation. It will be removed in future versions");
            client.setStandardFlowEnabled(!resourceRep.isDirectGrantsOnly());
            client.setDirectAccessGrantsEnabled(resourceRep.isDirectGrantsOnly());
        }

        if ("saml".equals(resourceRep.getProtocol())
                && (resourceRep.getAttributes() == null
                || !resourceRep.getAttributes().containsKey("saml.artifact.binding.identifier"))) {
            client.setAttribute("saml.artifact.binding.identifier", computeArtifactBindingIdentifierString(resourceRep.getClientId()));
        }

        if (resourceRep.getAuthenticationFlowBindingOverrides() != null) {
            for (Map.Entry<String, String> entry : resourceRep.getAuthenticationFlowBindingOverrides().entrySet()) {
                if (entry.getValue() == null || entry.getValue().trim().equals("")) {
                    continue;
                } else {
                    String flowId = entry.getValue();
                    // check if flow id was mapped when the flows were imported
                    if (mappedFlows != null && mappedFlows.containsKey(flowId)) {
                        flowId = mappedFlows.get(flowId);
                    }
                    if (client.getRealm().getAuthenticationFlowById(flowId) == null) {
                        throw new RuntimeException("Unable to resolve auth flow binding override for: " + entry.getKey());
                    }
                    client.setAuthenticationFlowBindingOverride(entry.getKey(), flowId);
                }
            }
        }

        if (resourceRep.getRegisteredNodes() != null) {
            for (Map.Entry<String, Integer> entry : resourceRep.getRegisteredNodes().entrySet()) {
                client.registerNode(entry.getKey(), entry.getValue());
            }
        }

        if (resourceRep.getProtocolMappers() != null) {
            // first, remove all default/built in mappers
            client.getProtocolMappersStream().collect(Collectors.toList()).forEach(client::removeProtocolMapper);

            for (ProtocolMapperRepresentation mapper : resourceRep.getProtocolMappers()) {
                client.addProtocolMapper(toModel(mapper));
            }

            MigrationUtils.updateProtocolMappers(client);
        }

        if (resourceRep.getClientTemplate() != null) {
            String clientTemplateName = KeycloakModelUtils.convertClientScopeName(resourceRep.getClientTemplate());
            addClientScopeToClient(realm, client, clientTemplateName, true);
        }

        updateClientScopes(resourceRep, client);

        client.updateClient();
        resourceRep.setId(client.getId());

        return client;
    }

    private static void addClientScopeToClient(RealmModel realm, ClientModel client, String clientScopeName, boolean defaultScope) {
        ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(realm, clientScopeName);
        if (clientScope != null) {
            client.addClientScope(clientScope, defaultScope);
        } else {
            logger.warnf("Referenced client scope '%s' doesn't exist. Ignoring", clientScopeName);
        }
    }

    public static void updateClient(ClientRepresentation rep, ClientModel resource, KeycloakSession session) {

        if (Profile.isFeatureEnabled(Profile.Feature.CLIENT_TYPES)) {
            if (!ObjectUtil.isEqualOrBothNull(resource.getType(), rep.getType())) {
                throw ClientTypeException.Message.CANNOT_CHANGE_CLIENT_TYPE.exception();
            }
            if (rep.getType() != null) {
                RealmModel realm = session.getContext().getRealm();
                ClientTypeManager mgr = session.getProvider(ClientTypeManager.class);
                ClientType clientType = mgr.getClientType(realm, rep.getType());
                resource = clientType.augment(resource);
            }
        }

        String newClientId = rep.getClientId();
        String previousClientId = resource.getClientId();

        if (newClientId != null) resource.setClientId(newClientId);

        updateClientProperties(resource, rep, false);

        if (newClientId != null && "saml".equals(rep.getProtocol())
                && (rep.getAttributes() == null
                || !rep.getAttributes().containsKey("saml.artifact.binding.identifier"))) {
            resource.setAttribute("saml.artifact.binding.identifier", computeArtifactBindingIdentifierString(newClientId));
        }

        if (rep.getAuthenticationFlowBindingOverrides() != null) {
            for (Map.Entry<String, String> entry : rep.getAuthenticationFlowBindingOverrides().entrySet()) {
                if (entry.getValue() == null || entry.getValue().trim().equals("")) {
                    resource.removeAuthenticationFlowBindingOverride(entry.getKey());
                } else {
                    String flowId = entry.getValue();
                    if (resource.getRealm().getAuthenticationFlowById(flowId) == null) {
                        throw new RuntimeException("Unable to resolve auth flow binding override for: " + entry.getKey());
                    }
                    resource.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                }
            }
        }

        if (rep.getRegisteredNodes() != null) {
            for (Map.Entry<String, Integer> entry : rep.getRegisteredNodes().entrySet()) {
                resource.registerNode(entry.getKey(), entry.getValue());
            }
        }

        resource.updateClient();

        if (!Objects.equals(newClientId, previousClientId)) {
            ClientModel finalResource = resource;
            ClientModel.ClientIdChangeEvent event = new ClientModel.ClientIdChangeEvent() {
                @Override
                public ClientModel getUpdatedClient() {
                    return finalResource;
                }

                @Override
                public String getPreviousClientId() {
                    return previousClientId;
                }

                @Override
                public String getNewClientId() {
                    return newClientId;
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }
            };
            session.getKeycloakSessionFactory().publish(event);
        }
    }

    /**
     * Update client properties and process any {@link ClientTypeException} validation errors combining into one to be thrown.
     *
     * @param client {@link ClientModel} to update
     * @param rep {@link ClientRepresentation} to apply updates.
     * @param isNew  Whether the client is new or not (needed because some getters cannot return null).
     */
    private static void updateClientProperties(ClientModel client, ClientRepresentation rep, boolean isNew) {
        List<Supplier<ClientTypeException>> clientPropertyUpdates = new LinkedList<Supplier<ClientTypeException>>() {{
            /**
             * Values from the ClientRepresentation take precedence.
             * Then values from the ClientModel (these may be augmented from the client type configuration).
             * Otherwise, we can choose some sane default.
              */
            add(updatePropertyAction(client::setName, rep::getName, client::getName));
            add(updatePropertyAction(client::setDescription, rep::getDescription, client::getDescription));
            add(updatePropertyAction(client::setType, rep::getType, client::getType));
            add(updatePropertyAction(client::setEnabled, rep::isEnabled, client::isEnabled));
            add(updatePropertyAction(client::setAlwaysDisplayInConsole, rep::isAlwaysDisplayInConsole, client::isAlwaysDisplayInConsole));
            add(updatePropertyAction(client::setManagementUrl, rep::getAdminUrl, client::getManagementUrl));
            add(updatePropertyAction(client::setSurrogateAuthRequired, rep::isSurrogateAuthRequired, client::isSurrogateAuthRequired));
            add(updatePropertyAction(client::setRootUrl, rep::getRootUrl, client::getRootUrl));
            add(updatePropertyAction(client::setBaseUrl, rep::getBaseUrl, client::getBaseUrl));
            add(updatePropertyAction(client::setBearerOnly, rep::isBearerOnly, client::isBearerOnly));
            add(updatePropertyAction(client::setConsentRequired, rep::isConsentRequired, client::isConsentRequired));
            add(updatePropertyAction(client::setStandardFlowEnabled, rep::isStandardFlowEnabled, client::isStandardFlowEnabled));
            add(updatePropertyAction(client::setImplicitFlowEnabled, rep::isImplicitFlowEnabled, client::isImplicitFlowEnabled));
            add(updatePropertyAction(client::setDirectAccessGrantsEnabled, rep::isDirectAccessGrantsEnabled, client::isDirectAccessGrantsEnabled));
            add(updatePropertyAction(client::setServiceAccountsEnabled, rep::isServiceAccountsEnabled, client::isServiceAccountsEnabled));
            add(updatePropertyAction(client::setPublicClient, rep::isPublicClient, client::isPublicClient));
            add(updatePropertyAction(client::setFrontchannelLogout, rep::isFrontchannelLogout, client::isFrontchannelLogout));
            add(updatePropertyAction(client::setNotBefore, rep::getNotBefore, client::getNotBefore));
            // Fields with defaults if not initially provided
            add(updatePropertyAction(client::setProtocol, rep::getProtocol, client::getProtocol, () -> OIDC));
            add(updatePropertyAction(client::setNodeReRegistrationTimeout, rep::getNodeReRegistrationTimeout, () -> defaultNodeReRegistrationTimeout(client, isNew)));
            add(updatePropertyAction(client::setClientAuthenticatorType, rep::getClientAuthenticatorType, client::getClientAuthenticatorType, KeycloakModelUtils::getDefaultClientAuthenticatorType));
            add(updatePropertyAction(client::setFullScopeAllowed, rep::isFullScopeAllowed, () -> defaultFullScopeAllowed(client, isNew)));
            // Client Secret
            add(updatePropertyAction(client::setSecret, () -> determineNewSecret(client, rep)));
            // Redirect uris / Web origins
            add(updatePropertyAction(client::setRedirectUris, () -> CollectionUtil.collectionToSet(rep.getRedirectUris()), client::getRedirectUris));
            add(updatePropertyAction(client::setWebOrigins, () -> CollectionUtil.collectionToSet(rep.getWebOrigins()), () -> defaultWebOrigins(client)));
        }};

        // Extended client attributes
        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                clientPropertyUpdates.add(
                    updatePropertyAction(val -> client.setAttribute(entry.getKey(), val), entry::getValue));
            }
        }

        List<ClientTypeException> propertyUpdateExceptions = clientPropertyUpdates
                .stream()
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (propertyUpdateExceptions.size() > 0) {
            Object[] paramsWithFailures = propertyUpdateExceptions.stream().map(ClientTypeException::getParameters).flatMap(Stream::of).toArray();
            throw ClientTypeException.Message.CLIENT_UPDATE_FAILED_CLIENT_TYPE_VALIDATION.exception(paramsWithFailures);
        }
    }

    private static Boolean defaultFullScopeAllowed(ClientModel client, boolean isNew) {
        // If the client is newly created and the value is set to true, the value must be augmented through a
        // client type configuration, so do not update. Yet if new and false, the field MAY be controlled through a
        // client type. We can only attempt to set the sane default based on consent required.
        return isNew && !client.isFullScopeAllowed() ? !client.isConsentRequired() : client.isFullScopeAllowed();
    }

    private static Integer defaultNodeReRegistrationTimeout(ClientModel client, boolean isNew) {
        // If the client is newly created and the value is 0, the client MAY not be augmented with a client type.
        if (isNew && Objects.equals(client.getNodeReRegistrationTimeout(), 0)) {
            // Return the sane default.
            return -1;
        }
        // Update client request without an overriding value or is new and augmented with a client type,
        // do not attempt to update.
        return client.getNodeReRegistrationTimeout();
    }

    private static String determineNewSecret(ClientModel client, ClientRepresentation rep) {
        if (client.isPublicClient() || client.isBearerOnly()) {
            // Clear out the secret with null
            return null;
        }

        // adding secret if the client isn't public nor bearer only
        String currentSecret = client.getSecret();
        String newSecret = rep.getSecret();

        if (newSecret == null && currentSecret == null) {
            return KeycloakModelUtils.generateSecret(client);
        } else if (newSecret != null) {
            return newSecret;
        }
        // Do not change current secret.
        return currentSecret;
    }

    private static Set<String> defaultWebOrigins(ClientModel client) {
        Set<String> webOrigins = client.getWebOrigins();
        if (webOrigins != null && !webOrigins.isEmpty()) {
            return webOrigins;
        }

        Set<String> redirectUris = client.getRedirectUris();
        if (redirectUris == null || redirectUris.isEmpty()) {
            return new HashSet<>();
        }

        return client.getRedirectUris()
                .stream()
                .filter(uri -> uri.startsWith("http"))
                .map(UriUtils::getOrigin)
                .collect(Collectors.toSet());
    }

    public static void updateClientProtocolMappers(ClientRepresentation rep, ClientModel resource) {

        if (rep.getProtocolMappers() != null) {
            Map<String,ProtocolMapperModel> existingProtocolMappers =
                    resource.getProtocolMappersStream().collect(Collectors.toMap(mapper ->
                            generateProtocolNameKey(mapper.getProtocol(), mapper.getName()), Function.identity()));


            for (ProtocolMapperRepresentation protocolMapperRepresentation : rep.getProtocolMappers()) {
                String protocolNameKey = generateProtocolNameKey(protocolMapperRepresentation.getProtocol(), protocolMapperRepresentation.getName());
                ProtocolMapperModel existingMapper = existingProtocolMappers.get(protocolNameKey);
                    if (existingMapper != null) {
                        ProtocolMapperModel updatedProtocolMapperModel = toModel(protocolMapperRepresentation);
                        updatedProtocolMapperModel.setId(existingMapper.getId());
                        resource.updateProtocolMapper(updatedProtocolMapperModel);

                        existingProtocolMappers.remove(protocolNameKey);

                } else {
                    resource.addProtocolMapper(toModel(protocolMapperRepresentation));
                }
            }

            for (Map.Entry<String, ProtocolMapperModel> entryToDelete : existingProtocolMappers.entrySet()) {
                resource.removeProtocolMapper(entryToDelete.getValue());
            }
        }
    }

    public static void updateClientScopes(ClientRepresentation resourceRep, ClientModel client) {
        if (resourceRep.getDefaultClientScopes() != null || resourceRep.getOptionalClientScopes() != null) {
            // first collect all the desired scopes
            LinkedHashMap<String, Boolean> allScopes = new LinkedHashMap<String, Boolean>();
            Optional.ofNullable(resourceRep.getOptionalClientScopes()).ifPresent(scopes -> scopes.forEach(scope -> allScopes.put(scope, false)));
            Optional.ofNullable(resourceRep.getDefaultClientScopes()).ifPresent(scopes -> scopes.forEach(scope -> allScopes.put(scope, true)));

            // next determine what already exists
            Map<Map.Entry<String, Boolean>, ClientScopeModel> existing = new HashMap<Map.Entry<String,Boolean>, ClientScopeModel>();
            client.getClientScopes(false).entrySet().stream().forEach(entry -> existing.put(new AbstractMap.SimpleEntry<String, Boolean>(entry.getKey(), false), entry.getValue()));
            client.getClientScopes(true).entrySet().stream().forEach(entry -> existing.put(new AbstractMap.SimpleEntry<String, Boolean>(entry.getKey(), true), entry.getValue()));

            // remove anything that isn't desired - this includes client scopes that are toggling the default flag
            for (Entry<Entry<String, Boolean>, ClientScopeModel> entry : existing.entrySet()) {
                if (Optional.ofNullable(allScopes.get(entry.getKey().getKey())).filter(entry.getKey().getValue()::equals).isEmpty()) {
                    client.removeClientScope(entry.getValue());
                }
            }

            // finally add in all the desired
            for (Map.Entry<String, Boolean> entry : allScopes.entrySet()) {
                if (!existing.containsKey(entry)) {
                    addClientScopeToClient(client.getRealm(), client, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Create Supplier to update property.
     * Captures and returns {@link ClientTypeException} if thrown by the setter.
     *
     * @param modelSetter setter to call.
     * @param representationGetter getter supplying the property update.
     * @return {@link Supplier<T>} resulting whether a {@link ClientTypeException} was thrown.
     * @param <T> Type of property.
     */
    private static <T> Supplier<ClientTypeException> updateProperty(Consumer<T> modelSetter, Supplier<T> representationGetter) {
        return () -> {
            try {
                T value = representationGetter.get();
                modelSetter.accept(value);
            } catch (ClientTypeException cte) {
                return cte;
            }
            return null;
        };
    }

    /**
     * Create an update property action, passing the first non-null value supplied to the setter, in argument order.
     *
     * @param modelSetter {@link Consumer<T>} The setter to call.
     * @param getters {@link Supplier<T>} to query for the first non-null value.
     * @return {@link Supplier} with results of operation.
     * @param <T> Type of property.
     */
    @SafeVarargs
    private static <T> Supplier<ClientTypeException> updatePropertyAction(Consumer<T> modelSetter, Supplier<T>... getters) {
        Stream<T> firstNonNullSupplied = Stream.of(getters)
                .map(Supplier::get)
                .filter(Objects::nonNull);
        return updateProperty(modelSetter, () -> firstNonNullSupplied.findFirst().orElse(null));
    }

    private static String generateProtocolNameKey(String protocol, String name) {
        return String.format("%s%%%s", protocol, name);
    }

    // CLIENT SCOPES


    public static ClientScopeModel createClientScope(RealmModel realm, ClientScopeRepresentation resourceRep) {
        logger.debugv("Create client scope: {0}", resourceRep.getName());

        ClientScopeModel clientScope = resourceRep.getId() != null ? realm.addClientScope(resourceRep.getId(), resourceRep.getName()) : realm.addClientScope(resourceRep.getName());
        if (resourceRep.getName() != null) clientScope.setName(resourceRep.getName());
        if (resourceRep.getDescription() != null) clientScope.setDescription(resourceRep.getDescription());
        if (resourceRep.getProtocol() != null) clientScope.setProtocol(resourceRep.getProtocol());
        if (resourceRep.getProtocolMappers() != null) {
            // first, remove all default/built in mappers
            clientScope.getProtocolMappersStream().collect(Collectors.toList()).forEach(clientScope::removeProtocolMapper);

            for (ProtocolMapperRepresentation mapper : resourceRep.getProtocolMappers()) {
                clientScope.addProtocolMapper(toModel(mapper));
            }
            MigrationUtils.updateProtocolMappers(clientScope);
        }

        if (resourceRep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : resourceRep.getAttributes().entrySet()) {
                clientScope.setAttribute(entry.getKey(), entry.getValue());
            }
        }


        return clientScope;
    }

    public static void updateClientScope(ClientScopeRepresentation rep, ClientScopeModel resource) {
        if (rep.getName() != null) resource.setName(rep.getName());
        if (rep.getDescription() != null) resource.setDescription(rep.getDescription());

        if (rep.getProtocol() != null) resource.setProtocol(rep.getProtocol());

        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                resource.setAttribute(entry.getKey(), entry.getValue());
            }
        }

    }

    // Users

    public static UserModel createUser(KeycloakSession session, RealmModel newRealm, UserRepresentation userRep) {
        return session.getProvider(DatastoreProvider.class).getExportImportManager().createUser(newRealm, userRep);
    }

    public static void createGroups(KeycloakSession session, UserRepresentation userRep, RealmModel newRealm, UserModel user) {
        if (userRep.getGroups() != null) {
            for (String path : userRep.getGroups()) {
                GroupModel group = KeycloakModelUtils.findGroupByPath(session, newRealm, path);
                if (group == null) {
                    throw new RuntimeException("Unable to find group specified by path: " + path);

                }
                user.joinGroup(group);
            }
        }
    }

    public static void createFederatedIdentities(UserRepresentation userRep, KeycloakSession session, RealmModel realm, UserModel user) {
        if (userRep.getFederatedIdentities() != null) {
            for (FederatedIdentityRepresentation identity : userRep.getFederatedIdentities()) {
                FederatedIdentityModel mappingModel = new FederatedIdentityModel(identity.getIdentityProvider(), identity.getUserId(), identity.getUserName());
                session.users().addFederatedIdentity(realm, user, mappingModel);
            }
        }
    }

    public static void createCredentials(UserRepresentation userRep, KeycloakSession session, RealmModel realm, UserModel user, boolean adminRequest) {
        convertDeprecatedCredentialsFormat(userRep);
        if (userRep.getCredentials() != null) {
            for (CredentialRepresentation cred : userRep.getCredentials()) {
                if (cred.getId() != null && user.credentialManager().getStoredCredentialById(cred.getId()) != null) {
                    continue;
                }
                if (cred.getValue() != null && !cred.getValue().isEmpty()) {
                    RealmModel origRealm = session.getContext().getRealm();
                    try {
                        session.getContext().setRealm(realm);
                        user.credentialManager().updateCredential(UserCredentialModel.password(cred.getValue(), false));
                    } catch (ModelException ex) {
                        PasswordPolicyNotMetException passwordPolicyNotMetException = new PasswordPolicyNotMetException(ex.getMessage(), user.getUsername(), ex);
                        passwordPolicyNotMetException.setParameters(ex.getParameters());
                        throw passwordPolicyNotMetException;
                    } finally {
                        session.getContext().setRealm(origRealm);
                    }
                } else {
                    user.credentialManager().createCredentialThroughProvider(toModel(cred));
                }
            }
        }
    }

    public static CredentialModel toModel(CredentialRepresentation cred) {
        CredentialModel model = new CredentialModel();
        model.setCreatedDate(cred.getCreatedDate());
        model.setType(cred.getType());
        model.setUserLabel(cred.getUserLabel());
        model.setSecretData(cred.getSecretData());
        model.setCredentialData(cred.getCredentialData());
        model.setId(cred.getId());
        return model;
    }

    // Role mappings

    public static void createRoleMappings(UserRepresentation userRep, UserModel user, RealmModel realm) {
        if (userRep.getRealmRoles() != null) {
            for (String roleString : userRep.getRealmRoles()) {
                RoleModel role = realm.getRole(roleString.trim());
                if (role == null) {
                    role = realm.addRole(roleString.trim());
                }
                user.grantRole(role);
            }
        }
        if (userRep.getClientRoles() != null) {
            for (Map.Entry<String, List<String>> entry : userRep.getClientRoles().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new RuntimeException("Unable to find client role mappings for client: " + entry.getKey());
                }
                createClientRoleMappings(client, user, entry.getValue());
            }
        }
    }

    private static void createClientRoleMappings(ClientModel clientModel, UserModel user, List<String> roleNames) {
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        for (String roleName : roleNames) {
            RoleModel role = clientModel.getRole(roleName.trim());
            if (role == null) {
                role = clientModel.addRole(roleName.trim());
            }
            user.grantRole(role);

        }
    }

    public static IdentityProviderModel toModel(RealmModel realm, IdentityProviderRepresentation representation, KeycloakSession session) {
        IdentityProviderFactory providerFactory = (IdentityProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(
                IdentityProvider.class, representation.getProviderId());

        if (providerFactory == null) {
            providerFactory = (IdentityProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(
                    SocialIdentityProvider.class, representation.getProviderId());
        }

        if (providerFactory == null) {
            throw new IllegalArgumentException("Invalid identity provider id [" + representation.getProviderId() + "]");
        }

        IdentityProviderModel identityProviderModel = providerFactory.createConfig();

        identityProviderModel.setInternalId(representation.getInternalId());
        identityProviderModel.setAlias(representation.getAlias());
        identityProviderModel.setDisplayName(representation.getDisplayName());
        identityProviderModel.setProviderId(representation.getProviderId());
        identityProviderModel.setEnabled(representation.isEnabled());
        identityProviderModel.setLinkOnly(representation.isLinkOnly());
        identityProviderModel.setHideOnLogin(representation.isHideOnLogin());
        // remove the legacy hide on login attribute if present.
        String hideOnLoginAttr = representation.getConfig().remove(IdentityProviderModel.LEGACY_HIDE_ON_LOGIN_ATTR);
        if (hideOnLoginAttr != null) identityProviderModel.setHideOnLogin(Boolean.parseBoolean(hideOnLoginAttr));
        identityProviderModel.setTrustEmail(representation.isTrustEmail());
        identityProviderModel.setAuthenticateByDefault(representation.isAuthenticateByDefault());
        identityProviderModel.setStoreToken(representation.isStoreToken());
        identityProviderModel.setAddReadTokenRoleOnCreate(representation.isAddReadTokenRoleOnCreate());
        updateOrganizationBroker(representation, session);
        identityProviderModel.setOrganizationId(representation.getOrganizationId());

        // Merge config from the identity provider model in case the provider sets some default config
        Map<String, String> repConfig = removeEmptyString(representation.getConfig());
        if (repConfig != null && !repConfig.isEmpty()) {
            if (identityProviderModel.getConfig() == null) {
                identityProviderModel.setConfig(new HashMap<>());
            }
            identityProviderModel.getConfig().putAll(repConfig);
        }

        String flowAlias = representation.getFirstBrokerLoginFlowAlias();
        if (flowAlias == null || flowAlias.trim().isEmpty()) {
            identityProviderModel.setFirstBrokerLoginFlowId(null);
        } else {
            AuthenticationFlowModel flowModel = realm.getFlowByAlias(flowAlias);
            if (flowModel == null) {
                throw new ModelException("No available authentication flow with alias: " + flowAlias);
            }
            identityProviderModel.setFirstBrokerLoginFlowId(flowModel.getId());
        }

        flowAlias = representation.getPostBrokerLoginFlowAlias();
        if (flowAlias == null || flowAlias.trim().isEmpty()) {
            identityProviderModel.setPostBrokerLoginFlowId(null);
        } else {
            AuthenticationFlowModel flowModel = realm.getFlowByAlias(flowAlias);
            if (flowModel == null) {
                throw new ModelException("No available authentication flow with alias: " + flowAlias);
            }
            identityProviderModel.setPostBrokerLoginFlowId(flowModel.getId());
        }

        identityProviderModel.validate(realm);

        return identityProviderModel;
    }

    public static ProtocolMapperModel toModel(ProtocolMapperRepresentation rep) {
        ProtocolMapperModel model = new ProtocolMapperModel();
        model.setId(rep.getId());
        model.setName(rep.getName());
        model.setProtocol(rep.getProtocol());
        model.setProtocolMapper(rep.getProtocolMapper());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }

    public static IdentityProviderMapperModel toModel(IdentityProviderMapperRepresentation rep) {
        IdentityProviderMapperModel model = new IdentityProviderMapperModel();
        model.setId(rep.getId());
        model.setName(rep.getName());
        model.setIdentityProviderAlias(rep.getIdentityProviderAlias());
        model.setIdentityProviderMapper(rep.getIdentityProviderMapper());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }

    public static UserConsentModel toModel(RealmModel newRealm, UserConsentRepresentation consentRep) {
        ClientModel client = newRealm.getClientByClientId(consentRep.getClientId());
        if (client == null) {
            throw new RuntimeException("Unable to find client consent mappings for client: " + consentRep.getClientId());
        }

        UserConsentModel consentModel = new UserConsentModel(client);
        consentModel.setCreatedDate(consentRep.getCreatedDate());
        consentModel.setLastUpdatedDate(consentRep.getLastUpdatedDate());

        if (consentRep.getGrantedClientScopes() != null) {
            for (String scopeName : consentRep.getGrantedClientScopes()) {
                ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(newRealm, scopeName);
                if (clientScope == null) {
                    throw new RuntimeException("Unable to find client scope referenced in consent mappings of user. Client scope name: " + scopeName);
                }
                consentModel.addGrantedClientScope(clientScope);
            }
        }

        // Backwards compatibility. If user had consent for "offline_access" role, we treat it as he has consent for "offline_access" client scope
        if (consentRep.getGrantedRealmRoles() != null) {
            if (consentRep.getGrantedRealmRoles().contains(OAuth2Constants.OFFLINE_ACCESS)) {
                ClientScopeModel offlineScope = client.getClientScopes(false).get(OAuth2Constants.OFFLINE_ACCESS);
                if (offlineScope == null) {
                    logger.warn("Unable to find offline_access scope referenced in grantedRoles of user");
                }
                consentModel.addGrantedClientScope(offlineScope);
            }
        }

        return consentModel;
    }

    public static AuthenticationFlowModel toModel(AuthenticationFlowRepresentation rep) {
        AuthenticationFlowModel model = new AuthenticationFlowModel();
        model.setId(rep.getId());
        model.setBuiltIn(rep.isBuiltIn());
        model.setTopLevel(rep.isTopLevel());
        model.setProviderId(rep.getProviderId());
        model.setAlias(rep.getAlias());
        model.setDescription(rep.getDescription());
        return model;

    }


    public static AuthenticationExecutionModel toModel(KeycloakSession session, RealmModel realm, AuthenticationExecutionRepresentation rep) {
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        model.setId(rep.getId());
        model.setFlowId(rep.getFlowId());

        model.setAuthenticator(rep.getAuthenticator());
        if (rep.getPriority() != null) {
            model.setPriority(rep.getPriority());
        }
        model.setParentFlow(rep.getParentFlow());
        model.setAuthenticatorFlow(rep.isAuthenticatorFlow());
        model.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(rep.getRequirement()));

        if (rep.getAuthenticatorConfig() != null) {
            AuthenticatorConfigModel cfg = new DeployedConfigurationsManager(session).getAuthenticatorConfigByAlias(realm, rep.getAuthenticatorConfig());
            model.setAuthenticatorConfig(cfg.getId());
        }
        return model;
    }

    public static AuthenticatorConfigModel toModel(AuthenticatorConfigRepresentation rep) {
        AuthenticatorConfigModel model = new AuthenticatorConfigModel();
        model.setId(rep.getId());
        model.setAlias(rep.getAlias());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }

    public static ComponentModel toModel(KeycloakSession session, ComponentRepresentation rep) {
        ComponentModel model = new ComponentModel();
        model.setId(rep.getId());
        model.setParentId(rep.getParentId());
        model.setProviderType(rep.getProviderType());
        model.setProviderId(rep.getProviderId());
        model.setConfig(new MultivaluedHashMap<>());
        model.setName(rep.getName());
        model.setSubType(rep.getSubType());

        if (rep.getConfig() != null) {
            Set<String> keys = new HashSet<>(rep.getConfig().keySet());
            for (String k : keys) {
                List<String> values = rep.getConfig().get(k);
                if (values != null) {
                    ListIterator<String> itr = values.listIterator();
                    while (itr.hasNext()) {
                        String v = itr.next();
                        if (v == null || v.trim().isEmpty()) {
                            itr.remove();
                        }
                    }

                    if (!values.isEmpty()) {
                        model.getConfig().put(k, values);
                    }
                }
            }
        }

        return model;
    }

    public static void updateComponent(KeycloakSession session, ComponentRepresentation rep, ComponentModel component, boolean internal) {
        if (rep.getName() != null) {
            component.setName(rep.getName());
        }

        if (rep.getParentId() != null) {
            component.setParentId(rep.getParentId());
        }

        if (rep.getProviderType() != null) {
            component.setProviderType(rep.getProviderType());
        }

        if (rep.getProviderId() != null) {
            component.setProviderId(rep.getProviderId());
        }

        if (rep.getSubType() != null) {
            component.setSubType(rep.getSubType());
        }

        Map<String, ProviderConfigProperty> providerConfiguration = null;
        if (!internal) {
            providerConfiguration = ComponentUtil.getComponentConfigProperties(session, component);
        }

        if (rep.getConfig() != null) {
            Set<String> keys = new HashSet<>(rep.getConfig().keySet());
            for (String k : keys) {
                if (!internal && !providerConfiguration.containsKey(k)) {
                    continue;
                }

                List<String> values = rep.getConfig().get(k);
                if (values == null || values.isEmpty() || values.get(0) == null || values.get(0).trim().isEmpty()) {
                    component.getConfig().remove(k);
                } else {
                    ListIterator<String> itr = values.listIterator();
                    while (itr.hasNext()) {
                        String v = itr.next();
                        if (v == null || v.trim().isEmpty() || v.equals(ComponentRepresentation.SECRET_VALUE)) {
                            itr.remove();
                        }
                    }

                    if (!values.isEmpty()) {
                        component.getConfig().put(k, values);
                    }
                }
            }
        }
    }

    public static void importAuthorizationSettings(ClientRepresentation clientRepresentation, ClientModel client, KeycloakSession session) {
        if (Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION) && Boolean.TRUE.equals(clientRepresentation.getAuthorizationServicesEnabled())) {
            AuthorizationProviderFactory authorizationFactory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(AuthorizationProvider.class);
            AuthorizationProvider authorization = authorizationFactory.create(session, client.getRealm());

            client.setServiceAccountsEnabled(true);
            client.setBearerOnly(false);
            client.setPublicClient(false);

            ResourceServerRepresentation rep = clientRepresentation.getAuthorizationSettings();

            if (rep == null) {
                rep = new ResourceServerRepresentation();
            }

            rep.setClientId(client.getId());

            toModel(rep, authorization, client);
        }
    }

    public static ResourceServer toModel(ResourceServerRepresentation rep, AuthorizationProvider authorization, ClientModel client) {
        ResourceServerStore resourceServerStore = authorization.getStoreFactory().getResourceServerStore();
        ResourceServer resourceServer;
        ResourceServer existing = resourceServerStore.findByClient(client);

        if (existing == null) {
            resourceServer = resourceServerStore.create(client);
            resourceServer.setAllowRemoteResourceManagement(true);
            resourceServer.setPolicyEnforcementMode(PolicyEnforcementMode.ENFORCING);
        } else {
            resourceServer = existing;
        }

        resourceServer.setPolicyEnforcementMode(rep.getPolicyEnforcementMode());
        resourceServer.setAllowRemoteResourceManagement(rep.isAllowRemoteResourceManagement());

        DecisionStrategy decisionStrategy = rep.getDecisionStrategy();

        if (decisionStrategy == null) {
            decisionStrategy = DecisionStrategy.UNANIMOUS;
        }

        resourceServer.setDecisionStrategy(decisionStrategy);

        for (ScopeRepresentation scope : rep.getScopes()) {
            toModel(scope, resourceServer, authorization);
        }

        KeycloakSession session = authorization.getKeycloakSession();
        RealmModel realm = authorization.getRealm();

        for (ResourceRepresentation resource : rep.getResources()) {
            ResourceOwnerRepresentation owner = resource.getOwner();

            if (owner == null) {
                owner = new ResourceOwnerRepresentation();
                owner.setId(resourceServer.getClientId());
                resource.setOwner(owner);
            } else if (owner.getName() != null) {
                UserModel user = session.users().getUserByUsername(realm, owner.getName());

                if (user != null) {
                    owner.setId(user.getId());
                }
            }

            toModel(resource, resourceServer, authorization);
        }

        importPolicies(authorization, resourceServer, rep.getPolicies(), null);

        return resourceServer;
    }

    private static Policy importPolicies(AuthorizationProvider authorization, ResourceServer resourceServer, List<PolicyRepresentation> policiesToImport, String parentPolicyName) {
        StoreFactory storeFactory = authorization.getStoreFactory();

        for (PolicyRepresentation policyRepresentation : policiesToImport) {
            if (parentPolicyName != null && !parentPolicyName.equals(policyRepresentation.getName())) {
                continue;
            }

            Map<String, String> config = policyRepresentation.getConfig();
            String applyPolicies = config.get("applyPolicies");

            if (applyPolicies != null && !applyPolicies.isEmpty()) {
                PolicyStore policyStore = storeFactory.getPolicyStore();
                try {
                    List<String> policies = JsonSerialization.readValue(applyPolicies, List.class);
                    Set<String> policyIds = new HashSet<>();

                    for (String policyName : policies) {
                        Policy policy = policyStore.findByName(resourceServer, policyName);

                        if (policy == null) {
                            policy = policyStore.findById(resourceServer, policyName);
                        }

                        if (policy == null) {
                            policy = importPolicies(authorization, resourceServer, policiesToImport, policyName);
                            if (policy == null) {
                                throw new RuntimeException("Policy with name [" + policyName + "] not defined.");
                            }
                        }

                        policyIds.add(policy.getId());
                    }

                    config.put("applyPolicies", JsonSerialization.writeValueAsString(policyIds));
                } catch (Exception e) {
                    throw new RuntimeException("Error while importing policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            PolicyStore policyStore = storeFactory.getPolicyStore();
            Policy policy = policyStore.findById(resourceServer, policyRepresentation.getId());

            if (policy == null) {
                policy = policyStore.findByName(resourceServer, policyRepresentation.getName());
            }

            if (policy == null) {
                policy = policyStore.create(resourceServer, policyRepresentation);
            } else {
                policy = toModel(policyRepresentation, authorization, policy);
            }

            if (parentPolicyName != null && parentPolicyName.equals(policyRepresentation.getName())) {
                return policy;
            }
        }

        return null;
    }

    public static Policy toModel(AbstractPolicyRepresentation representation, AuthorizationProvider authorization, Policy model) {
        model.setName(representation.getName());
        model.setDescription(representation.getDescription());
        model.setDecisionStrategy(representation.getDecisionStrategy());
        model.setLogic(representation.getLogic());
        model.setResourceType(representation.getResourceType());

        Set resources = representation.getResources();
        Set scopes = representation.getScopes();
        Set policies = representation.getPolicies();

        if (representation instanceof PolicyRepresentation) {
            PolicyRepresentation policy = PolicyRepresentation.class.cast(representation);

            if (resources == null) {
                String resourcesConfig = policy.getConfig().get("resources");

                if (resourcesConfig != null) {
                    try {
                        resources = JsonSerialization.readValue(resourcesConfig, Set.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                representation.setResources(resources);
            }

            if (scopes == null) {
                String scopesConfig = policy.getConfig().get("scopes");

                if (scopesConfig != null) {
                    try {
                        scopes = JsonSerialization.readValue(scopesConfig, Set.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                representation.setScopes(scopes);
            }

            if (policies == null) {
                String policiesConfig = policy.getConfig().get("applyPolicies");

                if (policiesConfig != null) {
                    try {
                        policies = JsonSerialization.readValue(policiesConfig, Set.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                representation.setPolicies(policies);
            }

            model.setConfig(policy.getConfig());
        }

        StoreFactory storeFactory = authorization.getStoreFactory();

        updateResources(representation, model, authorization);
        updateScopes(representation, model, storeFactory);
        updateAssociatedPolicies(representation, model, storeFactory);

        PolicyProviderFactory provider = authorization.getProviderFactory(model.getType());

        if (provider == null) {
            throw new RuntimeException("Couldn't find policy provider with type [" + model.getType() + "]");
        }

        if (representation instanceof PolicyRepresentation) {
            provider.onImport(model, PolicyRepresentation.class.cast(representation), authorization);
        } else if (representation.getId() == null) {
            provider.onCreate(model, representation, authorization);
        } else {
            provider.onUpdate(model, representation, authorization);
        }


        representation.setId(model.getId());

        return model;
    }

    private static void updateScopes(AbstractPolicyRepresentation representation, Policy policy, StoreFactory storeFactory) {
        Set<String> scopeIds = representation.getScopes();

        if (scopeIds == null) {
            return;
        }

        if (scopeIds.isEmpty()) {
            for (Scope scope : new HashSet<Scope>(policy.getScopes())) {
                policy.removeScope(scope);
            }
            return;
        }
        ResourceServer resourceServer = policy.getResourceServer();
        for (String scopeId : scopeIds) {
            boolean hasScope = false;

            for (Scope scopeModel : new HashSet<Scope>(policy.getScopes())) {
                if (scopeModel.getId().equals(scopeId) || scopeModel.getName().equals(scopeId)) {
                    hasScope = true;
                }
            }
            if (!hasScope) {
                Scope scope = storeFactory.getScopeStore().findById(resourceServer, scopeId);

                if (scope == null) {
                    scope = storeFactory.getScopeStore().findByName(resourceServer, scopeId);
                    if (scope == null) {
                        throw new RuntimeException("Scope with id or name [" + scopeId + "] does not exist");
                    }
                }

                policy.addScope(scope);
            }
        }

        for (Scope scopeModel : new HashSet<Scope>(policy.getScopes())) {
            boolean hasScope = false;

            for (String scopeId : scopeIds) {
                if (scopeModel.getId().equals(scopeId) || scopeModel.getName().equals(scopeId)) {
                    hasScope = true;
                }
            }
            if (!hasScope) {
                policy.removeScope(scopeModel);
            }
        }

        policy.removeConfig("scopes");
    }

    private static void updateAssociatedPolicies(AbstractPolicyRepresentation representation, Policy policy, StoreFactory storeFactory) {
        ResourceServer resourceServer = policy.getResourceServer();
        Set<String> policyIds = representation.getPolicies();

        if (policyIds == null) {
            return;
        }

        if (policyIds.isEmpty()) {
            for (Policy associated: new HashSet<Policy>(policy.getAssociatedPolicies())) {
                policy.removeAssociatedPolicy(associated);
            }
            return;
        }

        PolicyStore policyStore = storeFactory.getPolicyStore();

        for (String policyId : policyIds) {
            boolean hasPolicy = false;

            for (Policy policyModel : new HashSet<Policy>(policy.getAssociatedPolicies())) {
                if (policyModel.getId().equals(policyId) || policyModel.getName().equals(policyId)) {
                    hasPolicy = true;
                }
            }

            if (!hasPolicy) {
                Policy associatedPolicy = policyStore.findById(resourceServer, policyId);

                if (associatedPolicy == null) {
                    associatedPolicy = policyStore.findByName(resourceServer, policyId);
                    if (associatedPolicy == null) {
                        throw new RuntimeException("Policy with id or name [" + policyId + "] does not exist");
                    }
                }

                policy.addAssociatedPolicy(associatedPolicy);
            }
        }

        for (Policy policyModel : new HashSet<Policy>(policy.getAssociatedPolicies())) {
            boolean hasPolicy = false;

            for (String policyId : policyIds) {
                if (policyModel.getId().equals(policyId) || policyModel.getName().equals(policyId)) {
                    hasPolicy = true;
                }
            }
            if (!hasPolicy) {
                policy.removeAssociatedPolicy(policyModel);
            }
        }

        policy.removeConfig("applyPolicies");
    }

    private static void updateResources(AbstractPolicyRepresentation representation, Policy policy, AuthorizationProvider authorization) {
        Set<String> resourceIds = representation.getResources();

        if (resourceIds == null) {
            return;
        }

        StoreFactory storeFactory = authorization.getStoreFactory();
        KeycloakSession session = authorization.getKeycloakSession();
        ResourceServer resourceServer = policy.getResourceServer();

        if (resourceIds.isEmpty()) {
            for (Resource resource : new HashSet<>(policy.getResources())) {
                AdminPermissionsSchema.SCHEMA.removeResource(resource, policy, authorization);
            }
        }

        resourceIds = resourceIds.stream().map(id -> {
            Resource resource = AdminPermissionsSchema.SCHEMA.getOrCreateResource(session, resourceServer, policy.getType(), policy.getResourceType(), id);

            if (resource == null) {
                return id;
            }

            return resource.getId();
        }).collect(Collectors.toSet());

        for (String resourceId : resourceIds) {
            boolean hasResource = false;
            for (Resource resourceModel : new HashSet<>(policy.getResources())) {
                if (resourceModel.getId().equals(resourceId) || resourceModel.getName().equals(resourceId)) {
                    hasResource = true;
                }
            }
            if (!hasResource && !"".equals(resourceId)) {
                Resource resource = storeFactory.getResourceStore().findById(resourceServer, resourceId);

                if (resource == null) {
                    resource = storeFactory.getResourceStore().findByName(resourceServer, resourceId);
                    if (resource == null) {
                        throw new RuntimeException("Resource with id or name [" + resourceId + "] does not exist or is not owned by the resource server");
                    }
                }

                policy.addResource(resource);
            }
        }

        for (Resource resourceModel : new HashSet<>(policy.getResources())) {
            boolean hasResource = false;

            for (String resourceId : resourceIds) {
                if (resourceModel.getId().equals(resourceId) || resourceModel.getName().equals(resourceId)) {
                    hasResource = true;
                }
            }

            if (!hasResource) {
                AdminPermissionsSchema.SCHEMA.removeResource(resourceModel, policy, authorization);
            }
        }

        AdminPermissionsSchema.SCHEMA.addUResourceTypeResource(session, resourceServer, policy, representation.getResourceType());

        policy.removeConfig("resources");
    }

    public static Resource toModel(ResourceRepresentation resource, ResourceServer resourceServer, AuthorizationProvider authorization) {
        ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();
        RealmModel realm = authorization.getRealm();
        ResourceOwnerRepresentation owner = resource.getOwner();

        if (owner == null) {
            owner = new ResourceOwnerRepresentation();
            owner.setId(resourceServer.getClientId());
        }

        String ownerId = owner.getId();

        if (ownerId == null) {
            ownerId = resourceServer.getClientId();
        }

        if (!resourceServer.getClientId().equals(ownerId)) {
            KeycloakSession keycloakSession = authorization.getKeycloakSession();
            UserProvider users = keycloakSession.users();
            UserModel ownerModel = users.getUserById(realm, ownerId);

            if (ownerModel == null) {
                ownerModel = users.getUserByUsername(realm, ownerId);
            }

            if (ownerModel == null) {
                throw new RuntimeException("Owner must be a valid username or user identifier. If the resource server, the client id or null.");
            }

            ownerId = ownerModel.getId();
        }

        Resource existing;

        if (resource.getId() != null) {
            existing = resourceStore.findById(resourceServer, resource.getId());
        } else {
            existing = resourceStore.findByName(resourceServer, resource.getName(), ownerId);
        }

        if (existing != null) {
            existing.setName(resource.getName());
            existing.setDisplayName(resource.getDisplayName());
            existing.setType(resource.getType());
            existing.updateUris(resource.getUris());
            existing.setIconUri(resource.getIconUri());
            existing.setOwnerManagedAccess(Boolean.TRUE.equals(resource.getOwnerManagedAccess()));
            existing.updateScopes(resource.getScopes().stream()
                    .map((ScopeRepresentation scope) -> toModel(scope, resourceServer, authorization, false))
                    .collect(Collectors.toSet()));
            Map<String, List<String>> attributes = resource.getAttributes();

            if (attributes != null) {

                for (String name : existing.getAttributes().keySet()) {
                    if (attributes.containsKey(name)) {
                        existing.setAttribute(name, attributes.get(name));
                        attributes.remove(name);
                    } else {
                        existing.removeAttribute(name);
                    }
                }

                for (var entry : attributes.entrySet()) {
                    existing.setAttribute(entry.getKey(), entry.getValue());
                }
            }

            return existing;
        }

        Resource model = resourceStore.create(resourceServer, resource.getId(), resource.getName(), ownerId);

        model.setDisplayName(resource.getDisplayName());
        model.setType(resource.getType());
        model.updateUris(resource.getUris());
        model.setIconUri(resource.getIconUri());
        model.setOwnerManagedAccess(Boolean.TRUE.equals(resource.getOwnerManagedAccess()));

        Set<ScopeRepresentation> scopes = resource.getScopes();

        if (scopes != null) {
            model.updateScopes(scopes.stream().map(scope -> toModel(scope, resourceServer, authorization, false)).collect(Collectors.toSet()));
        }

        Map<String, List<String>> attributes = resource.getAttributes();

        if (attributes != null) {
            for (Entry<String, List<String>> entry : attributes.entrySet()) {
                model.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        resource.setId(model.getId());

        return model;
    }

    public static Scope toModel(ScopeRepresentation scope, ResourceServer resourceServer, AuthorizationProvider authorization) {
        return toModel(scope, resourceServer, authorization, true);
    }

    public static Scope toModel(ScopeRepresentation scope, ResourceServer resourceServer, AuthorizationProvider authorization, boolean updateIfExists) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        ScopeStore scopeStore = storeFactory.getScopeStore();
        Scope existing;

        if (scope.getId() != null) {
            existing = scopeStore.findById(resourceServer, scope.getId());
        } else {
            existing = scopeStore.findByName(resourceServer, scope.getName());
        }

        if (existing != null) {
            if (updateIfExists) {
                existing.setName(scope.getName());
                existing.setDisplayName(scope.getDisplayName());
                existing.setIconUri(scope.getIconUri());
            }
            return existing;
        }

        Scope model = scopeStore.create(resourceServer, scope.getId(), scope.getName());

        model.setDisplayName(scope.getDisplayName());
        model.setIconUri(scope.getIconUri());

        scope.setId(model.getId());

        return model;
    }

    public static PermissionTicket toModel(PermissionTicketRepresentation representation, ResourceServer resourceServer, AuthorizationProvider authorization) {
        PermissionTicketStore ticketStore = authorization.getStoreFactory().getPermissionTicketStore();
        PermissionTicket ticket = ticketStore.findById(resourceServer, representation.getId());
        boolean granted = representation.isGranted();

        if (granted && !ticket.isGranted()) {
            ticket.setGrantedTimestamp(System.currentTimeMillis());
        } else if (!granted) {
            ticketStore.delete(ticket.getId());
        }

        return ticket;
    }

    public static Map<String, String> removeEmptyString(Map<String, String> map) {
        if (map == null) {
            return null;
        }

        Map<String, String> m = new HashMap<>(map);
        for (Iterator<Map.Entry<String, String>> itr = m.entrySet().iterator(); itr.hasNext(); ) {
            Map.Entry<String, String> e = itr.next();
            if (e.getValue() == null || e.getValue().equals("")) {
                itr.remove();
            }
        }
        return m;
    }

    public static ResourceServer createResourceServer(ClientModel client, KeycloakSession session, boolean addDefaultRoles) {
        if ((client.isBearerOnly() || client.isPublicClient())
                && !(client.getClientId().equals(Config.getAdminRealm() + "-realm") || client.getClientId().equals(Constants.REALM_MANAGEMENT_CLIENT_ID))) {
            throw new RuntimeException("Only confidential clients are allowed to set authorization settings");
        }
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        UserModel serviceAccount = session.users().getServiceAccount(client);

        if (serviceAccount == null) {
            client.setServiceAccountsEnabled(true);
        }

        if (addDefaultRoles) {
            RoleModel umaProtectionRole = client.getRole(Constants.AUTHZ_UMA_PROTECTION);

            if (umaProtectionRole == null) {
                umaProtectionRole = client.addRole(Constants.AUTHZ_UMA_PROTECTION);
            }

            if (serviceAccount != null) {
                serviceAccount.grantRole(umaProtectionRole);
            }
        }

        ResourceServerRepresentation representation = new ResourceServerRepresentation();

        representation.setAllowRemoteResourceManagement(true);
        representation.setClientId(client.getId());

        return toModel(representation, authorization, client);
    }

    private static void updateOrganizationBroker(IdentityProviderRepresentation representation, KeycloakSession session) {
        if (!Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
            return;
        }

        IdentityProviderModel existing = Optional.ofNullable(session.identityProviders().getByAlias(representation.getAlias()))
                        .orElse(session.identityProviders().getById(representation.getInternalId()));
        String repOrgId = representation.getOrganizationId() != null ? representation.getOrganizationId() :
                representation.getConfig().remove(OrganizationModel.ORGANIZATION_ATTRIBUTE);
        String orgId = existing != null ? existing.getOrganizationId() : repOrgId;

        if (orgId != null) {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = provider.getById(orgId);

            if (org == null || (repOrgId != null && provider.getById(repOrgId) == null)) {
                throw new IllegalArgumentException("Organization associated with broker does not exist");
            }

            String domain = representation.getConfig().get(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);

            if (StringUtil.isBlank(domain)) {
                representation.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
            } else if (!ANY_DOMAIN.equals(domain) && org.getDomains().map(OrganizationDomainModel::getName).noneMatch(domain::equals)) {
                throw new IllegalArgumentException("Domain does not match any domain from the organization");
            }

            // make sure the link to an organization does not change
            representation.setOrganizationId(orgId);
        }
    }

    public static OrganizationModel toModel(OrganizationRepresentation rep, OrganizationModel model) {
        if (rep == null) {
            return null;
        }

        model.setName(rep.getName());
        model.setAlias(rep.getAlias());
        model.setEnabled(rep.isEnabled());
        model.setRedirectUrl(rep.getRedirectUrl());
        model.setDescription(rep.getDescription());
        model.setAttributes(rep.getAttributes());
        model.setDomains(ofNullable(rep.getDomains()).orElse(Set.of()).stream()
                .filter(Objects::nonNull)
                .filter(domain -> StringUtil.isNotBlank(domain.getName()))
                .map(RepresentationToModel::toModel)
                .collect(Collectors.toSet()));

        return model;
    }

    public static OrganizationDomainModel toModel(OrganizationDomainRepresentation domainRepresentation) {
        return new OrganizationDomainModel(domainRepresentation.getName(), domainRepresentation.isVerified());
    }
}
