package org.keycloak.models.cache;

import org.keycloak.Config;
import org.keycloak.enums.SslRequired;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdapter implements RealmModel {
    protected CachedRealm cached;
    protected CacheRealmProvider cacheSession;
    protected RealmModel updated;
    protected RealmCache cache;
    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;
    protected volatile transient Key codeSecretKey;
    protected volatile transient X509Certificate certificate;

    public RealmAdapter(CachedRealm cached, CacheRealmProvider cacheSession) {
        this.cached = cached;
        this.cacheSession = cacheSession;
    }

    protected void getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerRealmInvalidation(getId());
            updated = cacheSession.getDelegate().getRealm(getId());
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
    }

    @Override
    public String getId() {
        if (updated != null) return updated.getId();
        return cached.getId();
    }

    @Override
    public String getName() {
        if (updated != null) return updated.getName();
        return cached.getName();
    }

    @Override
    public void setName(String name) {
        getDelegateForUpdate();
        updated.setName(name);
    }

    @Override
    public boolean isEnabled() {
        if (updated != null) return updated.isEnabled();
        return cached.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setEnabled(enabled);
    }

    @Override
    public SslRequired getSslRequired() {
        if (updated != null) return updated.getSslRequired();
        return cached.getSslRequired();
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        getDelegateForUpdate();
        updated.setSslRequired(sslRequired);
    }

    @Override
    public boolean isRegistrationAllowed() {
        if (updated != null) return updated.isRegistrationAllowed();
        return cached.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        getDelegateForUpdate();
        updated.setRegistrationAllowed(registrationAllowed);
    }

    @Override
    public boolean isRegistrationEmailAsUsername() {
        if (updated != null) return updated.isRegistrationEmailAsUsername();
        return cached.isRegistrationEmailAsUsername();
    }

    @Override
    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        getDelegateForUpdate();
        updated.setRegistrationEmailAsUsername(registrationEmailAsUsername);
    }

    @Override
    public boolean isRememberMe() {
        if (updated != null) return updated.isRememberMe();
        return cached.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        getDelegateForUpdate();
        updated.setRememberMe(rememberMe);
    }

    @Override
    public boolean isBruteForceProtected() {
        if (updated != null) return updated.isBruteForceProtected();
        return cached.isBruteForceProtected();
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        getDelegateForUpdate();
        updated.setBruteForceProtected(value);
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        if (updated != null) return updated.getMaxFailureWaitSeconds();
        return cached.getMaxFailureWaitSeconds();
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        getDelegateForUpdate();
        updated.setMaxFailureWaitSeconds(val);
    }

    @Override
    public int getWaitIncrementSeconds() {
        if (updated != null) return updated.getWaitIncrementSeconds();
        return cached.getWaitIncrementSeconds();
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        getDelegateForUpdate();
        updated.setWaitIncrementSeconds(val);
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        if (updated != null) return updated.getMinimumQuickLoginWaitSeconds();
        return cached.getMinimumQuickLoginWaitSeconds();
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        getDelegateForUpdate();
        updated.setMinimumQuickLoginWaitSeconds(val);
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        if (updated != null) return updated.getQuickLoginCheckMilliSeconds();
        return cached.getQuickLoginCheckMilliSeconds();
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        getDelegateForUpdate();
        updated.setQuickLoginCheckMilliSeconds(val);
    }

    @Override
    public int getMaxDeltaTimeSeconds() {
        if (updated != null) return updated.getMaxDeltaTimeSeconds();
        return cached.getMaxDeltaTimeSeconds();
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        getDelegateForUpdate();
        updated.setMaxDeltaTimeSeconds(val);
    }

    @Override
    public int getFailureFactor() {
        if (updated != null) return updated.getFailureFactor();
        return cached.getFailureFactor();
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        getDelegateForUpdate();
        updated.setFailureFactor(failureFactor);
    }

    @Override
    public boolean isVerifyEmail() {
        if (updated != null) return updated.isVerifyEmail();
        return cached.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        getDelegateForUpdate();
        updated.setVerifyEmail(verifyEmail);
    }

    @Override
    public boolean isResetPasswordAllowed() {
        if (updated != null) return updated.isResetPasswordAllowed();
        return cached.isResetPasswordAllowed();
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        getDelegateForUpdate();
        updated.setResetPasswordAllowed(resetPasswordAllowed);
    }

    @Override
    public boolean isEditUsernameAllowed() {
        if (updated != null) return updated.isEditUsernameAllowed();
        return cached.isEditUsernameAllowed();
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        getDelegateForUpdate();
        updated.setEditUsernameAllowed(editUsernameAllowed);
    }

    @Override
    public int getSsoSessionIdleTimeout() {
        if (updated != null) return updated.getSsoSessionIdleTimeout();
        return cached.getSsoSessionIdleTimeout();
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        getDelegateForUpdate();
        updated.setSsoSessionIdleTimeout(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        if (updated != null) return updated.getSsoSessionMaxLifespan();
        return cached.getSsoSessionMaxLifespan();
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setSsoSessionMaxLifespan(seconds);
    }

    @Override
    public int getAccessTokenLifespan() {
        if (updated != null) return updated.getAccessTokenLifespan();
        return cached.getAccessTokenLifespan();
    }

    @Override
    public void setAccessTokenLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setAccessTokenLifespan(seconds);
    }

    @Override
    public int getAccessCodeLifespan() {
        if (updated != null) return updated.getAccessCodeLifespan();
        return cached.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setAccessCodeLifespan(seconds);
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        if (updated != null) return updated.getAccessCodeLifespanUserAction();
        return cached.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(int seconds) {
        getDelegateForUpdate();
        updated.setAccessCodeLifespanUserAction(seconds);
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        if (updated != null) return updated.getAccessCodeLifespanLogin();
        return cached.getAccessCodeLifespanLogin();
    }

    @Override
    public void setAccessCodeLifespanLogin(int seconds) {
        getDelegateForUpdate();
        updated.setAccessCodeLifespanLogin(seconds);
    }

    @Override
    public String getPublicKeyPem() {
        if (updated != null) return updated.getPublicKeyPem();
        return cached.getPublicKeyPem();
    }

    @Override
    public void setPublicKeyPem(String publicKeyPem) {
        getDelegateForUpdate();
        updated.setPublicKeyPem(publicKeyPem);
    }

    @Override
    public String getPrivateKeyPem() {
        if (updated != null) return updated.getPrivateKeyPem();
        return cached.getPrivateKeyPem();
    }

    @Override
    public void setPrivateKeyPem(String privateKeyPem) {
        getDelegateForUpdate();
        updated.setPrivateKeyPem(privateKeyPem);
    }

    @Override
    public PublicKey getPublicKey() {
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
        if (updated != null) return updated.getCertificatePem();
        return cached.getCertificatePem();
    }

    @Override
    public void setCertificatePem(String certificate) {
        getDelegateForUpdate();
        updated.setCertificatePem(certificate);

    }

    @Override
    public PrivateKey getPrivateKey() {
        if (privateKey != null) return privateKey;
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
        return updated != null ? updated.getCodeSecret() : cached.getCodeSecret();
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

        List<RequiredCredentialModel> copy = new LinkedList<RequiredCredentialModel>();
        if (updated != null) copy.addAll(updated.getRequiredCredentials());
        else copy.addAll(cached.getRequiredCredentials());
        return copy;
    }

    @Override
    public void addRequiredCredential(String cred) {
        getDelegateForUpdate();
        updated.addRequiredCredential(cred);
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        if (updated != null) return updated.getPasswordPolicy();
        return cached.getPasswordPolicy();
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        getDelegateForUpdate();
        updated.setPasswordPolicy(policy);
    }

    @Override
    public RoleModel getRoleById(String id) {
        if (updated != null) return updated.getRoleById(id);
        return cacheSession.getRoleById(id, this);
     }

    @Override
    public List<String> getDefaultRoles() {
        if (updated != null) return updated.getDefaultRoles();
        return cached.getDefaultRoles();
    }

    @Override
    public void addDefaultRole(String name) {
        getDelegateForUpdate();
        updated.addDefaultRole(name);
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        getDelegateForUpdate();
        updated.updateDefaultRoles(defaultRoles);
    }

    @Override
    public Map<String, ClientModel> getClientNameMap() {
        if (updated != null) return updated.getClientNameMap();
        Map<String, ClientModel> map = new HashMap<String, ClientModel>();
        for (String id : cached.getClients().values()) {
            ClientModel model = cacheSession.getClientById(id, this);
            if (model == null) {
                throw new IllegalStateException("Cached application not found: " + id);
            }
            map.put(model.getClientId(), model);
        }
        return map;
    }

    @Override
    public List<ClientModel> getClients() {
        if (updated != null) return updated.getClients();
        List<ClientModel> apps = new LinkedList<ClientModel>();
        for (String id : cached.getClients().values()) {
            ClientModel model = cacheSession.getClientById(id, this);
            if (model == null) {
                throw new IllegalStateException("Cached application not found: " + id);
            }
            apps.add(model);
        }
        return apps;

    }

    @Override
    public ClientModel addClient(String name) {
        getDelegateForUpdate();
        ClientModel app = updated.addClient(name);
        cacheSession.registerApplicationInvalidation(app.getId());
        return app;
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        getDelegateForUpdate();
        ClientModel app =  updated.addClient(id, clientId);
        cacheSession.registerApplicationInvalidation(app.getId());
        return app;
    }

    @Override
    public boolean removeClient(String id) {
        cacheSession.registerApplicationInvalidation(id);
        getDelegateForUpdate();
        return updated.removeClient(id);
    }

    @Override
    public ClientModel getClientById(String id) {
        if (updated != null) return updated.getClientById(id);
        return cacheSession.getClientById(id, this);
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        if (updated != null) return updated.getClientByClientId(clientId);
        String id = cached.getClients().get(clientId);
        if (id == null) return null;
        return getClientById(id);
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        getDelegateForUpdate();
        updated.updateRequiredCredentials(creds);
    }

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        if (updated != null) return updated.getBrowserSecurityHeaders();
        return cached.getBrowserSecurityHeaders();
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        getDelegateForUpdate();
        updated.setBrowserSecurityHeaders(headers);

    }

    @Override
    public Map<String, String> getSmtpConfig() {
        if (updated != null) return updated.getSmtpConfig();
        return cached.getSmtpConfig();
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        getDelegateForUpdate();
        updated.setSmtpConfig(smtpConfig);
    }


    @Override
    public List<IdentityProviderModel> getIdentityProviders() {
        if (updated != null) return updated.getIdentityProviders();
        return cached.getIdentityProviders();
    }

    @Override
    public IdentityProviderModel getIdentityProviderByAlias(String alias) {
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
        if (updated != null) return updated.getUserFederationProviders();
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
        if (updated != null) return updated.getLoginTheme();
        return cached.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        getDelegateForUpdate();
        updated.setLoginTheme(name);
    }

    @Override
    public String getAccountTheme() {
        if (updated != null) return updated.getAccountTheme();
        return cached.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        getDelegateForUpdate();
        updated.setAccountTheme(name);
    }

    @Override
    public String getAdminTheme() {
        if (updated != null) return updated.getAdminTheme();
        return cached.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        getDelegateForUpdate();
        updated.setAdminTheme(name);
    }

    @Override
    public String getEmailTheme() {
        if (updated != null) return updated.getEmailTheme();
        return cached.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        getDelegateForUpdate();
        updated.setEmailTheme(name);
    }

    @Override
    public int getNotBefore() {
        if (updated != null) return updated.getNotBefore();
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
        if (updated != null) return updated.isEventsEnabled();
        return cached.isEventsEnabled();
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setEventsEnabled(enabled);
    }

    @Override
    public long getEventsExpiration() {
        if (updated != null) return updated.getEventsExpiration();
        return cached.getEventsExpiration();
    }

    @Override
    public void setEventsExpiration(long expiration) {
        getDelegateForUpdate();
        updated.setEventsExpiration(expiration);
    }

    @Override
    public Set<String> getEventsListeners() {
        if (updated != null) return updated.getEventsListeners();
        return cached.getEventsListeners();
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        getDelegateForUpdate();
        updated.setEventsListeners(listeners);
    }

    @Override
    public Set<String> getEnabledEventTypes() {
        if (updated != null) return updated.getEnabledEventTypes();
        return cached.getEnabledEventTypes();
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        getDelegateForUpdate();
        updated.setEnabledEventTypes(enabledEventTypes);        
    }
    
    @Override
    public boolean isAdminEventsEnabled() {
        if (updated != null) return updated.isAdminEventsEnabled();
        return cached.isAdminEventsEnabled();
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setAdminEventsEnabled(enabled);
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        if (updated != null) return updated.isAdminEventsDetailsEnabled();
        return cached.isAdminEventsDetailsEnabled();
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setAdminEventsDetailsEnabled(enabled);
    }
    
    @Override
    public ClientModel getMasterAdminClient() {
        return cacheSession.getRealm(Config.getAdminRealm()).getClientById(cached.getMasterAdminClient());
    }

    @Override
    public void setMasterAdminClient(ClientModel client) {
        getDelegateForUpdate();
        updated.setMasterAdminClient(client);
    }

    @Override
    public RoleModel getRole(String name) {
        if (updated != null) return updated.getRole(name);
        String id = cached.getRealmRoles().get(name);
        if (id == null) return null;
        return cacheSession.getRoleById(id, this);
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
        cacheSession.registerRoleInvalidation(role.getId());
        getDelegateForUpdate();
        return updated.removeRole(role);
    }

    @Override
    public Set<RoleModel> getRoles() {
        if (updated != null) return updated.getRoles();

        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (String id : cached.getRealmRoles().values()) {
            RoleModel roleById = cacheSession.getRoleById(id, this);
            if (roleById == null) continue;
            roles.add(roleById);
        }
        return roles;
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        if (updated != null) return updated.isIdentityFederationEnabled();
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
        if (updated != null) return updated.isInternationalizationEnabled();
        return cached.isInternationalizationEnabled();
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setInternationalizationEnabled(enabled);
    }

    @Override
    public Set<String> getSupportedLocales() {
        if (updated != null) return updated.getSupportedLocales();
        return cached.getSupportedLocales();
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        getDelegateForUpdate();
        updated.setSupportedLocales(locales);
    }

    @Override
    public String getDefaultLocale() {
        if (updated != null) return updated.getDefaultLocale();
        return cached.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String locale) {
        updated.setDefaultLocale(locale);
    }

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappers() {
        if (updated != null) return updated.getIdentityProviderMappers();
        Set<IdentityProviderMapperModel> mappings = new HashSet<>();
        for (List<IdentityProviderMapperModel> models : cached.getIdentityProviderMappers().values()) {
            for (IdentityProviderMapperModel model : models) {
                mappings.add(model);
            }
        }
        return mappings;
    }

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(String brokerAlias) {
        if (updated != null) return updated.getIdentityProviderMappersByAlias(brokerAlias);
        Set<IdentityProviderMapperModel> mappings = new HashSet<>();
        List<IdentityProviderMapperModel> list = cached.getIdentityProviderMappers().getList(brokerAlias);
        for (IdentityProviderMapperModel entity : list) {
            mappings.add(entity);
        }
        return mappings;
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
        if (updated != null) return updated.getIdentityProviderMapperById(id);
        for (List<IdentityProviderMapperModel> models : cached.getIdentityProviderMappers().values()) {
            for (IdentityProviderMapperModel model : models) {
                if (model.getId().equals(id)) return model;
            }
        }
        return null;
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(String alias, String name) {
        if (updated != null) return updated.getIdentityProviderMapperByName(alias, name);
        List<IdentityProviderMapperModel> models = cached.getIdentityProviderMappers().getList(alias);
        if (models == null) return null;
        for (IdentityProviderMapperModel model : models) {
            if (model.getName().equals(name)) return model;
        }
        return null;
    }

    @Override
    public Set<UserFederationMapperModel> getUserFederationMappers() {
        if (updated != null) return updated.getUserFederationMappers();
        Set<UserFederationMapperModel> mappers = new HashSet<UserFederationMapperModel>();
        for (List<UserFederationMapperModel> models : cached.getUserFederationMappers().values()) {
            for (UserFederationMapperModel model : models) {
                mappers.add(model);
            }
        }
        return mappers;
    }

    @Override
    public Set<UserFederationMapperModel> getUserFederationMappersByFederationProvider(String federationProviderId) {
        if (updated != null) return updated.getUserFederationMappersByFederationProvider(federationProviderId);
        Set<UserFederationMapperModel> mappers = new HashSet<>();
        List<UserFederationMapperModel> list = cached.getUserFederationMappers().getList(federationProviderId);
        for (UserFederationMapperModel entity : list) {
            mappers.add(entity);
        }
        return mappers;
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
        if (updated != null) return updated.getUserFederationMapperById(id);
        for (List<UserFederationMapperModel> models : cached.getUserFederationMappers().values()) {
            for (UserFederationMapperModel model : models) {
                if (model.getId().equals(id)) return model;
            }
        }
        return null;
    }

    @Override
    public UserFederationMapperModel getUserFederationMapperByName(String federationProviderId, String name) {
        if (updated != null) return updated.getUserFederationMapperByName(federationProviderId, name);
        List<UserFederationMapperModel> models = cached.getUserFederationMappers().getList(federationProviderId);
        if (models == null) return null;
        for (UserFederationMapperModel model : models) {
            if (model.getName().equals(name)) return model;
        }
        return null;
    }

    @Override
    public List<AuthenticationFlowModel> getAuthenticationFlows() {
        if (updated != null) return updated.getAuthenticationFlows();
        List<AuthenticationFlowModel> models = new ArrayList<>();
        models.addAll(cached.getAuthenticationFlows().values());
        return models;
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
    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        getDelegateForUpdate();
        return updated.addAuthenticationFlow(model);
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String id) {
        if (updated != null) return updated.getAuthenticationFlowById(id);
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
        if (updated != null) return updated.getAuthenticationExecutions(flowId);
        List<AuthenticationExecutionModel> models = new ArrayList<>();
        return cached.getAuthenticationExecutions().get(flowId);
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        if (updated != null) return updated.getAuthenticationExecutionById(id);
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
        if (updated != null) return updated.getAuthenticatorConfigs();
        List<AuthenticatorConfigModel> models = new ArrayList<>();
        models.addAll(cached.getAuthenticatorConfigs().values());
        return models;
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
        if (updated != null) return updated.getAuthenticatorConfigById(id);
        return cached.getAuthenticatorConfigs().get(id);
    }

    @Override
    public List<RequiredActionProviderModel> getRequiredActionProviders() {
        if (updated != null) return updated.getRequiredActionProviders();
        List<RequiredActionProviderModel> models = new ArrayList<>();
        models.addAll(cached.getRequiredActionProviders().values());
        return models;
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
        if (updated != null) return updated.getRequiredActionProviderById(id);
        return cached.getRequiredActionProviders().get(id);
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        if (updated != null) return updated.getRequiredActionProviderByAlias(alias);
        return cached.getRequiredActionProvidersByAlias().get(alias);
    }
}
