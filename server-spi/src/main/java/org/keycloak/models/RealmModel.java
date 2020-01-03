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

package org.keycloak.models;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentModel;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.client.ClientStorageProviderModel;

import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmModel extends RoleContainerModel {
    interface RealmCreationEvent extends ProviderEvent {
        RealmModel getCreatedRealm();
        KeycloakSession getKeycloakSession();
    }

    interface RealmPostCreateEvent extends ProviderEvent {
        RealmModel getCreatedRealm();
        KeycloakSession getKeycloakSession();
    }

    interface RealmRemovedEvent extends ProviderEvent {
        RealmModel getRealm();
        KeycloakSession getKeycloakSession();
    }

    interface ClientCreationEvent extends ProviderEvent {
        ClientModel getCreatedClient();
    }

    // Called also during client creation after client is fully initialized (including all attributes etc)
    interface ClientUpdatedEvent extends ProviderEvent {
        ClientModel getUpdatedClient();
        KeycloakSession getKeycloakSession();
    }

    interface ClientRemovedEvent extends ProviderEvent {
        ClientModel getClient();
        KeycloakSession getKeycloakSession();
    }

    interface IdentityProviderUpdatedEvent extends ProviderEvent {
        RealmModel getRealm();
        IdentityProviderModel getUpdatedIdentityProvider();
        KeycloakSession getKeycloakSession();
    }

    interface IdentityProviderRemovedEvent extends ProviderEvent {
        RealmModel getRealm();
        IdentityProviderModel getRemovedIdentityProvider();
        KeycloakSession getKeycloakSession();
    }

    String getId();

    String getName();

    void setName(String name);

    String getDisplayName();

    void setDisplayName(String displayName);

    String getDisplayNameHtml();

    void setDisplayNameHtml(String displayNameHtml);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    SslRequired getSslRequired();

    void setSslRequired(SslRequired sslRequired);

    boolean isRegistrationAllowed();

    void setRegistrationAllowed(boolean registrationAllowed);

    boolean isRegistrationEmailAsUsername();

    void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername);

    boolean isRememberMe();

    void setRememberMe(boolean rememberMe);

    boolean isEditUsernameAllowed();

    void setEditUsernameAllowed(boolean editUsernameAllowed);

    boolean isUserManagedAccessAllowed();

    void setUserManagedAccessAllowed(boolean userManagedAccessAllowed);

    void setAttribute(String name, String value);
    void setAttribute(String name, Boolean value);
    void setAttribute(String name, Integer value);
    void setAttribute(String name, Long value);
    void removeAttribute(String name);
    String getAttribute(String name);
    Integer getAttribute(String name, Integer defaultValue);
    Long getAttribute(String name, Long defaultValue);
    Boolean getAttribute(String name, Boolean defaultValue);
    Map<String, String> getAttributes();

    //--- brute force settings
    boolean isBruteForceProtected();
    void setBruteForceProtected(boolean value);
    boolean isPermanentLockout();
    void setPermanentLockout(boolean val);
    int getMaxFailureWaitSeconds();
    void setMaxFailureWaitSeconds(int val);
    int getWaitIncrementSeconds();
    void setWaitIncrementSeconds(int val);
    int getMinimumQuickLoginWaitSeconds();
    void setMinimumQuickLoginWaitSeconds(int val);
    long getQuickLoginCheckMilliSeconds();
    void setQuickLoginCheckMilliSeconds(long val);
    int getMaxDeltaTimeSeconds();
    void setMaxDeltaTimeSeconds(int val);
    int getFailureFactor();
    void setFailureFactor(int failureFactor);
    //--- end brute force settings


    boolean isVerifyEmail();

    void setVerifyEmail(boolean verifyEmail);

    boolean isLoginWithEmailAllowed();

    void setLoginWithEmailAllowed(boolean loginWithEmailAllowed);

    boolean isDuplicateEmailsAllowed();

    void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed);

    boolean isResetPasswordAllowed();

    void setResetPasswordAllowed(boolean resetPasswordAllowed);

    String getDefaultSignatureAlgorithm();
    void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm);

    boolean isRevokeRefreshToken();
    void setRevokeRefreshToken(boolean revokeRefreshToken);

    int getRefreshTokenMaxReuse();
    void setRefreshTokenMaxReuse(int revokeRefreshTokenCount);

    int getSsoSessionIdleTimeout();
    void setSsoSessionIdleTimeout(int seconds);

    int getSsoSessionMaxLifespan();
    void setSsoSessionMaxLifespan(int seconds);

    int getSsoSessionIdleTimeoutRememberMe();
    void setSsoSessionIdleTimeoutRememberMe(int seconds);

    int getSsoSessionMaxLifespanRememberMe();
    void setSsoSessionMaxLifespanRememberMe(int seconds);

    int getOfflineSessionIdleTimeout();
    void setOfflineSessionIdleTimeout(int seconds);

    int getAccessTokenLifespan();

    // KEYCLOAK-7688 Offline Session Max for Offline Token
    boolean isOfflineSessionMaxLifespanEnabled();
    void setOfflineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled);

    int getOfflineSessionMaxLifespan();
    void setOfflineSessionMaxLifespan(int seconds);

    void setAccessTokenLifespan(int seconds);

    int getAccessTokenLifespanForImplicitFlow();
    void setAccessTokenLifespanForImplicitFlow(int seconds);

    int getAccessCodeLifespan();

    void setAccessCodeLifespan(int seconds);

    int getAccessCodeLifespanUserAction();

    void setAccessCodeLifespanUserAction(int seconds);

    /**
     * This method will return a map with all the lifespans available
     * or an empty map, but never null.
     * @return map with user action token lifespans
     */
    Map<String, Integer> getUserActionTokenLifespans();

    int getAccessCodeLifespanLogin();

    void setAccessCodeLifespanLogin(int seconds);

    int getActionTokenGeneratedByAdminLifespan();
    void setActionTokenGeneratedByAdminLifespan(int seconds);

    int getActionTokenGeneratedByUserLifespan();
    void setActionTokenGeneratedByUserLifespan(int seconds);

    int getActionTokenGeneratedByUserLifespan(String actionTokenType);
    void setActionTokenGeneratedByUserLifespan(String actionTokenType, Integer seconds);

    List<RequiredCredentialModel> getRequiredCredentials();

    void addRequiredCredential(String cred);

    PasswordPolicy getPasswordPolicy();

    void setPasswordPolicy(PasswordPolicy policy);

    OTPPolicy getOTPPolicy();
    void setOTPPolicy(OTPPolicy policy);

    /**
     * @return  WebAuthn policy for 2-factor authentication
     */
    WebAuthnPolicy getWebAuthnPolicy();

    /**
     * Set WebAuthn policy for 2-factor authentication
     *
     * @param policy
     */
    void setWebAuthnPolicy(WebAuthnPolicy policy);

    /**
     *
     * @return WebAuthn passwordless policy below. This is temporary and will be removed later.
     */
    WebAuthnPolicy getWebAuthnPolicyPasswordless();

    /**
     * Set WebAuthn passwordless policy below. This is temporary and will be removed later.
     * @param policy
     */
    void setWebAuthnPolicyPasswordless(WebAuthnPolicy policy);

    RoleModel getRoleById(String id);

    List<GroupModel> getDefaultGroups();

    void addDefaultGroup(GroupModel group);

    void removeDefaultGroup(GroupModel group);

    List<ClientModel> getClients();
    List<ClientModel> getClients(Integer firstResult, Integer maxResults);
    Long getClientsCount();

    List<ClientModel> getAlwaysDisplayInConsoleClients();

    ClientModel addClient(String name);

    ClientModel addClient(String id, String clientId);

    boolean removeClient(String id);

    ClientModel getClientById(String id);
    ClientModel getClientByClientId(String clientId);
    List<ClientModel> searchClientByClientId(String clientId, Integer firstResult, Integer maxResults);
    
    void updateRequiredCredentials(Set<String> creds);

    Map<String, String> getBrowserSecurityHeaders();
    void setBrowserSecurityHeaders(Map<String, String> headers);

    Map<String, String> getSmtpConfig();

    void setSmtpConfig(Map<String, String> smtpConfig);

    AuthenticationFlowModel getBrowserFlow();
    void setBrowserFlow(AuthenticationFlowModel flow);

    AuthenticationFlowModel getRegistrationFlow();
    void setRegistrationFlow(AuthenticationFlowModel flow);

    AuthenticationFlowModel getDirectGrantFlow();
    void setDirectGrantFlow(AuthenticationFlowModel flow);

    AuthenticationFlowModel getResetCredentialsFlow();
    void setResetCredentialsFlow(AuthenticationFlowModel flow);

    AuthenticationFlowModel getClientAuthenticationFlow();
    void setClientAuthenticationFlow(AuthenticationFlowModel flow);

    AuthenticationFlowModel getDockerAuthenticationFlow();
    void setDockerAuthenticationFlow(AuthenticationFlowModel flow);

    List<AuthenticationFlowModel> getAuthenticationFlows();
    AuthenticationFlowModel getFlowByAlias(String alias);
    AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model);
    AuthenticationFlowModel getAuthenticationFlowById(String id);
    void removeAuthenticationFlow(AuthenticationFlowModel model);
    void updateAuthenticationFlow(AuthenticationFlowModel model);

    List<AuthenticationExecutionModel> getAuthenticationExecutions(String flowId);
    AuthenticationExecutionModel getAuthenticationExecutionById(String id);
    AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId);
    AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model);
    void updateAuthenticatorExecution(AuthenticationExecutionModel model);
    void removeAuthenticatorExecution(AuthenticationExecutionModel model);


    List<AuthenticatorConfigModel> getAuthenticatorConfigs();
    AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model);
    void updateAuthenticatorConfig(AuthenticatorConfigModel model);
    void removeAuthenticatorConfig(AuthenticatorConfigModel model);
    AuthenticatorConfigModel getAuthenticatorConfigById(String id);
    AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias);

    List<RequiredActionProviderModel> getRequiredActionProviders();
    RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model);
    void updateRequiredActionProvider(RequiredActionProviderModel model);
    void removeRequiredActionProvider(RequiredActionProviderModel model);
    RequiredActionProviderModel getRequiredActionProviderById(String id);
    RequiredActionProviderModel getRequiredActionProviderByAlias(String alias);

    List<IdentityProviderModel> getIdentityProviders();
    IdentityProviderModel getIdentityProviderByAlias(String alias);
    void addIdentityProvider(IdentityProviderModel identityProvider);
    void removeIdentityProviderByAlias(String alias);
    void updateIdentityProvider(IdentityProviderModel identityProvider);
    Set<IdentityProviderMapperModel> getIdentityProviderMappers();
    Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(String brokerAlias);
    IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model);
    void removeIdentityProviderMapper(IdentityProviderMapperModel mapping);
    void updateIdentityProviderMapper(IdentityProviderMapperModel mapping);
    IdentityProviderMapperModel getIdentityProviderMapperById(String id);
    IdentityProviderMapperModel getIdentityProviderMapperByName(String brokerAlias, String name);


    /**
     * Adds component model.  Will call onCreate() method of ComponentFactory
     *
     * @param model
     * @return
     */
    ComponentModel addComponentModel(ComponentModel model);

    /**
     * Adds component model.  Will NOT call onCreate() method of ComponentFactory
     *
     * @param model
     * @return
     */
    ComponentModel importComponentModel(ComponentModel model);

    void updateComponent(ComponentModel component);
    void removeComponent(ComponentModel component);
    void removeComponents(String parentId);
    List<ComponentModel> getComponents(String parentId, String providerType);

    List<ComponentModel> getComponents(String parentId);

    List<ComponentModel> getComponents();
    ComponentModel getComponent(String id);

    default
    List<UserStorageProviderModel> getUserStorageProviders() {
        List<UserStorageProviderModel> list = new LinkedList<>();
        for (ComponentModel component : getComponents(getId(), UserStorageProvider.class.getName())) {
            list.add(new UserStorageProviderModel(component));
        }
        Collections.sort(list, UserStorageProviderModel.comparator);
        return list;
    }

    default
    List<ClientStorageProviderModel> getClientStorageProviders() {
        List<ClientStorageProviderModel> list = new LinkedList<>();
        for (ComponentModel component : getComponents(getId(), ClientStorageProvider.class.getName())) {
            list.add(new ClientStorageProviderModel(component));
        }
        Collections.sort(list, ClientStorageProviderModel.comparator);
        return list;
    }

    String getLoginTheme();

    void setLoginTheme(String name);

    String getAccountTheme();

    void setAccountTheme(String name);

    String getAdminTheme();

    void setAdminTheme(String name);

    String getEmailTheme();

    void setEmailTheme(String name);


    /**
     * Time in seconds since epoc
     *
     * @return
     */
    int getNotBefore();

    void setNotBefore(int notBefore);

    boolean isEventsEnabled();

    void setEventsEnabled(boolean enabled);

