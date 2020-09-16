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
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.client.ClientStorageProviderModel;
import org.keycloak.storage.role.RoleStorageProvider;
import org.keycloak.storage.role.RoleStorageProviderModel;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    int getClientSessionIdleTimeout();
    void setClientSessionIdleTimeout(int seconds);

    int getClientSessionMaxLifespan();
    void setClientSessionMaxLifespan(int seconds);

    int getClientOfflineSessionIdleTimeout();
    void setClientOfflineSessionIdleTimeout(int seconds);

    int getClientOfflineSessionMaxLifespan();
    void setClientOfflineSessionMaxLifespan(int seconds);

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

    /**
     * @deprecated Use {@link #getRequiredCredentialsStream()  getRequiredCredentialsStream} instead.
     */
    @Deprecated
    default List<RequiredCredentialModel> getRequiredCredentials() {
        return getRequiredCredentialsStream().collect(Collectors.toList());
    }

    Stream<RequiredCredentialModel> getRequiredCredentialsStream();

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

    @Deprecated
    default List<GroupModel> getDefaultGroups() {
        return getDefaultGroupsStream().collect(Collectors.toList());
    }

    Stream<GroupModel> getDefaultGroupsStream();

    void addDefaultGroup(GroupModel group);

    void removeDefaultGroup(GroupModel group);

    @Deprecated
    default List<ClientModel> getClients() {
        return getClientsStream(null, null).collect(Collectors.toList());
    }

    Stream<ClientModel> getClientsStream();

    @Deprecated
    default List<ClientModel> getClients(Integer firstResult, Integer maxResults) {
        return getClientsStream(firstResult, maxResults).collect(Collectors.toList());
    }

    Stream<ClientModel> getClientsStream(Integer firstResult, Integer maxResults);

    Long getClientsCount();

    @Deprecated
    default List<ClientModel> getAlwaysDisplayInConsoleClients() {
        return getAlwaysDisplayInConsoleClientsStream().collect(Collectors.toList());
    }

    Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream();

    ClientModel addClient(String name);

    ClientModel addClient(String id, String clientId);

    boolean removeClient(String id);

    ClientModel getClientById(String id);
    ClientModel getClientByClientId(String clientId);

    @Deprecated
    default List<ClientModel> searchClientByClientId(String clientId, Integer firstResult, Integer maxResults) {
        return searchClientByClientIdStream(clientId, firstResult, maxResults).collect(Collectors.toList());
    }

    Stream<ClientModel> searchClientByClientIdStream(String clientId, Integer firstResult, Integer maxResults);
    
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

    /**
     * @deprecated Use {@link #getAuthenticationFlowsStream()  getAuthenticationFlowsStream} instead.
     */
    @Deprecated
    default List<AuthenticationFlowModel> getAuthenticationFlows() {
        return getAuthenticationFlowsStream().collect(Collectors.toList());
    }

    Stream<AuthenticationFlowModel> getAuthenticationFlowsStream();

    AuthenticationFlowModel getFlowByAlias(String alias);
    AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model);
    AuthenticationFlowModel getAuthenticationFlowById(String id);
    void removeAuthenticationFlow(AuthenticationFlowModel model);
    void updateAuthenticationFlow(AuthenticationFlowModel model);

    /**
     * @deprecated Use {@link #getAuthenticationExecutionsStream(String) getAuthenticationExecutionsStream} instead.
     */
    @Deprecated
    default List<AuthenticationExecutionModel> getAuthenticationExecutions(String flowId) {
        return getAuthenticationExecutionsStream(flowId).collect(Collectors.toList());
    }

    /**
     * Returns sorted {@link AuthenticationExecutionModel AuthenticationExecutionModel} as a stream.
     * It should be used with forEachOrdered if the ordering is required.
     * @return Sorted stream
     */
    Stream<AuthenticationExecutionModel> getAuthenticationExecutionsStream(String flowId);

    AuthenticationExecutionModel getAuthenticationExecutionById(String id);
    AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId);
    AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model);
    void updateAuthenticatorExecution(AuthenticationExecutionModel model);
    void removeAuthenticatorExecution(AuthenticationExecutionModel model);

    /**
     * @deprecated Use {@link #getAuthenticatorConfigsStream() getAuthenticatorConfigsStream} instead.
     */
    @Deprecated
    default List<AuthenticatorConfigModel> getAuthenticatorConfigs() {
        return getAuthenticatorConfigsStream().collect(Collectors.toList());
    }

    Stream<AuthenticatorConfigModel> getAuthenticatorConfigsStream();

    AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model);
    void updateAuthenticatorConfig(AuthenticatorConfigModel model);
    void removeAuthenticatorConfig(AuthenticatorConfigModel model);
    AuthenticatorConfigModel getAuthenticatorConfigById(String id);
    AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias);

    /**
     * @deprecated Use {@link #getRequiredActionProvidersStream() getRequiredActionProvidersStream} instead.
     */
    @Deprecated
    default List<RequiredActionProviderModel> getRequiredActionProviders() {
        return getRequiredActionProvidersStream().collect(Collectors.toList());
    }

    /**
     * Returns sorted {@link RequiredActionProviderModel RequiredActionProviderModel} as a stream.
     * It should be used with forEachOrdered if the ordering is required.
     * @return Sorted stream
     */
    Stream<RequiredActionProviderModel> getRequiredActionProvidersStream();

    RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model);
    void updateRequiredActionProvider(RequiredActionProviderModel model);
    void removeRequiredActionProvider(RequiredActionProviderModel model);
    RequiredActionProviderModel getRequiredActionProviderById(String id);
    RequiredActionProviderModel getRequiredActionProviderByAlias(String alias);

    /**
     * @deprecated Use {@link #getIdentityProvidersStream() getIdentityProvidersStream} instead.
     */
    @Deprecated
    default List<IdentityProviderModel> getIdentityProviders() {
        return getIdentityProvidersStream().collect(Collectors.toList());
    }

    Stream<IdentityProviderModel> getIdentityProvidersStream();

    IdentityProviderModel getIdentityProviderByAlias(String alias);
    void addIdentityProvider(IdentityProviderModel identityProvider);
    void removeIdentityProviderByAlias(String alias);
    void updateIdentityProvider(IdentityProviderModel identityProvider);

    /**
     * @deprecated Use {@link #getIdentityProviderMappersStream() getIdentityProviderMappersStream} instead.
     */
    @Deprecated
    default Set<IdentityProviderMapperModel> getIdentityProviderMappers() {
        return getIdentityProviderMappersStream().collect(Collectors.toSet());
    }

    Stream<IdentityProviderMapperModel> getIdentityProviderMappersStream();

    /**
     * @deprecated Use {@link #getIdentityProviderMappersByAliasStream(String) getIdentityProviderMappersByAliasStream} instead.
     */
    @Deprecated
    default Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(String brokerAlias) {
        return getIdentityProviderMappersByAliasStream(brokerAlias).collect(Collectors.toSet());
    }

    Stream<IdentityProviderMapperModel> getIdentityProviderMappersByAliasStream(String brokerAlias);

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

    /**
     * @deprecated Use {@link #getComponentsStream(String, String) getComponentsStream} instead.
     */
    @Deprecated
    default List<ComponentModel> getComponents(String parentId, String providerType) {
        return getComponentsStream(parentId, providerType).collect(Collectors.toList());
    }


    /**
     * Returns stream of ComponentModels for specific parentId and providerType.
     * @param parentId id of parent
     * @param providerType type of provider
     * @return stream of ComponentModels
     */
    Stream<ComponentModel> getComponentsStream(String parentId, String providerType);

    Stream<ComponentModel> getComponentsStream(String parentId);

    /**
     * @deprecated Use {@link #getComponentsStream(String) getComponentsStream} instead.
     */
    @Deprecated
    default List<ComponentModel> getComponents(String parentId) {
        return getComponentsStream(parentId).collect(Collectors.toList());
    }

    /**
     * @deprecated Use {@link #getComponentsStream() getComponentsStream} instead.
     */
    @Deprecated
    default List<ComponentModel> getComponents() {
        return getComponentsStream().collect(Collectors.toList());
    }

    Stream<ComponentModel> getComponentsStream();

    ComponentModel getComponent(String id);

    /**
     * @deprecated Use {@link #getUserStorageProvidersStream() getUserStorageProvidersStream} instead.
     */
    @Deprecated
    default List<UserStorageProviderModel> getUserStorageProviders() {
        return getUserStorageProvidersStream().collect(Collectors.toList());
    }

    /**
     * Returns sorted {@link UserStorageProviderModel UserStorageProviderModel} as a stream.
     * It should be used with forEachOrdered if the ordering is required.
     * @return Sorted stream
     */
    default Stream<UserStorageProviderModel> getUserStorageProvidersStream() {
        return getComponentsStream(getId(), UserStorageProvider.class.getName())
                .map(UserStorageProviderModel::new)
                .sorted(UserStorageProviderModel.comparator);
    }

    /**
     * @deprecated Use {@link #getClientStorageProvidersStream() getClientStorageProvidersStream} instead.
     */
    @Deprecated
    default List<ClientStorageProviderModel> getClientStorageProviders() {
        return getClientStorageProvidersStream().collect(Collectors.toList());
    }

    /**
     * Returns sorted {@link ClientStorageProviderModel ClientStorageProviderModel} as a stream.
     * It should be used with forEachOrdered if the ordering is required.
     * @return Sorted stream
     */
    default Stream<ClientStorageProviderModel> getClientStorageProvidersStream() {
        return getComponentsStream(getId(), ClientStorageProvider.class.getName())
                .map(ClientStorageProviderModel::new)
                .sorted(ClientStorageProviderModel.comparator);
    }

    /**
     * @deprecated Use {@link #getRoleStorageProvidersStream() getRoleStorageProvidersStream} instead.
     */
    @Deprecated
    default List<RoleStorageProviderModel> getRoleStorageProviders() {
        return getRoleStorageProvidersStream().collect(Collectors.toList());
    }

    /**
     * Returns sorted {@link RoleStorageProviderModel RoleStorageProviderModel} as a stream.
     * It should be used with forEachOrdered if the ordering is required.
     * @return Sorted stream
     */
    default Stream<RoleStorageProviderModel> getRoleStorageProvidersStream() {
        return getComponentsStream(getId(), RoleStorageProvider.class.getName())
                .map(RoleStorageProviderModel::new)
                .sorted(RoleStorageProviderModel.comparator);
    }

    /**
     * Returns stream of ComponentModels that represent StorageProviders for class storageProviderClass in this realm
     * @param storageProviderClass class
     * @return stream of StorageProviders
     */
    default Stream<ComponentModel> getStorageProviders(Class<? extends Provider> storageProviderClass) {
        return getComponentsStream(getId(), storageProviderClass.getName());
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

    /**
     * @deprecated Use {@link #getEventsListenersStream() getEventsListenersStream} instead.
     */
    @Deprecated
    default Set<String> getEventsListeners() {
        return getEventsListenersStream().collect(Collectors.toSet());
    }

    Stream<String> getEventsListenersStream();

    void setEventsListeners(Set<String> listeners);

    /**
     * @deprecated Use {@link #getEnabledEventTypesStream() getEnabledEventTypesStream} instead.
     */
    @Deprecated
    default Set<String> getEnabledEventTypes() {
        return getEnabledEventTypesStream().collect(Collectors.toSet());
    }

    Stream<String> getEnabledEventTypesStream();

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

    /**
     * @deprecated Use {@link #getSupportedLocalesStream() getSupportedLocalesStream} instead.
     */
    @Deprecated
    default Set<String> getSupportedLocales() {
        return getSupportedLocalesStream().collect(Collectors.toSet());
    }

    Stream<String> getSupportedLocalesStream();

    void setSupportedLocales(Set<String> locales);
    String getDefaultLocale();
    void setDefaultLocale(String locale);

    default GroupModel createGroup(String name) {
        return createGroup(null, name, null);
    };

    default GroupModel createGroup(String id, String name) {
        return createGroup(id, name, null);
    };

    default GroupModel createGroup(String name, GroupModel toParent) {
        return createGroup(null, name, toParent);
    };

    GroupModel createGroup(String id, String name, GroupModel toParent);

    GroupModel getGroupById(String id);

    /**
     * @deprecated Use {@link #getGroupsStream() getGroupsStream} instead.
     */
    @Deprecated
    default List<GroupModel> getGroups() {
        return getGroupsStream().collect(Collectors.toList());
    }

    Stream<GroupModel> getGroupsStream();

    Long getGroupsCount(Boolean onlyTopGroups);
    Long getGroupsCountByNameContaining(String search);

    /**
     * @deprecated Use {@link #getTopLevelGroups() getTopLevelGroups} instead.
     */
    @Deprecated
    default List<GroupModel> getTopLevelGroups() {
        return getTopLevelGroupsStream().collect(Collectors.toList());
    }

    Stream<GroupModel> getTopLevelGroupsStream();

    /**
     * @deprecated Use {@link #getTopLevelGroupsStream(Integer, Integer) getTopLevelGroupsStream} instead.
     */
    @Deprecated
    default List<GroupModel> getTopLevelGroups(Integer first, Integer max) {
        return getTopLevelGroupsStream(first, max).collect(Collectors.toList());
    }

    Stream<GroupModel> getTopLevelGroupsStream(Integer first, Integer max);

    /**
     * @deprecated Use {@link #searchForGroupByNameStream(String, Integer, Integer) searchForGroupByName} instead.
     */
    @Deprecated
    default List<GroupModel> searchForGroupByName(String search, Integer first, Integer max) {
        return searchForGroupByNameStream(search, first, max).collect(Collectors.toList());
    }

    Stream<GroupModel> searchForGroupByNameStream(String search, Integer first, Integer max);

    boolean removeGroup(GroupModel group);
    void moveGroup(GroupModel group, GroupModel toParent);

    /**
     * @deprecated Use {@link #getClientScopesStream() getClientScopesStream} instead.
     */
    @Deprecated
    default List<ClientScopeModel> getClientScopes() {
        return getClientScopesStream().collect(Collectors.toList());
    }

    Stream<ClientScopeModel> getClientScopesStream();

    ClientScopeModel addClientScope(String name);

    ClientScopeModel addClientScope(String id, String name);

    boolean removeClientScope(String id);

    ClientScopeModel getClientScopeById(String id);

    void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope);
    void removeDefaultClientScope(ClientScopeModel clientScope);

    /**
     * @deprecated Use {@link #getDefaultClientScopesStream(boolean) getDefaultClientScopesStream} instead.
     */
    @Deprecated
    default List<ClientScopeModel> getDefaultClientScopes(boolean defaultScope) {
        return getDefaultClientScopesStream(defaultScope).collect(Collectors.toList());
    }

    Stream<ClientScopeModel> getDefaultClientScopesStream(boolean defaultScope);
}
