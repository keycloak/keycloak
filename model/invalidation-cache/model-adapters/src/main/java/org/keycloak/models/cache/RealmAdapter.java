package org.keycloak.models.cache;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
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
    protected CacheKeycloakSession cacheSession;
    protected RealmModel updated;
    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;

    public RealmAdapter(CachedRealm cached, CacheKeycloakSession cacheSession) {
        this.cached = cached;
        this.cacheSession = cacheSession;
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

    protected void getDelegateForUpdate() {
        if (updated == null) {
            updated = cacheSession.getRealm(getId());
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
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
    public boolean isSslNotRequired() {
        if (updated != null) return updated.isSslNotRequired();
        return cached.isSslNotRequired();
    }

    @Override
    public void setSslNotRequired(boolean sslNotRequired) {
        getDelegateForUpdate();
        updated.setSslNotRequired(sslNotRequired);
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
    public boolean isPasswordCredentialGrantAllowed() {
        if (updated != null) return updated.isPasswordCredentialGrantAllowed();
        return cached.isPasswordCredentialGrantAllowed();
    }

    @Override
    public void setPasswordCredentialGrantAllowed(boolean passwordCredentialGrantAllowed) {
        getDelegateForUpdate();
        updated.setPasswordCredentialGrantAllowed(passwordCredentialGrantAllowed);
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
    public boolean validatePassword(UserModel user, String password) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean validateTOTP(UserModel user, String password, String token) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserModel getUser(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserModel getUserByEmail(String email) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserModel getUserById(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserModel addUser(String id, String username) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserModel addUser(String username) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean removeUser(String name) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RoleModel getRoleById(String id) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getDefaultRoles() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addDefaultRole(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ClientModel findClient(String clientId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, ApplicationModel> getApplicationNameMap() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<ApplicationModel> getApplications() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ApplicationModel addApplication(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ApplicationModel addApplication(String id, String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean removeApplication(String id) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ApplicationModel getApplicationById(String id) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ApplicationModel getApplicationByName(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SocialLinkModel getSocialLink(UserModel user, String socialProvider) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addSocialLink(UserModel user, SocialLinkModel socialLink) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean removeSocialLink(UserModel user, String socialProvider) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AuthenticationLinkModel getAuthenticationLink(UserModel user) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAuthenticationLink(UserModel user, AuthenticationLinkModel authenticationLink) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isSocial() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSocial(boolean social) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isUpdateProfileOnInitialSocialLogin() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setUpdateProfileOnInitialSocialLogin(boolean updateProfileOnInitialSocialLogin) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(String username) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(String username) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> getUsers() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> searchForUser(String search) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public OAuthClientModel addOAuthClient(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public OAuthClientModel addOAuthClient(String id, String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public OAuthClientModel getOAuthClient(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean removeOAuthClient(String id) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<OAuthClientModel> getOAuthClients() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, String> getSocialConfig() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSocialConfig(Map<String, String> socialConfig) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, String> getLdapServerConfig() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setLdapServerConfig(Map<String, String> ldapServerConfig) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<AuthenticationProviderModel> getAuthenticationProviders() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAuthenticationProviders(List<AuthenticationProviderModel> authenticationProviders) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings(UserModel user) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings(ClientModel client) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getLoginTheme() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setLoginTheme(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAccountTheme() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAccountTheme(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAdminTheme() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAdminTheme(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getEmailTheme() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setEmailTheme(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasScope(ClientModel client, RoleModel role) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getNotBefore() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setNotBefore(int notBefore) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean removeRoleById(String id) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isAuditEnabled() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAuditEnabled(boolean enabled) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getAuditExpiration() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAuditExpiration(long expiration) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<String> getAuditListeners() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAuditListeners(Set<String> listeners) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ApplicationModel getMasterAdminApp() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setMasterAdminApp(ApplicationModel app) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserSessionModel createUserSession(UserModel user, String ipAddress) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserSessionModel getUserSession(String id) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserSessionModel> getUserSessions(UserModel user) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserSession(UserSessionModel session) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserSessions(UserModel user) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeExpiredUserSessions() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ClientModel findClientById(String id) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserSessions() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RoleModel getRole(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RoleModel addRole(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<RoleModel> getRoles() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasRole(UserModel user, RoleModel role) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void grantRole(UserModel user, RoleModel role) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<RoleModel> getRoleMappings(UserModel user) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void deleteRoleMapping(UserModel user, RoleModel role) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<RoleModel> getScopeMappings(ClientModel client) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addScopeMapping(ClientModel client, RoleModel role) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void deleteScopeMapping(ClientModel client, RoleModel role) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
