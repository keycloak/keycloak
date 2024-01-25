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

package org.keycloak.protocol.oid4vc.issuance.signing.mock;

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
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.WebAuthnPolicy;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class RealmModelMock implements RealmModel {

    private final String realmName;

    public RealmModelMock(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public String getId() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public RoleModel getRole(String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public RoleModel addRole(String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public RoleModel addRole(String id, String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return false;
    }

    @Override
    public Stream<RoleModel> getRolesStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public String getName() {
        return this.realmName;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public String getDisplayName() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setDisplayName(String displayName) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public String getDisplayNameHtml() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setDisplayNameHtml(String displayNameHtml) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public SslRequired getSslRequired() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isRegistrationAllowed() {
        return false;
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isRegistrationEmailAsUsername() {
        return false;
    }

    @Override
    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isRememberMe() {
        return false;
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isEditUsernameAllowed() {
        return false;
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isUserManagedAccessAllowed() {
        return false;
    }

    @Override
    public void setUserManagedAccessAllowed(boolean userManagedAccessAllowed) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setAttribute(String name, String value) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void removeAttribute(String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public String getAttribute(String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Map<String, String> getAttributes() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isBruteForceProtected() {
        return false;
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isPermanentLockout() {
        return false;
    }

    @Override
    public void setPermanentLockout(boolean val) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        return 0;
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getWaitIncrementSeconds() {
        return 0;
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        return 0;
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        return 0;
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getMaxDeltaTimeSeconds() {
        return 0;
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getFailureFactor() {
        return 0;
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isVerifyEmail() {
        return false;
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isLoginWithEmailAllowed() {
        return false;
    }

    @Override
    public void setLoginWithEmailAllowed(boolean loginWithEmailAllowed) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isDuplicateEmailsAllowed() {
        return false;
    }

    @Override
    public void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isResetPasswordAllowed() {
        return false;
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public String getDefaultSignatureAlgorithm() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isRevokeRefreshToken() {
        return false;
    }

    @Override
    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getRefreshTokenMaxReuse() {
        return 0;
    }

    @Override
    public void setRefreshTokenMaxReuse(int revokeRefreshTokenCount) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getSsoSessionIdleTimeout() {
        return 0;
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        return 0;
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getSsoSessionIdleTimeoutRememberMe() {
        return 0;
    }

    @Override
    public void setSsoSessionIdleTimeoutRememberMe(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getSsoSessionMaxLifespanRememberMe() {
        return 0;
    }

    @Override
    public void setSsoSessionMaxLifespanRememberMe(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getOfflineSessionIdleTimeout() {
        return 0;
    }

    @Override
    public void setOfflineSessionIdleTimeout(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getAccessTokenLifespan() {
        return 0;
    }

    @Override
    public boolean isOfflineSessionMaxLifespanEnabled() {
        return false;
    }

    @Override
    public void setOfflineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getOfflineSessionMaxLifespan() {
        return 0;
    }

    @Override
    public void setOfflineSessionMaxLifespan(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getClientSessionIdleTimeout() {
        return 0;
    }

    @Override
    public void setClientSessionIdleTimeout(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getClientSessionMaxLifespan() {
        return 0;
    }

    @Override
    public void setClientSessionMaxLifespan(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getClientOfflineSessionIdleTimeout() {
        return 0;
    }

    @Override
    public void setClientOfflineSessionIdleTimeout(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getClientOfflineSessionMaxLifespan() {
        return 0;
    }

    @Override
    public void setClientOfflineSessionMaxLifespan(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setAccessTokenLifespan(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getAccessTokenLifespanForImplicitFlow() {
        return 0;
    }

    @Override
    public void setAccessTokenLifespanForImplicitFlow(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getAccessCodeLifespan() {
        return 0;
    }

    @Override
    public void setAccessCodeLifespan(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        return 0;
    }

    @Override
    public void setAccessCodeLifespanUserAction(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public OAuth2DeviceConfig getOAuth2DeviceConfig() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public CibaConfig getCibaPolicy() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ParConfig getParPolicy() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Map<String, Integer> getUserActionTokenLifespans() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        return 0;
    }

    @Override
    public void setAccessCodeLifespanLogin(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getActionTokenGeneratedByAdminLifespan() {
        return 0;
    }

    @Override
    public void setActionTokenGeneratedByAdminLifespan(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan() {
        return 0;
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(int seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan(String actionTokenType) {
        return 0;
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(String actionTokenType, Integer seconds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<RequiredCredentialModel> getRequiredCredentialsStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void addRequiredCredential(String cred) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public OTPPolicy getOTPPolicy() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setOTPPolicy(OTPPolicy policy) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicy() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setWebAuthnPolicy(WebAuthnPolicy policy) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicyPasswordless() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setWebAuthnPolicyPasswordless(WebAuthnPolicy policy) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public RoleModel getRoleById(String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<GroupModel> getDefaultGroupsStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void addDefaultGroup(GroupModel group) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void removeDefaultGroup(GroupModel group) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<ClientModel> getClientsStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<ClientModel> getClientsStream(Integer firstResult, Integer maxResults) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Long getClientsCount() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ClientModel addClient(String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean removeClient(String id) {
        return false;
    }

    @Override
    public ClientModel getClientById(String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<ClientModel> searchClientByClientIdStream(String clientId, Integer firstResult, Integer maxResults) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<ClientModel> searchClientByAttributes(Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<ClientModel> searchClientByAuthenticationFlowBindingOverrides(Map<String, String> overrides, Integer firstResult, Integer maxResults) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationFlowModel getBrowserFlow() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setBrowserFlow(AuthenticationFlowModel flow) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationFlowModel getRegistrationFlow() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setRegistrationFlow(AuthenticationFlowModel flow) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationFlowModel getDirectGrantFlow() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setDirectGrantFlow(AuthenticationFlowModel flow) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationFlowModel getResetCredentialsFlow() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setResetCredentialsFlow(AuthenticationFlowModel flow) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationFlowModel getClientAuthenticationFlow() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setClientAuthenticationFlow(AuthenticationFlowModel flow) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationFlowModel getDockerAuthenticationFlow() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setDockerAuthenticationFlow(AuthenticationFlowModel flow) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<AuthenticationFlowModel> getAuthenticationFlowsStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationFlowModel getFlowByAlias(String alias) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void removeAuthenticationFlow(AuthenticationFlowModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<AuthenticationExecutionModel> getAuthenticationExecutionsStream(String flowId) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<AuthenticatorConfigModel> getAuthenticatorConfigsStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void updateAuthenticatorConfig(AuthenticatorConfigModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void updateRequiredActionProvider(RequiredActionProviderModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderById(String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<IdentityProviderModel> getIdentityProvidersStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public IdentityProviderModel getIdentityProviderByAlias(String alias) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void addIdentityProvider(IdentityProviderModel identityProvider) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void removeIdentityProviderByAlias(String alias) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersByAliasStream(String brokerAlias) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void removeIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void updateIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(String brokerAlias, String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ComponentModel addComponentModel(ComponentModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ComponentModel importComponentModel(ComponentModel model) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void updateComponent(ComponentModel component) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void removeComponent(ComponentModel component) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void removeComponents(String parentId) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(String parentId, String providerType) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(String parentId) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<ComponentModel> getComponentsStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ComponentModel getComponent(String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public String getLoginTheme() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setLoginTheme(String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public String getAccountTheme() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setAccountTheme(String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public String getAdminTheme() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setAdminTheme(String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public String getEmailTheme() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setEmailTheme(String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public int getNotBefore() {
        return 0;
    }

    @Override
    public void setNotBefore(int notBefore) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isEventsEnabled() {
        return false;
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public long getEventsExpiration() {
        return 0;
    }

    @Override
    public void setEventsExpiration(long expiration) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<String> getEventsListenersStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<String> getEnabledEventTypesStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isAdminEventsEnabled() {
        return false;
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        return false;
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ClientModel getMasterAdminClient() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setMasterAdminClient(ClientModel client) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public RoleModel getDefaultRole() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setDefaultRole(RoleModel role) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        return false;
    }

    @Override
    public boolean isInternationalizationEnabled() {
        return false;
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<String> getSupportedLocalesStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public String getDefaultLocale() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setDefaultLocale(String locale) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public GroupModel createGroup(String id, String name, GroupModel toParent) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public GroupModel getGroupById(String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Long getGroupsCount(Boolean onlyTopGroups) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Long getGroupsCountByNameContaining(String search) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(Integer first, Integer max) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean removeGroup(GroupModel group) {
        return false;
    }

    @Override
    public void moveGroup(GroupModel group, GroupModel toParent) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesStream() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ClientScopeModel addClientScope(String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ClientScopeModel addClientScope(String id, String name) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean removeClientScope(String id) {
        return false;
    }

    @Override
    public ClientScopeModel getClientScopeById(String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void removeDefaultClientScope(ClientScopeModel clientScope) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void createOrUpdateRealmLocalizationTexts(String locale, Map<String, String> localizationTexts) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public boolean removeRealmLocalizationTexts(String locale) {
        return false;
    }

    @Override
    public Map<String, Map<String, String>> getRealmLocalizationTexts() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Map<String, String> getRealmLocalizationTextsByLocale(String locale) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<ClientScopeModel> getDefaultClientScopesStream(boolean defaultScope) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ClientInitialAccessModel createClientInitialAccessModel(int expiration, int count) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ClientInitialAccessModel getClientInitialAccessModel(String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void removeClientInitialAccessModel(String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<ClientInitialAccessModel> getClientInitialAccesses() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void decreaseRemainingCount(ClientInitialAccessModel clientInitialAccess) {
    }

    @Override
    public int hashCode() {
        return realmName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RealmModelMock rmm) {
            return this.getName().equals((rmm).getName());
        }
        return false;
    }
}
