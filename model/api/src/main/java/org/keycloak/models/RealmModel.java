package org.keycloak.models;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmModel extends RoleContainerModel, RoleMapperModel, ScopeMapperModel {

    String getId();

    String getName();

    void setName(String name);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isSslNotRequired();

    void setSslNotRequired(boolean sslNotRequired);

    boolean isRegistrationAllowed();

    void setRegistrationAllowed(boolean registrationAllowed);
    boolean isRememberMe();

    void setRememberMe(boolean rememberMe);

    //--- brute force settings
    boolean isBruteForceProtected();
    void setBruteForceProtected(boolean value);
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

    boolean isResetPasswordAllowed();

    void setResetPasswordAllowed(boolean resetPasswordAllowed);

    int getCentralLoginLifespan();

    void setCentralLoginLifespan(int lifespan);

    int getAccessTokenLifespan();

    void setAccessTokenLifespan(int tokenLifespan);

    int getRefreshTokenLifespan();

    void setRefreshTokenLifespan(int tokenLifespan);

    int getAccessCodeLifespan();

    void setAccessCodeLifespan(int accessCodeLifespan);

    int getAccessCodeLifespanUserAction();

    void setAccessCodeLifespanUserAction(int accessCodeLifespanUserAction);

    String getPublicKeyPem();

    void setPublicKeyPem(String publicKeyPem);

    String getPrivateKeyPem();

    void setPrivateKeyPem(String privateKeyPem);

    PublicKey getPublicKey();

    void setPublicKey(PublicKey publicKey);

    PrivateKey getPrivateKey();

    void setPrivateKey(PrivateKey privateKey);

    List<RequiredCredentialModel> getRequiredCredentials();

    void addRequiredCredential(String cred);

    PasswordPolicy getPasswordPolicy();

    void setPasswordPolicy(PasswordPolicy policy);

    boolean validatePassword(UserModel user, String password);

    boolean validateTOTP(UserModel user, String password, String token);

    void updateCredential(UserModel user, UserCredentialModel cred);

    List<UserCredentialValueModel> getCredentialsDirectly(UserModel user);

    void updateCredentialDirectly(UserModel user, UserCredentialValueModel cred);

    UserModel getUser(String name);

    UserModel getUserByEmail(String email);

    UserModel getUserById(String name);

    UserModel addUser(String id, String username);

    UserModel addUser(String username);

    boolean removeUser(String name);

    RoleModel getRoleById(String id);

    List<String> getDefaultRoles();
    
    void addDefaultRole(String name);
    
    void updateDefaultRoles(String[] defaultRoles);

    ClientModel findClient(String clientId);

    Map<String, ApplicationModel> getApplicationNameMap();

    List<ApplicationModel> getApplications();

    ApplicationModel addApplication(String name);

    ApplicationModel addApplication(String id, String name);

    boolean removeApplication(String id);

    ApplicationModel getApplicationById(String id);
    ApplicationModel getApplicationByName(String name);

    void updateRequiredCredentials(Set<String> creds);

    UserModel getUserBySocialLink(SocialLinkModel socialLink);

    Set<SocialLinkModel> getSocialLinks(UserModel user);

    SocialLinkModel getSocialLink(UserModel user, String socialProvider);

    void addSocialLink(UserModel user, SocialLinkModel socialLink);

    boolean removeSocialLink(UserModel user, String socialProvider);

    AuthenticationLinkModel getAuthenticationLink(UserModel user);

    void setAuthenticationLink(UserModel user, AuthenticationLinkModel authenticationLink);

    boolean isSocial();

    void setSocial(boolean social);

    boolean isUpdateProfileOnInitialSocialLogin();

    void setUpdateProfileOnInitialSocialLogin(boolean updateProfileOnInitialSocialLogin);

    UsernameLoginFailureModel getUserLoginFailure(String username);
    UsernameLoginFailureModel addUserLoginFailure(String username);
    List<UsernameLoginFailureModel> getAllUserLoginFailures();

    List<UserModel> getUsers();

    List<UserModel> searchForUser(String search);

    List<UserModel> searchForUserByAttributes(Map<String, String> attributes);

    OAuthClientModel addOAuthClient(String name);

    OAuthClientModel addOAuthClient(String id, String name);

    OAuthClientModel getOAuthClient(String name);
    OAuthClientModel getOAuthClientById(String id);
    boolean removeOAuthClient(String id);

    List<OAuthClientModel> getOAuthClients();

    Map<String, String> getSmtpConfig();

    void setSmtpConfig(Map<String, String> smtpConfig);

    Map<String, String> getSocialConfig();

    void setSocialConfig(Map<String, String> socialConfig);

    Map<String, String> getLdapServerConfig();

    void setLdapServerConfig(Map<String, String> ldapServerConfig);

    List<AuthenticationProviderModel> getAuthenticationProviders();

    void setAuthenticationProviders(List<AuthenticationProviderModel> authenticationProviders);

    Set<RoleModel> getRealmRoleMappings(UserModel user);

    Set<RoleModel> getRealmScopeMappings(ClientModel client);

    String getLoginTheme();

    void setLoginTheme(String name);

    String getAccountTheme();

    void setAccountTheme(String name);

    String getAdminTheme();

    void setAdminTheme(String name);

    boolean hasScope(ClientModel client, RoleModel role);

    /**
     * Time in seconds since epoc
     *
     * @return
     */
    int getNotBefore();

    void setNotBefore(int notBefore);

    boolean removeRoleById(String id);

    boolean isAuditEnabled();

    void setAuditEnabled(boolean enabled);

    long getAuditExpiration();

    void setAuditExpiration(long expiration);

    Set<String> getAuditListeners();

    void setAuditListeners(Set<String> listeners);

    ApplicationModel getMasterAdminApp();

    void setMasterAdminApp(ApplicationModel app);

    UserSessionModel createUserSession(UserModel user, String ipAddress);

    UserSessionModel getUserSession(String id);

    List<UserSessionModel> getUserSessions(UserModel user);

    void removeUserSession(UserSessionModel session);

    void removeUserSessions(UserModel user);

    void removeExpiredUserSessions();

}
