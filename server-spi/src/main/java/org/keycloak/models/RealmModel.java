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

import java.util.Comparator;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.storage.SearchableModelField;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.client.ClientStorageProviderModel;
import org.keycloak.storage.role.RoleStorageProvider;
import org.keycloak.storage.role.RoleStorageProviderModel;
import org.keycloak.utils.StringUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmModel extends RoleContainerModel {

    Comparator<RealmModel> COMPARE_BY_NAME = Comparator.comparing(RealmModel::getName);

    public static class SearchableFields {
        public static final SearchableModelField<RealmModel> ID                     = new SearchableModelField<>("id", String.class);
        public static final SearchableModelField<RealmModel> NAME                   = new SearchableModelField<>("name", String.class);
        /**
         * Search for realms that have some client initial access set.
         */
        public static final SearchableModelField<RealmModel> CLIENT_INITIAL_ACCESS  = new SearchableModelField<>("clientInitialAccess", Boolean.class);
        /**
         * Search for realms that have some component with 
         */
        public static final SearchableModelField<RealmModel> COMPONENT_PROVIDER_TYPE  = new SearchableModelField<>("componentProviderType", String.class);
    }

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

    @Override
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
    default void setAttribute(String name, Boolean value) {
        setAttribute(name, value.toString());
    }
    default void setAttribute(String name, Integer value) {
        setAttribute(name, value.toString());
    }
    default void setAttribute(String name, Long value) {
        setAttribute(name, value.toString());
    }
    void removeAttribute(String name);
    String getAttribute(String name);
    default Integer getAttribute(String name, Integer defaultValue) {
        String v = getAttribute(name);
        return v != null ? Integer.valueOf(v) : defaultValue;
    }
    default Long getAttribute(String name, Long defaultValue) {
        String v = getAttribute(name);
        return v != null ? Long.valueOf(v) : defaultValue;
    }
    default Boolean getAttribute(String name, Boolean defaultValue) {
        String v = getAttribute(name);
        return v != null ? Boolean.valueOf(v) : defaultValue;
    }
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

    OAuth2DeviceConfig getOAuth2DeviceConfig();

    CibaConfig getCibaPolicy();

    ParConfig getParPolicy();

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
     * @deprecated Use {@link #getRequiredCredentialsStream() getRequiredCredentialsStream} instead.
     */
    @Deprecated
    default List<RequiredCredentialModel> getRequiredCredentials() {
        return getRequiredCredentialsStream().collect(Collectors.toList());
    }

    /**
     * Returns required credentials as a stream.
     * @return Stream of {@link RequiredCredentialModel}. Never returns {@code null}.
     */
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

    /**
     * @deprecated Use {@link #getDefaultGroupsStream() getDefaultGroupsStream} instead.
     */
    @Deprecated
    default List<GroupModel> getDefaultGroups() {
        return getDefaultGroupsStream().collect(Collectors.toList());
    }

    /**
     * Returns default groups as a stream.
     * @return Stream of {@link GroupModel}. Never returns {@code null}.
     */
    Stream<GroupModel> getDefaultGroupsStream();

    void addDefaultGroup(GroupModel group);

    void removeDefaultGroup(GroupModel group);

    /**
     * @deprecated Use {@link #getClientsStream() getClientsStream} instead.
     */
    @Deprecated
    default List<ClientModel> getClients() {
        return getClientsStream(null, null).collect(Collectors.toList());
    }

    /**
     * Returns clients as a stream.
     * @return Stream of {@link ClientModel}. Never returns {@code null}.
     */
    Stream<ClientModel> getClientsStream();

    /**
     * @deprecated Use {@link #getClientsStream(Integer, Integer) getClientsStream} instead.
     */
    @Deprecated
    default List<ClientModel> getClients(Integer firstResult, Integer maxResults) {
        return getClientsStream(firstResult, maxResults).collect(Collectors.toList());
    }

    /**
     * Returns clients as a stream.
     * @param firstResult {@code Integer} Index of the first desired client. Ignored if negative or {@code null}.
     * @param maxResults {@code Integer} Maximum number of returned clients. Ignored if negative or {@code null}.
     * @return Stream of {@link ClientModel}. Never returns {@code null}.
     */
    Stream<ClientModel> getClientsStream(Integer firstResult, Integer maxResults);

    Long getClientsCount();

    /**
     * @deprecated Use {@link #getAlwaysDisplayInConsoleClientsStream() getAlwaysDisplayInConsoleClientsStream} instead.
     */
    @Deprecated
    default List<ClientModel> getAlwaysDisplayInConsoleClients() {
        return getAlwaysDisplayInConsoleClientsStream().collect(Collectors.toList());
    }

    /**
     * Returns clients which are always displayed in the admin console as a stream.
     * @return Stream of {@link ClientModel}. Never returns {@code null}.
     */
    Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream();

    ClientModel addClient(String name);

    ClientModel addClient(String id, String clientId);

    boolean removeClient(String id);

    ClientModel getClientById(String id);
    ClientModel getClientByClientId(String clientId);

    /**
     * @deprecated Use {@link #searchClientByClientIdStream(String, Integer, Integer) searchClientByClientId} instead.
     */
    @Deprecated
    default List<ClientModel> searchClientByClientId(String clientId, Integer firstResult, Integer maxResults) {
        return searchClientByClientIdStream(clientId, firstResult, maxResults).collect(Collectors.toList());
    }

    /**
     * Search for clients by provided client's id.
     * @param clientId {@code String} Id of the client.
     * @param firstResult Index of the first desired client. Ignored if negative or {@code null}.
     * @param maxResults Maximum number of returned clients. Ignored if negative or {@code null}.
     * @return Stream of {@link ClientModel}. Never returns {@code null}.
     */
    Stream<ClientModel> searchClientByClientIdStream(String clientId, Integer firstResult, Integer maxResults);

    Stream<ClientModel> searchClientByAttributes(Map<String, String> attributes, Integer firstResult, Integer maxResults);

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
     * @deprecated Use {@link #getAuthenticationFlowsStream() getAuthenticationFlowsStream} instead.
     */
    @Deprecated
    default List<AuthenticationFlowModel> getAuthenticationFlows() {
        return getAuthenticationFlowsStream().collect(Collectors.toList());
    }

    /**
     * Returns authentications flows as a stream.
     * @return Stream of {@link AuthenticationFlowModel}. Never returns {@code null}.
     */
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
     * Returns sorted (according to priority) {@link AuthenticationExecutionModel AuthenticationExecutionModel} as a stream.
     * It should be used with forEachOrdered if the ordering is required.
     * @param flowId {@code String} Id of the flow.
     * @return Sorted stream of {@link AuthenticationExecutionModel}. Never returns {@code null}.
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

    /**
     * Returns authentication configs as a stream.
     * @return Stream of {@link AuthenticatorConfigModel}. Never returns {@code null}.
     */
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
     * @return Sorted stream of {@link RequiredActionProviderModel}. Never returns {@code null}.
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

    /**
     * Returns identity providers as a stream.
     * @return Stream of {@link IdentityProviderModel}. Never returns {@code null}.
     */
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

    /**
     * Returns identity provider mappers as a stream.
     * @return Stream of {@link IdentityProviderMapperModel}. Never returns {@code null}.
     */
    Stream<IdentityProviderMapperModel> getIdentityProviderMappersStream();

    /**
     * @deprecated Use {@link #getIdentityProviderMappersByAliasStream(String) getIdentityProviderMappersByAliasStream} instead.
     */
    @Deprecated
    default Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(String brokerAlias) {
        return getIdentityProviderMappersByAliasStream(brokerAlias).collect(Collectors.toSet());
    }

    /**
     * Returns identity provider mappers by the provided alias as a stream.
     * @param brokerAlias {@code String} Broker's alias to filter results.
     * @return Stream of {@link IdentityProviderMapperModel} Never returns {@code null}.
     */
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

    /**
     * Updates component model. Will call onUpdate() method of ComponentFactory
     * @param component to be updated
     */
    void updateComponent(ComponentModel component);

    /**
     * Removes given component. Will call preRemove() method of ComponentFactory.
     * Also calls {@code this.removeComponents(component.getId())}.
     * 
     * @param component to be removed
     */
    void removeComponent(ComponentModel component);

    /**
     * Removes all components with given {@code parentId}
     * @param parentId {@code String} id of parent
     */
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
     * @param parentId {@code String} id of parent
     * @param providerType {@code String} type of provider
     * @return Stream of {@link ComponentModel}. Never returns {@code null}.
     */
    Stream<ComponentModel> getComponentsStream(String parentId, String providerType);

    /**
     * Returns stream of ComponentModels for specific parentId.
     * @param parentId {@code String} id of parent
     * @return Stream of {@link ComponentModel}. Never returns {@code null}.
     */
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

    /**
     * Returns stream of component models.
     * @return Stream of {@link ComponentModel}. Never returns {@code null}.
     */
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
     * @return Sorted stream of {@link UserStorageProviderModel}. Never returns {@code null}.
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
     * @return Sorted stream of {@link ClientStorageProviderModel}. Never returns {@code null}.
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
     * @return Sorted stream of {@link RoleStorageProviderModel}. Never returns {@code null}.
     */
    default Stream<RoleStorageProviderModel> getRoleStorageProvidersStream() {
        return getComponentsStream(getId(), RoleStorageProvider.class.getName())
                .map(RoleStorageProviderModel::new)
                .sorted(RoleStorageProviderModel.comparator);
    }

    /**
     * Returns stream of ComponentModels that represent StorageProviders for class storageProviderClass in this realm.
     * @param storageProviderClass {@code Class<? extends Provider>}
     * @return Stream of {@link ComponentModel}. Never returns {@code null}.
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

    /**
     * Returns events listeners as a stream.
     * @return Stream of {@code String}. Never returns {@code null}.
     */
    Stream<String> getEventsListenersStream();

    void setEventsListeners(Set<String> listeners);

    /**
     * @deprecated Use {@link #getEnabledEventTypesStream() getEnabledEventTypesStream} instead.
     */
    @Deprecated
    default Set<String> getEnabledEventTypes() {
        return getEnabledEventTypesStream().collect(Collectors.toSet());
    }

    /**
     * Returns enabled event types as a stream.
     * @return Stream of {@code String}. Never returns {@code null}.
     */
    Stream<String> getEnabledEventTypesStream();

    void setEnabledEventTypes(Set<String> enabledEventTypes);

    boolean isAdminEventsEnabled();

    void setAdminEventsEnabled(boolean enabled);

    boolean isAdminEventsDetailsEnabled();

    void setAdminEventsDetailsEnabled(boolean enabled);

    ClientModel getMasterAdminClient();

    void setMasterAdminClient(ClientModel client);

    /**
     * Returns default realm role. All both realm and client default roles are assigned as composite of this role.
     * @return Default role of this realm
     */
    RoleModel getDefaultRole();

    /**
     * Sets default role for this realm
     * @param role to be set
     */
    void setDefaultRole(RoleModel role);

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

    /**
     * Returns supported locales as a stream.
     * @return Stream of {@code String}. Never returns {@code null}.
     */
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

    /**
     * Returns groups as a stream.
     * @return Stream of {@link GroupModel}. Never returns {@code null}.
     */
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

    /**
     * Returns top level groups as a stream.
     * @return Stream of {@link GroupModel}. Never returns {@code null}.
     */
    Stream<GroupModel> getTopLevelGroupsStream();

    /**
     * @deprecated Use {@link #getTopLevelGroupsStream(Integer, Integer) getTopLevelGroupsStream} instead.
     */
    @Deprecated
    default List<GroupModel> getTopLevelGroups(Integer first, Integer max) {
        return getTopLevelGroupsStream(first, max).collect(Collectors.toList());
    }

    /**
     * Returns top level groups as a stream.
     * @param first {@code Integer} Index of the first desired group. Ignored if negative or {@code null}.
     * @param max {@code Integer} Maximum number of returned groups. Ignored if negative or {@code null}.
     * @return Stream of {@link GroupModel}. Never returns {@code null}.
     */
    Stream<GroupModel> getTopLevelGroupsStream(Integer first, Integer max);

    /**
     * @deprecated Use {@link #searchForGroupByNameStream(String, Integer, Integer) searchForGroupByName} instead.
     */
    @Deprecated
    default List<GroupModel> searchForGroupByName(String search, Integer first, Integer max) {
        return searchForGroupByNameStream(search, first, max).collect(Collectors.toList());
    }

    /**
     * Searches for groups by provided name. Results that match the given filter are returned as a stream.
     * @param search {@code String} Name of a group to be used as a filter.
     * @param first {@code Integer} Index of the first desired group. Ignored if negative or {@code null}.
     * @param max {@code Integer} Maximum number of returned groups. Ignored if negative or {@code null}.
     * @return Stream of {@link GroupModel}. Never returns {@code null}.
     */
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

    /**
     * Returns all client scopes of this realm as a stream.
     * @return Stream of {@link ClientScopeModel}. Never returns {@code null}.
     */
    Stream<ClientScopeModel> getClientScopesStream();

    /**
     * Creates new client scope with the given name. Internal ID is created automatically.
     * If given name contains spaces, those are replaced by underscores.
     * @param name {@code String} name of the client scope.
     * @return Model of the created client scope.
     * @throws ModelDuplicateException if client scope with same id or name already exists.
     */
    ClientScopeModel addClientScope(String name);

    /**
     * Creates new client scope with the given internal ID and name. 
     * If given name contains spaces, those are replaced by underscores.
     * @param id {@code String} id of the client scope.
     * @param name {@code String} name of the client scope.
     * @return Model of the created client scope.
     * @throws ModelDuplicateException if client scope with same id or name already exists.
     */
    ClientScopeModel addClientScope(String id, String name);

    /**
     * Removes client scope with given {@code id} from this realm.
     * @param id of the client scope
     * @return true if the realm contained the scope and the removal was successful, false otherwise
     */
    boolean removeClientScope(String id);

    /**
     * @param id of the client scope
     * @return Client scope with the given {@code id}, or {@code null} when the scope does not exist.
     */
    ClientScopeModel getClientScopeById(String id);

    /**
     * Adds given client scope among default/optional client scopes of this realm. 
     * The scope will be assigned to each new client.
     * @param clientScope to be added
     * @param defaultScope if {@code true} the scope will be added among default client scopes, 
     * if {@code false} it will be added among optional client scopes
     */
    void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope);

    /**
     * Removes given client scope from default or optional client scopes of this realm.
     * @param clientScope to be removed
     */
    void removeDefaultClientScope(ClientScopeModel clientScope);

    /**
     * Creates or updates the realm-specific localization texts for the given locale.
     * This method will not delete any text.
     * It updates texts, which are already stored or create new ones if the key does not exist yet.
     */
    void createOrUpdateRealmLocalizationTexts(String locale, Map<String, String> localizationTexts);
    boolean removeRealmLocalizationTexts(String locale);
    Map<String, Map<String, String>> getRealmLocalizationTexts();
    Map<String, String> getRealmLocalizationTextsByLocale(String locale);

    /**
     * @deprecated Use {@link #getDefaultClientScopesStream(boolean) getDefaultClientScopesStream} instead.
     */
    @Deprecated
    default List<ClientScopeModel> getDefaultClientScopes(boolean defaultScope) {
        return getDefaultClientScopesStream(defaultScope).collect(Collectors.toList());
    }

    /**
     * Returns default client scopes of this realm either default ones or optional ones.
     * @param defaultScope if {@code true} default client scopes are returned, 
     * if {@code false} optional client scopes are returned.
     * @return Stream of {@link ClientScopeModel}. Never returns {@code null}.
     */
    Stream<ClientScopeModel> getDefaultClientScopesStream(boolean defaultScope);

    /**
     * Adds a role as a composite to default role of this realm. 
     * @param role to be added
     */ 
    default void addToDefaultRoles(RoleModel role) {
        getDefaultRole().addCompositeRole(role);
    }

    ClientInitialAccessModel createClientInitialAccessModel(int expiration, int count);
    ClientInitialAccessModel getClientInitialAccessModel(String id);
    void removeClientInitialAccessModel(String id);
    Stream<ClientInitialAccessModel> getClientInitialAccesses();
    void decreaseRemainingCount(ClientInitialAccessModel clientInitialAccess);
}
