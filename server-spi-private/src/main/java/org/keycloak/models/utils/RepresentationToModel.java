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

package org.keycloak.models.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.keys.KeyProvider;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.migrators.MigrationUtils;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.ScopeContainerModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ClaimRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.representations.idm.SocialLinkRepresentation;
import org.keycloak.representations.idm.UserConsentRepresentation;
import org.keycloak.representations.idm.UserFederationMapperRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.util.JsonSerialization;

public class RepresentationToModel {

    private static Logger logger = Logger.getLogger(RepresentationToModel.class);

    public static OTPPolicy toPolicy(RealmRepresentation rep) {
        OTPPolicy policy = new OTPPolicy();
        if (rep.getOtpPolicyType() != null) policy.setType(rep.getOtpPolicyType());
        if (rep.getOtpPolicyLookAheadWindow() != null) policy.setLookAheadWindow(rep.getOtpPolicyLookAheadWindow());
        if (rep.getOtpPolicyInitialCounter() != null) policy.setInitialCounter(rep.getOtpPolicyInitialCounter());
        if (rep.getOtpPolicyAlgorithm() != null) policy.setAlgorithm(rep.getOtpPolicyAlgorithm());
        if (rep.getOtpPolicyDigits() != null) policy.setDigits(rep.getOtpPolicyDigits());
        if (rep.getOtpPolicyPeriod() != null) policy.setPeriod(rep.getOtpPolicyPeriod());
        return policy;

    }

