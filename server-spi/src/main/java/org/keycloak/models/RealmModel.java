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
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmModel extends RoleContainerModel {

    Comparator<RealmModel> COMPARE_BY_NAME = Comparator.comparing(RealmModel::getName);

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

    interface RealmAttributeUpdateEvent extends ProviderEvent {
        RealmModel getRealm();
        String getAttributeName();
        String getAttributeValue();
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

    boolean isOrganizationsEnabled();

    void setOrganizationsEnabled(boolean organizationsEnabled);

    boolean isAdminPermissionsEnabled();

    void setAdminPermissionsEnabled(boolean adminPermissionsEnabled);

    boolean isVerifiableCredentialsEnabled();

    void setVerifiableCredentialsEnabled(boolean verifiableCredentialsEnabled);

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
        return v != null && !v.isEmpty() ? Integer.valueOf(v) : defaultValue;
    }
    default Long getAttribute(String name, Long defaultValue) {
        String v = getAttribute(name);
        return v != null && !v.isEmpty() ? Long.valueOf(v) : defaultValue;
    }
    default Boolean getAttribute(String name, Boolean defaultValue) {
        String v = getAttribute(name);
        return v != null && !v.isEmpty() ? Boolean.valueOf(v) : defaultValue;
    }
    default <V extends Enum<V>> V getAttribute(String name, Class<V> enumClass, V defaultValue) {
        String value = getAttribute(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
    Map<String, String> getAttributes();

    //--- brute force settings
    boolean isBruteForceProtected();
    void setBruteForceProtected(boolean value);
    boolean isPermanentLockout();
    void setPermanentLockout(boolean val);
    int getMaxTemporaryLockouts();
    void setMaxTemporaryLockouts(int val);
    RealmRepresentation.BruteForceStrategy getBruteForceStrategy();
    void setBruteForceStrategy(RealmRepresentation.BruteForceStrategy val);
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
     * Returns default groups as a stream.
     * @return Stream of {@link GroupModel}. Never returns {@code null}.
     */
    Stream<GroupModel> getDefaultGroupsStream();

    void addDefaultGroup(GroupModel group);

    void removeDefaultGroup(GroupModel group);

    /**
     * Returns clients as a stream.
     * @return Stream of {@link ClientModel}. Never returns {@code null}.
     */
    Stream<ClientModel> getClientsStream();

    /**
     * Returns clients as a stream.
     * @param firstResult {@code Integer} Index of the first desired client. Ignored if negative or {@code null}.
     * @param maxResults {@code Integer} Maximum number of returned clients. Ignored if negative or {@code null}.
     * @return Stream of {@link ClientModel}. Never returns {@code null}.
     */
    Stream<ClientModel> getClientsStream(Integer firstResult, Integer maxResults);

    Long getClientsCount();

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
     * Search for clients by provided client's id.
     * @param clientId {@code String} Id of the client.
     * @param firstResult Index of the first desired client. Ignored if negative or {@code null}.
     * @param maxResults Maximum number of returned clients. Ignored if negative or {@code null}.
     * @return Stream of {@link ClientModel}. Never returns {@code null}.
     */
    Stream<ClientModel> searchClientByClientIdStream(String clientId, Integer firstResult, Integer maxResults);

    Stream<ClientModel> searchClientByAttributes(Map<String, String> attributes, Integer firstResult, Integer maxResults);

    Stream<ClientModel> searchClientByAuthenticationFlowBindingOverrides(Map<String, String> overrides, Integer firstResult, Integer maxResults);

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

    AuthenticationFlowModel getFirstBrokerLoginFlow();
    void setFirstBrokerLoginFlow(AuthenticationFlowModel flow);

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
     * Returns authentication configs as a stream.
     * @return Stream of {@link AuthenticatorConfigModel}. Never returns {@code null}.
     */
    Stream<AuthenticatorConfigModel> getAuthenticatorConfigsStream();

    AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model);
    void updateAuthenticatorConfig(AuthenticatorConfigModel model);
    void removeAuthenticatorConfig(AuthenticatorConfigModel model);
    AuthenticatorConfigModel getAuthenticatorConfigById(String id);
    AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias);

    RequiredActionConfigModel getRequiredActionConfigById(String id);
    RequiredActionConfigModel getRequiredActionConfigByAlias(String alias);
    void removeRequiredActionProviderConfig(RequiredActionConfigModel model);
    void updateRequiredActionConfig(RequiredActionConfigModel model);
    Stream<RequiredActionConfigModel> getRequiredActionConfigsStream();

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
     * Returns identity providers as a stream.
     *
     * @return Stream of {@link IdentityProviderModel}. Never returns {@code null}.
     * @deprecated Use {@link IdentityProviderStorageProvider#getAllStream(IdentityProviderQuery)} instead.
     */
    @Deprecated
    Stream<IdentityProviderModel> getIdentityProvidersStream();

    /**
     * @deprecated Use {@link IdentityProviderStorageProvider#getByAlias(String)} instead.
     */
    @Deprecated
    IdentityProviderModel getIdentityProviderByAlias(String alias);

    /**
     * @deprecated Use {@link IdentityProviderStorageProvider#create(IdentityProviderModel)} instead.
     */
    @Deprecated
    void addIdentityProvider(IdentityProviderModel identityProvider);

    /**
     * @deprecated Use {@link IdentityProviderStorageProvider#remove(String)} instead.
     */
    @Deprecated
    void removeIdentityProviderByAlias(String alias);

    /**
     * @deprecated Use {@link IdentityProviderStorageProvider#update(IdentityProviderModel)} instead.
     */
    @Deprecated
    void updateIdentityProvider(IdentityProviderModel identityProvider);

    /**
     * Returns identity provider mappers as a stream.
     * @return Stream of {@link IdentityProviderMapperModel}. Never returns {@code null}.
     * @deprecated Use {@link IDPProvider#getMappersStream()} instead.
     */
    @Deprecated
    Stream<IdentityProviderMapperModel> getIdentityProviderMappersStream();

    /**
     * Returns identity provider mappers by the provided alias as a stream.
     * @param brokerAlias {@code String} Broker's alias to filter results.
     * @return Stream of {@link IdentityProviderMapperModel} Never returns {@code null}.
     * @deprecated Use {@link IDPProvider#getMappersByAliasStream(String)} instead.
     */
    @Deprecated
    Stream<IdentityProviderMapperModel> getIdentityProviderMappersByAliasStream(String brokerAlias);

    /**
     * @deprecated Use {@link IDPProvider#createMapper(IdentityProviderMapperModel)} instead.
     */
    @Deprecated
    IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model);

    /**
     * @deprecated Use {@link IDPProvider#removeMapper(IdentityProviderMapperModel)} instead.
     */
    @Deprecated
    void removeIdentityProviderMapper(IdentityProviderMapperModel mapping);

    /**
     * @deprecated Use {@link IDPProvider#updateMapper(IdentityProviderMapperModel)} instead.
     */
    @Deprecated
    void updateIdentityProviderMapper(IdentityProviderMapperModel mapping);

    /**
     * @deprecated Use {@link IDPProvider#getMapperById(String)} instead.
     */
    @Deprecated
    IdentityProviderMapperModel getIdentityProviderMapperById(String id);

    /**
     * @deprecated Use {@link IDPProvider#getMapperByName(String, String)} instead.
     */
    @Deprecated
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
     * Returns stream of component models.
     * @return Stream of {@link ComponentModel}. Never returns {@code null}.
     */
    Stream<ComponentModel> getComponentsStream();

    ComponentModel getComponent(String id);

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
     * Returns events listeners as a stream.
     * @return Stream of {@code String}. Never returns {@code null}.
     */
    Stream<String> getEventsListenersStream();

    void setEventsListeners(Set<String> listeners);

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

    ClientModel getAdminPermissionsClient();

    void setAdminPermissionsClient(ClientModel client);

    /**
     * @deprecated use {@link IdentityProviderStorageProvider#isIdentityFederationEnabled()} instead.
     */
    @Deprecated
    boolean isIdentityFederationEnabled();

    boolean isInternationalizationEnabled();
    void setInternationalizationEnabled(boolean enabled);

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
     * Returns groups as a stream.
     * @return Stream of {@link GroupModel}. Never returns {@code null}.
     */
    Stream<GroupModel> getGroupsStream();

    Long getGroupsCount(Boolean onlyTopGroups);
    Long getGroupsCountByNameContaining(String search);

    @Deprecated
    /**
     * @deprecated It is now preferable to use {@link GroupProvider} from a {@link KeycloakSession}
     * Returns top level groups as a stream.
     * @return Stream of {@link GroupModel}. Never returns {@code null}.
     */
    Stream<GroupModel> getTopLevelGroupsStream();

    @Deprecated
    /**
     * @deprecated It is now preferable to use {@link GroupProvider} from a {@link KeycloakSession}
     * Returns top level groups as a stream.
     * @param first {@code Integer} Index of the first desired group. Ignored if negative or {@code null}.
     * @param max {@code Integer} Maximum number of returned groups. Ignored if negative or {@code null}.
     * @return Stream of {@link GroupModel}. Never returns {@code null}.
     */
    Stream<GroupModel> getTopLevelGroupsStream(Integer first, Integer max);

    boolean removeGroup(GroupModel group);
    void moveGroup(GroupModel group, GroupModel toParent);

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