//    boolean isPersistUserSessions();
//
//    void setPersistUserSessions();

    long getEventsExpiration();

    void setEventsExpiration(long expiration);

    Set<String> getEventsListeners();

    void setEventsListeners(Set<String> listeners);

    Set<String> getEnabledEventTypes();

    void setEnabledEventTypes(Set<String> enabledEventTypes);

    boolean isAdminEventsEnabled();

    void setAdminEventsEnabled(boolean enabled);

    boolean isAdminEventsDetailsEnabled();

    void setAdminEventsDetailsEnabled(boolean enabled);

    ClientModel getMasterAdminClient();

    void setMasterAdminClient(ClientModel client);

    boolean isIdentityFederationEnabled();

    boolean isInternationalizationEnabled();
    void setInternationalizationEnabled(boolean enabled);
    Set<String> getSupportedLocales();
    void setSupportedLocales(Set<String> locales);
    String getDefaultLocale();
    void setDefaultLocale(String locale);

    GroupModel createGroup(String name);
    GroupModel createGroup(String id, String name);

    GroupModel getGroupById(String id);
    List<GroupModel> getGroups();
    Long getGroupsCount(Boolean onlyTopGroups);
    Long getGroupsCountByNameContaining(String search);
    List<GroupModel> getTopLevelGroups();
    List<GroupModel> getTopLevelGroups(Integer first, Integer max);
    List<GroupModel> searchForGroupByName(String search, Integer first, Integer max);
    boolean removeGroup(GroupModel group);
    void moveGroup(GroupModel group, GroupModel toParent);

    List<ClientScopeModel> getClientScopes();

    ClientScopeModel addClientScope(String name);

    ClientScopeModel addClientScope(String id, String name);

    boolean removeClientScope(String id);

    ClientScopeModel getClientScopeById(String id);

    void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope);
    void removeDefaultClientScope(ClientScopeModel clientScope);
    List<ClientScopeModel> getDefaultClientScopes(boolean defaultScope);

}
