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

    UserModel getUser(String name);

    UserModel getUserByEmail(String email);

    UserModel getUserById(String name);

    UserModel addUser(String username);

    boolean removeUser(String name);

    List<String> getDefaultRoles();
    
    void addDefaultRole(String name);
    
    void updateDefaultRoles(String[] defaultRoles);

    ClientModel findClient(String clientId);

    Map<String, ApplicationModel> getApplicationNameMap();

    List<ApplicationModel> getApplications();

    ApplicationModel addApplication(String name);

    boolean removeApplication(String id);

    List<RequiredCredentialModel> getRequiredApplicationCredentials();


    List<RequiredCredentialModel> getRequiredOAuthClientCredentials();

    ApplicationModel getApplicationById(String id);
    ApplicationModel getApplicationByName(String name);

    void addRequiredOAuthClientCredential(String type);

    void addRequiredResourceCredential(String type);

    void updateRequiredCredentials(Set<String> creds);

    void updateRequiredOAuthClientCredentials(Set<String> creds);

    void updateRequiredApplicationCredentials(Set<String> creds);

    UserModel getUserBySocialLink(SocialLinkModel socialLink);

    Set<SocialLinkModel> getSocialLinks(UserModel user);

    void addSocialLink(UserModel user, SocialLinkModel socialLink);

    void removeSocialLink(UserModel user, SocialLinkModel socialLink);

    boolean isSocial();

    void setSocial(boolean social);

    public boolean isUpdateProfileOnInitialSocialLogin();

    public void setUpdateProfileOnInitialSocialLogin(boolean updateProfileOnInitialSocialLogin);

    List<UserModel> getUsers();

    List<UserModel> searchForUser(String search);

    List<UserModel> searchForUserByAttributes(Map<String, String> attributes);

    OAuthClientModel addOAuthClient(String name);

    OAuthClientModel getOAuthClient(String name);
    OAuthClientModel getOAuthClientById(String id);
    boolean removeOAuthClient(String id);

    List<OAuthClientModel> getOAuthClients();

    Map<String, String> getSmtpConfig();

    void setSmtpConfig(Map<String, String> smtpConfig);

    Map<String, String> getSocialConfig();

    void setSocialConfig(Map<String, String> socialConfig);

    Set<RoleModel> getRealmRoleMappings(UserModel user);

    Set<RoleModel> getRealmScopeMappings(ClientModel client);

    String getLoginTheme();

    void setLoginTheme(String name);

    String getAccountTheme();

    void setAccountTheme(String name);

    boolean hasScope(ClientModel client, RoleModel role);
}
