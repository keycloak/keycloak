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

package org.keycloak.models.cache.infinispan;

import org.keycloak.Config;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.models.cache.CachedRealmModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.cache.infinispan.entities.CachedRealm;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.client.ClientStorageProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdapter implements CachedRealmModel {
    protected CachedRealm cached;
    protected RealmCacheSession cacheSession;
    protected volatile RealmModel updated;
    protected KeycloakSession session;

    public RealmAdapter(KeycloakSession session, CachedRealm cached, RealmCacheSession cacheSession) {
        this.cached = cached;
        this.cacheSession = cacheSession;
        this.session = session;
    }

    @Override
    public RealmModel getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerRealmInvalidation(cached.getId(), cached.getName());
            updated = cacheSession.getRealmDelegate().getRealm(cached.getId());
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
        return updated;
    }

    protected volatile boolean invalidated;

    protected void invalidateFlag() {
        invalidated = true;

    }

    @Override
    public void invalidate() {
        invalidated = true;
        getDelegateForUpdate();
    }

    @Override
    public long getCacheTimestamp() {
        return cached.getCacheTimestamp();
    }

    @Override
    public ConcurrentHashMap getCachedWith() {
        return cached.getCachedWith();
    }

    protected boolean isUpdated() {
        if (updated != null) return true;
        if (!invalidated) return false;
        updated = cacheSession.getRealmDelegate().getRealm(cached.getId());
        if (updated == null) throw new IllegalStateException("Not found in database");
        return true;
    }


    @Override
    public String getId() {
        if (isUpdated()) return updated.getId();
        return cached.getId();
    }

    @Override
    public String getName() {
        if (isUpdated()) return updated.getName();
        return cached.getName();
    }

    @Override
    public void setName(String name) {
        getDelegateForUpdate();
        updated.setName(name);
    }

    @Override
    public String getDisplayName() {
        if (isUpdated()) return updated.getDisplayName();
        return cached.getDisplayName();
    }

    @Override
    public void setDisplayName(String displayName) {
        getDelegateForUpdate();
        updated.setDisplayName(displayName);
    }

    @Override
    public String getDisplayNameHtml() {
        if (isUpdated()) return updated.getDisplayNameHtml();
        return cached.getDisplayNameHtml();
    }

    @Override
    public void setDisplayNameHtml(String displayNameHtml) {
        getDelegateForUpdate();
        updated.setDisplayNameHtml(displayNameHtml);
    }

    @Override
    public boolean isEnabled() {
        if (isUpdated()) return updated.isEnabled();
        return cached.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setEnabled(enabled);
    }

    @Override
    public boolean isUserManagedAccessAllowed() {
        if (isUpdated()) return updated.isEnabled();
        return cached.isAllowUserManagedAccess();
    }

    @Override
    public void setUserManagedAccessAllowed(boolean userManagedAccessAllowed) {
        getDelegateForUpdate();
        updated.setUserManagedAccessAllowed(userManagedAccessAllowed);
    }

    @Override
    public SslRequired getSslRequired() {
        if (isUpdated()) return updated.getSslRequired();
        return cached.getSslRequired();
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        getDelegateForUpdate();
        updated.setSslRequired(sslRequired);
    }

    @Override
    public boolean isRegistrationAllowed() {
        if (isUpdated()) return updated.isRegistrationAllowed();
        return cached.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        getDelegateForUpdate();
        updated.setRegistrationAllowed(registrationAllowed);
    }

    @Override
    public boolean isRegistrationEmailAsUsername() {
        if (isUpdated()) return updated.isRegistrationEmailAsUsername();
        return cached.isRegistrationEmailAsUsername();
    }

    @Override
    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        getDelegateForUpdate();
        updated.setRegistrationEmailAsUsername(registrationEmailAsUsername);
    }

    @Override
    public boolean isRememberMe() {
        if (isUpdated()) return updated.isRememberMe();
        return cached.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        getDelegateForUpdate();
        updated.setRememberMe(rememberMe);
    }

    @Override
    public String getDefaultSignatureAlgorithm() {
        if(isUpdated()) return updated.getDefaultSignatureAlgorithm();
        return cached.getDefaultSignatureAlgorithm();
    }

    @Override
    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        getDelegateForUpdate();
        updated.setDefaultSignatureAlgorithm(defaultSignatureAlgorithm);
    }

    @Override
    public boolean isBruteForceProtected() {
        if (isUpdated()) return updated.isBruteForceProtected();
        return cached.isBruteForceProtected();
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        getDelegateForUpdate();
        updated.setBruteForceProtected(value);
    }

    @Override
    public boolean isPermanentLockout() {
        if(isUpdated()) return updated.isPermanentLockout();
        return cached.isPermanentLockout();
    }

    @Override
    public void setPermanentLockout(final boolean val) {
        getDelegateForUpdate();
        updated.setPermanentLockout(val);
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        if (isUpdated()) return updated.getMaxFailureWaitSeconds();
        return cached.getMaxFailureWaitSeconds();
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        getDelegateForUpdate();
        updated.setMaxFailureWaitSeconds(val);
    }

    @Override
    public int getWaitIncrementSeconds() {
        if (isUpdated()) return updated.getWaitIncrementSeconds();
        return cached.getWaitIncrementSeconds();
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        getDelegateForUpdate();
        updated.setWaitIncrementSeconds(val);
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        if (isUpdated()) return updated.getMinimumQuickLoginWaitSeconds();
        return cached.getMinimumQuickLoginWaitSeconds();
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        getDelegateForUpdate();
        updated.setMinimumQuickLoginWaitSeconds(val);
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        if (isUpdated()) return updated.getQuickLoginCheckMilliSeconds();
        return cached.getQuickLoginCheckMilliSeconds();
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        getDelegateForUpdate();
        updated.setQuickLoginCheckMilliSeconds(val);
    }

    @Override
    public int getMaxDeltaTimeSeconds() {
        if (isUpdated()) return updated.getMaxDeltaTimeSeconds();
        return cached.getMaxDeltaTimeSeconds();
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        getDelegateForUpdate();
        updated.setMaxDeltaTimeSeconds(val);
    }

    @Override
    public int getFailureFactor() {
        if (isUpdated()) return updated.getFailureFactor();
        return cached.getFailureFactor();
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        getDelegateForUpdate();
        updated.setFailureFactor(failureFactor);
    }

    @Override
    public boolean isVerifyEmail() {
        if (isUpdated()) return updated.isVerifyEmail();
        return cached.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        getDelegateForUpdate();
        updated.setVerifyEmail(verifyEmail);
    }

    @Override
    public boolean isLoginWithEmailAllowed() {
        if (isUpdated()) return updated.isLoginWithEmailAllowed();
        return cached.isLoginWithEmailAllowed();
    }

    @Override
    public void setLoginWithEmailAllowed(boolean loginWithEmailAllowed) {
        getDelegateForUpdate();
        updated.setLoginWithEmailAllowed(loginWithEmailAllowed);
    }

    @Override
    public boolean isDuplicateEmailsAllowed() {
        if (isUpdated()) return updated.isDuplicateEmailsAllowed();
        return cached.isDuplicateEmailsAllowed();
    }

    @Override
    public void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed) {
        getDelegateForUpdate();
        updated.setDuplicateEmailsAllowed(duplicateEmailsAllowed);
    }

    @Override
    public boolean isResetPasswordAllowed() {
        if (isUpdated()) return updated.isResetPasswordAllowed();
        return cached.isResetPasswordAllowed();
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        getDelegateForUpdate();
        updated.setResetPasswordAllowed(resetPasswordAllowed);
    }

    @Override
    public boolean isEditUsernameAllowed() {
        if (isUpdated()) return updated.isEditUsernameAllowed();
        return cached.isEditUsernameAllowed();
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        getDelegateForUpdate();
        updated.setEditUsernameAllowed(editUsernameAllowed);
    }

    @Override
    public boolean isRevokeRefreshToken() {
        if (isUpdated()) return updated.isRevokeRefreshToken();
        return cached.isRevokeRefreshToken();
    }

    @Override
    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        getDelegateForUpdate();
        updated.setRevokeRefreshToken(revokeRefreshToken);
    }

    @Override
    public int getRefreshTokenMaxReuse() {
        if (isUpdated()) return updated.getRefreshTokenMaxReuse();
        return cached.getRefreshTokenMaxReuse();
    }

    @Override
    public void setRefreshTokenMaxReuse(int refreshTokenMaxReuse) {
        getDelegateForUpdate();
        updated.setRefreshTokenMaxReuse(refreshTokenMaxReuse);
    }

    @Override
    public int getSsoSessionIdleTimeout() {
        if (isUpdated()) return updated.getSsoSessionIdleTimeout();
        return cached.getSsoSessionIdleTimeout();
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        getDelegateForUpdate();
        updated.setSsoSessionIdleTimeout(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        if (isUpdated()) return updated.getSsoSessionMaxLifespan();
        return cached.getSsoSessionMaxLifespan();
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setSsoSessionMaxLifespan(seconds);
    }

    @Override
    public int getSsoSessionIdleTimeoutRememberMe() {
        if (updated != null) return updated.getSsoSessionIdleTimeoutRememberMe();
        return cached.getSsoSessionIdleTimeoutRememberMe();
    }

    @Override
    public void setSsoSessionIdleTimeoutRememberMe(int seconds) {
        getDelegateForUpdate();
        updated.setSsoSessionIdleTimeoutRememberMe(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespanRememberMe() {
        if (updated != null) return updated.getSsoSessionMaxLifespanRememberMe();
        return cached.getSsoSessionMaxLifespanRememberMe();
    }

    @Override
    public void setSsoSessionMaxLifespanRememberMe(int seconds) {
        getDelegateForUpdate();
        updated.setSsoSessionMaxLifespanRememberMe(seconds);
    }

    @Override
    public int getOfflineSessionIdleTimeout() {
        if (isUpdated()) return updated.getOfflineSessionIdleTimeout();
        return cached.getOfflineSessionIdleTimeout();
    }


    @Override
    public void setOfflineSessionIdleTimeout(int seconds) {
        getDelegateForUpdate();
        updated.setOfflineSessionIdleTimeout(seconds);
    }

    // KEYCLOAK-7688 Offline Session Max for Offline Token
    @Override
    public boolean isOfflineSessionMaxLifespanEnabled() {
        if (isUpdated()) return updated.isOfflineSessionMaxLifespanEnabled();
        return cached.isOfflineSessionMaxLifespanEnabled();
    }

    @Override
    public void setOfflineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {
        getDelegateForUpdate();
        updated.setOfflineSessionMaxLifespanEnabled(offlineSessionMaxLifespanEnabled);
    }

    @Override
    public int getOfflineSessionMaxLifespan() {
        if (isUpdated()) return updated.getOfflineSessionMaxLifespan();
        return cached.getOfflineSessionMaxLifespan();
    }

    @Override
    public void setOfflineSessionMaxLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setOfflineSessionMaxLifespan(seconds);
    }

    @Override
    public int getAccessTokenLifespan() {
        if (isUpdated()) return updated.getAccessTokenLifespan();
        return cached.getAccessTokenLifespan();
    }

    @Override
    public void setAccessTokenLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setAccessTokenLifespan(seconds);
    }

    @Override
    public int getAccessTokenLifespanForImplicitFlow() {
        if (isUpdated()) return updated.getAccessTokenLifespanForImplicitFlow();
        return cached.getAccessTokenLifespanForImplicitFlow();
    }

    @Override
    public void setAccessTokenLifespanForImplicitFlow(int seconds) {
        getDelegateForUpdate();
        updated.setAccessTokenLifespanForImplicitFlow(seconds);
    }

    @Override
    public int getAccessCodeLifespan() {
        if (isUpdated()) return updated.getAccessCodeLifespan();
        return cached.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setAccessCodeLifespan(seconds);
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        if (isUpdated()) return updated.getAccessCodeLifespanUserAction();
        return cached.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(int seconds) {
        getDelegateForUpdate();
        updated.setAccessCodeLifespanUserAction(seconds);
    }

    @Override
    public Map<String, Integer> getUserActionTokenLifespans() {
        if (isUpdated()) return updated.getUserActionTokenLifespans();
        return cached.getUserActionTokenLifespans();
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        if (isUpdated()) return updated.getAccessCodeLifespanLogin();
        return cached.getAccessCodeLifespanLogin();
    }

    @Override
    public void setAccessCodeLifespanLogin(int seconds) {
        getDelegateForUpdate();
        updated.setAccessCodeLifespanLogin(seconds);
    }

    @Override
    public int getActionTokenGeneratedByAdminLifespan() {
        if (isUpdated()) return updated.getActionTokenGeneratedByAdminLifespan();
        return cached.getActionTokenGeneratedByAdminLifespan();
    }

    @Override
    public void setActionTokenGeneratedByAdminLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setActionTokenGeneratedByAdminLifespan(seconds);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan() {
        if (isUpdated()) return updated.getActionTokenGeneratedByUserLifespan();
        return cached.getActionTokenGeneratedByUserLifespan();
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setActionTokenGeneratedByUserLifespan(seconds);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan(String actionTokenId) {
        if (isUpdated()) return updated.getActionTokenGeneratedByUserLifespan(actionTokenId);
        return cached.getActionTokenGeneratedByUserLifespan(actionTokenId);
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(String actionTokenId, Integer seconds) {
        if (seconds != null) {
            getDelegateForUpdate();
            updated.setActionTokenGeneratedByUserLifespan(actionTokenId, seconds);
        }
    }

    @Override
    public List<RequiredCredentialModel> getRequiredCredentials() {
        if (isUpdated()) return updated.getRequiredCredentials();
        return cached.getRequiredCredentials();
    }

    @Override
    public void addRequiredCredential(String cred) {
        getDelegateForUpdate();
        updated.addRequiredCredential(cred);
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        if (isUpdated()) return updated.getPasswordPolicy();
        return cached.getPasswordPolicy();
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        getDelegateForUpdate();
        updated.setPasswordPolicy(policy);
    }

    @Override
    public OTPPolicy getOTPPolicy() {
        if (isUpdated()) return updated.getOTPPolicy();
        return cached.getOtpPolicy();
    }

    @Override
    public void setOTPPolicy(OTPPolicy policy) {
        getDelegateForUpdate();
        updated.setOTPPolicy(policy);

    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicy() {
        if (isUpdated()) return updated.getWebAuthnPolicy();
        return cached.getWebAuthnPolicy();
    }

    @Override
    public void setWebAuthnPolicy(WebAuthnPolicy policy) {
        getDelegateForUpdate();
        updated.setWebAuthnPolicy(policy);
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicyPasswordless() {
        if (isUpdated()) return updated.getWebAuthnPolicyPasswordless();
        return cached.getWebAuthnPasswordlessPolicy();
    }

    @Override
    public void setWebAuthnPolicyPasswordless(WebAuthnPolicy policy) {
        getDelegateForUpdate();
        updated.setWebAuthnPolicyPasswordless(policy);
    }

    @Override
    public RoleModel getRoleById(String id) {
        if (isUpdated()) return updated.getRoleById(id);
        return cacheSession.getRoleById(id, this);
     }

    @Override
    public List<GroupModel> getDefaultGroups() {
        if (isUpdated()) return updated.getDefaultGroups();

        List<GroupModel> defaultGroups = new LinkedList<>();
        for (String id : cached.getDefaultGroups()) {
            defaultGroups.add(cacheSession.getGroupById(id, this));
        }
        return Collections.unmodifiableList(defaultGroups);

    }

    @Override
    public void addDefaultGroup(GroupModel group) {
        getDelegateForUpdate();
        updated.addDefaultGroup(group);

    }

    @Override
    public void removeDefaultGroup(GroupModel group) {
        getDelegateForUpdate();
        updated.removeDefaultGroup(group);

    }

    @Override
    public List<String> getDefaultRoles() {
        if (isUpdated()) return updated.getDefaultRoles();
        return cached.getDefaultRoles();
    }

    @Override
    public void addDefaultRole(String name) {
        getDelegateForUpdate();
        updated.addDefaultRole(name);
    }

    @Override
    public void updateDefaultRoles(String... defaultRoles) {
        getDelegateForUpdate();
        updated.updateDefaultRoles(defaultRoles);
    }

    @Override
    public void removeDefaultRoles(String... defaultRoles) {
        getDelegateForUpdate();
        updated.removeDefaultRoles(defaultRoles);

    }

    @Override
    public List<ClientModel> getClients() {
        return cacheSession.getClients(this);
    }

    @Override
    public List<ClientModel> getAlwaysDisplayInConsoleClients() {
        return cacheSession.getAlwaysDisplayInConsoleClients(this);
    }

    @Override
    public ClientModel addClient(String name) {
        return cacheSession.addClient(this, name);
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        return cacheSession.addClient(this, id, clientId);
    }

    @Override
    public boolean removeClient(String id) {
        return cacheSession.removeClient(id, this);
    }

    @Override
    public ClientModel getClientById(String id) {
        if (isUpdated()) return updated.getClientById(id);
        return cacheSession.getClientById(id, this);
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        return cacheSession.getClientByClientId(clientId, this);
    }

    @Override
    public List<ClientModel> searchClientByClientId(String clientId, Integer firstResult, Integer maxResults) {
        return cacheSession.searchClientsByClientId(clientId, firstResult, maxResults, this);
    }

    @Override
    public List<ClientModel> getClients(Integer firstResult, Integer maxResults) {
        return cacheSession.getClients(this, firstResult, maxResults);
    }

    @Override
    public Long getClientsCount() {
        return cacheSession.getClientsCount(this);
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        getDelegateForUpdate();
        updated.updateRequiredCredentials(creds);
    }

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        if (isUpdated()) return updated.getBrowserSecurityHeaders();
        return cached.getBrowserSecurityHeaders();
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        getDelegateForUpdate();
        updated.setBrowserSecurityHeaders(headers);

    }

    @Override
    public Map<String, String> getSmtpConfig() {
        if (isUpdated()) return updated.getSmtpConfig();
        return cached.getSmtpConfig();
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        getDelegateForUpdate();
        updated.setSmtpConfig(smtpConfig);
    }


    @Override
    public List<IdentityProviderModel> getIdentityProviders() {
        if (isUpdated()) return updated.getIdentityProviders();
        return cached.getIdentityProviders();
    }

    @Override
    public IdentityProviderModel getIdentityProviderByAlias(String alias) {
        if (isUpdated()) return updated.getIdentityProviderByAlias(alias);
        for (IdentityProviderModel identityProviderModel : getIdentityProviders()) {
            if (identityProviderModel.getAlias().equals(alias)) {
                return identityProviderModel;
            }
        }

        return null;
    }

    @Override
    public void addIdentityProvider(IdentityProviderModel identityProvider) {
        getDelegateForUpdate();
        updated.addIdentityProvider(identityProvider);
    }

    @Override
    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        getDelegateForUpdate();
        updated.updateIdentityProvider(identityProvider);
    }

    @Override
    public void removeIdentityProviderByAlias(String alias) {
        getDelegateForUpdate();
        updated.removeIdentityProviderByAlias(alias);
    }

    @Override
    public String getLoginTheme() {
        if (isUpdated()) return updated.getLoginTheme();
        return cached.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        getDelegateForUpdate();
        updated.setLoginTheme(name);
    }

    @Override
    public String getAccountTheme() {
        if (isUpdated()) return updated.getAccountTheme();
        return cached.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        getDelegateForUpdate();
        updated.setAccountTheme(name);
    }

    @Override
    public String getAdminTheme() {
        if (isUpdated()) return updated.getAdminTheme();
        return cached.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        getDelegateForUpdate();
        updated.setAdminTheme(name);
    }

    @Override
    public String getEmailTheme() {
        if (isUpdated()) return updated.getEmailTheme();
        return cached.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        getDelegateForUpdate();
        updated.setEmailTheme(name);
    }

    @Override
    public int getNotBefore() {
        if (isUpdated()) return updated.getNotBefore();
        return cached.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        getDelegateForUpdate();
        updated.setNotBefore(notBefore);
    }

    @Override
    public boolean isEventsEnabled() {
        if (isUpdated()) return updated.isEventsEnabled();
        return cached.isEventsEnabled();
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setEventsEnabled(enabled);
    }

    @Override
    public long getEventsExpiration() {
        if (isUpdated()) return updated.getEventsExpiration();
        return cached.getEventsExpiration();
    }

    @Override
    public void setEventsExpiration(long expiration) {
        getDelegateForUpdate();
        updated.setEventsExpiration(expiration);
    }

    @Override
    public Set<String> getEventsListeners() {
        if (isUpdated()) return updated.getEventsListeners();
        return cached.getEventsListeners();
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        getDelegateForUpdate();
        updated.setEventsListeners(listeners);
    }

    @Override
    public Set<String> getEnabledEventTypes() {
        if (isUpdated()) return updated.getEnabledEventTypes();
        return cached.getEnabledEventTypes();
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        getDelegateForUpdate();
        updated.setEnabledEventTypes(enabledEventTypes);
    }

    @Override
    public boolean isAdminEventsEnabled() {
        if (isUpdated()) return updated.isAdminEventsEnabled();
        return cached.isAdminEventsEnabled();
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setAdminEventsEnabled(enabled);
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        if (isUpdated()) return updated.isAdminEventsDetailsEnabled();
        return cached.isAdminEventsDetailsEnabled();
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setAdminEventsDetailsEnabled(enabled);
    }

    @Override
    public ClientModel getMasterAdminClient() {
        return cached.getMasterAdminClient()==null ? null : cacheSession.getRealm(Config.getAdminRealm()).getClientById(cached.getMasterAdminClient());
    }

    @Override
    public void setMasterAdminClient(ClientModel client) {
        getDelegateForUpdate();
        updated.setMasterAdminClient(client);
    }

    @Override
    public RoleModel getRole(String name) {
        return cacheSession.getRealmRole(this, name);
    }

    @Override
    public Set<RoleModel> getRoles() {
        return cacheSession.getRealmRoles(this);
    }
    
    @Override
    public Set<RoleModel> getRoles(Integer first, Integer max) {
        return cacheSession.getRealmRoles(this, first, max);
    }

    @Override
    public Set<RoleModel> searchForRoles(String search, Integer first, Integer max) {
        return cacheSession.searchForRoles(this, search, first, max);
    }
    
    @Override
    public RoleModel addRole(String name) {
        return cacheSession.addRealmRole(this, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return cacheSession.addRealmRole(this, id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return cacheSession.removeRole(this, role);
    }


    @Override
    public boolean isIdentityFederationEnabled() {
        if (isUpdated()) return updated.isIdentityFederationEnabled();
        return cached.isIdentityFederationEnabled();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RealmModel)) return false;

        RealmModel that = (RealmModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean isInternationalizationEnabled() {
        if (isUpdated()) return updated.isInternationalizationEnabled();
        return cached.isInternationalizationEnabled();
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setInternationalizationEnabled(enabled);
    }

    @Override
    public Set<String> getSupportedLocales() {
        if (isUpdated()) return updated.getSupportedLocales();
        return cached.getSupportedLocales();
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        getDelegateForUpdate();
        updated.setSupportedLocales(locales);
    }

    @Override
    public String getDefaultLocale() {
        if (isUpdated()) return updated.getDefaultLocale();
        return cached.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String locale) {
        updated.setDefaultLocale(locale);
    }

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappers() {
        if (isUpdated()) return updated.getIdentityProviderMappers();
        return cached.getIdentityProviderMapperSet();
    }

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(String brokerAlias) {
        if (isUpdated()) return updated.getIdentityProviderMappersByAlias(brokerAlias);
        Set<IdentityProviderMapperModel> mappings = new HashSet<>();
        List<IdentityProviderMapperModel> list = cached.getIdentityProviderMappers().getList(brokerAlias);
        for (IdentityProviderMapperModel entity : list) {
            mappings.add(entity);
        }
        return Collections.unmodifiableSet(mappings);
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model) {
        getDelegateForUpdate();
        return updated.addIdentityProviderMapper(model);
    }

    @Override
    public void removeIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        getDelegateForUpdate();
        updated.removeIdentityProviderMapper(mapping);
    }

    @Override
    public void updateIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        getDelegateForUpdate();
        updated.updateIdentityProviderMapper(mapping);
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(String id) {
        if (isUpdated()) return updated.getIdentityProviderMapperById(id);
        for (List<IdentityProviderMapperModel> models : cached.getIdentityProviderMappers().values()) {
            for (IdentityProviderMapperModel model : models) {
                if (model.getId().equals(id)) return model;
            }
        }
        return null;
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(String alias, String name) {
        if (isUpdated()) return updated.getIdentityProviderMapperByName(alias, name);
        List<IdentityProviderMapperModel> models = cached.getIdentityProviderMappers().getList(alias);
        if (models == null) return null;
        for (IdentityProviderMapperModel model : models) {
            if (model.getName().equals(name)) return model;
        }
        return null;
    }

    @Override
    public AuthenticationFlowModel getBrowserFlow() {
        if (isUpdated()) return updated.getBrowserFlow();
        return cached.getBrowserFlow();
    }

    @Override
    public void setBrowserFlow(AuthenticationFlowModel flow) {
        getDelegateForUpdate();
        updated.setBrowserFlow(flow);

    }

    @Override
    public AuthenticationFlowModel getRegistrationFlow() {
        if (isUpdated()) return updated.getRegistrationFlow();
        return cached.getRegistrationFlow();
    }

    @Override
    public void setRegistrationFlow(AuthenticationFlowModel flow) {
        getDelegateForUpdate();
        updated.setRegistrationFlow(flow);

    }

    @Override
    public AuthenticationFlowModel getDirectGrantFlow() {
        if (isUpdated()) return updated.getDirectGrantFlow();
        return cached.getDirectGrantFlow();
    }

    @Override
    public void setDirectGrantFlow(AuthenticationFlowModel flow) {
        getDelegateForUpdate();
        updated.setDirectGrantFlow(flow);

    }
    @Override
    public AuthenticationFlowModel getResetCredentialsFlow() {
        if (isUpdated()) return updated.getResetCredentialsFlow();
        return cached.getResetCredentialsFlow();
    }

    @Override
    public void setResetCredentialsFlow(AuthenticationFlowModel flow) {
        getDelegateForUpdate();
        updated.setResetCredentialsFlow(flow);

    }

    @Override
    public AuthenticationFlowModel getClientAuthenticationFlow() {
        if (isUpdated()) return updated.getClientAuthenticationFlow();
        return cached.getClientAuthenticationFlow();
    }

    @Override
    public void setClientAuthenticationFlow(AuthenticationFlowModel flow) {
        getDelegateForUpdate();
        updated.setClientAuthenticationFlow(flow);
    }

    @Override
    public AuthenticationFlowModel getDockerAuthenticationFlow() {
        if (isUpdated()) return updated.getDockerAuthenticationFlow();
        return cached.getDockerAuthenticationFlow();
    }

    @Override
    public void setDockerAuthenticationFlow(final AuthenticationFlowModel flow) {
        getDelegateForUpdate();
        updated.setDockerAuthenticationFlow(flow);
    }

    @Override
    public List<AuthenticationFlowModel> getAuthenticationFlows() {
        if (isUpdated()) return updated.getAuthenticationFlows();
        return cached.getAuthenticationFlowList();
    }

    @Override
    public AuthenticationFlowModel getFlowByAlias(String alias) {
        for (AuthenticationFlowModel flow : getAuthenticationFlows()) {
            if (flow.getAlias().equals(alias)) {
                return flow;
            }
        }
        return null;
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias) {
        for (AuthenticatorConfigModel config : getAuthenticatorConfigs()) {
            if (config.getAlias().equals(alias)) {
                return config;
            }
        }
        return null;
    }


    @Override
    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        getDelegateForUpdate();
        return updated.addAuthenticationFlow(model);
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String id) {
        if (isUpdated()) return updated.getAuthenticationFlowById(id);
        return cached.getAuthenticationFlows().get(id);
    }

    @Override
    public void removeAuthenticationFlow(AuthenticationFlowModel model) {
        getDelegateForUpdate();
        updated.removeAuthenticationFlow(model);

    }

    @Override
    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        getDelegateForUpdate();
        updated.updateAuthenticationFlow(model);

    }

    @Override
    public List<AuthenticationExecutionModel> getAuthenticationExecutions(String flowId) {
        if (isUpdated()) return updated.getAuthenticationExecutions(flowId);
        return cached.getAuthenticationExecutions().get(flowId);
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        if (isUpdated()) return updated.getAuthenticationExecutionById(id);
        return cached.getExecutionsById().get(id);
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId) {
        if (isUpdated()) return updated.getAuthenticationExecutionByFlowId(flowId);
        return cached.getAuthenticationExecutionByFlowId(flowId);
    }

    @Override
    public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
        getDelegateForUpdate();
        return updated.addAuthenticatorExecution(model);
    }

    @Override
    public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {
        getDelegateForUpdate();
        updated.updateAuthenticatorExecution(model);

    }

    @Override
    public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {
        getDelegateForUpdate();
        updated.removeAuthenticatorExecution(model);

    }

    @Override
    public List<AuthenticatorConfigModel> getAuthenticatorConfigs() {
        if (isUpdated()) return updated.getAuthenticatorConfigs();
        List<AuthenticatorConfigModel> models = new ArrayList<>();
        models.addAll(cached.getAuthenticatorConfigs().values());
        return Collections.unmodifiableList(models);
    }

    @Override
    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        getDelegateForUpdate();
        return updated.addAuthenticatorConfig(model);
    }

    @Override
    public void updateAuthenticatorConfig(AuthenticatorConfigModel model) {
        getDelegateForUpdate();
        updated.updateAuthenticatorConfig(model);

    }

    @Override
    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        getDelegateForUpdate();
        updated.removeAuthenticatorConfig(model);

    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        if (isUpdated()) return updated.getAuthenticatorConfigById(id);
        return cached.getAuthenticatorConfigs().get(id);
    }

    @Override
    public List<RequiredActionProviderModel> getRequiredActionProviders() {
        if (isUpdated()) return updated.getRequiredActionProviders();
        return cached.getRequiredActionProviderList();
    }

    @Override
    public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
        getDelegateForUpdate();
        return updated.addRequiredActionProvider(model);
    }

    @Override
    public void updateRequiredActionProvider(RequiredActionProviderModel model) {
        getDelegateForUpdate();
        updated.updateRequiredActionProvider(model);

    }

    @Override
    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        getDelegateForUpdate();
        updated.removeRequiredActionProvider(model);

    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderById(String id) {
        if (isUpdated()) return updated.getRequiredActionProviderById(id);
        return cached.getRequiredActionProviders().get(id);
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        if (isUpdated()) return updated.getRequiredActionProviderByAlias(alias);
        return cached.getRequiredActionProvidersByAlias().get(alias);
    }

    @Override
    public GroupModel createGroup(String name) {
        return cacheSession.createGroup(this, name);
    }

    @Override
    public GroupModel createGroup(String id, String name) {
        return cacheSession.createGroup(this, id, name);
    }

    @Override
    public void moveGroup(GroupModel group, GroupModel toParent) {
        cacheSession.moveGroup(this, group, toParent);
    }

    @Override
    public GroupModel getGroupById(String id) {
        return cacheSession.getGroupById(id, this);
    }

    @Override
    public List<GroupModel> getGroups() {
        return cacheSession.getGroups(this);
    }

    @Override
    public Long getGroupsCount(Boolean onlyTopGroups) {
        return cacheSession.getGroupsCount(this, onlyTopGroups);
    }

    @Override
    public Long getGroupsCountByNameContaining(String search) {
        return cacheSession.getGroupsCountByNameContaining(this, search);
    }

    @Override
    public List<GroupModel> getTopLevelGroups() {
        return cacheSession.getTopLevelGroups(this);
    }

    @Override
    public List<GroupModel> getTopLevelGroups(Integer first, Integer max) {
        return cacheSession.getTopLevelGroups(this, first, max);
    }

    @Override
    public List<GroupModel> searchForGroupByName(String search, Integer first, Integer max) {
        return cacheSession.searchForGroupByName(this, search, first, max);
    }

    @Override
    public boolean removeGroup(GroupModel group) {
        return cacheSession.removeGroup(this, group);
    }

    @Override
    public List<ClientScopeModel> getClientScopes() {
        if (isUpdated()) return updated.getClientScopes();
        List<String> clientScopes = cached.getClientScopes();
        if (clientScopes.isEmpty()) return Collections.EMPTY_LIST;
        List<ClientScopeModel> apps = new LinkedList<>();
        for (String id : clientScopes) {
            ClientScopeModel model = cacheSession.getClientScopeById(id, this);
            if (model == null) {
                throw new IllegalStateException("Cached clientScope not found: " + id);
            }
            apps.add(model);
        }
        return Collections.unmodifiableList(apps);

    }

    @Override
    public ClientScopeModel addClientScope(String name) {
        getDelegateForUpdate();
        ClientScopeModel app = updated.addClientScope(name);
        cacheSession.registerClientScopeInvalidation(app.getId());
        return app;
    }

    @Override
    public ClientScopeModel addClientScope(String id, String name) {
        getDelegateForUpdate();
        ClientScopeModel app =  updated.addClientScope(id, name);
        cacheSession.registerClientScopeInvalidation(app.getId());
        return app;
    }

    @Override
    public boolean removeClientScope(String id) {
        cacheSession.registerClientScopeInvalidation(id);
        getDelegateForUpdate();
        return updated.removeClientScope(id);
    }

    @Override
    public ClientScopeModel getClientScopeById(String id) {
        if (isUpdated()) return updated.getClientScopeById(id);
        return cacheSession.getClientScopeById(id, this);
    }

    @Override
    public void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        getDelegateForUpdate();
        updated.addDefaultClientScope(clientScope, defaultScope);
    }

    @Override
    public void removeDefaultClientScope(ClientScopeModel clientScope) {
        getDelegateForUpdate();
        updated.removeDefaultClientScope(clientScope);
    }

    @Override
    public List<ClientScopeModel> getDefaultClientScopes(boolean defaultScope) {
        if (isUpdated()) return updated.getDefaultClientScopes(defaultScope);

        List<String> clientScopeIds = defaultScope ? cached.getDefaultDefaultClientScopes() : cached.getOptionalDefaultClientScopes();

        List<ClientScopeModel> clientScopes = new LinkedList<>();
        for (String scopeId : clientScopeIds) {
            ClientScopeModel clientScope = cacheSession.getClientScopeById(scopeId, this);
            if (clientScope != null) {
                clientScopes.add(clientScope);
            }
        }
        return clientScopes;
    }

    @Override
    public ComponentModel addComponentModel(ComponentModel model) {
        getDelegateForUpdate();
        executeEvictions(model);
        return updated.addComponentModel(model);
    }

    @Override
    public ComponentModel importComponentModel(ComponentModel model) {
        getDelegateForUpdate();
        executeEvictions(model);
        return updated.importComponentModel(model);
    }

    public void executeEvictions(ComponentModel model) {
        if (model == null) return;
        
        // if user cache is disabled this is null
        UserCache userCache = session.userCache(); 
        if (userCache != null) {        
          // If not realm component, check to see if it is a user storage provider child component (i.e. LDAP mapper)
          if (model.getParentId() != null && !model.getParentId().equals(getId())) {
              ComponentModel parent = getComponent(model.getParentId());
              if (parent != null && UserStorageProvider.class.getName().equals(parent.getProviderType())) {
                userCache.evict(this);
              }
              return;
          }
  
          // invalidate entire user cache if we're dealing with user storage SPI
          if (UserStorageProvider.class.getName().equals(model.getProviderType())) {
            userCache.evict(this);
          }
        }
        
        // invalidate entire realm if we're dealing with client storage SPI
        // entire realm because of client roles, client lists, and clients
        if (ClientStorageProvider.class.getName().equals(model.getProviderType())) {
            cacheSession.evictRealmOnRemoval(this);
        }
    }

    @Override
    public void updateComponent(ComponentModel component) {
        getDelegateForUpdate();
        executeEvictions(component);
        updated.updateComponent(component);

    }

    @Override
    public void removeComponent(ComponentModel component) {
        getDelegateForUpdate();
        executeEvictions(component);
        updated.removeComponent(component);

    }

    @Override
    public void removeComponents(String parentId) {
        getDelegateForUpdate();
        updated.removeComponents(parentId);

    }

    @Override
    public List<ComponentModel> getComponents(String parentId, String providerType) {
        if (isUpdated()) return updated.getComponents(parentId, providerType);
        List<ComponentModel> components = cached.getComponentsByParentAndType().getList(parentId + providerType);
        if (components == null) return Collections.EMPTY_LIST;
        return Collections.unmodifiableList(components);
    }

    @Override
    public List<ComponentModel> getComponents(String parentId) {
        if (isUpdated()) return updated.getComponents(parentId);
        List<ComponentModel> components = cached.getComponentsByParent().getList(parentId);
        if (components == null) return Collections.EMPTY_LIST;
        return Collections.unmodifiableList(components);
    }

    @Override
    public List<ComponentModel> getComponents() {
        if (isUpdated()) return updated.getComponents();
        List<ComponentModel> results = new LinkedList<>();
        results.addAll(cached.getComponents().values());
         return Collections.unmodifiableList(results);
    }

    @Override
    public ComponentModel getComponent(String id) {
        if (isUpdated()) return updated.getComponent(id);
        return cached.getComponents().get(id);
    }

    public void setAttribute(String name, String value) {
        getDelegateForUpdate();
        updated.setAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, Boolean value) {
        getDelegateForUpdate();
        updated.setAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, Integer value) {
        getDelegateForUpdate();
        updated.setAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, Long value) {
        getDelegateForUpdate();
        updated.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        getDelegateForUpdate();
        updated.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        if (isUpdated()) return updated.getAttribute(name);
        return cached.getAttribute(name);
    }

    @Override
    public Integer getAttribute(String name, Integer defaultValue) {
        if (isUpdated()) return updated.getAttribute(name, defaultValue);
        return cached.getAttribute(name, defaultValue);
    }

    @Override
    public Long getAttribute(String name, Long defaultValue) {
        if (isUpdated()) return updated.getAttribute(name, defaultValue);
        return cached.getAttribute(name, defaultValue);
    }

    @Override
    public Boolean getAttribute(String name, Boolean defaultValue) {
        if (isUpdated()) return updated.getAttribute(name, defaultValue);
        return cached.getAttribute(name, defaultValue);
    }

    @Override
    public Map<String, String> getAttributes() {
        if (isUpdated()) return updated.getAttributes();
        return cached.getAttributes();
    }

}
