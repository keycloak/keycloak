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
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.models.cache.infinispan.entities.CachedRealm;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdapter implements RealmModel {
    protected CachedRealm cached;
    protected RealmCacheSession cacheSession;
    protected RealmModel updated;
    protected RealmCache cache;
    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;
    protected volatile transient Key codeSecretKey;
    protected volatile transient X509Certificate certificate;

    public RealmAdapter(CachedRealm cached, RealmCacheSession cacheSession) {
        this.cached = cached;
        this.cacheSession = cacheSession;
    }

    protected void getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerRealmInvalidation(cached.getId());
            updated = cacheSession.getDelegate().getRealm(cached.getId());
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
    }

    protected boolean invalidated;
    public void invalidate() {
        invalidated = true;
    }

    protected boolean isUpdated() {
        if (updated != null) return true;
        if (!invalidated) return false;
        updated = cacheSession.getDelegate().getRealm(cached.getId());
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
    public int getOfflineSessionIdleTimeout() {
        if (isUpdated()) return updated.getOfflineSessionIdleTimeout();
        return cached.getOfflineSessionIdleTimeout();
    }


    @Override
    public void setOfflineSessionIdleTimeout(int seconds) {
        getDelegateForUpdate();
        updated.setOfflineSessionIdleTimeout(seconds);
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
    public String getKeyId() {
        if (isUpdated()) return updated.getKeyId();
        return cached.getKeyId();
    }

    @Override
    public String getPublicKeyPem() {
        if (isUpdated()) return updated.getPublicKeyPem();
        return cached.getPublicKeyPem();
    }

    @Override
    public void setPublicKeyPem(String publicKeyPem) {
        getDelegateForUpdate();
        updated.setPublicKeyPem(publicKeyPem);
    }

    @Override
    public String getPrivateKeyPem() {
        if (isUpdated()) return updated.getPrivateKeyPem();
        return cached.getPrivateKeyPem();
    }

    @Override
    public void setPrivateKeyPem(String privateKeyPem) {
        getDelegateForUpdate();
        updated.setPrivateKeyPem(privateKeyPem);
    }

    @Override
    public PublicKey getPublicKey() {
        if (isUpdated()) return updated.getPublicKey();
        if (publicKey != null) return publicKey;
        publicKey = cached.getPublicKey();
        if (publicKey != null) return publicKey;
        publicKey = KeycloakModelUtils.getPublicKey(getPublicKeyPem());
        return publicKey;
    }

    @Override
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        String publicKeyPem = KeycloakModelUtils.getPemFromKey(publicKey);
        setPublicKeyPem(publicKeyPem);
    }

    @Override
    public X509Certificate getCertificate() {
        if (isUpdated()) return updated.getCertificate();
        if (certificate != null) return certificate;
        certificate = cached.getCertificate();
        if (certificate != null) return certificate;
        certificate = KeycloakModelUtils.getCertificate(getCertificatePem());
        return certificate;
    }

    @Override
    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
        String certPem = KeycloakModelUtils.getPemFromCertificate(certificate);
        setCertificatePem(certPem);
    }

    @Override
    public String getCertificatePem() {
        if (isUpdated()) return updated.getCertificatePem();
        return cached.getCertificatePem();
    }

    @Override
    public void setCertificatePem(String certificate) {
        getDelegateForUpdate();
        updated.setCertificatePem(certificate);

    }

    @Override
    public PrivateKey getPrivateKey() {
        if (isUpdated()) return updated.getPrivateKey();
        if (privateKey != null) {
            return privateKey;
        }
        privateKey = cached.getPrivateKey();
        if (privateKey != null) {
            return privateKey;
        }
        privateKey = KeycloakModelUtils.getPrivateKey(getPrivateKeyPem());
        return privateKey;
    }

    @Override
    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
        String privateKeyPem = KeycloakModelUtils.getPemFromKey(privateKey);
        setPrivateKeyPem(privateKeyPem);
    }

    @Override
    public String getCodeSecret() {
        return isUpdated() ? updated.getCodeSecret() : cached.getCodeSecret();
    }

    @Override
    public Key getCodeSecretKey() {
        if (codeSecretKey == null) {
            codeSecretKey = KeycloakModelUtils.getSecretKey(getCodeSecret());
        }
        return codeSecretKey;
    }

    @Override
    public void setCodeSecret(String codeSecret) {
        getDelegateForUpdate();
        updated.setCodeSecret(codeSecret);
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
    public List<UserFederationProviderModel> getUserFederationProviders() {
        if (isUpdated()) return updated.getUserFederationProviders();
        return cached.getUserFederationProviders();
    }

    @Override
    public void setUserFederationProviders(List<UserFederationProviderModel> providers) {
        getDelegateForUpdate();
        updated.setUserFederationProviders(providers);
    }

    @Override
    public UserFederationProviderModel addUserFederationProvider(String providerName, Map<String, String> config, int priority, String displayName, int fullSyncPeriod, int changedSyncPeriod, int lastSync) {
        getDelegateForUpdate();
        return updated.addUserFederationProvider(providerName, config, priority, displayName, fullSyncPeriod, changedSyncPeriod, lastSync);
    }

    @Override
    public void removeUserFederationProvider(UserFederationProviderModel provider) {
        getDelegateForUpdate();
        updated.removeUserFederationProvider(provider);

    }

    @Override
    public void updateUserFederationProvider(UserFederationProviderModel provider) {
        getDelegateForUpdate();
        updated.updateUserFederationProvider(provider);

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
    public boolean removeRoleById(String id) {
        cacheSession.registerRoleInvalidation(id);
        getDelegateForUpdate();
        return updated.removeRoleById(id);
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
        for (RoleModel role : getRoles()) {
            if (role.getName().equals(name)) return role;
        }
        return null;
    }

    @Override
    public Set<RoleModel> getRoles() {
        return cacheSession.getRealmRoles(this);
    }


    @Override
    public RoleModel addRole(String name) {
        getDelegateForUpdate();
        RoleModel role = updated.addRole(name);
        cacheSession.registerRoleInvalidation(role.getId());
        return role;
    }

    @Override
    public RoleModel addRole(String id, String name) {
        getDelegateForUpdate();
        RoleModel role =  updated.addRole(id, name);
        cacheSession.registerRoleInvalidation(role.getId());
        return role;
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
        if (o == null || !(o instanceof RealmModel)) return false;

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
    public Set<UserFederationMapperModel> getUserFederationMappers() {
        if (isUpdated()) return updated.getUserFederationMappers();
        return cached.getUserFederationMapperSet();
    }

    @Override
    public Set<UserFederationMapperModel> getUserFederationMappersByFederationProvider(String federationProviderId) {
        if (isUpdated()) return updated.getUserFederationMappersByFederationProvider(federationProviderId);
        Set<UserFederationMapperModel> mappers = new HashSet<>();
        List<UserFederationMapperModel> list = cached.getUserFederationMappers().getList(federationProviderId);
        for (UserFederationMapperModel entity : list) {
            mappers.add(entity);
        }
        return Collections.unmodifiableSet(mappers);
    }

    @Override
    public UserFederationMapperModel addUserFederationMapper(UserFederationMapperModel mapper) {
        getDelegateForUpdate();
        return updated.addUserFederationMapper(mapper);
    }

    @Override
    public void removeUserFederationMapper(UserFederationMapperModel mapper) {
        getDelegateForUpdate();
        updated.removeUserFederationMapper(mapper);
    }

    @Override
    public void updateUserFederationMapper(UserFederationMapperModel mapper) {
        getDelegateForUpdate();
        updated.updateUserFederationMapper(mapper);
    }

    @Override
    public UserFederationMapperModel getUserFederationMapperById(String id) {
        if (isUpdated()) return updated.getUserFederationMapperById(id);
        for (List<UserFederationMapperModel> models : cached.getUserFederationMappers().values()) {
            for (UserFederationMapperModel model : models) {
                if (model.getId().equals(id)) return model;
            }
        }
        return null;
    }

    @Override
    public UserFederationMapperModel getUserFederationMapperByName(String federationProviderId, String name) {
        if (isUpdated()) return updated.getUserFederationMapperByName(federationProviderId, name);
        List<UserFederationMapperModel> models = cached.getUserFederationMappers().getList(federationProviderId);
        if (models == null) return null;
        for (UserFederationMapperModel model : models) {
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
    public void addTopLevelGroup(GroupModel subGroup) {
        cacheSession.addTopLevelGroup(this, subGroup);

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
    public List<GroupModel> getTopLevelGroups() {
        return cacheSession.getTopLevelGroups(this);
    }

    @Override
    public boolean removeGroup(GroupModel group) {
        return cacheSession.removeGroup(this, group);
    }

    @Override
    public List<ClientTemplateModel> getClientTemplates() {
        if (isUpdated()) return updated.getClientTemplates();
        List<String> clientTemplates = cached.getClientTemplates();
        if (clientTemplates.isEmpty()) return Collections.EMPTY_LIST;
        List<ClientTemplateModel> apps = new LinkedList<ClientTemplateModel>();
        for (String id : clientTemplates) {
            ClientTemplateModel model = cacheSession.getClientTemplateById(id, this);
            if (model == null) {
                throw new IllegalStateException("Cached clientemplate not found: " + id);
            }
            apps.add(model);
        }
        return Collections.unmodifiableList(apps);

    }

    @Override
    public ClientTemplateModel addClientTemplate(String name) {
        getDelegateForUpdate();
        ClientTemplateModel app = updated.addClientTemplate(name);
        cacheSession.registerClientTemplateInvalidation(app.getId());
        return app;
    }

    @Override
    public ClientTemplateModel addClientTemplate(String id, String name) {
        getDelegateForUpdate();
        ClientTemplateModel app =  updated.addClientTemplate(id, name);
        cacheSession.registerClientTemplateInvalidation(app.getId());
        return app;
    }

    @Override
    public boolean removeClientTemplate(String id) {
        cacheSession.registerClientTemplateInvalidation(id);
        getDelegateForUpdate();
        return updated.removeClientTemplate(id);
    }

    @Override
    public ClientTemplateModel getClientTemplateById(String id) {
        if (isUpdated()) return updated.getClientTemplateById(id);
        return cacheSession.getClientTemplateById(id, this);
    }

    @Override
    public ComponentModel addComponentModel(ComponentModel model) {
        getDelegateForUpdate();
        return updated.addComponentModel(model);
    }

    @Override
    public void updateComponent(ComponentModel component) {
        getDelegateForUpdate();
        updated.updateComponent(component);

    }

    @Override
    public void removeComponent(ComponentModel component) {
        getDelegateForUpdate();
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
}
