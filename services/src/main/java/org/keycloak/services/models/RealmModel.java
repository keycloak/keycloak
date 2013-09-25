package org.keycloak.services.models;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmModel {
    String DEFAULT_REALM = "default";

    String getId();

    String getName();

    void setName(String name);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isSslNotRequired();

    void setSslNotRequired(boolean sslNotRequired);

    boolean isCookieLoginAllowed();

    void setCookieLoginAllowed(boolean cookieLoginAllowed);

    boolean isRegistrationAllowed();

    void setRegistrationAllowed(boolean registrationAllowed);

    boolean isVerifyEmail();

    void setVerifyEmail(boolean verifyEmail);

    boolean isResetPasswordAllowed();

    void setResetPasswordAllowed(boolean resetPasswordAllowed);

    int getTokenLifespan();

    void setTokenLifespan(int tokenLifespan);

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

    boolean validatePassword(UserModel user, String password);

    boolean validateTOTP(UserModel user, String password, String token);

    void updateCredential(UserModel user, UserCredentialModel cred);

    UserModel getUser(String name);

    UserModel addUser(String username);

    RoleModel getRole(String name);

    RoleModel addRole(String name);

    List<RoleModel> getRoles();
    
    List<RoleModel> getDefaultRoles();
    
    void addDefaultRole(String name);
    
    void updateDefaultRoles(String[] defaultRoles);

    Map<String, ApplicationModel> getResourceNameMap();

    List<ApplicationModel> getApplications();

    ApplicationModel addApplication(String name);

    boolean hasRole(UserModel user, RoleModel role);

    void grantRole(UserModel user, RoleModel role);

    Set<String> getRoleMappingValues(UserModel user);

    void addScope(UserModel agent, String roleName);

    Set<String> getScope(UserModel agent);

    boolean isRealmAdmin(UserModel agent);

    void addRealmAdmin(UserModel agent);

    RoleModel getRoleById(String id);


    List<RequiredCredentialModel> getRequiredApplicationCredentials();


    List<RequiredCredentialModel> getRequiredOAuthClientCredentials();

    boolean hasRole(UserModel user, String role);

    ApplicationModel getApplicationById(String id);

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

    public boolean isAutomaticRegistrationAfterSocialLogin();

    public void setAutomaticRegistrationAfterSocialLogin(boolean automaticRegistrationAfterSocialLogin);

    List<UserModel> searchForUserByAttributes(Map<String, String> attributes);

    List<RoleModel> getRoleMappings(UserModel user);

    void deleteRoleMapping(UserModel user, RoleModel role);
}
