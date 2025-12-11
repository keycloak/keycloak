/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.ParConfig;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author Alexander Schwartz
 */
public class RealmModelDelegate implements RealmModel {
    private RealmModel delegate;

    public RealmModelDelegate(RealmModel delegate) {
        this.delegate = delegate;
    }

    public String getId() {
        return delegate.getId();
    }

    public String getName() {
        return delegate.getName();
    }

    public void setName(String name) {
        delegate.setName(name);
    }

    public String getDisplayName() {
        return delegate.getDisplayName();
    }

    public void setDisplayName(String displayName) {
        delegate.setDisplayName(displayName);
    }

    public String getDisplayNameHtml() {
        return delegate.getDisplayNameHtml();
    }

    public void setDisplayNameHtml(String displayNameHtml) {
        delegate.setDisplayNameHtml(displayNameHtml);
    }

    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    public SslRequired getSslRequired() {
        return delegate.getSslRequired();
    }

    public void setSslRequired(SslRequired sslRequired) {
        delegate.setSslRequired(sslRequired);
    }

    public boolean isRegistrationAllowed() {
        return delegate.isRegistrationAllowed();
    }

    public void setRegistrationAllowed(boolean registrationAllowed) {
        delegate.setRegistrationAllowed(registrationAllowed);
    }