    public static void importRealm(KeycloakSession session, RealmRepresentation rep, RealmModel newRealm, boolean skipUserDependent) {
        convertDeprecatedSocialProviders(rep);
        convertDeprecatedApplications(session, rep);

        newRealm.setName(rep.getRealm());
        if (rep.getDisplayName() != null) newRealm.setDisplayName(rep.getDisplayName());
        if (rep.getDisplayNameHtml() != null) newRealm.setDisplayNameHtml(rep.getDisplayNameHtml());
        if (rep.isEnabled() != null) newRealm.setEnabled(rep.isEnabled());
        if (rep.isBruteForceProtected() != null) newRealm.setBruteForceProtected(rep.isBruteForceProtected());
        if (rep.isPermanentLockout() != null) newRealm.setPermanentLockout(rep.isPermanentLockout());
        if (rep.getMaxFailureWaitSeconds() != null) newRealm.setMaxFailureWaitSeconds(rep.getMaxFailureWaitSeconds());
        if (rep.getMinimumQuickLoginWaitSeconds() != null)
            newRealm.setMinimumQuickLoginWaitSeconds(rep.getMinimumQuickLoginWaitSeconds());
        if (rep.getWaitIncrementSeconds() != null) newRealm.setWaitIncrementSeconds(rep.getWaitIncrementSeconds());
        if (rep.getQuickLoginCheckMilliSeconds() != null)
            newRealm.setQuickLoginCheckMilliSeconds(rep.getQuickLoginCheckMilliSeconds());
        if (rep.getMaxDeltaTimeSeconds() != null) newRealm.setMaxDeltaTimeSeconds(rep.getMaxDeltaTimeSeconds());
        if (rep.getFailureFactor() != null) newRealm.setFailureFactor(rep.getFailureFactor());
        if (rep.isEventsEnabled() != null) newRealm.setEventsEnabled(rep.isEventsEnabled());
        if (rep.getEnabledEventTypes() != null)
            newRealm.setEnabledEventTypes(new HashSet<>(rep.getEnabledEventTypes()));
        if (rep.getEventsExpiration() != null) newRealm.setEventsExpiration(rep.getEventsExpiration());
        if (rep.getEventsListeners() != null) newRealm.setEventsListeners(new HashSet<>(rep.getEventsListeners()));
        if (rep.isAdminEventsEnabled() != null) newRealm.setAdminEventsEnabled(rep.isAdminEventsEnabled());
        if (rep.isAdminEventsDetailsEnabled() != null)
            newRealm.setAdminEventsDetailsEnabled(rep.isAdminEventsDetailsEnabled());

        if (rep.getNotBefore() != null) newRealm.setNotBefore(rep.getNotBefore());

        if (rep.getRevokeRefreshToken() != null) newRealm.setRevokeRefreshToken(rep.getRevokeRefreshToken());
        else newRealm.setRevokeRefreshToken(false);

        if (rep.getAccessTokenLifespan() != null) newRealm.setAccessTokenLifespan(rep.getAccessTokenLifespan());
        else newRealm.setAccessTokenLifespan(300);

        if (rep.getAccessTokenLifespanForImplicitFlow() != null)
            newRealm.setAccessTokenLifespanForImplicitFlow(rep.getAccessTokenLifespanForImplicitFlow());
        else
            newRealm.setAccessTokenLifespanForImplicitFlow(Constants.DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT);

        if (rep.getSsoSessionIdleTimeout() != null) newRealm.setSsoSessionIdleTimeout(rep.getSsoSessionIdleTimeout());
        else newRealm.setSsoSessionIdleTimeout(1800);
        if (rep.getSsoSessionMaxLifespan() != null) newRealm.setSsoSessionMaxLifespan(rep.getSsoSessionMaxLifespan());
        else newRealm.setSsoSessionMaxLifespan(36000);
        if (rep.getOfflineSessionIdleTimeout() != null)
            newRealm.setOfflineSessionIdleTimeout(rep.getOfflineSessionIdleTimeout());
        else newRealm.setOfflineSessionIdleTimeout(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT);

        if (rep.getAccessCodeLifespan() != null) newRealm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        else newRealm.setAccessCodeLifespan(60);

        if (rep.getAccessCodeLifespanUserAction() != null)
            newRealm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        else newRealm.setAccessCodeLifespanUserAction(300);

        if (rep.getAccessCodeLifespanLogin() != null)
            newRealm.setAccessCodeLifespanLogin(rep.getAccessCodeLifespanLogin());
        else newRealm.setAccessCodeLifespanLogin(1800);

        if (rep.getActionTokenGeneratedByAdminLifespan() != null)
            newRealm.setActionTokenGeneratedByAdminLifespan(rep.getActionTokenGeneratedByAdminLifespan());
        else newRealm.setActionTokenGeneratedByAdminLifespan(12 * 60 * 60);

        if (rep.getActionTokenGeneratedByUserLifespan() != null)
            newRealm.setActionTokenGeneratedByUserLifespan(rep.getActionTokenGeneratedByUserLifespan());
        else newRealm.setActionTokenGeneratedByUserLifespan(newRealm.getAccessCodeLifespanUserAction());

        if (rep.getSslRequired() != null)
            newRealm.setSslRequired(SslRequired.valueOf(rep.getSslRequired().toUpperCase()));
        if (rep.isRegistrationAllowed() != null) newRealm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isRegistrationEmailAsUsername() != null)
            newRealm.setRegistrationEmailAsUsername(rep.isRegistrationEmailAsUsername());
        if (rep.isRememberMe() != null) newRealm.setRememberMe(rep.isRememberMe());
        if (rep.isVerifyEmail() != null) newRealm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isLoginWithEmailAllowed() != null) newRealm.setLoginWithEmailAllowed(rep.isLoginWithEmailAllowed());
        if (rep.isDuplicateEmailsAllowed() != null) newRealm.setDuplicateEmailsAllowed(rep.isDuplicateEmailsAllowed());
        if (rep.isResetPasswordAllowed() != null) newRealm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.isEditUsernameAllowed() != null) newRealm.setEditUsernameAllowed(rep.isEditUsernameAllowed());
        if (rep.getLoginTheme() != null) newRealm.setLoginTheme(rep.getLoginTheme());
        if (rep.getAccountTheme() != null) newRealm.setAccountTheme(rep.getAccountTheme());
        if (rep.getAdminTheme() != null) newRealm.setAdminTheme(rep.getAdminTheme());
        if (rep.getEmailTheme() != null) newRealm.setEmailTheme(rep.getEmailTheme());

        // todo remove this stuff as its all deprecated
        if (rep.getRequiredCredentials() != null) {
            for (String requiredCred : rep.getRequiredCredentials()) {
                newRealm.addRequiredCredential(requiredCred);
            }
        } else {
            newRealm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        }

        if (rep.getPasswordPolicy() != null)
            newRealm.setPasswordPolicy(PasswordPolicy.parse(session, rep.getPasswordPolicy()));
        if (rep.getOtpPolicyType() != null) newRealm.setOTPPolicy(toPolicy(rep));
        else newRealm.setOTPPolicy(OTPPolicy.DEFAULT_POLICY);

        importAuthenticationFlows(newRealm, rep);
        if (rep.getRequiredActions() != null) {
            for (RequiredActionProviderRepresentation action : rep.getRequiredActions()) {
                RequiredActionProviderModel model = toModel(action);

                MigrationUtils.updateOTPRequiredAction(model);

                newRealm.addRequiredActionProvider(model);
            }
        } else {
            DefaultRequiredActions.addActions(newRealm);
        }

        importIdentityProviders(rep, newRealm);
        importIdentityProviderMappers(rep, newRealm);

        if (rep.getClientTemplates() != null) {
            createClientTemplates(session, rep, newRealm);
        }

        if (rep.getClients() != null) {
            createClients(session, rep, newRealm);
        }

        importRoles(rep.getRoles(), newRealm);

        // Setup realm default roles
        if (rep.getDefaultRoles() != null) {
            for (String roleString : rep.getDefaultRoles()) {
                newRealm.addDefaultRole(roleString.trim());
            }
        }
        // Setup client default roles
        if (rep.getClients() != null) {
            for (ClientRepresentation resourceRep : rep.getClients()) {
                if (resourceRep.getDefaultRoles() != null) {
                    ClientModel clientModel = newRealm.getClientByClientId(resourceRep.getClientId());
                    clientModel.updateDefaultRoles(resourceRep.getDefaultRoles());
                }
            }
        }

        // Now that all possible roles and clients are created, create scope mappings

        //Map<String, ClientModel> appMap = newRealm.getClientNameMap();

        if (rep.getClientScopeMappings() != null) {

            for (Map.Entry<String, List<ScopeMappingRepresentation>> entry : rep.getClientScopeMappings().entrySet()) {
                ClientModel app = newRealm.getClientByClientId(entry.getKey());
                if (app == null) {
                    throw new RuntimeException("Unable to find client role mappings for client: " + entry.getKey());
                }
                createClientScopeMappings(newRealm, app, entry.getValue());
            }
        }

        if (rep.getScopeMappings() != null) {
            for (ScopeMappingRepresentation scope : rep.getScopeMappings()) {
                ScopeContainerModel scopeContainer = getScopeContainerHavingScope(newRealm, scope);

                for (String roleString : scope.getRoles()) {
                    RoleModel role = newRealm.getRole(roleString.trim());
                    if (role == null) {
                        role = newRealm.addRole(roleString.trim());
                    }
                    scopeContainer.addScopeMapping(role);
                }

            }
        }

        if (rep.getSmtpServer() != null) {
            newRealm.setSmtpConfig(new HashMap(rep.getSmtpServer()));
        }

        if (rep.getBrowserSecurityHeaders() != null) {
            newRealm.setBrowserSecurityHeaders(rep.getBrowserSecurityHeaders());
        } else {
            newRealm.setBrowserSecurityHeaders(BrowserSecurityHeaders.defaultHeaders);
        }

        if (rep.getComponents() != null) {
            MultivaluedHashMap<String, ComponentExportRepresentation> components = rep.getComponents();
            String parentId = newRealm.getId();
            importComponents(newRealm, components, parentId);
        }
        importUserFederationProvidersAndMappers(session, rep, newRealm);


        if (rep.getGroups() != null) {
            importGroups(newRealm, rep);
            if (rep.getDefaultGroups() != null) {
                for (String path : rep.getDefaultGroups()) {
                    GroupModel found = KeycloakModelUtils.findGroupByPath(newRealm, path);
                    if (found == null) throw new RuntimeException("default group in realm rep doesn't exist: " + path);
                    newRealm.addDefaultGroup(found);
                }
            }
        }


        // create users and their role mappings and social mappings

        if (rep.getUsers() != null) {
            for (UserRepresentation userRep : rep.getUsers()) {
                UserModel user = createUser(session, newRealm, userRep);
            }
        }

        if (rep.getFederatedUsers() != null) {
            for (UserRepresentation userRep : rep.getFederatedUsers()) {
                importFederatedUser(session, newRealm, userRep);

            }
        }

        if (!skipUserDependent) {
            importRealmAuthorizationSettings(rep, newRealm, session);
        }

        if (rep.isInternationalizationEnabled() != null) {
            newRealm.setInternationalizationEnabled(rep.isInternationalizationEnabled());
        }
        if (rep.getSupportedLocales() != null) {
            newRealm.setSupportedLocales(new HashSet<String>(rep.getSupportedLocales()));
        }
        if (rep.getDefaultLocale() != null) {
            newRealm.setDefaultLocale(rep.getDefaultLocale());
        }

        // import attributes

        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> attr : rep.getAttributes().entrySet()) {
                newRealm.setAttribute(attr.getKey(), attr.getValue());
            }
        }

        if (newRealm.getComponents(newRealm.getId(), KeyProvider.class.getName()).isEmpty()) {
            if (rep.getPrivateKey() != null) {
                DefaultKeyProviders.createProviders(newRealm, rep.getPrivateKey(), rep.getCertificate());
            } else {
                DefaultKeyProviders.createProviders(newRealm);
            }
        }
    }

    public static void importUserFederationProvidersAndMappers(KeycloakSession session, RealmRepresentation rep, RealmModel newRealm) {
        // providers to convert to component model
        Set<String> convertSet = new HashSet<>();
        convertSet.add(LDAPConstants.LDAP_PROVIDER);
        convertSet.add("kerberos");
        Map<String, String> mapperConvertSet = new HashMap<>();
        mapperConvertSet.put(LDAPConstants.LDAP_PROVIDER, "org.keycloak.storage.ldap.mappers.LDAPStorageMapper");


        Map<String, ComponentModel> userStorageModels = new HashMap<>();

        if (rep.getUserFederationProviders() != null) {
            for (UserFederationProviderRepresentation fedRep : rep.getUserFederationProviders()) {
                if (convertSet.contains(fedRep.getProviderName())) {
                    ComponentModel component = convertFedProviderToComponent(newRealm.getId(), fedRep);
                    userStorageModels.put(fedRep.getDisplayName(), newRealm.importComponentModel(component));
                }
            }
        }

        // This is for case, when you have hand-written JSON file with LDAP userFederationProvider, but WITHOUT any userFederationMappers configured. Default LDAP mappers need to be created in that case.
        Set<String> storageProvidersWhichShouldImportDefaultMappers = new HashSet<>(userStorageModels.keySet());

        if (rep.getUserFederationMappers() != null) {
            for (UserFederationMapperRepresentation representation : rep.getUserFederationMappers()) {
                if (userStorageModels.containsKey(representation.getFederationProviderDisplayName())) {
                    ComponentModel parent = userStorageModels.get(representation.getFederationProviderDisplayName());
                    String newMapperType = mapperConvertSet.get(parent.getProviderId());
                    ComponentModel mapper = convertFedMapperToComponent(newRealm, parent, representation, newMapperType);
                    newRealm.importComponentModel(mapper);


                    storageProvidersWhichShouldImportDefaultMappers.remove(representation.getFederationProviderDisplayName());

                }
            }
        }

        for (String providerDisplayName : storageProvidersWhichShouldImportDefaultMappers) {
            ComponentUtil.notifyCreated(session, newRealm, userStorageModels.get(providerDisplayName));
        }
    }

    protected static void importComponents(RealmModel newRealm, MultivaluedHashMap<String, ComponentExportRepresentation> components, String parentId) {
        for (Map.Entry<String, List<ComponentExportRepresentation>> entry : components.entrySet()) {
            String providerType = entry.getKey();
            for (ComponentExportRepresentation compRep : entry.getValue()) {
                ComponentModel component = new ComponentModel();
                component.setId(compRep.getId());
                component.setName(compRep.getName());
                component.setConfig(compRep.getConfig());
                component.setProviderType(providerType);
                component.setProviderId(compRep.getProviderId());
                component.setSubType(compRep.getSubType());
                component.setParentId(parentId);
                component = newRealm.importComponentModel(component);
                if (compRep.getSubComponents() != null) {
                    importComponents(newRealm, compRep.getSubComponents(), component.getId());
                }
            }
        }
    }

    public static void importRoles(RolesRepresentation realmRoles, RealmModel realm) {
        if (realmRoles == null) return;

        if (realmRoles.getRealm() != null) { // realm roles
            for (RoleRepresentation roleRep : realmRoles.getRealm()) {
                createRole(realm, roleRep);
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
                    boolean scopeParamRequired = roleRep.isScopeParamRequired() == null ? false : roleRep.isScopeParamRequired();
                    role.setScopeParamRequired(scopeParamRequired);
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

    public static void importGroups(RealmModel realm, RealmRepresentation rep) {
        List<GroupRepresentation> groups = rep.getGroups();
        if (groups == null) return;

        GroupModel parent = null;
        for (GroupRepresentation group : groups) {
            importGroup(realm, parent, group);
        }
    }

    public static void importGroup(RealmModel realm, GroupModel parent, GroupRepresentation group) {
        GroupModel newGroup = realm.createGroup(group.getId(), group.getName());
        if (group.getAttributes() != null) {
            for (Map.Entry<String, List<String>> attr : group.getAttributes().entrySet()) {
                newGroup.setAttribute(attr.getKey(), attr.getValue());
            }
        }
        realm.moveGroup(newGroup, parent);

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

    public static void importAuthenticationFlows(RealmModel newRealm, RealmRepresentation rep) {
        if (rep.getAuthenticationFlows() == null) {
            // assume this is an old version being imported
            DefaultAuthenticationFlows.migrateFlows(newRealm);
        } else {
            for (AuthenticatorConfigRepresentation configRep : rep.getAuthenticatorConfig()) {
                AuthenticatorConfigModel model = toModel(configRep);
                newRealm.addAuthenticatorConfig(model);
            }
            for (AuthenticationFlowRepresentation flowRep : rep.getAuthenticationFlows()) {
                AuthenticationFlowModel model = toModel(flowRep);
                // make sure new id is generated for new AuthenticationFlowModel instance
                model.setId(null);
                model = newRealm.addAuthenticationFlow(model);
            }
            for (AuthenticationFlowRepresentation flowRep : rep.getAuthenticationFlows()) {
                AuthenticationFlowModel model = newRealm.getFlowByAlias(flowRep.getAlias());
                for (AuthenticationExecutionExportRepresentation exeRep : flowRep.getAuthenticationExecutions()) {
                    AuthenticationExecutionModel execution = toModel(newRealm, exeRep);
                    execution.setParentFlow(model.getId());
                    newRealm.addAuthenticatorExecution(execution);
                }
            }
        }
        if (rep.getBrowserFlow() == null) {
            newRealm.setBrowserFlow(newRealm.getFlowByAlias(DefaultAuthenticationFlows.BROWSER_FLOW));
        } else {
            newRealm.setBrowserFlow(newRealm.getFlowByAlias(rep.getBrowserFlow()));
        }
        if (rep.getRegistrationFlow() == null) {
            newRealm.setRegistrationFlow(newRealm.getFlowByAlias(DefaultAuthenticationFlows.REGISTRATION_FLOW));
        } else {
            newRealm.setRegistrationFlow(newRealm.getFlowByAlias(rep.getRegistrationFlow()));
        }
        if (rep.getDirectGrantFlow() == null) {
            newRealm.setDirectGrantFlow(newRealm.getFlowByAlias(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW));
        } else {
            newRealm.setDirectGrantFlow(newRealm.getFlowByAlias(rep.getDirectGrantFlow()));
        }

        // reset credentials + client flow needs to be more defensive as they were added later (in 1.5 )
        if (rep.getResetCredentialsFlow() == null) {
            AuthenticationFlowModel resetFlow = newRealm.getFlowByAlias(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW);
            if (resetFlow == null) {
                DefaultAuthenticationFlows.resetCredentialsFlow(newRealm);
            } else {
                newRealm.setResetCredentialsFlow(resetFlow);
            }
        } else {
            newRealm.setResetCredentialsFlow(newRealm.getFlowByAlias(rep.getResetCredentialsFlow()));
        }
        if (rep.getClientAuthenticationFlow() == null) {
            AuthenticationFlowModel clientFlow = newRealm.getFlowByAlias(DefaultAuthenticationFlows.CLIENT_AUTHENTICATION_FLOW);
            if (clientFlow == null) {
                DefaultAuthenticationFlows.clientAuthFlow(newRealm);
            } else {
                newRealm.setClientAuthenticationFlow(clientFlow);
            }
        } else {
            newRealm.setClientAuthenticationFlow(newRealm.getFlowByAlias(rep.getClientAuthenticationFlow()));
        }

        // Added in 1.7
        if (newRealm.getFlowByAlias(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW) == null) {
            DefaultAuthenticationFlows.firstBrokerLoginFlow(newRealm, true);
        }

        // Added in 2.2
        String defaultProvider = null;
        if (rep.getIdentityProviders() != null) {
            for (IdentityProviderRepresentation i : rep.getIdentityProviders()) {
                if (i.isEnabled() && i.isAuthenticateByDefault()) {
                    defaultProvider = i.getProviderId();
                    break;
                }
            }
        }

        // Added in 3.2
        if (rep.getDockerAuthenticationFlow() == null) {
            AuthenticationFlowModel dockerAuthenticationFlow = newRealm.getFlowByAlias(DefaultAuthenticationFlows.DOCKER_AUTH);
            if (dockerAuthenticationFlow == null) {
                DefaultAuthenticationFlows.dockerAuthenticationFlow(newRealm);
            } else {
                newRealm.setDockerAuthenticationFlow(dockerAuthenticationFlow);
            }
        } else {
            newRealm.setDockerAuthenticationFlow(newRealm.getFlowByAlias(rep.getDockerAuthenticationFlow()));
        }

        DefaultAuthenticationFlows.addIdentityProviderAuthenticator(newRealm, defaultProvider);
    }

    private static void convertDeprecatedSocialProviders(RealmRepresentation rep) {
        if (rep.isSocial() != null && rep.isSocial() && rep.getSocialProviders() != null && !rep.getSocialProviders().isEmpty() && rep.getIdentityProviders() == null) {
            Boolean updateProfileFirstLogin = rep.isUpdateProfileOnInitialSocialLogin() != null && rep.isUpdateProfileOnInitialSocialLogin();
            if (rep.getSocialProviders() != null) {

                logger.warn("Using deprecated 'social' configuration in JSON representation. It will be removed in future versions");
                List<IdentityProviderRepresentation> identityProviders = new LinkedList<>();
                for (String k : rep.getSocialProviders().keySet()) {
                    if (k.endsWith(".key")) {
                        String providerId = k.split("\\.")[0];
                        String key = rep.getSocialProviders().get(k);
                        String secret = rep.getSocialProviders().get(k.replace(".key", ".secret"));

                        IdentityProviderRepresentation identityProvider = new IdentityProviderRepresentation();
                        identityProvider.setAlias(providerId);
                        identityProvider.setProviderId(providerId);
                        identityProvider.setEnabled(true);
                        identityProvider.setLinkOnly(false);
                        identityProvider.setUpdateProfileFirstLogin(updateProfileFirstLogin);

                        Map<String, String> config = new HashMap<>();
                        config.put("clientId", key);
                        config.put("clientSecret", secret);
                        identityProvider.setConfig(config);

                        identityProviders.add(identityProvider);
                    }
                }
                rep.setIdentityProviders(identityProviders);
            }
        }
    }

    private static void convertDeprecatedSocialProviders(UserRepresentation user) {
        if (user.getSocialLinks() != null && !user.getSocialLinks().isEmpty() && user.getFederatedIdentities() == null) {

            logger.warnf("Using deprecated 'socialLinks' configuration in JSON representation for user '%s'. It will be removed in future versions", user.getUsername());
            List<FederatedIdentityRepresentation> federatedIdentities = new LinkedList<>();
            for (SocialLinkRepresentation social : user.getSocialLinks()) {
                FederatedIdentityRepresentation federatedIdentity = new FederatedIdentityRepresentation();
                federatedIdentity.setIdentityProvider(social.getSocialProvider());
                federatedIdentity.setUserId(social.getSocialUserId());
                federatedIdentity.setUserName(social.getSocialUsername());
                federatedIdentities.add(federatedIdentity);
            }
            user.setFederatedIdentities(federatedIdentities);
        }

        user.setSocialLinks(null);
    }

    private static void convertDeprecatedApplications(KeycloakSession session, RealmRepresentation realm) {
        if (realm.getApplications() != null || realm.getOauthClients() != null) {
            if (realm.getClients() == null) {
                realm.setClients(new LinkedList<ClientRepresentation>());
            }

            List<ApplicationRepresentation> clients = new LinkedList<>();
            if (realm.getApplications() != null) {
                clients.addAll(realm.getApplications());
            }
            if (realm.getOauthClients() != null) {
                clients.addAll(realm.getOauthClients());
            }

            for (ApplicationRepresentation app : clients) {
                app.setClientId(app.getName());
                app.setName(null);

                if (app instanceof OAuthClientRepresentation) {
                    app.setConsentRequired(true);
                    app.setFullScopeAllowed(false);
                }

                if (app.getProtocolMappers() == null && app.getClaims() != null) {
                    long mask = getClaimsMask(app.getClaims());
                    List<ProtocolMapperRepresentation> convertedProtocolMappers = session.getProvider(MigrationProvider.class).getMappersForClaimMask(mask);
                    app.setProtocolMappers(convertedProtocolMappers);
                    app.setClaims(null);
                }

                realm.getClients().add(app);
            }
        }

        if (realm.getApplicationScopeMappings() != null && realm.getClientScopeMappings() == null) {
            realm.setClientScopeMappings(realm.getApplicationScopeMappings());
        }

        if (realm.getRoles() != null && realm.getRoles().getApplication() != null && realm.getRoles().getClient() == null) {
            realm.getRoles().setClient(realm.getRoles().getApplication());
        }

        if (realm.getUsers() != null) {
            for (UserRepresentation user : realm.getUsers()) {
                if (user.getApplicationRoles() != null && user.getClientRoles() == null) {
                    user.setClientRoles(user.getApplicationRoles());
                }
            }
        }

        if (realm.getRoles() != null && realm.getRoles().getRealm() != null) {
            for (RoleRepresentation role : realm.getRoles().getRealm()) {
                if (role.getComposites() != null && role.getComposites().getApplication() != null && role.getComposites().getClient() == null) {
                    role.getComposites().setClient(role.getComposites().getApplication());
                }
            }
        }

        if (realm.getRoles() != null && realm.getRoles().getClient() != null) {
            for (Map.Entry<String, List<RoleRepresentation>> clientRoles : realm.getRoles().getClient().entrySet()) {
                for (RoleRepresentation role : clientRoles.getValue()) {
                    if (role.getComposites() != null && role.getComposites().getApplication() != null && role.getComposites().getClient() == null) {
                        role.getComposites().setClient(role.getComposites().getApplication());
                    }
                }
            }
        }
    }

    public static void renameRealm(RealmModel realm, String name) {
        if (name.equals(realm.getName())) return;

        String oldName = realm.getName();

        ClientModel masterApp = realm.getMasterAdminClient();
        masterApp.setClientId(KeycloakModelUtils.getMasterRealmAdminApplicationClientId(name));
        realm.setName(name);

        ClientModel adminClient = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        if (adminClient != null) {
            if (adminClient.getBaseUrl() != null) {
                adminClient.setBaseUrl(adminClient.getBaseUrl().replace("/admin/" + oldName + "/", "/admin/" + name + "/"));
            }
            Set<String> adminRedirectUris = new HashSet<>();
            for (String r : adminClient.getRedirectUris()) {
                adminRedirectUris.add(replace(r, "/admin/" + oldName + "/", "/admin/" + name + "/"));
            }
            adminClient.setRedirectUris(adminRedirectUris);
        }

        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (accountClient != null) {
            if (accountClient.getBaseUrl() != null) {
                accountClient.setBaseUrl(accountClient.getBaseUrl().replace("/realms/" + oldName + "/", "/realms/" + name + "/"));
            }
            Set<String> accountRedirectUris = new HashSet<>();
            for (String r : accountClient.getRedirectUris()) {
                accountRedirectUris.add(replace(r, "/realms/" + oldName + "/", "/realms/" + name + "/"));
            }
            accountClient.setRedirectUris(accountRedirectUris);
        }
    }

    private static String replace(String url, String target, String replacement) {
        return url != null ? url.replace(target, replacement) : null;
    }

    public static void updateRealm(RealmRepresentation rep, RealmModel realm, KeycloakSession session) {
        if (rep.getRealm() != null) {
            renameRealm(realm, rep.getRealm());
        }

        // Import attributes first, so the stuff saved directly on representation (displayName, bruteForce etc) has bigger priority
        if (rep.getAttributes() != null) {
            Set<String> attrsToRemove = new HashSet<>(realm.getAttributes().keySet());
            attrsToRemove.removeAll(rep.getAttributes().keySet());

            for (Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                realm.setAttribute(entry.getKey(), entry.getValue());
            }

            for (String attr : attrsToRemove) {
                realm.removeAttribute(attr);
            }
        }

        if (rep.getDisplayName() != null) realm.setDisplayName(rep.getDisplayName());
        if (rep.getDisplayNameHtml() != null) realm.setDisplayNameHtml(rep.getDisplayNameHtml());
        if (rep.isEnabled() != null) realm.setEnabled(rep.isEnabled());
        if (rep.isBruteForceProtected() != null) realm.setBruteForceProtected(rep.isBruteForceProtected());
        if (rep.isPermanentLockout() != null) realm.setPermanentLockout(rep.isPermanentLockout());
        if (rep.getMaxFailureWaitSeconds() != null) realm.setMaxFailureWaitSeconds(rep.getMaxFailureWaitSeconds());
        if (rep.getMinimumQuickLoginWaitSeconds() != null)
            realm.setMinimumQuickLoginWaitSeconds(rep.getMinimumQuickLoginWaitSeconds());
        if (rep.getWaitIncrementSeconds() != null) realm.setWaitIncrementSeconds(rep.getWaitIncrementSeconds());
        if (rep.getQuickLoginCheckMilliSeconds() != null)
            realm.setQuickLoginCheckMilliSeconds(rep.getQuickLoginCheckMilliSeconds());
        if (rep.getMaxDeltaTimeSeconds() != null) realm.setMaxDeltaTimeSeconds(rep.getMaxDeltaTimeSeconds());
        if (rep.getFailureFactor() != null) realm.setFailureFactor(rep.getFailureFactor());
        if (rep.isRegistrationAllowed() != null) realm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isRegistrationEmailAsUsername() != null)
            realm.setRegistrationEmailAsUsername(rep.isRegistrationEmailAsUsername());
        if (rep.isRememberMe() != null) realm.setRememberMe(rep.isRememberMe());
        if (rep.isVerifyEmail() != null) realm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isLoginWithEmailAllowed() != null) realm.setLoginWithEmailAllowed(rep.isLoginWithEmailAllowed());
        if (rep.isDuplicateEmailsAllowed() != null) realm.setDuplicateEmailsAllowed(rep.isDuplicateEmailsAllowed());
        if (rep.isResetPasswordAllowed() != null) realm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.isEditUsernameAllowed() != null) realm.setEditUsernameAllowed(rep.isEditUsernameAllowed());
        if (rep.getSslRequired() != null) realm.setSslRequired(SslRequired.valueOf(rep.getSslRequired().toUpperCase()));
        if (rep.getAccessCodeLifespan() != null) realm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        if (rep.getAccessCodeLifespanUserAction() != null)
            realm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        if (rep.getAccessCodeLifespanLogin() != null)
            realm.setAccessCodeLifespanLogin(rep.getAccessCodeLifespanLogin());
        if (rep.getActionTokenGeneratedByAdminLifespan() != null)
            realm.setActionTokenGeneratedByAdminLifespan(rep.getActionTokenGeneratedByAdminLifespan());
        if (rep.getActionTokenGeneratedByUserLifespan() != null)
            realm.setActionTokenGeneratedByUserLifespan(rep.getActionTokenGeneratedByUserLifespan());
        if (rep.getNotBefore() != null) realm.setNotBefore(rep.getNotBefore());
        if (rep.getRevokeRefreshToken() != null) realm.setRevokeRefreshToken(rep.getRevokeRefreshToken());
        if (rep.getAccessTokenLifespan() != null) realm.setAccessTokenLifespan(rep.getAccessTokenLifespan());
        if (rep.getAccessTokenLifespanForImplicitFlow() != null)
            realm.setAccessTokenLifespanForImplicitFlow(rep.getAccessTokenLifespanForImplicitFlow());
        if (rep.getSsoSessionIdleTimeout() != null) realm.setSsoSessionIdleTimeout(rep.getSsoSessionIdleTimeout());
        if (rep.getSsoSessionMaxLifespan() != null) realm.setSsoSessionMaxLifespan(rep.getSsoSessionMaxLifespan());
        if (rep.getOfflineSessionIdleTimeout() != null)
            realm.setOfflineSessionIdleTimeout(rep.getOfflineSessionIdleTimeout());
        if (rep.getRequiredCredentials() != null) {
            realm.updateRequiredCredentials(rep.getRequiredCredentials());
        }
        if (rep.getLoginTheme() != null) realm.setLoginTheme(rep.getLoginTheme());
        if (rep.getAccountTheme() != null) realm.setAccountTheme(rep.getAccountTheme());
        if (rep.getAdminTheme() != null) realm.setAdminTheme(rep.getAdminTheme());
        if (rep.getEmailTheme() != null) realm.setEmailTheme(rep.getEmailTheme());

        if (rep.isEventsEnabled() != null) realm.setEventsEnabled(rep.isEventsEnabled());
        if (rep.getEventsExpiration() != null) realm.setEventsExpiration(rep.getEventsExpiration());
        if (rep.getEventsListeners() != null) realm.setEventsListeners(new HashSet<>(rep.getEventsListeners()));
        if (rep.getEnabledEventTypes() != null) realm.setEnabledEventTypes(new HashSet<>(rep.getEnabledEventTypes()));

        if (rep.isAdminEventsEnabled() != null) realm.setAdminEventsEnabled(rep.isAdminEventsEnabled());
        if (rep.isAdminEventsDetailsEnabled() != null)
            realm.setAdminEventsDetailsEnabled(rep.isAdminEventsDetailsEnabled());


        if (rep.getPasswordPolicy() != null)
            realm.setPasswordPolicy(PasswordPolicy.parse(session, rep.getPasswordPolicy()));
        if (rep.getOtpPolicyType() != null) realm.setOTPPolicy(toPolicy(rep));

        if (rep.getDefaultRoles() != null) {
            realm.updateDefaultRoles(rep.getDefaultRoles().toArray(new String[rep.getDefaultRoles().size()]));
        }

        if (rep.getSmtpServer() != null) {
            Map<String, String> config = new HashMap(rep.getSmtpServer());
            if (rep.getSmtpServer().containsKey("password") && ComponentRepresentation.SECRET_VALUE.equals(rep.getSmtpServer().get("password"))) {
                String passwordValue = realm.getSmtpConfig() != null ? realm.getSmtpConfig().get("password") : null;
                config.put("password", passwordValue);
            }
            realm.setSmtpConfig(config);
        }

        if (rep.getBrowserSecurityHeaders() != null) {
            realm.setBrowserSecurityHeaders(rep.getBrowserSecurityHeaders());
        }

        if (rep.isInternationalizationEnabled() != null) {
            realm.setInternationalizationEnabled(rep.isInternationalizationEnabled());
        }
        if (rep.getSupportedLocales() != null) {
            realm.setSupportedLocales(new HashSet<String>(rep.getSupportedLocales()));
        }
        if (rep.getDefaultLocale() != null) {
            realm.setDefaultLocale(rep.getDefaultLocale());
        }
        if (rep.getBrowserFlow() != null) {
            realm.setBrowserFlow(realm.getFlowByAlias(rep.getBrowserFlow()));
        }
        if (rep.getRegistrationFlow() != null) {
            realm.setRegistrationFlow(realm.getFlowByAlias(rep.getRegistrationFlow()));
        }
        if (rep.getDirectGrantFlow() != null) {
            realm.setDirectGrantFlow(realm.getFlowByAlias(rep.getDirectGrantFlow()));
        }
        if (rep.getResetCredentialsFlow() != null) {
            realm.setResetCredentialsFlow(realm.getFlowByAlias(rep.getResetCredentialsFlow()));
        }
        if (rep.getClientAuthenticationFlow() != null) {
            realm.setClientAuthenticationFlow(realm.getFlowByAlias(rep.getClientAuthenticationFlow()));
        }
        if (rep.getDockerAuthenticationFlow() != null) {
            realm.setDockerAuthenticationFlow(realm.getFlowByAlias(rep.getDockerAuthenticationFlow()));
        }
    }

    // Basic realm stuff


    public static ComponentModel convertFedProviderToComponent(String realmId, UserFederationProviderRepresentation fedModel) {
        UserStorageProviderModel model = new UserStorageProviderModel();
        model.setId(fedModel.getId());
        model.setName(fedModel.getDisplayName());
        model.setParentId(realmId);
        model.setProviderId(fedModel.getProviderName());
        model.setProviderType(UserStorageProvider.class.getName());
        model.setFullSyncPeriod(fedModel.getFullSyncPeriod());
        model.setPriority(fedModel.getPriority());
        model.setChangedSyncPeriod(fedModel.getChangedSyncPeriod());
        model.setLastSync(fedModel.getLastSync());
        if (fedModel.getConfig() != null) {
            for (Map.Entry<String, String> entry : fedModel.getConfig().entrySet()) {
                model.getConfig().putSingle(entry.getKey(), entry.getValue());
            }
        }
        return model;
    }

    public static ComponentModel convertFedMapperToComponent(RealmModel realm, ComponentModel parent, UserFederationMapperRepresentation rep, String newMapperType) {
        ComponentModel mapper = new ComponentModel();
        mapper.setId(rep.getId());
        mapper.setName(rep.getName());
        mapper.setProviderId(rep.getFederationMapperType());
        mapper.setProviderType(newMapperType);
        mapper.setParentId(parent.getId());
        if (rep.getConfig() != null) {
            for (Map.Entry<String, String> entry : rep.getConfig().entrySet()) {
                mapper.getConfig().putSingle(entry.getKey(), entry.getValue());
            }
        }
        return mapper;
    }


    // Roles

    public static void createRole(RealmModel newRealm, RoleRepresentation roleRep) {
        RoleModel role = roleRep.getId() != null ? newRealm.addRole(roleRep.getId(), roleRep.getName()) : newRealm.addRole(roleRep.getName());
        if (roleRep.getDescription() != null) role.setDescription(roleRep.getDescription());
        boolean scopeParamRequired = roleRep.isScopeParamRequired() == null ? false : roleRep.isScopeParamRequired();
        role.setScopeParamRequired(scopeParamRequired);
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

    private static Map<String, ClientModel> createClients(KeycloakSession session, RealmRepresentation rep, RealmModel realm) {
        Map<String, ClientModel> appMap = new HashMap<String, ClientModel>();
        for (ClientRepresentation resourceRep : rep.getClients()) {
            ClientModel app = createClient(session, realm, resourceRep, false);
            appMap.put(app.getClientId(), app);
        }
        return appMap;
    }

    /**
     * Does not create scope or role mappings!
     *
     * @param realm
     * @param resourceRep
     * @return
     */
    public static ClientModel createClient(KeycloakSession session, RealmModel realm, ClientRepresentation resourceRep, boolean addDefaultRoles) {
        logger.debug("Create client: {0}" + resourceRep.getClientId());

        ClientModel client = resourceRep.getId() != null ? realm.addClient(resourceRep.getId(), resourceRep.getClientId()) : realm.addClient(resourceRep.getClientId());
        if (resourceRep.getName() != null) client.setName(resourceRep.getName());
        if (resourceRep.getDescription() != null) client.setDescription(resourceRep.getDescription());
        if (resourceRep.isEnabled() != null) client.setEnabled(resourceRep.isEnabled());
        client.setManagementUrl(resourceRep.getAdminUrl());
        if (resourceRep.isSurrogateAuthRequired() != null)
            client.setSurrogateAuthRequired(resourceRep.isSurrogateAuthRequired());
        if (resourceRep.getRootUrl() != null) client.setRootUrl(resourceRep.getRootUrl());
        if (resourceRep.getBaseUrl() != null) client.setBaseUrl(resourceRep.getBaseUrl());
        if (resourceRep.isBearerOnly() != null) client.setBearerOnly(resourceRep.isBearerOnly());
        if (resourceRep.isConsentRequired() != null) client.setConsentRequired(resourceRep.isConsentRequired());

        // Backwards compatibility only
        if (resourceRep.isDirectGrantsOnly() != null) {
            logger.warn("Using deprecated 'directGrantsOnly' configuration in JSON representation. It will be removed in future versions");
            client.setStandardFlowEnabled(!resourceRep.isDirectGrantsOnly());
            client.setDirectAccessGrantsEnabled(resourceRep.isDirectGrantsOnly());
        }

        if (resourceRep.isStandardFlowEnabled() != null)
            client.setStandardFlowEnabled(resourceRep.isStandardFlowEnabled());
        if (resourceRep.isImplicitFlowEnabled() != null)
            client.setImplicitFlowEnabled(resourceRep.isImplicitFlowEnabled());
        if (resourceRep.isDirectAccessGrantsEnabled() != null)
            client.setDirectAccessGrantsEnabled(resourceRep.isDirectAccessGrantsEnabled());
        if (resourceRep.isServiceAccountsEnabled() != null)
            client.setServiceAccountsEnabled(resourceRep.isServiceAccountsEnabled());

        if (resourceRep.isPublicClient() != null) client.setPublicClient(resourceRep.isPublicClient());
        if (resourceRep.isFrontchannelLogout() != null)
            client.setFrontchannelLogout(resourceRep.isFrontchannelLogout());
        if (resourceRep.getProtocol() != null) client.setProtocol(resourceRep.getProtocol());
        if (resourceRep.getNodeReRegistrationTimeout() != null) {
            client.setNodeReRegistrationTimeout(resourceRep.getNodeReRegistrationTimeout());
        } else {
            client.setNodeReRegistrationTimeout(-1);
        }

        if (resourceRep.getNotBefore() != null) {
            client.setNotBefore(resourceRep.getNotBefore());
        }

        if (resourceRep.getClientAuthenticatorType() != null) {
            client.setClientAuthenticatorType(resourceRep.getClientAuthenticatorType());
        } else {
            client.setClientAuthenticatorType(KeycloakModelUtils.getDefaultClientAuthenticatorType());
        }

        client.setSecret(resourceRep.getSecret());
        if (client.getSecret() == null) {
            KeycloakModelUtils.generateSecret(client);
        }

        if (resourceRep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : resourceRep.getAttributes().entrySet()) {
                client.setAttribute(entry.getKey(), entry.getValue());
            }
        }


        if (resourceRep.getRedirectUris() != null) {
            for (String redirectUri : resourceRep.getRedirectUris()) {
                client.addRedirectUri(redirectUri);
            }
        }
        if (resourceRep.getWebOrigins() != null) {
            for (String webOrigin : resourceRep.getWebOrigins()) {
                logger.debugv("Client: {0} webOrigin: {1}", resourceRep.getClientId(), webOrigin);
                client.addWebOrigin(webOrigin);
            }
        } else {
            // add origins from redirect uris
            if (resourceRep.getRedirectUris() != null) {
                Set<String> origins = new HashSet<String>();
                for (String redirectUri : resourceRep.getRedirectUris()) {
                    logger.debugv("add redirect-uri to origin: {0}", redirectUri);
                    if (redirectUri.startsWith("http")) {
                        String origin = UriUtils.getOrigin(redirectUri);
                        logger.debugv("adding default client origin: {0}", origin);
                        origins.add(origin);
                    }
                }
                if (origins.size() > 0) {
                    client.setWebOrigins(origins);
                }
            }
        }

        if (resourceRep.getRegisteredNodes() != null) {
            for (Map.Entry<String, Integer> entry : resourceRep.getRegisteredNodes().entrySet()) {
                client.registerNode(entry.getKey(), entry.getValue());
            }
        }

        if (addDefaultRoles && resourceRep.getDefaultRoles() != null) {
            client.updateDefaultRoles(resourceRep.getDefaultRoles());
        }


        if (resourceRep.getProtocolMappers() != null) {
            // first, remove all default/built in mappers
            Set<ProtocolMapperModel> mappers = client.getProtocolMappers();
            for (ProtocolMapperModel mapper : mappers) client.removeProtocolMapper(mapper);

            for (ProtocolMapperRepresentation mapper : resourceRep.getProtocolMappers()) {
                client.addProtocolMapper(toModel(mapper));
            }

            MigrationUtils.updateProtocolMappers(client);

        }

        if (resourceRep.getClientTemplate() != null) {
            for (ClientTemplateModel template : realm.getClientTemplates()) {
                if (template.getName().equals(resourceRep.getClientTemplate())) {
                    client.setClientTemplate(template);
                    break;
                }
                MigrationUtils.updateProtocolMappers(template);
            }
        }

        if (resourceRep.isFullScopeAllowed() != null) {
            client.setFullScopeAllowed(resourceRep.isFullScopeAllowed());
        } else {
            if (client.getClientTemplate() != null) {
                client.setFullScopeAllowed(!client.isConsentRequired() && client.getClientTemplate().isFullScopeAllowed());

            } else {
                client.setFullScopeAllowed(!client.isConsentRequired());
            }
        }
        if (resourceRep.isUseTemplateConfig() != null) client.setUseTemplateConfig(resourceRep.isUseTemplateConfig());
        else client.setUseTemplateConfig(false); // default to false for now

        if (resourceRep.isUseTemplateScope() != null) client.setUseTemplateScope(resourceRep.isUseTemplateScope());
        else client.setUseTemplateScope(resourceRep.getClientTemplate() != null);

        if (resourceRep.isUseTemplateMappers() != null)
            client.setUseTemplateMappers(resourceRep.isUseTemplateMappers());
        else client.setUseTemplateMappers(resourceRep.getClientTemplate() != null);

        client.updateClient();

        return client;
    }

    public static void updateClient(ClientRepresentation rep, ClientModel resource) {
        if (rep.getClientId() != null) resource.setClientId(rep.getClientId());
        if (rep.getName() != null) resource.setName(rep.getName());
        if (rep.getDescription() != null) resource.setDescription(rep.getDescription());
        if (rep.isEnabled() != null) resource.setEnabled(rep.isEnabled());
        if (rep.isBearerOnly() != null) resource.setBearerOnly(rep.isBearerOnly());
        if (rep.isConsentRequired() != null) resource.setConsentRequired(rep.isConsentRequired());
        if (rep.isStandardFlowEnabled() != null) resource.setStandardFlowEnabled(rep.isStandardFlowEnabled());
        if (rep.isImplicitFlowEnabled() != null) resource.setImplicitFlowEnabled(rep.isImplicitFlowEnabled());
        if (rep.isDirectAccessGrantsEnabled() != null)
            resource.setDirectAccessGrantsEnabled(rep.isDirectAccessGrantsEnabled());
        if (rep.isServiceAccountsEnabled() != null) resource.setServiceAccountsEnabled(rep.isServiceAccountsEnabled());
        if (rep.isPublicClient() != null) resource.setPublicClient(rep.isPublicClient());
        if (rep.isFullScopeAllowed() != null) resource.setFullScopeAllowed(rep.isFullScopeAllowed());
        if (rep.isFrontchannelLogout() != null) resource.setFrontchannelLogout(rep.isFrontchannelLogout());
        if (rep.getRootUrl() != null) resource.setRootUrl(rep.getRootUrl());
        if (rep.getAdminUrl() != null) resource.setManagementUrl(rep.getAdminUrl());
        if (rep.getBaseUrl() != null) resource.setBaseUrl(rep.getBaseUrl());
        if (rep.isSurrogateAuthRequired() != null) resource.setSurrogateAuthRequired(rep.isSurrogateAuthRequired());
        if (rep.getNodeReRegistrationTimeout() != null)
            resource.setNodeReRegistrationTimeout(rep.getNodeReRegistrationTimeout());
        if (rep.getClientAuthenticatorType() != null)
            resource.setClientAuthenticatorType(rep.getClientAuthenticatorType());

        if (rep.getProtocol() != null) resource.setProtocol(rep.getProtocol());
        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                resource.setAttribute(entry.getKey(), entry.getValue());
            }
        }


        if (rep.getNotBefore() != null) {
            resource.setNotBefore(rep.getNotBefore());
        }
        if (rep.getDefaultRoles() != null) {
            resource.updateDefaultRoles(rep.getDefaultRoles());
        }

        List<String> redirectUris = rep.getRedirectUris();
        if (redirectUris != null) {
            resource.setRedirectUris(new HashSet<String>(redirectUris));
        }

        List<String> webOrigins = rep.getWebOrigins();
        if (webOrigins != null) {
            resource.setWebOrigins(new HashSet<String>(webOrigins));
        }

        if (rep.getRegisteredNodes() != null) {
            for (Map.Entry<String, Integer> entry : rep.getRegisteredNodes().entrySet()) {
                resource.registerNode(entry.getKey(), entry.getValue());
            }
        }

        if (rep.isUseTemplateConfig() != null) resource.setUseTemplateConfig(rep.isUseTemplateConfig());
        if (rep.isUseTemplateScope() != null) resource.setUseTemplateScope(rep.isUseTemplateScope());
        if (rep.isUseTemplateMappers() != null) resource.setUseTemplateMappers(rep.isUseTemplateMappers());

        if (rep.getSecret() != null) resource.setSecret(rep.getSecret());

        if (rep.getClientTemplate() != null) {
            if (rep.getClientTemplate().equals(ClientTemplateRepresentation.NONE)) {
                resource.setClientTemplate(null);
            } else {
                RealmModel realm = resource.getRealm();
                for (ClientTemplateModel template : realm.getClientTemplates()) {

                    if (template.getName().equals(rep.getClientTemplate())) {
                        resource.setClientTemplate(template);
                        if (rep.isUseTemplateConfig() == null) resource.setUseTemplateConfig(true);
                        if (rep.isUseTemplateScope() == null) resource.setUseTemplateScope(true);
                        if (rep.isUseTemplateMappers() == null) resource.setUseTemplateMappers(true);
                        break;
                    }
                }
            }
        }

        resource.updateClient();
    }

    // CLIENT TEMPLATES

    private static Map<String, ClientTemplateModel> createClientTemplates(KeycloakSession session, RealmRepresentation rep, RealmModel realm) {
        Map<String, ClientTemplateModel> appMap = new HashMap<>();
        for (ClientTemplateRepresentation resourceRep : rep.getClientTemplates()) {
            ClientTemplateModel app = createClientTemplate(session, realm, resourceRep);
            appMap.put(app.getName(), app);
        }
        return appMap;
    }

    public static ClientTemplateModel createClientTemplate(KeycloakSession session, RealmModel realm, ClientTemplateRepresentation resourceRep) {
        logger.debug("Create client template: {0}" + resourceRep.getName());

        ClientTemplateModel client = resourceRep.getId() != null ? realm.addClientTemplate(resourceRep.getId(), resourceRep.getName()) : realm.addClientTemplate(resourceRep.getName());
        if (resourceRep.getName() != null) client.setName(resourceRep.getName());
        if (resourceRep.getDescription() != null) client.setDescription(resourceRep.getDescription());
        if (resourceRep.getProtocol() != null) client.setProtocol(resourceRep.getProtocol());
        if (resourceRep.isFullScopeAllowed() != null) client.setFullScopeAllowed(resourceRep.isFullScopeAllowed());
        if (resourceRep.getProtocolMappers() != null) {
            // first, remove all default/built in mappers
            Set<ProtocolMapperModel> mappers = client.getProtocolMappers();
            for (ProtocolMapperModel mapper : mappers) client.removeProtocolMapper(mapper);

            for (ProtocolMapperRepresentation mapper : resourceRep.getProtocolMappers()) {
                client.addProtocolMapper(toModel(mapper));
            }
        }
        if (resourceRep.isBearerOnly() != null) client.setBearerOnly(resourceRep.isBearerOnly());
        if (resourceRep.isConsentRequired() != null) client.setConsentRequired(resourceRep.isConsentRequired());

        if (resourceRep.isStandardFlowEnabled() != null)
            client.setStandardFlowEnabled(resourceRep.isStandardFlowEnabled());
        if (resourceRep.isImplicitFlowEnabled() != null)
            client.setImplicitFlowEnabled(resourceRep.isImplicitFlowEnabled());
        if (resourceRep.isDirectAccessGrantsEnabled() != null)
            client.setDirectAccessGrantsEnabled(resourceRep.isDirectAccessGrantsEnabled());
        if (resourceRep.isServiceAccountsEnabled() != null)
            client.setServiceAccountsEnabled(resourceRep.isServiceAccountsEnabled());

        if (resourceRep.isPublicClient() != null) client.setPublicClient(resourceRep.isPublicClient());
        if (resourceRep.isFrontchannelLogout() != null)
            client.setFrontchannelLogout(resourceRep.isFrontchannelLogout());

        if (resourceRep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : resourceRep.getAttributes().entrySet()) {
                client.setAttribute(entry.getKey(), entry.getValue());
            }
        }


        return client;
    }

    public static void updateClientTemplate(ClientTemplateRepresentation rep, ClientTemplateModel resource) {
        if (rep.getName() != null) resource.setName(rep.getName());
        if (rep.getDescription() != null) resource.setDescription(rep.getDescription());
        if (rep.isFullScopeAllowed() != null) {
            resource.setFullScopeAllowed(rep.isFullScopeAllowed());
        }


        if (rep.getProtocol() != null) resource.setProtocol(rep.getProtocol());

        if (rep.isBearerOnly() != null) resource.setBearerOnly(rep.isBearerOnly());
        if (rep.isConsentRequired() != null) resource.setConsentRequired(rep.isConsentRequired());
        if (rep.isStandardFlowEnabled() != null) resource.setStandardFlowEnabled(rep.isStandardFlowEnabled());
        if (rep.isImplicitFlowEnabled() != null) resource.setImplicitFlowEnabled(rep.isImplicitFlowEnabled());
        if (rep.isDirectAccessGrantsEnabled() != null)
            resource.setDirectAccessGrantsEnabled(rep.isDirectAccessGrantsEnabled());
        if (rep.isServiceAccountsEnabled() != null) resource.setServiceAccountsEnabled(rep.isServiceAccountsEnabled());
        if (rep.isPublicClient() != null) resource.setPublicClient(rep.isPublicClient());
        if (rep.isFullScopeAllowed() != null) resource.setFullScopeAllowed(rep.isFullScopeAllowed());
        if (rep.isFrontchannelLogout() != null) resource.setFrontchannelLogout(rep.isFrontchannelLogout());

        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                resource.setAttribute(entry.getKey(), entry.getValue());
            }
        }

    }

    public static long getClaimsMask(ClaimRepresentation rep) {
        long mask = ClaimMask.ALL;

        if (rep.getAddress()) {
            mask |= ClaimMask.ADDRESS;
        } else {
            mask &= ~ClaimMask.ADDRESS;
        }
        if (rep.getEmail()) {
            mask |= ClaimMask.EMAIL;
        } else {
            mask &= ~ClaimMask.EMAIL;
        }
        if (rep.getGender()) {
            mask |= ClaimMask.GENDER;
        } else {
            mask &= ~ClaimMask.GENDER;
        }
        if (rep.getLocale()) {
            mask |= ClaimMask.LOCALE;
        } else {
            mask &= ~ClaimMask.LOCALE;
        }
        if (rep.getName()) {
            mask |= ClaimMask.NAME;
        } else {
            mask &= ~ClaimMask.NAME;
        }
        if (rep.getPhone()) {
            mask |= ClaimMask.PHONE;
        } else {
            mask &= ~ClaimMask.PHONE;
        }
        if (rep.getPicture()) {
            mask |= ClaimMask.PICTURE;
        } else {
            mask &= ~ClaimMask.PICTURE;
        }
        if (rep.getProfile()) {
            mask |= ClaimMask.PROFILE;
        } else {
            mask &= ~ClaimMask.PROFILE;
        }
        if (rep.getUsername()) {
            mask |= ClaimMask.USERNAME;
        } else {
            mask &= ~ClaimMask.USERNAME;
        }
        if (rep.getWebsite()) {
            mask |= ClaimMask.WEBSITE;
        } else {
            mask &= ~ClaimMask.WEBSITE;
        }
        return mask;
    }

    // Scope mappings

    public static void createClientScopeMappings(RealmModel realm, ClientModel clientModel, List<ScopeMappingRepresentation> mappings) {
        for (ScopeMappingRepresentation mapping : mappings) {
            ScopeContainerModel scopeContainer = getScopeContainerHavingScope(realm, mapping);

            for (String roleString : mapping.getRoles()) {
                RoleModel role = clientModel.getRole(roleString.trim());
                if (role == null) {
                    role = clientModel.addRole(roleString.trim());
                }
                scopeContainer.addScopeMapping(role);
            }
        }
    }

    private static ScopeContainerModel getScopeContainerHavingScope(RealmModel realm, ScopeMappingRepresentation scope) {
        if (scope.getClient() != null) {
            ClientModel client = realm.getClientByClientId(scope.getClient());
            if (client == null) {
                throw new RuntimeException("Unknown client specification in scope mappings: " + scope.getClient());
            }
            return client;
        } else if (scope.getClientTemplate() != null) {
            ClientTemplateModel clientTemplate = KeycloakModelUtils.getClientTemplateByName(realm, scope.getClientTemplate());
            if (clientTemplate == null) {
                throw new RuntimeException("Unknown clientTemplate specification in scope mappings: " + scope.getClientTemplate());
            }
            return clientTemplate;
        } else {
            throw new RuntimeException("Either client or clientTemplate needs to be specified in scope mappings");
        }
    }

    // Users

    public static UserModel createUser(KeycloakSession session, RealmModel newRealm, UserRepresentation userRep) {
        convertDeprecatedSocialProviders(userRep);

        // Import users just to user storage. Don't federate
        UserModel user = session.userLocalStorage().addUser(newRealm, userRep.getId(), userRep.getUsername(), false, false);
        user.setEnabled(userRep.isEnabled() != null && userRep.isEnabled());
        user.setCreatedTimestamp(userRep.getCreatedTimestamp());
        user.setEmail(userRep.getEmail());
        if (userRep.isEmailVerified() != null) user.setEmailVerified(userRep.isEmailVerified());
        user.setFirstName(userRep.getFirstName());
        user.setLastName(userRep.getLastName());
        user.setFederationLink(userRep.getFederationLink());
        if (userRep.getAttributes() != null) {
            for (Map.Entry<String, List<String>> entry : userRep.getAttributes().entrySet()) {
                List<String> value = entry.getValue();
                if (value != null) {
                    user.setAttribute(entry.getKey(), new ArrayList<>(value));
                }
            }
        }
        if (userRep.getRequiredActions() != null) {
            for (String requiredAction : userRep.getRequiredActions()) {
                user.addRequiredAction(UserModel.RequiredAction.valueOf(requiredAction.toUpperCase()));
            }
        }
        createCredentials(userRep, session, newRealm, user);
        if (userRep.getFederatedIdentities() != null) {
            for (FederatedIdentityRepresentation identity : userRep.getFederatedIdentities()) {
                FederatedIdentityModel mappingModel = new FederatedIdentityModel(identity.getIdentityProvider(), identity.getUserId(), identity.getUserName());
                session.users().addFederatedIdentity(newRealm, user, mappingModel);
            }
        }
        createRoleMappings(userRep, user, newRealm);
        if (userRep.getClientConsents() != null) {
            for (UserConsentRepresentation consentRep : userRep.getClientConsents()) {
                UserConsentModel consentModel = toModel(newRealm, consentRep);
                session.users().addConsent(newRealm, user.getId(), consentModel);
            }
        }
        if (userRep.getServiceAccountClientId() != null) {
            String clientId = userRep.getServiceAccountClientId();
            ClientModel client = newRealm.getClientByClientId(clientId);
            if (client == null) {
                throw new RuntimeException("Unable to find client specified for service account link. Client: " + clientId);
            }
            user.setServiceAccountClientLink(client.getId());
            ;
        }
        if (userRep.getGroups() != null) {
            for (String path : userRep.getGroups()) {
                GroupModel group = KeycloakModelUtils.findGroupByPath(newRealm, path);
                if (group == null) {
                    throw new RuntimeException("Unable to find group specified by path: " + path);

                }
                user.joinGroup(group);
            }
        }
        return user;
    }

    public static void createCredentials(UserRepresentation userRep, KeycloakSession session, RealmModel realm, UserModel user) {
        if (userRep.getCredentials() != null) {
            for (CredentialRepresentation cred : userRep.getCredentials()) {
                updateCredential(session, realm, user, cred);
            }
        }
    }

    // Detect if it is "plain-text" or "hashed" representation and update model according to it
    private static void updateCredential(KeycloakSession session, RealmModel realm, UserModel user, CredentialRepresentation cred) {
        if (cred.getValue() != null) {
            UserCredentialModel plainTextCred = convertCredential(cred);
            session.userCredentialManager().updateCredential(realm, user, plainTextCred);
        } else {
            CredentialModel hashedCred = new CredentialModel();
            hashedCred.setType(cred.getType());
            hashedCred.setDevice(cred.getDevice());
            if (cred.getHashIterations() != null) hashedCred.setHashIterations(cred.getHashIterations());
            try {
                if (cred.getSalt() != null) hashedCred.setSalt(Base64.decode(cred.getSalt()));
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            hashedCred.setValue(cred.getHashedSaltedValue());
            if (cred.getCounter() != null) hashedCred.setCounter(cred.getCounter());
            if (cred.getDigits() != null) hashedCred.setDigits(cred.getDigits());

            if (cred.getAlgorithm() != null) {

                // Could happen when migrating from some early version
                if ((UserCredentialModel.PASSWORD.equals(cred.getType()) || UserCredentialModel.PASSWORD_HISTORY.equals(cred.getType())) &&
                        (cred.getAlgorithm().equals(HmacOTP.HMAC_SHA1))) {
                    hashedCred.setAlgorithm("pbkdf2");
                } else {
                    hashedCred.setAlgorithm(cred.getAlgorithm());
                }

            } else {
                if (UserCredentialModel.PASSWORD.equals(cred.getType()) || UserCredentialModel.PASSWORD_HISTORY.equals(cred.getType())) {
                    hashedCred.setAlgorithm("pbkdf2");
                } else if (UserCredentialModel.isOtp(cred.getType())) {
                    hashedCred.setAlgorithm(HmacOTP.HMAC_SHA1);
                }
            }

            if (cred.getPeriod() != null) hashedCred.setPeriod(cred.getPeriod());
            if (cred.getDigits() == null && UserCredentialModel.isOtp(cred.getType())) {
                hashedCred.setDigits(6);
            }
            if (cred.getPeriod() == null && UserCredentialModel.TOTP.equals(cred.getType())) {
                hashedCred.setPeriod(30);
            }
            hashedCred.setCreatedDate(cred.getCreatedDate());
            session.userCredentialManager().createCredential(realm, user, hashedCred);
        }
    }

    public static UserCredentialModel convertCredential(CredentialRepresentation cred) {
        UserCredentialModel credential = new UserCredentialModel();
        credential.setType(cred.getType());
        credential.setValue(cred.getValue());
        return credential;
    }

    public static CredentialModel toModel(CredentialRepresentation cred) {
        CredentialModel model = new CredentialModel();
        model.setHashIterations(cred.getHashIterations());
        model.setCreatedDate(cred.getCreatedDate());
        model.setType(cred.getType());
        model.setDigits(cred.getDigits());
        model.setConfig(cred.getConfig());
        model.setDevice(cred.getDevice());
        model.setAlgorithm(cred.getAlgorithm());
        model.setCounter(cred.getCounter());
        model.setPeriod(cred.getPeriod());
        if (cred.getSalt() != null) {
            try {
                model.setSalt(Base64.decode(cred.getSalt()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        model.setValue(cred.getValue());
        if (cred.getHashedSaltedValue() != null) {
            model.setValue(cred.getHashedSaltedValue());
        }
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

    public static void createClientRoleMappings(ClientModel clientModel, UserModel user, List<String> roleNames) {
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

    private static void importIdentityProviders(RealmRepresentation rep, RealmModel newRealm) {
        if (rep.getIdentityProviders() != null) {
            for (IdentityProviderRepresentation representation : rep.getIdentityProviders()) {
                newRealm.addIdentityProvider(toModel(newRealm, representation));
            }
        }
    }

    private static void importIdentityProviderMappers(RealmRepresentation rep, RealmModel newRealm) {
        if (rep.getIdentityProviderMappers() != null) {
            for (IdentityProviderMapperRepresentation representation : rep.getIdentityProviderMappers()) {
                newRealm.addIdentityProviderMapper(toModel(representation));
            }
        }
    }

    public static IdentityProviderModel toModel(RealmModel realm, IdentityProviderRepresentation representation) {
        IdentityProviderModel identityProviderModel = new IdentityProviderModel();

        identityProviderModel.setInternalId(representation.getInternalId());
        identityProviderModel.setAlias(representation.getAlias());
        identityProviderModel.setDisplayName(representation.getDisplayName());
        identityProviderModel.setProviderId(representation.getProviderId());
        identityProviderModel.setEnabled(representation.isEnabled());
        identityProviderModel.setLinkOnly(representation.isLinkOnly());
        identityProviderModel.setTrustEmail(representation.isTrustEmail());
        identityProviderModel.setAuthenticateByDefault(representation.isAuthenticateByDefault());
        identityProviderModel.setStoreToken(representation.isStoreToken());
        identityProviderModel.setAddReadTokenRoleOnCreate(representation.isAddReadTokenRoleOnCreate());
        identityProviderModel.setConfig(new HashMap<>(representation.getConfig()));

        String flowAlias = representation.getFirstBrokerLoginFlowAlias();
        if (flowAlias == null) {
            flowAlias = DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW;
        }

        AuthenticationFlowModel flowModel = realm.getFlowByAlias(flowAlias);
        if (flowModel == null) {
            throw new ModelException("No available authentication flow with alias: " + flowAlias);
        }
        identityProviderModel.setFirstBrokerLoginFlowId(flowModel.getId());

        flowAlias = representation.getPostBrokerLoginFlowAlias();
        if (flowAlias == null || flowAlias.trim().length() == 0) {
            identityProviderModel.setPostBrokerLoginFlowId(null);
        } else {
            flowModel = realm.getFlowByAlias(flowAlias);
            if (flowModel == null) {
                throw new ModelException("No available authentication flow with alias: " + flowAlias);
            }
            identityProviderModel.setPostBrokerLoginFlowId(flowModel.getId());
        }

        return identityProviderModel;
    }

    public static ProtocolMapperModel toModel(ProtocolMapperRepresentation rep) {
        ProtocolMapperModel model = new ProtocolMapperModel();
        model.setId(rep.getId());
        model.setName(rep.getName());
        model.setConsentRequired(rep.isConsentRequired());
        model.setConsentText(rep.getConsentText());
        model.setProtocol(rep.getProtocol());
        model.setProtocolMapper(rep.getProtocolMapper());
        model.setConfig(rep.getConfig());
        return model;
    }

    public static IdentityProviderMapperModel toModel(IdentityProviderMapperRepresentation rep) {
        IdentityProviderMapperModel model = new IdentityProviderMapperModel();
        model.setId(rep.getId());
        model.setName(rep.getName());
        model.setIdentityProviderAlias(rep.getIdentityProviderAlias());
        model.setIdentityProviderMapper(rep.getIdentityProviderMapper());
        model.setConfig(rep.getConfig());
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

        if (consentRep.getGrantedRealmRoles() != null) {
            for (String roleName : consentRep.getGrantedRealmRoles()) {
                RoleModel role = newRealm.getRole(roleName);
                if (role == null) {
                    throw new RuntimeException("Unable to find realm role referenced in consent mappings of user. Role name: " + roleName);
                }
                consentModel.addGrantedRole(role);
            }
        }
        if (consentRep.getGrantedClientRoles() != null) {
            for (Map.Entry<String, List<String>> entry : consentRep.getGrantedClientRoles().entrySet()) {
                String clientId2 = entry.getKey();
                ClientModel client2 = newRealm.getClientByClientId(clientId2);
                if (client2 == null) {
                    throw new RuntimeException("Unable to find client referenced in consent mappings. Client ID: " + clientId2);
                }
                for (String clientRoleName : entry.getValue()) {
                    RoleModel clientRole = client2.getRole(clientRoleName);
                    if (clientRole == null) {
                        throw new RuntimeException("Unable to find client role referenced in consent mappings of user. Role name: " + clientRole + ", Client: " + clientId2);
                    }
                    consentModel.addGrantedRole(clientRole);
                }
            }
        }
        if (consentRep.getGrantedProtocolMappers() != null) {
            for (Map.Entry<String, List<String>> protocolEntry : consentRep.getGrantedProtocolMappers().entrySet()) {
                String protocol = protocolEntry.getKey();
                for (String protocolMapperName : protocolEntry.getValue()) {
                    ProtocolMapperModel protocolMapper = client.getProtocolMapperByName(protocol, protocolMapperName);
                    if (protocolMapper == null) {
                        throw new RuntimeException("Unable to find protocol mapper for protocol " + protocol + ", mapper name " + protocolMapperName);
                    }

                    consentModel.addGrantedProtocolMapper(protocolMapper);
                }
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

    public static AuthenticationExecutionModel toModel(RealmModel realm, AuthenticationExecutionExportRepresentation rep) {
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        if (rep.getAuthenticatorConfig() != null) {
            AuthenticatorConfigModel config = realm.getAuthenticatorConfigByAlias(rep.getAuthenticatorConfig());
            model.setAuthenticatorConfig(config.getId());
        }
        model.setAuthenticator(rep.getAuthenticator());
        model.setAuthenticatorFlow(rep.isAutheticatorFlow());
        if (rep.getFlowAlias() != null) {
            AuthenticationFlowModel flow = realm.getFlowByAlias(rep.getFlowAlias());
            model.setFlowId(flow.getId());
        }
        model.setPriority(rep.getPriority());
        model.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(rep.getRequirement()));
        return model;
    }

    public static AuthenticationExecutionModel toModel(RealmModel realm, AuthenticationExecutionRepresentation rep) {
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        model.setId(rep.getId());
        model.setFlowId(rep.getFlowId());

        model.setAuthenticator(rep.getAuthenticator());
        model.setPriority(rep.getPriority());
        model.setParentFlow(rep.getParentFlow());
        model.setAuthenticatorFlow(rep.isAutheticatorFlow());
        model.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(rep.getRequirement()));

        if (rep.getAuthenticatorConfig() != null) {
            AuthenticatorConfigModel cfg = realm.getAuthenticatorConfigByAlias(rep.getAuthenticatorConfig());
            model.setAuthenticatorConfig(cfg.getId());
        }
        return model;
    }

    public static AuthenticatorConfigModel toModel(AuthenticatorConfigRepresentation rep) {
        AuthenticatorConfigModel model = new AuthenticatorConfigModel();
        model.setAlias(rep.getAlias());
        model.setConfig(rep.getConfig());
        return model;
    }

    public static RequiredActionProviderModel toModel(RequiredActionProviderRepresentation rep) {
        RequiredActionProviderModel model = new RequiredActionProviderModel();
        model.setConfig(rep.getConfig());
        model.setDefaultAction(rep.isDefaultAction());
        model.setEnabled(rep.isEnabled());
        model.setProviderId(rep.getProviderId());
        model.setName(rep.getName());
        model.setAlias(rep.getAlias());
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
                    break;
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

    public static void importRealmAuthorizationSettings(RealmRepresentation rep, RealmModel newRealm, KeycloakSession session) {
        if (rep.getClients() != null) {
            rep.getClients().forEach(clientRepresentation -> {
                ClientModel client = newRealm.getClientByClientId(clientRepresentation.getClientId());
                importAuthorizationSettings(clientRepresentation, client, session);
            });
        }
    }

    public static void importAuthorizationSettings(ClientRepresentation clientRepresentation, ClientModel client, KeycloakSession session) {
        if (Boolean.TRUE.equals(clientRepresentation.getAuthorizationServicesEnabled())) {
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

            toModel(rep, authorization);
        }
    }

    public static void toModel(ResourceServerRepresentation rep, AuthorizationProvider authorization) {
        ResourceServerStore resourceServerStore = authorization.getStoreFactory().getResourceServerStore();
        ResourceServer resourceServer;
        ResourceServer existing = resourceServerStore.findByClient(rep.getClientId());

        if (existing == null) {
            resourceServer = resourceServerStore.create(rep.getClientId());
            resourceServer.setAllowRemoteResourceManagement(true);
            resourceServer.setPolicyEnforcementMode(PolicyEnforcementMode.ENFORCING);
        } else {
            resourceServer = existing;
        }

        resourceServer.setPolicyEnforcementMode(rep.getPolicyEnforcementMode());
        resourceServer.setAllowRemoteResourceManagement(rep.isAllowRemoteResourceManagement());

        rep.getScopes().forEach(scope -> {
            toModel(scope, resourceServer, authorization);
        });

        KeycloakSession session = authorization.getKeycloakSession();
        RealmModel realm = authorization.getRealm();

        rep.getResources().forEach(resourceRepresentation -> {
            ResourceOwnerRepresentation owner = resourceRepresentation.getOwner();

            if (owner == null) {
                owner = new ResourceOwnerRepresentation();
                resourceRepresentation.setOwner(owner);
            }

            owner.setId(resourceServer.getClientId());

            if (owner.getName() != null) {
                UserModel user = session.users().getUserByUsername(owner.getName(), realm);

                if (user != null) {
                    owner.setId(user.getId());
                }
            }

            toModel(resourceRepresentation, resourceServer, authorization);
        });

        importPolicies(authorization, resourceServer, rep.getPolicies(), null);
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
                    List<String> policies = (List<String>) JsonSerialization.readValue(applyPolicies, List.class);
                    config.put("applyPolicies", JsonSerialization.writeValueAsString(policies.stream().map(policyName -> {
                        Policy policy = policyStore.findByName(policyName, resourceServer.getId());

                        if (policy == null) {
                            policy = policyStore.findById(policyName, resourceServer.getId());
                        }

                        if (policy == null) {
                            policy = importPolicies(authorization, resourceServer, policiesToImport, policyName);
                            if (policy == null) {
                                throw new RuntimeException("Policy with name [" + policyName + "] not defined.");
                            }
                        }

                        return policy.getId();
                    }).collect(Collectors.toList())));
                } catch (Exception e) {
                    throw new RuntimeException("Error while importing policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            PolicyStore policyStore = storeFactory.getPolicyStore();
            Policy policy = policyStore.findById(policyRepresentation.getId(), resourceServer.getId());

            if (policy == null) {
                policy = policyStore.findByName(policyRepresentation.getName(), resourceServer.getId());
            }

            if (policy == null) {
                policy = policyStore.create(policyRepresentation, resourceServer);
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

        Set resources = representation.getResources();
        Set scopes = representation.getScopes();
        Set policies = representation.getPolicies();

        if (representation instanceof PolicyRepresentation) {
            PolicyRepresentation policy = PolicyRepresentation.class.cast(representation);
            String resourcesConfig = policy.getConfig().get("resources");

            if (resourcesConfig != null) {
                try {
                    resources = JsonSerialization.readValue(resourcesConfig, Set.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            String scopesConfig = policy.getConfig().get("scopes");

            if (scopesConfig != null) {
                try {
                    scopes = JsonSerialization.readValue(scopesConfig, Set.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            String policiesConfig = policy.getConfig().get("applyPolicies");

            if (policiesConfig != null) {
                try {
                    policies = JsonSerialization.readValue(policiesConfig, Set.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            model.setConfig(policy.getConfig());
        }

        StoreFactory storeFactory = authorization.getStoreFactory();

        updateResources(resources, model, storeFactory);
        updateScopes(scopes, model, storeFactory);
        updateAssociatedPolicies(policies, model, storeFactory);

        PolicyProviderFactory provider = authorization.getProviderFactory(model.getType());

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

    private static void updateScopes(Set<String> scopeIds, Policy policy, StoreFactory storeFactory) {
        if (scopeIds != null) {
            if (scopeIds.isEmpty()) {
                for (Scope scope : new HashSet<Scope>(policy.getScopes())) {
                    policy.removeScope(scope);
                }
                return;
            }
            for (String scopeId : scopeIds) {
                boolean hasScope = false;

                for (Scope scopeModel : new HashSet<Scope>(policy.getScopes())) {
                    if (scopeModel.getId().equals(scopeId) || scopeModel.getName().equals(scopeId)) {
                        hasScope = true;
                    }
                }
                if (!hasScope) {
                    ResourceServer resourceServer = policy.getResourceServer();
                    Scope scope = storeFactory.getScopeStore().findById(scopeId, resourceServer.getId());

                    if (scope == null) {
                        scope = storeFactory.getScopeStore().findByName(scopeId, resourceServer.getId());
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
        }

        policy.removeConfig("scopes");
    }

    private static void updateAssociatedPolicies(Set<String> policyIds, Policy policy, StoreFactory storeFactory) {
        ResourceServer resourceServer = policy.getResourceServer();

        if (policyIds != null) {
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
                    Policy associatedPolicy = policyStore.findById(policyId, resourceServer.getId());

                    if (associatedPolicy == null) {
                        associatedPolicy = policyStore.findByName(policyId, resourceServer.getId());
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
        }

        policy.removeConfig("applyPolicies");
    }

    private static void updateResources(Set<String> resourceIds, Policy policy, StoreFactory storeFactory) {
        if (resourceIds != null) {
            if (resourceIds.isEmpty()) {
                for (Resource resource : new HashSet<>(policy.getResources())) {
                    policy.removeResource(resource);
                }
            }
            for (String resourceId : resourceIds) {
                boolean hasResource = false;
                for (Resource resourceModel : new HashSet<>(policy.getResources())) {
                    if (resourceModel.getId().equals(resourceId) || resourceModel.getName().equals(resourceId)) {
                        hasResource = true;
                    }
                }
                if (!hasResource && !"".equals(resourceId)) {
                    Resource resource = storeFactory.getResourceStore().findById(resourceId, policy.getResourceServer().getId());

                    if (resource == null) {
                        resource = storeFactory.getResourceStore().findByName(resourceId, policy.getResourceServer().getId());
                        if (resource == null) {
                            throw new RuntimeException("Resource with id or name [" + resourceId + "] does not exist");
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
                    policy.removeResource(resourceModel);
                }
            }
        }

        policy.removeConfig("resources");
    }

    public static Resource toModel(ResourceRepresentation resource, ResourceServer resourceServer, AuthorizationProvider authorization) {
        ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();
        Resource existing;

        if (resource.getId() != null) {
            existing = resourceStore.findById(resource.getId(), resourceServer.getId());
        } else {
            existing = resourceStore.findByName(resource.getName(), resourceServer.getId());
        }

        if (existing != null) {
            existing.setName(resource.getName());
            existing.setType(resource.getType());
            existing.setUri(resource.getUri());
            existing.setIconUri(resource.getIconUri());

            existing.updateScopes(resource.getScopes().stream()
                    .map((ScopeRepresentation scope) -> toModel(scope, resourceServer, authorization))
                    .collect(Collectors.toSet()));
            return existing;
        }

        ResourceOwnerRepresentation owner = resource.getOwner();

        if (owner == null) {
            owner = new ResourceOwnerRepresentation();
            owner.setId(resourceServer.getClientId());
        }

        if (owner.getId() == null) {
            throw new RuntimeException("No owner specified for resource [" + resource.getName() + "].");
        }

        Resource model = resourceStore.create(resource.getName(), resourceServer, owner.getId());

        model.setType(resource.getType());
        model.setUri(resource.getUri());
        model.setIconUri(resource.getIconUri());

        Set<ScopeRepresentation> scopes = resource.getScopes();

        if (scopes != null) {
            model.updateScopes(scopes.stream().map((Function<ScopeRepresentation, Scope>) scope -> toModel(scope, resourceServer, authorization)).collect(Collectors.toSet()));
        }

        resource.setId(model.getId());

        return model;
    }

    public static Scope toModel(ScopeRepresentation scope, ResourceServer resourceServer, AuthorizationProvider authorization) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        ScopeStore scopeStore = storeFactory.getScopeStore();
        Scope existing;

        if (scope.getId() != null) {
            existing = scopeStore.findById(scope.getId(), resourceServer.getId());
        } else {
            existing = scopeStore.findByName(scope.getName(), resourceServer.getId());
        }

        if (existing != null) {
            existing.setName(scope.getName());
            existing.setIconUri(scope.getIconUri());
            return existing;
        }

        Scope model = scopeStore.create(scope.getName(), resourceServer);
        model.setIconUri(scope.getIconUri());
        scope.setId(model.getId());

        return model;
    }

    public static void importFederatedUser(KeycloakSession session, RealmModel newRealm, UserRepresentation userRep) {
        UserFederatedStorageProvider federatedStorage = session.userFederatedStorage();
        if (userRep.getAttributes() != null) {
            for (Map.Entry<String, List<String>> entry : userRep.getAttributes().entrySet()) {
                String key = entry.getKey();
                List<String> value = entry.getValue();
                if (value != null) {
                    federatedStorage.setAttribute(newRealm, userRep.getId(), key, new LinkedList<>(value));
                }
            }
        }
        if (userRep.getRequiredActions() != null) {
            for (String action : userRep.getRequiredActions()) {
                federatedStorage.addRequiredAction(newRealm, userRep.getId(), action);
            }
        }
        if (userRep.getCredentials() != null) {
            for (CredentialRepresentation cred : userRep.getCredentials()) {
                federatedStorage.createCredential(newRealm, userRep.getId(), toModel(cred));
            }
        }
        createFederatedRoleMappings(federatedStorage, userRep, newRealm);

        if (userRep.getGroups() != null) {
            for (String path : userRep.getGroups()) {
                GroupModel group = KeycloakModelUtils.findGroupByPath(newRealm, path);
                if (group == null) {
                    throw new RuntimeException("Unable to find group specified by path: " + path);

                }
                federatedStorage.joinGroup(newRealm, userRep.getId(), group);
            }
        }

        if (userRep.getFederatedIdentities() != null) {
            for (FederatedIdentityRepresentation identity : userRep.getFederatedIdentities()) {
                FederatedIdentityModel mappingModel = new FederatedIdentityModel(identity.getIdentityProvider(), identity.getUserId(), identity.getUserName());
                federatedStorage.addFederatedIdentity(newRealm, userRep.getId(), mappingModel);
            }
        }
        if (userRep.getClientConsents() != null) {
            for (UserConsentRepresentation consentRep : userRep.getClientConsents()) {
                UserConsentModel consentModel = toModel(newRealm, consentRep);
                federatedStorage.addConsent(newRealm, userRep.getId(), consentModel);
            }
        }


    }

    public static void createFederatedRoleMappings(UserFederatedStorageProvider federatedStorage, UserRepresentation userRep, RealmModel realm) {
        if (userRep.getRealmRoles() != null) {
            for (String roleString : userRep.getRealmRoles()) {
                RoleModel role = realm.getRole(roleString.trim());
                if (role == null) {
                    role = realm.addRole(roleString.trim());
                }
                federatedStorage.grantRole(realm, userRep.getId(), role);
            }
        }
        if (userRep.getClientRoles() != null) {
            for (Map.Entry<String, List<String>> entry : userRep.getClientRoles().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new RuntimeException("Unable to find client role mappings for client: " + entry.getKey());
                }
                createFederatedClientRoleMappings(federatedStorage, realm, client, userRep, entry.getValue());
            }
        }
    }

    public static void createFederatedClientRoleMappings(UserFederatedStorageProvider federatedStorage, RealmModel realm, ClientModel clientModel, UserRepresentation userRep, List<String> roleNames) {
        if (userRep == null) {
            throw new RuntimeException("User not found");
        }

        for (String roleName : roleNames) {
            RoleModel role = clientModel.getRole(roleName.trim());
            if (role == null) {
                role = clientModel.addRole(roleName.trim());
            }
            federatedStorage.grantRole(realm, userRep.getId(), role);

        }
    }
}
