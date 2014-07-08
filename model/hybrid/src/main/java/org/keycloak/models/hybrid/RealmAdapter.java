package org.keycloak.models.hybrid;

import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.realms.Application;
import org.keycloak.models.realms.Client;
import org.keycloak.models.realms.OAuthClient;
import org.keycloak.models.realms.Realm;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.users.Credentials;
import org.keycloak.models.users.Feature;
import org.keycloak.models.users.User;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.Pbkdf2PasswordEncoder;
import org.keycloak.models.utils.TimeBasedOTP;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmAdapter implements RealmModel {

    private HybridModelProvider provider;
    private Realm realm;

    RealmAdapter(HybridModelProvider provider, Realm realm) {
        this.provider = provider;
        this.realm = realm;
    }

    Realm getRealm() {
        return realm;
    }

    @Override
    public String getId() {
        return realm.getId();
    }

    @Override
    public String getName() {
        return realm.getName();
    }

    @Override
    public void setName(String name) {
        realm.setName(name);
    }

    @Override
    public boolean isEnabled() {
        return realm.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        realm.setEnabled(enabled);
    }

    @Override
    public boolean isSslNotRequired() {
        return realm.isSslNotRequired();
    }

    @Override
    public void setSslNotRequired(boolean sslNotRequired) {
        realm.setSslNotRequired(sslNotRequired);
    }

    @Override
    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        realm.setRegistrationAllowed(registrationAllowed);
    }

    @Override
    public boolean isPasswordCredentialGrantAllowed() {
        return realm.isPasswordCredentialGrantAllowed();
    }

    @Override
    public void setPasswordCredentialGrantAllowed(boolean passwordCredentialGrantAllowed) {
        realm.setPasswordCredentialGrantAllowed(passwordCredentialGrantAllowed);
    }

    @Override
    public boolean isRememberMe() {
        return realm.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        realm.setRememberMe(rememberMe);
    }

    @Override
    public boolean isBruteForceProtected() {
        return realm.isBruteForceProtected();
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        realm.setBruteForceProtected(value);
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        return realm.getMaxFailureWaitSeconds();
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        realm.setMaxFailureWaitSeconds(val);
    }

    @Override
    public int getWaitIncrementSeconds() {
        return realm.getWaitIncrementSeconds();
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        realm.setWaitIncrementSeconds(val);
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        return realm.getMinimumQuickLoginWaitSeconds();
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        realm.setMinimumQuickLoginWaitSeconds(val);
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        return realm.getQuickLoginCheckMilliSeconds();
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        realm.setQuickLoginCheckMilliSeconds(val);
    }

    @Override
    public int getMaxDeltaTimeSeconds() {
        return realm.getMaxDeltaTimeSeconds();
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        realm.setMaxDeltaTimeSeconds(val);
    }

    @Override
    public int getFailureFactor() {
        return realm.getFailureFactor();
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        realm.setFailureFactor(failureFactor);
    }

    @Override
    public boolean isVerifyEmail() {
        return realm.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        realm.setVerifyEmail(verifyEmail);
    }

    @Override
    public boolean isResetPasswordAllowed() {
        return realm.isResetPasswordAllowed();
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        realm.setResetPasswordAllowed(resetPasswordAllowed);
    }

    @Override
    public int getSsoSessionIdleTimeout() {
        return realm.getSsoSessionIdleTimeout();
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        realm.setSsoSessionIdleTimeout(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        return realm.getSsoSessionMaxLifespan();
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        realm.setSsoSessionMaxLifespan(seconds);
    }

    @Override
    public int getAccessTokenLifespan() {
        return realm.getAccessTokenLifespan();
    }

    @Override
    public void setAccessTokenLifespan(int seconds) {
        realm.setAccessTokenLifespan(seconds);
    }

    @Override
    public int getAccessCodeLifespan() {
        return realm.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(int seconds) {
        realm.setAccessCodeLifespan(seconds);
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        return realm.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(int seconds) {
        realm.setAccessCodeLifespanUserAction(seconds);
    }

    @Override
    public String getPublicKeyPem() {
        return realm.getPublicKeyPem();
    }

    @Override
    public void setPublicKeyPem(String publicKeyPem) {
        realm.setPublicKeyPem(publicKeyPem);
    }

    @Override
    public String getPrivateKeyPem() {
        return realm.getPrivateKeyPem();
    }

    @Override
    public void setPrivateKeyPem(String privateKeyPem) {
        realm.setPrivateKeyPem(privateKeyPem);
    }

    @Override
    public PublicKey getPublicKey() {
        return realm.getPublicKey();
    }

    @Override
    public void setPublicKey(PublicKey publicKey) {
        realm.setPublicKey(publicKey);
    }

    @Override
    public PrivateKey getPrivateKey() {
        return realm.getPrivateKey();
    }

    @Override
    public void setPrivateKey(PrivateKey privateKey) {
        realm.setPrivateKey(privateKey);
    }

    @Override
    public List<RequiredCredentialModel> getRequiredCredentials() {
        return realm.getRequiredCredentials();
    }

    @Override
    public void addRequiredCredential(String cred) {
         realm.addRequiredCredential(cred);
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        return realm.getPasswordPolicy();
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        realm.setPasswordPolicy(policy);
    }

    @Override
    public boolean validatePassword(UserModel userModel, String password) {
        if (provider.users().supports(Feature.VERIFY_CREDENTIALS)) {
            User user = provider.mappings().unwrap(userModel);
            return provider.users().verifyCredentials(user, new Credentials(UserCredentialModel.PASSWORD, password));
        } else {
            for (UserCredentialValueModel cred : userModel.getCredentialsDirectly()) {
                if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                    return new Pbkdf2PasswordEncoder(cred.getSalt()).verify(password, cred.getValue());
                }
            }
            return false;
        }
    }

    @Override
    public boolean validateTOTP(UserModel userModel, String password, String token) {
        if (provider.users().supports(Feature.VERIFY_CREDENTIALS)) {
            User user = provider.mappings().unwrap(userModel);
            return provider.users().verifyCredentials(user, new Credentials(UserCredentialModel.PASSWORD, password),
                    new Credentials(UserCredentialModel.TOTP, token));
        } else {
            if (!validatePassword(userModel, password)) return false;
            for (UserCredentialValueModel cred : userModel.getCredentialsDirectly()) {
                if (cred.getType().equals(UserCredentialModel.TOTP)) {
                    return new TimeBasedOTP().validate(token, cred.getValue().getBytes());
                }
            }
            return false;
        }
    }

    @Override
    public UserModel getUser(String name) {
        return provider.getUserByUsername(name, this);
    }

    @Override
    public UserModel getUserByEmail(String email) {
        return provider.getUserByEmail(email, this);
    }

    @Override
    public UserModel getUserById(String name) {
        return provider.getUserById(name, this);
    }

    @Override
    public UserModel addUser(String id, String username, boolean addDefaultRoles) {
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        }

        Set<String> initialRoles = new HashSet<String>();

        if (addDefaultRoles) {
            for (String r : realm.getDefaultRoles()) {
                initialRoles.add(realm.getRole(r).getId());
            }

            for (Application app : realm.getApplications()) {
                for (String r : app.getDefaultRoles()) {
                    initialRoles.add(app.getRole(r).getId());
                }
            }
        }

        return provider.mappings().wrap(this, provider.users().addUser(id, username, initialRoles, realm.getId()));
    }

    @Override
    public UserModel addUser(String username) {
        return addUser(null, username, true);
    }

    @Override
    public boolean removeUser(String name) {
        return provider.users().removeUser(name, realm.getId());
    }

    @Override
    public RoleModel getRoleById(String id) {
        return provider.mappings().wrap(provider.realms().getRoleById(id, realm.getId()));
    }

    @Override
    public List<String> getDefaultRoles() {
        return realm.getDefaultRoles();
    }

    @Override
    public void addDefaultRole(String name) {
        if (getRole(name) == null) {
            addRole(name);
        }

        realm.addDefaultRole(name);
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        for (String name : defaultRoles) {
            if (getRole(name) == null) {
                addRole(name);
            }
        }

        realm.updateDefaultRoles(defaultRoles);
    }

    @Override
    public ClientModel findClient(String clientId) {
        Client client = realm.findClient(clientId);
        if (client instanceof Application) {
            return provider.mappings().wrap((Application) client);
        } else if (client instanceof OAuthClient) {
            return provider.mappings().wrap((OAuthClient) client);
        } else {
            throw new IllegalArgumentException("Unsupported client type");
        }
    }

    @Override
    public Map<String, ApplicationModel> getApplicationNameMap() {
        return provider.mappings().wrap(realm.getApplicationNameMap());
    }

    @Override
    public List<ApplicationModel> getApplications() {
        return provider.mappings().wrapApps(realm.getApplications());
    }

    @Override
    public ApplicationModel addApplication(String name) {
        return addApplication(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public ApplicationModel addApplication(String id, String name) {
        return provider.mappings().wrap(realm.addApplication(id, name));
    }

    @Override
    public boolean removeApplication(String id) {
        Application application = provider.realms().getApplicationById(id, realm.getId());
        if (application != null) {
            return realm.removeApplication(application);
        } else {
            return false;
        }
    }

    @Override
    public ApplicationModel getApplicationById(String id) {
        return provider.getApplicationById(id, this);
    }

    @Override
    public ApplicationModel getApplicationByName(String name) {
        return provider.mappings().wrap(realm.getApplicationByName(name));
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        realm.updateRequiredCredentials(creds);
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink) {
        return provider.getUserBySocialLink(socialLink, this);
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user) {
        return provider.getSocialLinks(user, this);
    }

    @Override
    public SocialLinkModel getSocialLink(UserModel user, String socialProvider) {
        return provider.getSocialLink(user, socialProvider, this);
    }

    @Override
    public void addSocialLink(UserModel user, SocialLinkModel socialLink) {
        user.setAttribute("keycloak.socialLink." + socialLink.getSocialProvider() + ".userId", socialLink.getSocialUserId());
        user.setAttribute("keycloak.socialLink." + socialLink.getSocialProvider() + ".username", socialLink.getSocialUsername());
    }

    @Override
    public boolean removeSocialLink(UserModel user, String socialProvider) {
        if (user.getAttribute("keycloak.socialLink." + socialProvider + ".userId") != null) {
            user.removeAttribute("keycloak.socialLink." + socialProvider + ".userId");
            user.removeAttribute("keycloak.socialLink." + socialProvider + ".username");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isSocial() {
        return realm.isSocial();
    }

    @Override
    public void setSocial(boolean social) {
        realm.setSocial(social);
    }

    @Override
    public boolean isUpdateProfileOnInitialSocialLogin() {
        return realm.isUpdateProfileOnInitialSocialLogin();
    }

    @Override
    public void setUpdateProfileOnInitialSocialLogin(boolean updateProfileOnInitialSocialLogin) {
        realm.setUpdateProfileOnInitialSocialLogin(updateProfileOnInitialSocialLogin);
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(String username) {
        return provider.getUserLoginFailure(username, this);
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(String username) {
        return provider.addUserLoginFailure(username, this);
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures() {
        return provider.getAllUserLoginFailures(this);
    }

    @Override
    public List<UserModel> getUsers() {
        return provider.getUsers(this);
    }

    @Override
    public List<UserModel> searchForUser(String search) {
        return provider.searchForUser(search, this);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes) {
        return provider.searchForUserByAttributes(attributes, this);
    }

    @Override
    public OAuthClientModel addOAuthClient(String name) {
        return addOAuthClient(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public OAuthClientModel addOAuthClient(String id, String name) {
        return provider.mappings().wrap(realm.addOAuthClient(id, name));
    }

    @Override
    public OAuthClientModel getOAuthClient(String name) {
        return provider.mappings().wrap(realm.getOAuthClient(name));
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id) {
        return provider.getOAuthClientById(id, this);
    }

    @Override
    public boolean removeOAuthClient(String id) {
        OAuthClient client = provider.realms().getOAuthClientById(id, realm.getId());
        if (client != null) {
            return realm.removeOAuthClient(client);
        } else {
            return false;
        }
    }

    @Override
    public List<OAuthClientModel> getOAuthClients() {
        return provider.mappings().wrapClients(realm.getOAuthClients());
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        return realm.getSmtpConfig();
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        realm.setSmtpConfig(smtpConfig);
    }

    @Override
    public Map<String, String> getSocialConfig() {
        return realm.getSocialConfig();
    }

    @Override
    public void setSocialConfig(Map<String, String> socialConfig) {
        realm.setSocialConfig(socialConfig);
    }

    @Override
    public Map<String, String> getLdapServerConfig() {
        return realm.getLdapServerConfig();
    }

    @Override
    public void setLdapServerConfig(Map<String, String> ldapServerConfig) {
        realm.setLdapServerConfig(ldapServerConfig);
    }

    @Override
    public List<AuthenticationProviderModel> getAuthenticationProviders() {
        return realm.getAuthenticationProviders();
    }

    @Override
    public void setAuthenticationProviders(List<AuthenticationProviderModel> authenticationProviders) {
        realm.setAuthenticationProviders(authenticationProviders);
    }

    @Override
    public String getLoginTheme() {
        return realm.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        realm.setLoginTheme(name);
    }

    @Override
    public String getAccountTheme() {
        return realm.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        realm.setAccountTheme(name);
    }

    @Override
    public String getAdminTheme() {
        return realm.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        realm.setAdminTheme(name);
    }

    @Override
    public String getEmailTheme() {
        return realm.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        realm.setEmailTheme(name);
    }

    @Override
    public int getNotBefore() {
        return realm.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        realm.setNotBefore(notBefore);
    }

    @Override
    public boolean isAuditEnabled() {
        return realm.isAuditEnabled();
    }

    @Override
    public void setAuditEnabled(boolean enabled) {
        realm.setAuditEnabled(enabled);
    }

    @Override
    public long getAuditExpiration() {
        return realm.getAuditExpiration();
    }

    @Override
    public void setAuditExpiration(long expiration) {
        realm.setAuditExpiration(expiration);
    }

    @Override
    public Set<String> getAuditListeners() {
        return realm.getAuditListeners();
    }

    @Override
    public void setAuditListeners(Set<String> listeners) {
        realm.setAuditListeners(listeners);
    }

    @Override
    public ApplicationModel getMasterAdminApp() {
        return provider.mappings().wrap(realm.getMasterAdminApp());
    }

    @Override
    public void setMasterAdminApp(ApplicationModel app) {
        realm.setMasterAdminApp(provider.mappings().unwrap(app));
    }

    @Override
    public UserSessionModel createUserSession(UserModel user, String ipAddress) {
        return provider.createUserSession(this, user, ipAddress);
    }

    @Override
    public UserSessionModel getUserSession(String id) {
        return provider.getUserSession(id, this);
    }

    @Override
    public List<UserSessionModel> getUserSessions(UserModel user) {
        return provider.getUserSessions(user, this);
    }

    @Override
    public void removeUserSession(UserSessionModel session) {
        provider.removeUserSession(session);
    }

    @Override
    public void removeUserSessions(UserModel user) {
        provider.removeUserSessions(this, user);
    }

    @Override
    public void removeExpiredUserSessions() {
        provider.removeExpiredUserSessions(this);
    }

    @Override
    public ClientModel findClientById(String id) {
        Application application = provider.realms().getApplicationById(id, realm.getId());
        if (application != null) {
            return provider.mappings().wrap(application);
        }

        OAuthClient client = provider.realms().getOAuthClientById(id, realm.getId());
        if (client != null) {
            return provider.mappings().wrap(client);
        }

        return null;
    }

    @Override
    public void removeUserSessions() {
        provider.removeUserSessions(this);
    }

    @Override
    public RoleModel getRole(String name) {
        return provider.mappings().wrap(realm.getRole(name));
    }

    @Override
    public RoleModel addRole(String name) {
        return addRole(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return provider.mappings().wrap(realm.addRole(id, name));

    }

    @Override
    public boolean removeRoleById(String id) {
        RoleModel role = getRoleById(id);
        if (role != null) {
            if (role.getContainer().removeRole(role)) {
                provider.users().onRoleRemoved(role.getId());
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return removeRoleById(role.getId());
    }

    @Override
    public Set<RoleModel> getRoles() {
        return provider.mappings().wrap(realm.getRoles());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof RealmModel)) return false;

        RealmModel that = (RealmModel) o;
        return that.getId().equals(getId());
    }

}