    public boolean isRegistrationEmailAsUsername() {
        return delegate.isRegistrationEmailAsUsername();
    }

    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        delegate.setRegistrationEmailAsUsername(registrationEmailAsUsername);
    }

    public boolean isRememberMe() {
        return delegate.isRememberMe();
    }

    public void setRememberMe(boolean rememberMe) {
        delegate.setRememberMe(rememberMe);
    }

    public boolean isEditUsernameAllowed() {
        return delegate.isEditUsernameAllowed();
    }

    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        delegate.setEditUsernameAllowed(editUsernameAllowed);
    }

    public boolean isUserManagedAccessAllowed() {
        return delegate.isUserManagedAccessAllowed();
    }

    public void setUserManagedAccessAllowed(boolean userManagedAccessAllowed) {
        delegate.setUserManagedAccessAllowed(userManagedAccessAllowed);
    }

    public void setAttribute(String name, String value) {
        delegate.setAttribute(name, value);
    }

    public void setAttribute(String name, Boolean value) {
        delegate.setAttribute(name, value);
    }

    public void setAttribute(String name, Integer value) {
        delegate.setAttribute(name, value);
    }

    public void setAttribute(String name, Long value) {
        delegate.setAttribute(name, value);
    }

    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    public String getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    public Integer getAttribute(String name, Integer defaultValue) {
        return delegate.getAttribute(name, defaultValue);
    }

    public Long getAttribute(String name, Long defaultValue) {
        return delegate.getAttribute(name, defaultValue);
    }

    public Boolean getAttribute(String name, Boolean defaultValue) {
        return delegate.getAttribute(name, defaultValue);
    }

    public Map<String, String> getAttributes() {
        return delegate.getAttributes();
    }

    public boolean isBruteForceProtected() {
        return delegate.isBruteForceProtected();
    }

    public void setBruteForceProtected(boolean value) {
        delegate.setBruteForceProtected(value);
    }

    public RealmRepresentation.BruteForceStrategy getBruteForceStrategy() { return delegate.getBruteForceStrategy(); }

    public void setBruteForceStrategy(RealmRepresentation.BruteForceStrategy value) { delegate.setBruteForceStrategy(value); }

    public boolean isPermanentLockout() {
        return delegate.isPermanentLockout();
    }

    public void setPermanentLockout(boolean val) {
        delegate.setPermanentLockout(val);
    }

    public int getMaxTemporaryLockouts() {
        return delegate.getMaxTemporaryLockouts();
    }

    public void setMaxTemporaryLockouts(int val) {
        delegate.setMaxTemporaryLockouts(val);
    }

    public int getMaxFailureWaitSeconds() {
        return delegate.getMaxFailureWaitSeconds();
    }

    public void setMaxFailureWaitSeconds(int val) {
        delegate.setMaxFailureWaitSeconds(val);
    }

    public int getWaitIncrementSeconds() {
        return delegate.getWaitIncrementSeconds();
    }

    public void setWaitIncrementSeconds(int val) {
        delegate.setWaitIncrementSeconds(val);
    }

    public int getMinimumQuickLoginWaitSeconds() {
        return delegate.getMinimumQuickLoginWaitSeconds();
    }

    public void setMinimumQuickLoginWaitSeconds(int val) {
        delegate.setMinimumQuickLoginWaitSeconds(val);
    }

    public long getQuickLoginCheckMilliSeconds() {
        return delegate.getQuickLoginCheckMilliSeconds();
    }

    public void setQuickLoginCheckMilliSeconds(long val) {
        delegate.setQuickLoginCheckMilliSeconds(val);
    }

    public int getMaxDeltaTimeSeconds() {
        return delegate.getMaxDeltaTimeSeconds();
    }

    public void setMaxDeltaTimeSeconds(int val) {
        delegate.setMaxDeltaTimeSeconds(val);
    }

    public int getFailureFactor() {
        return delegate.getFailureFactor();
    }

    public void setFailureFactor(int failureFactor) {
        delegate.setFailureFactor(failureFactor);
    }

    public boolean isVerifyEmail() {
        return delegate.isVerifyEmail();
    }

    public void setVerifyEmail(boolean verifyEmail) {
        delegate.setVerifyEmail(verifyEmail);
    }

    public boolean isLoginWithEmailAllowed() {
        return delegate.isLoginWithEmailAllowed();
    }

    public void setLoginWithEmailAllowed(boolean loginWithEmailAllowed) {
        delegate.setLoginWithEmailAllowed(loginWithEmailAllowed);
    }

    public boolean isDuplicateEmailsAllowed() {
        return delegate.isDuplicateEmailsAllowed();
    }

    public void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed) {
        delegate.setDuplicateEmailsAllowed(duplicateEmailsAllowed);
    }

    public boolean isResetPasswordAllowed() {
        return delegate.isResetPasswordAllowed();
    }

    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        delegate.setResetPasswordAllowed(resetPasswordAllowed);
    }

    public String getDefaultSignatureAlgorithm() {
        return delegate.getDefaultSignatureAlgorithm();
    }

    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        delegate.setDefaultSignatureAlgorithm(defaultSignatureAlgorithm);
    }

    public boolean isRevokeRefreshToken() {
        return delegate.isRevokeRefreshToken();
    }

    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        delegate.setRevokeRefreshToken(revokeRefreshToken);
    }

    public int getRefreshTokenMaxReuse() {
        return delegate.getRefreshTokenMaxReuse();
    }

    public void setRefreshTokenMaxReuse(int revokeRefreshTokenCount) {
        delegate.setRefreshTokenMaxReuse(revokeRefreshTokenCount);
    }

    public int getSsoSessionIdleTimeout() {
        return delegate.getSsoSessionIdleTimeout();
    }

    public void setSsoSessionIdleTimeout(int seconds) {
        delegate.setSsoSessionIdleTimeout(seconds);
    }

    public int getSsoSessionMaxLifespan() {
        return delegate.getSsoSessionMaxLifespan();
    }

    public void setSsoSessionMaxLifespan(int seconds) {
        delegate.setSsoSessionMaxLifespan(seconds);
    }

    public int getSsoSessionIdleTimeoutRememberMe() {
        return delegate.getSsoSessionIdleTimeoutRememberMe();
    }

    public void setSsoSessionIdleTimeoutRememberMe(int seconds) {
        delegate.setSsoSessionIdleTimeoutRememberMe(seconds);
    }

    public int getSsoSessionMaxLifespanRememberMe() {
        return delegate.getSsoSessionMaxLifespanRememberMe();
    }

    public void setSsoSessionMaxLifespanRememberMe(int seconds) {
        delegate.setSsoSessionMaxLifespanRememberMe(seconds);
    }

    public int getOfflineSessionIdleTimeout() {
        return delegate.getOfflineSessionIdleTimeout();
    }

    public void setOfflineSessionIdleTimeout(int seconds) {
        delegate.setOfflineSessionIdleTimeout(seconds);
    }

    public int getAccessTokenLifespan() {
        return delegate.getAccessTokenLifespan();
    }

    public boolean isOfflineSessionMaxLifespanEnabled() {
        return delegate.isOfflineSessionMaxLifespanEnabled();
    }

    public void setOfflineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {
        delegate.setOfflineSessionMaxLifespanEnabled(offlineSessionMaxLifespanEnabled);
    }

    public int getOfflineSessionMaxLifespan() {
        return delegate.getOfflineSessionMaxLifespan();
    }

    public void setOfflineSessionMaxLifespan(int seconds) {
        delegate.setOfflineSessionMaxLifespan(seconds);
    }

    public int getClientSessionIdleTimeout() {
        return delegate.getClientSessionIdleTimeout();
    }

    public void setClientSessionIdleTimeout(int seconds) {
        delegate.setClientSessionIdleTimeout(seconds);
    }

    public int getClientSessionMaxLifespan() {
        return delegate.getClientSessionMaxLifespan();
    }

    public void setClientSessionMaxLifespan(int seconds) {
        delegate.setClientSessionMaxLifespan(seconds);
    }

    public int getClientOfflineSessionIdleTimeout() {
        return delegate.getClientOfflineSessionIdleTimeout();
    }

    public void setClientOfflineSessionIdleTimeout(int seconds) {
        delegate.setClientOfflineSessionIdleTimeout(seconds);
    }

    public int getClientOfflineSessionMaxLifespan() {
        return delegate.getClientOfflineSessionMaxLifespan();
    }

    public void setClientOfflineSessionMaxLifespan(int seconds) {
        delegate.setClientOfflineSessionMaxLifespan(seconds);
    }

    public void setAccessTokenLifespan(int seconds) {
        delegate.setAccessTokenLifespan(seconds);
    }

    public int getAccessTokenLifespanForImplicitFlow() {
        return delegate.getAccessTokenLifespanForImplicitFlow();
    }

    public void setAccessTokenLifespanForImplicitFlow(int seconds) {
        delegate.setAccessTokenLifespanForImplicitFlow(seconds);
    }

    public int getAccessCodeLifespan() {
        return delegate.getAccessCodeLifespan();
    }

    public void setAccessCodeLifespan(int seconds) {
        delegate.setAccessCodeLifespan(seconds);
    }

    public int getAccessCodeLifespanUserAction() {
        return delegate.getAccessCodeLifespanUserAction();
    }

    public void setAccessCodeLifespanUserAction(int seconds) {
        delegate.setAccessCodeLifespanUserAction(seconds);
    }

    public OAuth2DeviceConfig getOAuth2DeviceConfig() {
        return delegate.getOAuth2DeviceConfig();
    }

    public CibaConfig getCibaPolicy() {
        return delegate.getCibaPolicy();
    }

    public ParConfig getParPolicy() {
        return delegate.getParPolicy();
    }

    public Map<String, Integer> getUserActionTokenLifespans() {
        return delegate.getUserActionTokenLifespans();
    }

    public int getAccessCodeLifespanLogin() {
        return delegate.getAccessCodeLifespanLogin();
    }

    public void setAccessCodeLifespanLogin(int seconds) {
        delegate.setAccessCodeLifespanLogin(seconds);
    }

    public int getActionTokenGeneratedByAdminLifespan() {
        return delegate.getActionTokenGeneratedByAdminLifespan();
    }

    public void setActionTokenGeneratedByAdminLifespan(int seconds) {
        delegate.setActionTokenGeneratedByAdminLifespan(seconds);
    }

    public int getActionTokenGeneratedByUserLifespan() {
        return delegate.getActionTokenGeneratedByUserLifespan();
    }

    public void setActionTokenGeneratedByUserLifespan(int seconds) {
        delegate.setActionTokenGeneratedByUserLifespan(seconds);
    }

    public int getActionTokenGeneratedByUserLifespan(String actionTokenType) {
        return delegate.getActionTokenGeneratedByUserLifespan(actionTokenType);
    }

    public void setActionTokenGeneratedByUserLifespan(String actionTokenType, Integer seconds) {
        delegate.setActionTokenGeneratedByUserLifespan(actionTokenType, seconds);
    }

    public Stream<RequiredCredentialModel> getRequiredCredentialsStream() {
        return delegate.getRequiredCredentialsStream();
    }

    public void addRequiredCredential(String cred) {
        delegate.addRequiredCredential(cred);
    }

    public PasswordPolicy getPasswordPolicy() {
        return delegate.getPasswordPolicy();
    }

    public void setPasswordPolicy(PasswordPolicy policy) {
        delegate.setPasswordPolicy(policy);
    }

    public OTPPolicy getOTPPolicy() {
        return delegate.getOTPPolicy();
    }

    public void setOTPPolicy(OTPPolicy policy) {
        delegate.setOTPPolicy(policy);
    }

    public WebAuthnPolicy getWebAuthnPolicy() {
        return delegate.getWebAuthnPolicy();
    }

    public void setWebAuthnPolicy(WebAuthnPolicy policy) {
        delegate.setWebAuthnPolicy(policy);
    }

    public WebAuthnPolicy getWebAuthnPolicyPasswordless() {
        return delegate.getWebAuthnPolicyPasswordless();
    }

    public void setWebAuthnPolicyPasswordless(WebAuthnPolicy policy) {
        delegate.setWebAuthnPolicyPasswordless(policy);
    }

    public RoleModel getRoleById(String id) {
        return delegate.getRoleById(id);
    }

    public Stream<GroupModel> getDefaultGroupsStream() {
        return delegate.getDefaultGroupsStream();
    }

    public void addDefaultGroup(GroupModel group) {
        delegate.addDefaultGroup(group);
    }

    public void removeDefaultGroup(GroupModel group) {
        delegate.removeDefaultGroup(group);
    }

    public Stream<ClientModel> getClientsStream() {
        return delegate.getClientsStream();
    }

    public Stream<ClientModel> getClientsStream(Integer firstResult, Integer maxResults) {
        return delegate.getClientsStream(firstResult, maxResults);
    }

    public Long getClientsCount() {
        return delegate.getClientsCount();
    }

    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream() {
        return delegate.getAlwaysDisplayInConsoleClientsStream();
    }

    public ClientModel addClient(String name) {
        return delegate.addClient(name);
    }

    public ClientModel addClient(String id, String clientId) {
        return delegate.addClient(id, clientId);
    }

    public boolean removeClient(String id) {
        return delegate.removeClient(id);
    }

    public ClientModel getClientById(String id) {
        return delegate.getClientById(id);
    }

    public ClientModel getClientByClientId(String clientId) {
        return delegate.getClientByClientId(clientId);
    }

    public Stream<ClientModel> searchClientByClientIdStream(String clientId, Integer firstResult, Integer maxResults) {
        return delegate.searchClientByClientIdStream(clientId, firstResult, maxResults);
    }

    public Stream<ClientModel> searchClientByAttributes(Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        return delegate.searchClientByAttributes(attributes, firstResult, maxResults);
    }

    public Stream<ClientModel> searchClientByAuthenticationFlowBindingOverrides(Map<String, String> overrides, Integer firstResult, Integer maxResults) {
        return delegate.searchClientByAuthenticationFlowBindingOverrides(overrides, firstResult, maxResults);
    }

    public void updateRequiredCredentials(Set<String> creds) {
        delegate.updateRequiredCredentials(creds);
    }

    public Map<String, String> getBrowserSecurityHeaders() {
        return delegate.getBrowserSecurityHeaders();
    }

    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        delegate.setBrowserSecurityHeaders(headers);
    }

    public Map<String, String> getSmtpConfig() {
        return delegate.getSmtpConfig();
    }

    public void setSmtpConfig(Map<String, String> smtpConfig) {
        delegate.setSmtpConfig(smtpConfig);
    }

    public AuthenticationFlowModel getBrowserFlow() {
        return delegate.getBrowserFlow();
    }

    public void setBrowserFlow(AuthenticationFlowModel flow) {
        delegate.setBrowserFlow(flow);
    }

    public AuthenticationFlowModel getRegistrationFlow() {
        return delegate.getRegistrationFlow();
    }

    public void setRegistrationFlow(AuthenticationFlowModel flow) {
        delegate.setRegistrationFlow(flow);
    }

    public AuthenticationFlowModel getDirectGrantFlow() {
        return delegate.getDirectGrantFlow();
    }

    public void setDirectGrantFlow(AuthenticationFlowModel flow) {
        delegate.setDirectGrantFlow(flow);
    }

    public AuthenticationFlowModel getResetCredentialsFlow() {
        return delegate.getResetCredentialsFlow();
    }

    public void setResetCredentialsFlow(AuthenticationFlowModel flow) {
        delegate.setResetCredentialsFlow(flow);
    }

    public AuthenticationFlowModel getClientAuthenticationFlow() {
        return delegate.getClientAuthenticationFlow();
    }

    public void setClientAuthenticationFlow(AuthenticationFlowModel flow) {
        delegate.setClientAuthenticationFlow(flow);
    }

    public AuthenticationFlowModel getDockerAuthenticationFlow() {
        return delegate.getDockerAuthenticationFlow();
    }

    public void setDockerAuthenticationFlow(AuthenticationFlowModel flow) {
        delegate.setDockerAuthenticationFlow(flow);
    }

    public AuthenticationFlowModel getFirstBrokerLoginFlow() {
        return delegate.getFirstBrokerLoginFlow();
    }

    public void setFirstBrokerLoginFlow(AuthenticationFlowModel flow) {
        delegate.setFirstBrokerLoginFlow(flow);
    }

    public Stream<AuthenticationFlowModel> getAuthenticationFlowsStream() {
        return delegate.getAuthenticationFlowsStream();
    }

    public AuthenticationFlowModel getFlowByAlias(String alias) {
        return delegate.getFlowByAlias(alias);
    }

    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        return delegate.addAuthenticationFlow(model);
    }

    public AuthenticationFlowModel getAuthenticationFlowById(String id) {
        return delegate.getAuthenticationFlowById(id);
    }

    public void removeAuthenticationFlow(AuthenticationFlowModel model) {
        delegate.removeAuthenticationFlow(model);
    }

    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        delegate.updateAuthenticationFlow(model);
    }

    public Stream<AuthenticationExecutionModel> getAuthenticationExecutionsStream(String flowId) {
        return delegate.getAuthenticationExecutionsStream(flowId);
    }

    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        return delegate.getAuthenticationExecutionById(id);
    }

    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId) {
        return delegate.getAuthenticationExecutionByFlowId(flowId);
    }

    public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
        return delegate.addAuthenticatorExecution(model);
    }

    public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {
        delegate.updateAuthenticatorExecution(model);
    }

    public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {
        delegate.removeAuthenticatorExecution(model);
    }

    public Stream<AuthenticatorConfigModel> getAuthenticatorConfigsStream() {
        return delegate.getAuthenticatorConfigsStream();
    }

    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        return delegate.addAuthenticatorConfig(model);
    }

    public void updateAuthenticatorConfig(AuthenticatorConfigModel model) {
        delegate.updateAuthenticatorConfig(model);
    }

    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        delegate.removeAuthenticatorConfig(model);
    }

    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        return delegate.getAuthenticatorConfigById(id);
    }

    public AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias) {
        return delegate.getAuthenticatorConfigByAlias(alias);
    }

    @Override
    public RequiredActionConfigModel getRequiredActionConfigById(String id) {
        return delegate.getRequiredActionConfigById(id);
    }

    @Override
    public RequiredActionConfigModel getRequiredActionConfigByAlias(String alias) {
        return delegate.getRequiredActionConfigByAlias(alias);
    }

    @Override
    public void removeRequiredActionProviderConfig(RequiredActionConfigModel model) {
        delegate.removeRequiredActionProviderConfig(model);
    }

    @Override
    public void updateRequiredActionConfig(RequiredActionConfigModel model) {
        delegate.updateRequiredActionConfig(model);
    }

    @Override
    public Stream<RequiredActionConfigModel> getRequiredActionConfigsStream() {
        return delegate.getRequiredActionConfigsStream();
    }

    public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream() {
        return delegate.getRequiredActionProvidersStream();
    }

    public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
        return delegate.addRequiredActionProvider(model);
    }

    public void updateRequiredActionProvider(RequiredActionProviderModel model) {
        delegate.updateRequiredActionProvider(model);
    }

    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        delegate.removeRequiredActionProvider(model);
    }

    public RequiredActionProviderModel getRequiredActionProviderById(String id) {
        return delegate.getRequiredActionProviderById(id);
    }

    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        return delegate.getRequiredActionProviderByAlias(alias);
    }

    public Stream<IdentityProviderModel> getIdentityProvidersStream() {
        return delegate.getIdentityProvidersStream();
    }

    public IdentityProviderModel getIdentityProviderByAlias(String alias) {
        return delegate.getIdentityProviderByAlias(alias);
    }

    public void addIdentityProvider(IdentityProviderModel identityProvider) {
        delegate.addIdentityProvider(identityProvider);
    }

    public void removeIdentityProviderByAlias(String alias) {
        delegate.removeIdentityProviderByAlias(alias);
    }

    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        delegate.updateIdentityProvider(identityProvider);
    }

    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersStream() {
        return delegate.getIdentityProviderMappersStream();
    }

    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersByAliasStream(String brokerAlias) {
        return delegate.getIdentityProviderMappersByAliasStream(brokerAlias);
    }

    public IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model) {
        return delegate.addIdentityProviderMapper(model);
    }

    public void removeIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        delegate.removeIdentityProviderMapper(mapping);
    }

    public void updateIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        delegate.updateIdentityProviderMapper(mapping);
    }

    public IdentityProviderMapperModel getIdentityProviderMapperById(String id) {
        return delegate.getIdentityProviderMapperById(id);
    }

    public IdentityProviderMapperModel getIdentityProviderMapperByName(String brokerAlias, String name) {
        return delegate.getIdentityProviderMapperByName(brokerAlias, name);
    }

    public ComponentModel addComponentModel(ComponentModel model) {
        return delegate.addComponentModel(model);
    }

    public ComponentModel importComponentModel(ComponentModel model) {
        return delegate.importComponentModel(model);
    }

    public void updateComponent(ComponentModel component) {
        delegate.updateComponent(component);
    }

    public void removeComponent(ComponentModel component) {
        delegate.removeComponent(component);
    }

    public void removeComponents(String parentId) {
        delegate.removeComponents(parentId);
    }

    public Stream<ComponentModel> getComponentsStream(String parentId, String providerType) {
        return delegate.getComponentsStream(parentId, providerType);
    }

    public Stream<ComponentModel> getComponentsStream(String parentId) {
        return delegate.getComponentsStream(parentId);
    }

    public Stream<ComponentModel> getComponentsStream() {
        return delegate.getComponentsStream();
    }

    public ComponentModel getComponent(String id) {
        return delegate.getComponent(id);
    }

    public Stream<ComponentModel> getStorageProviders(Class<? extends Provider> storageProviderClass) {
        return delegate.getStorageProviders(storageProviderClass);
    }

    public String getLoginTheme() {
        return delegate.getLoginTheme();
    }

    public void setLoginTheme(String name) {
        delegate.setLoginTheme(name);
    }

    public String getAccountTheme() {
        return delegate.getAccountTheme();
    }

    public void setAccountTheme(String name) {
        delegate.setAccountTheme(name);
    }

    public String getAdminTheme() {
        return delegate.getAdminTheme();
    }

    public void setAdminTheme(String name) {
        delegate.setAdminTheme(name);
    }

    public String getEmailTheme() {
        return delegate.getEmailTheme();
    }

    public void setEmailTheme(String name) {
        delegate.setEmailTheme(name);
    }

    public int getNotBefore() {
        return delegate.getNotBefore();
    }

    public void setNotBefore(int notBefore) {
        delegate.setNotBefore(notBefore);
    }

    public boolean isEventsEnabled() {
        return delegate.isEventsEnabled();
    }

    public void setEventsEnabled(boolean enabled) {
        delegate.setEventsEnabled(enabled);
    }

    public long getEventsExpiration() {
        return delegate.getEventsExpiration();
    }

    public void setEventsExpiration(long expiration) {
        delegate.setEventsExpiration(expiration);
    }

    public Stream<String> getEventsListenersStream() {
        return delegate.getEventsListenersStream();
    }

    public void setEventsListeners(Set<String> listeners) {
        delegate.setEventsListeners(listeners);
    }

    public Stream<String> getEnabledEventTypesStream() {
        return delegate.getEnabledEventTypesStream();
    }

    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        delegate.setEnabledEventTypes(enabledEventTypes);
    }

    public boolean isAdminEventsEnabled() {
        return delegate.isAdminEventsEnabled();
    }

    public void setAdminEventsEnabled(boolean enabled) {
        delegate.setAdminEventsEnabled(enabled);
    }

    public boolean isAdminEventsDetailsEnabled() {
        return delegate.isAdminEventsDetailsEnabled();
    }

    public void setAdminEventsDetailsEnabled(boolean enabled) {
        delegate.setAdminEventsDetailsEnabled(enabled);
    }

    public ClientModel getMasterAdminClient() {
        return delegate.getMasterAdminClient();
    }

    public void setMasterAdminClient(ClientModel client) {
        delegate.setMasterAdminClient(client);
    }

    public RoleModel getDefaultRole() {
        return delegate.getDefaultRole();
    }

    public void setDefaultRole(RoleModel role) {
        delegate.setDefaultRole(role);
    }

    @Override
    public ClientModel getAdminPermissionsClient() {
        return delegate.getAdminPermissionsClient();
    }

    @Override
    public void setAdminPermissionsClient(ClientModel client) {
        delegate.setAdminPermissionsClient(client);
    }

    public boolean isIdentityFederationEnabled() {
        return delegate.isIdentityFederationEnabled();
    }

    public boolean isInternationalizationEnabled() {
        return delegate.isInternationalizationEnabled();
    }

    public void setInternationalizationEnabled(boolean enabled) {
        delegate.setInternationalizationEnabled(enabled);
    }

    public Stream<String> getSupportedLocalesStream() {
        return delegate.getSupportedLocalesStream();
    }

    public void setSupportedLocales(Set<String> locales) {
        delegate.setSupportedLocales(locales);
    }

    public String getDefaultLocale() {
        return delegate.getDefaultLocale();
    }

    public void setDefaultLocale(String locale) {
        delegate.setDefaultLocale(locale);
    }

    public GroupModel createGroup(String name) {
        return delegate.createGroup(name);
    }

    public GroupModel createGroup(String id, String name) {
        return delegate.createGroup(id, name);
    }

    public GroupModel createGroup(String name, GroupModel toParent) {
        return delegate.createGroup(name, toParent);
    }

    public GroupModel createGroup(String id, String name, GroupModel toParent) {
        return delegate.createGroup(id, name, toParent);
    }

    public GroupModel getGroupById(String id) {
        return delegate.getGroupById(id);
    }

    public Stream<GroupModel> getGroupsStream() {
        return delegate.getGroupsStream();
    }

    public Long getGroupsCount(Boolean onlyTopGroups) {
        return delegate.getGroupsCount(onlyTopGroups);
    }

    public Long getGroupsCountByNameContaining(String search) {
        return delegate.getGroupsCountByNameContaining(search);
    }

    @Deprecated
    public Stream<GroupModel> getTopLevelGroupsStream() {
        return delegate.getTopLevelGroupsStream();
    }

    @Deprecated
    public Stream<GroupModel> getTopLevelGroupsStream(Integer first, Integer max) {
        return delegate.getTopLevelGroupsStream(first, max);
    }

    public boolean removeGroup(GroupModel group) {
        return delegate.removeGroup(group);
    }

    public void moveGroup(GroupModel group, GroupModel toParent) {
        delegate.moveGroup(group, toParent);
    }

    public Stream<ClientScopeModel> getClientScopesStream() {
        return delegate.getClientScopesStream();
    }

    public ClientScopeModel addClientScope(String name) {
        return delegate.addClientScope(name);
    }

    public ClientScopeModel addClientScope(String id, String name) {
        return delegate.addClientScope(id, name);
    }

    public boolean removeClientScope(String id) {
        return delegate.removeClientScope(id);
    }

    public ClientScopeModel getClientScopeById(String id) {
        return delegate.getClientScopeById(id);
    }

    public void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        delegate.addDefaultClientScope(clientScope, defaultScope);
    }

    public void removeDefaultClientScope(ClientScopeModel clientScope) {
        delegate.removeDefaultClientScope(clientScope);
    }

    public void createOrUpdateRealmLocalizationTexts(String locale, Map<String, String> localizationTexts) {
        delegate.createOrUpdateRealmLocalizationTexts(locale, localizationTexts);
    }

    public boolean removeRealmLocalizationTexts(String locale) {
        return delegate.removeRealmLocalizationTexts(locale);
    }

    public Map<String, Map<String, String>> getRealmLocalizationTexts() {
        return delegate.getRealmLocalizationTexts();
    }

    public Map<String, String> getRealmLocalizationTextsByLocale(String locale) {
        return delegate.getRealmLocalizationTextsByLocale(locale);
    }

    public Stream<ClientScopeModel> getDefaultClientScopesStream(boolean defaultScope) {
        return delegate.getDefaultClientScopesStream(defaultScope);
    }

    public void addToDefaultRoles(RoleModel role) {
        delegate.addToDefaultRoles(role);
    }

    public ClientInitialAccessModel createClientInitialAccessModel(int expiration, int count) {
        return delegate.createClientInitialAccessModel(expiration, count);
    }

    public ClientInitialAccessModel getClientInitialAccessModel(String id) {
        return delegate.getClientInitialAccessModel(id);
    }

    public void removeClientInitialAccessModel(String id) {
        delegate.removeClientInitialAccessModel(id);
    }

    public Stream<ClientInitialAccessModel> getClientInitialAccesses() {
        return delegate.getClientInitialAccesses();
    }

    public void decreaseRemainingCount(ClientInitialAccessModel clientInitialAccess) {
        delegate.decreaseRemainingCount(clientInitialAccess);
    }

    public RoleModel getRole(String name) {
        return delegate.getRole(name);
    }

    public RoleModel addRole(String name) {
        return delegate.addRole(name);
    }

    public RoleModel addRole(String id, String name) {
        return delegate.addRole(id, name);
    }

    public boolean removeRole(RoleModel role) {
        return delegate.removeRole(role);
    }

    public Stream<RoleModel> getRolesStream() {
        return delegate.getRolesStream();
    }

    public Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
        return delegate.getRolesStream(firstResult, maxResults);
    }

    public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        return delegate.searchForRolesStream(search, first, max);
    }

    @Override
    public boolean isOrganizationsEnabled() {
        return delegate.isOrganizationsEnabled();
    }

    @Override
    public void setOrganizationsEnabled(boolean organizationsEnabled) {
        delegate.setOrganizationsEnabled(organizationsEnabled);
    }

    @Override
    public boolean isAdminPermissionsEnabled() {
        return delegate.isAdminPermissionsEnabled();
    }

    @Override
    public void setAdminPermissionsEnabled(boolean adminPermissionsEnabled) {
        delegate.setAdminPermissionsEnabled(adminPermissionsEnabled);
    }

    @Override
    public boolean isVerifiableCredentialsEnabled() {
        return delegate.isVerifiableCredentialsEnabled();
    }

    @Override
    public void setVerifiableCredentialsEnabled(boolean verifiableCredentialsEnabled) {
        delegate.setVerifiableCredentialsEnabled(verifiableCredentialsEnabled);
    }
}
