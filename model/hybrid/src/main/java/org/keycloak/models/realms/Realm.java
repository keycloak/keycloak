package org.keycloak.models.realms;

import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RequiredCredentialModel;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Realm extends RoleContainer {

    String getId();

    String getName();

    void setName(String name);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isSslNotRequired();

    void setSslNotRequired(boolean sslNotRequired);

    boolean isRegistrationAllowed();

    void setRegistrationAllowed(boolean registrationAllowed);

    boolean isPasswordCredentialGrantAllowed();

    void setPasswordCredentialGrantAllowed(boolean passwordCredentialGrantAllowed);

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

    int getSsoSessionIdleTimeout();
    void setSsoSessionIdleTimeout(int seconds);

    int getSsoSessionMaxLifespan();
    void setSsoSessionMaxLifespan(int seconds);

    int getAccessTokenLifespan();

    void setAccessTokenLifespan(int seconds);

    int getAccessCodeLifespan();

    void setAccessCodeLifespan(int seconds);

    int getAccessCodeLifespanUserAction();

    void setAccessCodeLifespanUserAction(int seconds);

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

    List<String> getDefaultRoles();
    
    void addDefaultRole(String name);
    
    void updateDefaultRoles(String[] defaultRoles);

    Client findClient(String clientId);

    Map<String, Application> getApplicationNameMap();

    List<Application> getApplications();

    Application addApplication(String id, String name);

    boolean removeApplication(Application application);

    Application getApplicationByName(String name);

    void updateRequiredCredentials(Set<String> creds);

    boolean isSocial();

    void setSocial(boolean social);

    boolean isUpdateProfileOnInitialSocialLogin();

    void setUpdateProfileOnInitialSocialLogin(boolean updateProfileOnInitialSocialLogin);

    OAuthClient addOAuthClient(String id, String name);

    OAuthClient getOAuthClient(String name);
    boolean removeOAuthClient(OAuthClient client);

    List<OAuthClient> getOAuthClients();

    Map<String, String> getSmtpConfig();

    void setSmtpConfig(Map<String, String> smtpConfig);

    Map<String, String> getSocialConfig();

    void setSocialConfig(Map<String, String> socialConfig);

    Map<String, String> getLdapServerConfig();

    void setLdapServerConfig(Map<String, String> ldapServerConfig);

    List<AuthenticationProviderModel> getAuthenticationProviders();

    void setAuthenticationProviders(List<AuthenticationProviderModel> authenticationProviders);

    String getLoginTheme();

    void setLoginTheme(String name);

    String getAccountTheme();

    void setAccountTheme(String name);

    String getAdminTheme();

    void setAdminTheme(String name);

    String getEmailTheme();

    void setEmailTheme(String name);


    /**
     * Time in seconds since epoc
     *
     * @return
     */
    int getNotBefore();

    void setNotBefore(int notBefore);

    boolean removeRole(Role role);

    boolean isAuditEnabled();

    void setAuditEnabled(boolean enabled);

    long getAuditExpiration();

    void setAuditExpiration(long expiration);

    Set<String> getAuditListeners();

    void setAuditListeners(Set<String> listeners);

    Application getMasterAdminApp();

    void setMasterAdminApp(Application app);

}
