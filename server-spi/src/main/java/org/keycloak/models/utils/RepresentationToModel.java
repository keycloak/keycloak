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

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.migration.migrators.MigrationUtils;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.Constants;
import org.keycloak.common.util.Base64;
import org.jboss.logging.Logger;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
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
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.common.util.UriUtils;
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
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public static void importRealm(KeycloakSession session, RealmRepresentation rep, RealmModel newRealm) {
        convertDeprecatedSocialProviders(rep);
        convertDeprecatedApplications(session, rep);

        newRealm.setName(rep.getRealm());
        if (rep.getDisplayName() != null) newRealm.setDisplayName(rep.getDisplayName());
        if (rep.getDisplayNameHtml() != null) newRealm.setDisplayNameHtml(rep.getDisplayNameHtml());
        if (rep.isEnabled() != null) newRealm.setEnabled(rep.isEnabled());
        if (rep.isBruteForceProtected() != null) newRealm.setBruteForceProtected(rep.isBruteForceProtected());
        if (rep.getMaxFailureWaitSeconds() != null) newRealm.setMaxFailureWaitSeconds(rep.getMaxFailureWaitSeconds());
        if (rep.getMinimumQuickLoginWaitSeconds() != null) newRealm.setMinimumQuickLoginWaitSeconds(rep.getMinimumQuickLoginWaitSeconds());
        if (rep.getWaitIncrementSeconds() != null) newRealm.setWaitIncrementSeconds(rep.getWaitIncrementSeconds());
        if (rep.getQuickLoginCheckMilliSeconds() != null) newRealm.setQuickLoginCheckMilliSeconds(rep.getQuickLoginCheckMilliSeconds());
        if (rep.getMaxDeltaTimeSeconds() != null) newRealm.setMaxDeltaTimeSeconds(rep.getMaxDeltaTimeSeconds());
        if (rep.getFailureFactor() != null) newRealm.setFailureFactor(rep.getFailureFactor());
        if (rep.isEventsEnabled() != null) newRealm.setEventsEnabled(rep.isEventsEnabled());
        if (rep.getEventsExpiration() != null) newRealm.setEventsExpiration(rep.getEventsExpiration());
        if (rep.getEventsListeners() != null) newRealm.setEventsListeners(new HashSet<>(rep.getEventsListeners()));
        if (rep.isAdminEventsEnabled() != null) newRealm.setAdminEventsEnabled(rep.isAdminEventsEnabled());
        if (rep.isAdminEventsDetailsEnabled() != null) newRealm.setAdminEventsDetailsEnabled(rep.isAdminEventsDetailsEnabled());

        if (rep.getNotBefore() != null) newRealm.setNotBefore(rep.getNotBefore());

        if (rep.getRevokeRefreshToken() != null) newRealm.setRevokeRefreshToken(rep.getRevokeRefreshToken());
        else newRealm.setRevokeRefreshToken(false);

        if (rep.getAccessTokenLifespan() != null) newRealm.setAccessTokenLifespan(rep.getAccessTokenLifespan());
        else newRealm.setAccessTokenLifespan(300);

        if (rep.getAccessTokenLifespanForImplicitFlow() != null) newRealm.setAccessTokenLifespanForImplicitFlow(rep.getAccessTokenLifespanForImplicitFlow());
        else newRealm.setAccessTokenLifespanForImplicitFlow(Constants.DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT);

        if (rep.getSsoSessionIdleTimeout() != null) newRealm.setSsoSessionIdleTimeout(rep.getSsoSessionIdleTimeout());
        else newRealm.setSsoSessionIdleTimeout(1800);
        if (rep.getSsoSessionMaxLifespan() != null) newRealm.setSsoSessionMaxLifespan(rep.getSsoSessionMaxLifespan());
        else newRealm.setSsoSessionMaxLifespan(36000);
        if (rep.getOfflineSessionIdleTimeout() != null) newRealm.setOfflineSessionIdleTimeout(rep.getOfflineSessionIdleTimeout());
        else newRealm.setOfflineSessionIdleTimeout(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT);

        if (rep.getAccessCodeLifespan() != null) newRealm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        else newRealm.setAccessCodeLifespan(60);

        if (rep.getAccessCodeLifespanUserAction() != null)
            newRealm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        else newRealm.setAccessCodeLifespanUserAction(300);

        if (rep.getAccessCodeLifespanLogin() != null)
            newRealm.setAccessCodeLifespanLogin(rep.getAccessCodeLifespanLogin());
        else newRealm.setAccessCodeLifespanLogin(1800);

        if (rep.getSslRequired() != null) newRealm.setSslRequired(SslRequired.valueOf(rep.getSslRequired().toUpperCase()));
        if (rep.isRegistrationAllowed() != null) newRealm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isRegistrationEmailAsUsername() != null)
            newRealm.setRegistrationEmailAsUsername(rep.isRegistrationEmailAsUsername());
        if (rep.isRememberMe() != null) newRealm.setRememberMe(rep.isRememberMe());
        if (rep.isVerifyEmail() != null) newRealm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isResetPasswordAllowed() != null) newRealm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.isEditUsernameAllowed() != null) newRealm.setEditUsernameAllowed(rep.isEditUsernameAllowed());
        if (rep.getPrivateKey() == null || rep.getPublicKey() == null) {
            KeycloakModelUtils.generateRealmKeys(newRealm);
        } else {
            newRealm.setPrivateKeyPem(rep.getPrivateKey());
            newRealm.setPublicKeyPem(rep.getPublicKey());
        }
        if (rep.getCertificate() == null) {
            KeycloakModelUtils.generateRealmCertificate(newRealm);
        } else {
            newRealm.setCertificatePem(rep.getCertificate());
        }
        if (rep.getCodeSecret() == null) {
            newRealm.setCodeSecret(KeycloakModelUtils.generateCodeSecret());
        } else {
            newRealm.setCodeSecret(rep.getCodeSecret());
        }

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

        if (rep.getPasswordPolicy() != null) newRealm.setPasswordPolicy(PasswordPolicy.parse(session, rep.getPasswordPolicy()));
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

        if (rep.getClients() != null) {
            rep.getClients().forEach(clientRepresentation -> {
                ClientModel client = newRealm.getClientByClientId(clientRepresentation.getClientId());
                importAuthorizationSettings(clientRepresentation, client, session);
            });
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

        List<UserFederationProviderModel> providerModels = null;
        if (rep.getUserFederationProviders() != null) {
            providerModels = convertFederationProviders(rep.getUserFederationProviders());
            newRealm.setUserFederationProviders(providerModels);
        }
        if (rep.getUserFederationMappers() != null) {

            // Remove builtin mappers for federation providers, which have some mappers already provided in JSON (likely due to previous export)
            if (rep.getUserFederationProviders() != null) {
                Set<String> providerNames = new TreeSet<String>();
                for (UserFederationMapperRepresentation representation : rep.getUserFederationMappers()) {
                    providerNames.add(representation.getFederationProviderDisplayName());
                }
                for (String providerName : providerNames) {
                    for (UserFederationProviderModel providerModel : providerModels) {
                        if (providerName.equals(providerModel.getDisplayName())) {
                            Set<UserFederationMapperModel> toDelete = newRealm.getUserFederationMappersByFederationProvider(providerModel.getId());
                            for (UserFederationMapperModel mapperModel : toDelete) {
                                newRealm.removeUserFederationMapper(mapperModel);
                            }
                        }
                    }
                }
            }

            for (UserFederationMapperRepresentation representation : rep.getUserFederationMappers()) {
                newRealm.addUserFederationMapper(toModel(newRealm, representation));
            }
        }

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

        if(rep.isInternationalizationEnabled() != null){
            newRealm.setInternationalizationEnabled(rep.isInternationalizationEnabled());
        }
        if(rep.getSupportedLocales() != null){
            newRealm.setSupportedLocales(new HashSet<String>(rep.getSupportedLocales()));
        }
        if(rep.getDefaultLocale() != null){
            newRealm.setDefaultLocale(rep.getDefaultLocale());
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
                component.setParentId(parentId);
                component = newRealm.addComponentModel(component);
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
                    RoleModel role = roleRep.getId()!=null ? client.addRole(roleRep.getId(), roleRep.getName()) : client.addRole(roleRep.getName());
                    role.setDescription(roleRep.getDescription());
                    boolean scopeParamRequired = roleRep.isScopeParamRequired()==null ? false : roleRep.isScopeParamRequired();
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
        if (rep.getDisplayName() != null) realm.setDisplayName(rep.getDisplayName());
        if (rep.getDisplayNameHtml() != null) realm.setDisplayNameHtml(rep.getDisplayNameHtml());
        if (rep.isEnabled() != null) realm.setEnabled(rep.isEnabled());
        if (rep.isBruteForceProtected() != null) realm.setBruteForceProtected(rep.isBruteForceProtected());
        if (rep.getMaxFailureWaitSeconds() != null) realm.setMaxFailureWaitSeconds(rep.getMaxFailureWaitSeconds());
        if (rep.getMinimumQuickLoginWaitSeconds() != null) realm.setMinimumQuickLoginWaitSeconds(rep.getMinimumQuickLoginWaitSeconds());
        if (rep.getWaitIncrementSeconds() != null) realm.setWaitIncrementSeconds(rep.getWaitIncrementSeconds());
        if (rep.getQuickLoginCheckMilliSeconds() != null) realm.setQuickLoginCheckMilliSeconds(rep.getQuickLoginCheckMilliSeconds());
        if (rep.getMaxDeltaTimeSeconds() != null) realm.setMaxDeltaTimeSeconds(rep.getMaxDeltaTimeSeconds());
        if (rep.getFailureFactor() != null) realm.setFailureFactor(rep.getFailureFactor());
        if (rep.isRegistrationAllowed() != null) realm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isRegistrationEmailAsUsername() != null) realm.setRegistrationEmailAsUsername(rep.isRegistrationEmailAsUsername());
        if (rep.isRememberMe() != null) realm.setRememberMe(rep.isRememberMe());
        if (rep.isVerifyEmail() != null) realm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isResetPasswordAllowed() != null) realm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.isEditUsernameAllowed() != null) realm.setEditUsernameAllowed(rep.isEditUsernameAllowed());
        if (rep.getSslRequired() != null) realm.setSslRequired(SslRequired.valueOf(rep.getSslRequired().toUpperCase()));
        if (rep.getAccessCodeLifespan() != null) realm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        if (rep.getAccessCodeLifespanUserAction() != null) realm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        if (rep.getAccessCodeLifespanLogin() != null) realm.setAccessCodeLifespanLogin(rep.getAccessCodeLifespanLogin());
        if (rep.getNotBefore() != null) realm.setNotBefore(rep.getNotBefore());
        if (rep.getRevokeRefreshToken() != null) realm.setRevokeRefreshToken(rep.getRevokeRefreshToken());
        if (rep.getAccessTokenLifespan() != null) realm.setAccessTokenLifespan(rep.getAccessTokenLifespan());
        if (rep.getAccessTokenLifespanForImplicitFlow() != null) realm.setAccessTokenLifespanForImplicitFlow(rep.getAccessTokenLifespanForImplicitFlow());
        if (rep.getSsoSessionIdleTimeout() != null) realm.setSsoSessionIdleTimeout(rep.getSsoSessionIdleTimeout());
        if (rep.getSsoSessionMaxLifespan() != null) realm.setSsoSessionMaxLifespan(rep.getSsoSessionMaxLifespan());
        if (rep.getOfflineSessionIdleTimeout() != null) realm.setOfflineSessionIdleTimeout(rep.getOfflineSessionIdleTimeout());
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
        if (rep.isAdminEventsDetailsEnabled() != null) realm.setAdminEventsDetailsEnabled(rep.isAdminEventsDetailsEnabled());


        if (rep.getPasswordPolicy() != null) realm.setPasswordPolicy(PasswordPolicy.parse(session, rep.getPasswordPolicy()));
        if (rep.getOtpPolicyType() != null) realm.setOTPPolicy(toPolicy(rep));

        if (rep.getDefaultRoles() != null) {
            realm.updateDefaultRoles(rep.getDefaultRoles().toArray(new String[rep.getDefaultRoles().size()]));
        }

        if (rep.getSmtpServer() != null) {
            realm.setSmtpConfig(new HashMap(rep.getSmtpServer()));
        }

        if (rep.getBrowserSecurityHeaders() != null) {
            realm.setBrowserSecurityHeaders(rep.getBrowserSecurityHeaders());
        }

        if (rep.getUserFederationProviders() != null) {
            List<UserFederationProviderModel> providerModels = convertFederationProviders(rep.getUserFederationProviders());
            realm.setUserFederationProviders(providerModels);
        }

        if ("GENERATE".equals(rep.getPublicKey())) {
            KeycloakModelUtils.generateRealmKeys(realm);
        } else {
            if (rep.getPrivateKey() != null && rep.getPublicKey() != null) {
                realm.setPrivateKeyPem(rep.getPrivateKey());
                realm.setPublicKeyPem(rep.getPublicKey());
                realm.setCodeSecret(KeycloakModelUtils.generateCodeSecret());
            }

            if (rep.getCertificate() != null) {
                realm.setCertificatePem(rep.getCertificate());
            }
        }

        if(rep.isInternationalizationEnabled() != null){
            realm.setInternationalizationEnabled(rep.isInternationalizationEnabled());
        }
        if(rep.getSupportedLocales() != null){
            realm.setSupportedLocales(new HashSet<String>(rep.getSupportedLocales()));
        }
        if(rep.getDefaultLocale() != null){
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
    }

    // Basic realm stuff


    private static List<UserFederationProviderModel> convertFederationProviders(List<UserFederationProviderRepresentation> providers) {
        List<UserFederationProviderModel> result = new ArrayList<UserFederationProviderModel>();

        for (UserFederationProviderRepresentation representation : providers) {
            UserFederationProviderModel model = new UserFederationProviderModel(representation.getId(), representation.getProviderName(),
                    representation.getConfig(), representation.getPriority(), representation.getDisplayName(),
                    representation.getFullSyncPeriod(), representation.getChangedSyncPeriod(), representation.getLastSync());
            result.add(model);
        }
        return result;
    }

    public static UserFederationMapperModel toModel(RealmModel realm, UserFederationMapperRepresentation rep) {
        UserFederationMapperModel model = new UserFederationMapperModel();
        model.setId(rep.getId());
        model.setName(rep.getName());
        model.setFederationMapperType(rep.getFederationMapperType());
        model.setConfig(rep.getConfig());

        UserFederationProviderModel fedProvider = KeycloakModelUtils.findUserFederationProviderByDisplayName(rep.getFederationProviderDisplayName(), realm);
        if (fedProvider == null) {
            throw new ModelException("Couldn't find federation provider with display name [" + rep.getFederationProviderDisplayName() + "] referenced from mapper ["
                    + rep.getName());
        }
        model.setFederationProviderId(fedProvider.getId());

        return model;
    }

    // Roles

    public static void createRole(RealmModel newRealm, RoleRepresentation roleRep) {
        RoleModel role = roleRep.getId()!=null ? newRealm.addRole(roleRep.getId(), roleRep.getName()) : newRealm.addRole(roleRep.getName());
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
                    if (clientRole == null) throw new RuntimeException("Unable to find composite client role: " + roleStr);
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

        ClientModel client = resourceRep.getId()!=null ? realm.addClient(resourceRep.getId(), resourceRep.getClientId()) : realm.addClient(resourceRep.getClientId());
        if (resourceRep.getName() != null) client.setName(resourceRep.getName());
        if(resourceRep.getDescription() != null) client.setDescription(resourceRep.getDescription());
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

        if (resourceRep.isStandardFlowEnabled() != null) client.setStandardFlowEnabled(resourceRep.isStandardFlowEnabled());
        if (resourceRep.isImplicitFlowEnabled() != null) client.setImplicitFlowEnabled(resourceRep.isImplicitFlowEnabled());
        if (resourceRep.isDirectAccessGrantsEnabled() != null) client.setDirectAccessGrantsEnabled(resourceRep.isDirectAccessGrantsEnabled());
        if (resourceRep.isServiceAccountsEnabled() != null) client.setServiceAccountsEnabled(resourceRep.isServiceAccountsEnabled());

        if (resourceRep.isPublicClient() != null) client.setPublicClient(resourceRep.isPublicClient());
        if (resourceRep.isFrontchannelLogout() != null) client.setFrontchannelLogout(resourceRep.isFrontchannelLogout());
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
                        logger.debugv("adding default client origin: {0}" , origin);
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
        }

        if (resourceRep.getClientTemplate() != null) {
            for (ClientTemplateModel template : realm.getClientTemplates()) {
                if (template.getName().equals(resourceRep.getClientTemplate())) {
                    client.setClientTemplate(template);
                    break;
                }
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

        if (resourceRep.isUseTemplateMappers() != null) client.setUseTemplateMappers(resourceRep.isUseTemplateMappers());
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
        if (rep.isDirectAccessGrantsEnabled() != null) resource.setDirectAccessGrantsEnabled(rep.isDirectAccessGrantsEnabled());
        if (rep.isServiceAccountsEnabled() != null) resource.setServiceAccountsEnabled(rep.isServiceAccountsEnabled());
        if (rep.isPublicClient() != null) resource.setPublicClient(rep.isPublicClient());
        if (rep.isFullScopeAllowed() != null) resource.setFullScopeAllowed(rep.isFullScopeAllowed());
        if (rep.isFrontchannelLogout() != null) resource.setFrontchannelLogout(rep.isFrontchannelLogout());
        if (rep.getRootUrl() != null) resource.setRootUrl(rep.getRootUrl());
        if (rep.getAdminUrl() != null) resource.setManagementUrl(rep.getAdminUrl());
        if (rep.getBaseUrl() != null) resource.setBaseUrl(rep.getBaseUrl());
        if (rep.isSurrogateAuthRequired() != null) resource.setSurrogateAuthRequired(rep.isSurrogateAuthRequired());
        if (rep.getNodeReRegistrationTimeout() != null) resource.setNodeReRegistrationTimeout(rep.getNodeReRegistrationTimeout());
        if (rep.getClientAuthenticatorType() != null) resource.setClientAuthenticatorType(rep.getClientAuthenticatorType());

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

        ClientTemplateModel client = resourceRep.getId()!=null ? realm.addClientTemplate(resourceRep.getId(), resourceRep.getName()) : realm.addClientTemplate(resourceRep.getName());
        if (resourceRep.getName() != null) client.setName(resourceRep.getName());
        if(resourceRep.getDescription() != null) client.setDescription(resourceRep.getDescription());
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

        if (resourceRep.isStandardFlowEnabled() != null) client.setStandardFlowEnabled(resourceRep.isStandardFlowEnabled());
        if (resourceRep.isImplicitFlowEnabled() != null) client.setImplicitFlowEnabled(resourceRep.isImplicitFlowEnabled());
        if (resourceRep.isDirectAccessGrantsEnabled() != null) client.setDirectAccessGrantsEnabled(resourceRep.isDirectAccessGrantsEnabled());
        if (resourceRep.isServiceAccountsEnabled() != null) client.setServiceAccountsEnabled(resourceRep.isServiceAccountsEnabled());

        if (resourceRep.isPublicClient() != null) client.setPublicClient(resourceRep.isPublicClient());
        if (resourceRep.isFrontchannelLogout() != null) client.setFrontchannelLogout(resourceRep.isFrontchannelLogout());

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
        if (rep.isDirectAccessGrantsEnabled() != null) resource.setDirectAccessGrantsEnabled(rep.isDirectAccessGrantsEnabled());
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
        UserModel user = session.userStorage().addUser(newRealm, userRep.getId(), userRep.getUsername(), false, false);
        user.setEnabled(userRep.isEnabled() != null && userRep.isEnabled());
        user.setCreatedTimestamp(userRep.getCreatedTimestamp());
        user.setEmail(userRep.getEmail());
        if (userRep.isEmailVerified() != null) user.setEmailVerified(userRep.isEmailVerified());
        user.setFirstName(userRep.getFirstName());
        user.setLastName(userRep.getLastName());
        user.setFederationLink(userRep.getFederationLink());
        if (userRep.isTotp() != null) user.setOtpEnabled(userRep.isTotp());
        if (userRep.getAttributes() != null) {
            for (Map.Entry<String, Object> entry : userRep.getAttributes().entrySet()) {
                Object value = entry.getValue();

                if (value instanceof Collection) {
                    Collection<String> colVal = (Collection<String>) value;
                    user.setAttribute(entry.getKey(), new ArrayList<>(colVal));
                } else if (value instanceof String) {
                    // TODO: This is here just for backwards compatibility with KC 1.3 and earlier
                    String stringVal = (String) value;
                    user.setSingleAttribute(entry.getKey(), stringVal);
                }
            }
        }
        if (userRep.getRequiredActions() != null) {
            for (String requiredAction : userRep.getRequiredActions()) {
                user.addRequiredAction(UserModel.RequiredAction.valueOf(requiredAction));
            }
        }
        createCredentials(userRep, user);
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
                session.userStorage().addConsent(newRealm, user, consentModel);
            }
        }
        if (userRep.getServiceAccountClientId() != null) {
            String clientId = userRep.getServiceAccountClientId();
            ClientModel client = newRealm.getClientByClientId(clientId);
            if (client == null) {
                throw new RuntimeException("Unable to find client specified for service account link. Client: " + clientId);
            }
            user.setServiceAccountClientLink(client.getId());;
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

    public static void createCredentials(UserRepresentation userRep, UserModel user) {
        if (userRep.getCredentials() != null) {
            for (CredentialRepresentation cred : userRep.getCredentials()) {
                updateCredential(user, cred);
            }
        }
    }

    // Detect if it is "plain-text" or "hashed" representation and update model according to it
    private static void updateCredential(UserModel user, CredentialRepresentation cred) {
        if (cred.getValue() != null) {
            UserCredentialModel plainTextCred = convertCredential(cred);
            user.updateCredential(plainTextCred);
        } else {
            UserCredentialValueModel hashedCred = new UserCredentialValueModel();
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
                    hashedCred.setAlgorithm(Pbkdf2PasswordHashProvider.ID);
                } else {
                    hashedCred.setAlgorithm(cred.getAlgorithm());
                }

            } else {
                if (UserCredentialModel.PASSWORD.equals(cred.getType()) || UserCredentialModel.PASSWORD_HISTORY.equals(cred.getType())) {
                    hashedCred.setAlgorithm(Pbkdf2PasswordHashProvider.ID);
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
            user.updateCredentialDirectly(hashedCred);
        }
    }

    public static UserCredentialModel convertCredential(CredentialRepresentation cred) {
        UserCredentialModel credential = new UserCredentialModel();
        credential.setType(cred.getType());
        credential.setValue(cred.getValue());
        return credential;
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
        identityProviderModel.setProviderId(representation.getProviderId());
        identityProviderModel.setEnabled(representation.isEnabled());
        identityProviderModel.setTrustEmail(representation.isTrustEmail());
        identityProviderModel.setAuthenticateByDefault(representation.isAuthenticateByDefault());
        identityProviderModel.setStoreToken(representation.isStoreToken());
        identityProviderModel.setAddReadTokenRoleOnCreate(representation.isAddReadTokenRoleOnCreate());
        identityProviderModel.setConfig(representation.getConfig());

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


    public static ComponentModel toModel(ComponentRepresentation rep) {
        ComponentModel model = new ComponentModel();
        model.setParentId(rep.getParentId());
        model.setProviderType(rep.getProviderType());
        model.setProviderId(rep.getProviderId());
        model.setConfig(rep.getConfig());
        model.setName(rep.getName());
        return model;
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

        StoreFactory storeFactory = authorization.getStoreFactory();
        ScopeStore scopeStore = storeFactory.getScopeStore();

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

        rep.getPolicies().forEach(policyRepresentation -> {
            Map<String, String> config = policyRepresentation.getConfig();

            String roles = config.get("roles");

            if (roles != null && !roles.isEmpty()) {
                try {
                    List<Map> rolesMap = JsonSerialization.readValue(roles, List.class);
                    config.put("roles", JsonSerialization.writeValueAsString(rolesMap.stream().map(roleConfig -> {
                        String roleName = roleConfig.get("id").toString();
                        String clientId = null;
                        int clientIdSeparator = roleName.indexOf("/");

                        if (clientIdSeparator != -1) {
                            clientId = roleName.substring(0, clientIdSeparator);
                            roleName = roleName.substring(clientIdSeparator + 1);
                        }

                        RoleModel role;

                        if (clientId == null) {
                            role = realm.getRole(roleName);
                        } else {
                            role = realm.getClientByClientId(clientId).getRole(roleName);
                        }

                        // fallback to find any client role with the given name
                        if (role == null) {
                            String finalRoleName = roleName;
                            role = realm.getClients().stream().map(clientModel -> clientModel.getRole(finalRoleName)).filter(roleModel -> roleModel != null)
                                    .findFirst().orElse(null);
                        }

                        if (role == null) {
                            throw new RuntimeException("Error while importing configuration. Role [" + roleName + "] could not be found.");
                        }

                        roleConfig.put("id", role.getId());
                        return roleConfig;
                    }).collect(Collectors.toList())));
                } catch (Exception e) {
                    throw new RuntimeException("Error while exporting policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            String users = config.get("users");

            if (users != null && !users.isEmpty()) {
                try {
                    List<String> usersMap = JsonSerialization.readValue(users, List.class);
                    config.put("users", JsonSerialization.writeValueAsString(usersMap.stream().map(userName -> session.users().getUserByUsername(userName, realm).getId()).collect(Collectors.toList())));
                } catch (Exception e) {
                    throw new RuntimeException("Error while exporting policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            String scopes = config.get("scopes");

            if (scopes != null && !scopes.isEmpty()) {
                try {
                    List<String> scopesMap = JsonSerialization.readValue(scopes, List.class);
                    config.put("scopes", JsonSerialization.writeValueAsString(scopesMap.stream().map(scopeName -> {
                        Scope newScope = scopeStore.findByName(scopeName, resourceServer.getId());

                        if (newScope == null) {
                            throw new RuntimeException("Scope with name [" + scopeName + "] not defined.");
                        }

                        return newScope.getId();
                    }).collect(Collectors.toList())));
                } catch (Exception e) {
                    throw new RuntimeException("Error while exporting policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            String policyResources = config.get("resources");

            if (policyResources != null && !policyResources.isEmpty()) {
                ResourceStore resourceStore = storeFactory.getResourceStore();
                try {
                    List<String> resources = JsonSerialization.readValue(policyResources, List.class);
                    config.put("resources", JsonSerialization.writeValueAsString(resources.stream().map(resourceName -> {
                        return resourceStore.findByName(resourceName, resourceServer.getId()).getId();
                    }).collect(Collectors.toList())));
                } catch (Exception e) {
                    throw new RuntimeException("Error while exporting policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            String applyPolicies = config.get("applyPolicies");

            if (applyPolicies != null && !applyPolicies.isEmpty()) {
                PolicyStore policyStore = storeFactory.getPolicyStore();
                try {
                    List<String> policies = JsonSerialization.readValue(applyPolicies, List.class);
                    config.put("applyPolicies", JsonSerialization.writeValueAsString(policies.stream().map(policyName -> {
                        Policy policy = policyStore.findByName(policyName, resourceServer.getId());

                        if (policy == null) {
                            throw new RuntimeException("Policy with name [" + policyName + "] not defined.");
                        }

                        return policy.getId();
                    }).collect(Collectors.toList())));
                } catch (Exception e) {
                    throw new RuntimeException("Error while exporting policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            toModel(policyRepresentation, resourceServer, authorization);
        });
    }

    public static Policy toModel(PolicyRepresentation policy, ResourceServer resourceServer, AuthorizationProvider authorization) {
        PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();
        Policy existing;

        if (policy.getId() != null) {
            existing = policyStore.findById(policy.getId());
        } else {
            existing = policyStore.findByName(policy.getName(), resourceServer.getId());
        }

        if (existing != null) {
            existing.setName(policy.getName());
            existing.setDescription(policy.getDescription());
            existing.setConfig(policy.getConfig());
            existing.setDecisionStrategy(policy.getDecisionStrategy());
            existing.setLogic(policy.getLogic());

            updateResources(existing, authorization);
            updateAssociatedPolicies(existing, resourceServer, authorization);
            updateScopes(existing, authorization);

            return existing;
        }

        Policy model = policyStore.create(policy.getName(), policy.getType(), resourceServer);

        model.setDescription(policy.getDescription());
        model.setDecisionStrategy(policy.getDecisionStrategy());
        model.setLogic(policy.getLogic());
        model.setConfig(policy.getConfig());

        updateResources(model, authorization);
        updateAssociatedPolicies(model, resourceServer, authorization);
        updateScopes(model, authorization);

        policy.setId(model.getId());

        return model;
    }

    private static void updateScopes(Policy policy, AuthorizationProvider authorization) {
        String scopes = policy.getConfig().get("scopes");
        if (scopes != null) {
            String[] scopeIds;

            try {
                scopeIds = JsonSerialization.readValue(scopes, String[].class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            StoreFactory storeFactory = authorization.getStoreFactory();

            for (String scopeId : scopeIds) {
                boolean hasScope = false;

                for (Scope scopeModel : new HashSet<Scope>(policy.getScopes())) {
                    if (scopeModel.getId().equals(scopeId)) {
                        hasScope = true;
                    }
                }
                if (!hasScope) {
                    policy.addScope(storeFactory.getScopeStore().findById(scopeId));
                }
            }

            for (Scope scopeModel : new HashSet<Scope>(policy.getScopes())) {
                boolean hasScope = false;

                for (String scopeId : scopeIds) {
                    if (scopeModel.getId().equals(scopeId)) {
                        hasScope = true;
                    }
                }
                if (!hasScope) {
                    policy.removeScope(scopeModel);
                }
            }
        }
    }

    private static void updateAssociatedPolicies(Policy policy, ResourceServer resourceServer, AuthorizationProvider authorization) {
        String policies = policy.getConfig().get("applyPolicies");

        if (policies != null) {
            String[] policyIds;

            try {
                policyIds = JsonSerialization.readValue(policies, String[].class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            StoreFactory storeFactory = authorization.getStoreFactory();
            PolicyStore policyStore = storeFactory.getPolicyStore();

            for (String policyId : policyIds) {
                boolean hasPolicy = false;

                for (Policy policyModel : new HashSet<Policy>(policy.getAssociatedPolicies())) {
                    if (policyModel.getId().equals(policyId) || policyModel.getName().equals(policyId)) {
                        hasPolicy = true;
                    }
                }


                if (!hasPolicy) {
                    Policy associatedPolicy = policyStore.findById(policyId);

                    if (associatedPolicy == null) {
                        associatedPolicy = policyStore.findByName(policyId, resourceServer.getId());
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
                    policy.removeAssociatedPolicy(policyModel);;
                }
            }
        }
    }

    private static void updateResources(Policy policy, AuthorizationProvider authorization) {
        String resources = policy.getConfig().get("resources");
        if (resources != null) {
            String[] resourceIds;

            try {
                resourceIds = JsonSerialization.readValue(resources, String[].class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            StoreFactory storeFactory = authorization.getStoreFactory();

            for (String resourceId : resourceIds) {
                boolean hasResource = false;
                for (Resource resourceModel : new HashSet<Resource>(policy.getResources())) {
                    if (resourceModel.getId().equals(resourceId)) {
                        hasResource = true;
                    }
                }
                if (!hasResource && !"".equals(resourceId)) {
                    policy.addResource(storeFactory.getResourceStore().findById(resourceId));
                }
            }

            for (Resource resourceModel : new HashSet<Resource>(policy.getResources())) {
                boolean hasResource = false;

                for (String resourceId : resourceIds) {
                    if (resourceModel.getId().equals(resourceId)) {
                        hasResource = true;
                    }
                }

                if (!hasResource) {
                    policy.removeResource(resourceModel);
                }
            }
        }
    }

    public static Resource toModel(ResourceRepresentation resource, ResourceServer resourceServer, AuthorizationProvider authorization) {
        ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();
        Resource existing;

        if (resource.getId() != null) {
            existing = resourceStore.findById(resource.getId());
        } else {
            existing = resourceStore.findByName(resource.getName(), resourceServer.getId());
        }

        if (existing != null) {
            existing.setName(resource.getName());
            existing.setType(resource.getType());
            existing.setUri(resource.getUri());
            existing.setIconUri(resource.getIconUri());

            existing.updateScopes(resource.getScopes().stream()
                    .map((ScopeRepresentation scope) -> toModel(scope,  resourceServer, authorization))
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
            existing = scopeStore.findById(scope.getId());
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
}
