package org.keycloak.models;

import org.keycloak.enums.SslRequired;
import org.keycloak.provider.ProviderEvent;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmModel extends RoleContainerModel {
    interface RealmCreationEvent extends ProviderEvent {
        RealmModel getCreatedRealm();
    }
    interface ClientCreationEvent extends ProviderEvent {
        ClientModel getCreatedClient();
    }
    interface ApplicationCreationEvent extends ClientCreationEvent {
        ApplicationModel getCreatedApplication();
    }
    interface OAuthClientCreationEvent extends ClientCreationEvent {
        OAuthClientModel getCreatedOAuthClient();
    }

    String getId();

    String getName();

    void setName(String name);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    SslRequired getSslRequired();

    void setSslRequired(SslRequired sslRequired);

    boolean isRegistrationAllowed();

    void setRegistrationAllowed(boolean registrationAllowed);

    public boolean isRegistrationEmailAsUsername();

    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername);

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

    int getAccessCodeLifespanLogin();

    void setAccessCodeLifespanLogin(int seconds);

    String getPublicKeyPem();

    void setPublicKeyPem(String publicKeyPem);

    String getPrivateKeyPem();

    void setPrivateKeyPem(String privateKeyPem);

    PublicKey getPublicKey();

    void setPublicKey(PublicKey publicKey);

    String getCodeSecret();

    Key getCodeSecretKey();

    void setCodeSecret(String codeSecret);

    X509Certificate getCertificate();
    void setCertificate(X509Certificate certificate);
    String getCertificatePem();
    void setCertificatePem(String certificate);

    PrivateKey getPrivateKey();

    void setPrivateKey(PrivateKey privateKey);

    List<RequiredCredentialModel> getRequiredCredentials();

    void addRequiredCredential(String cred);

    PasswordPolicy getPasswordPolicy();

    void setPasswordPolicy(PasswordPolicy policy);

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

    OAuthClientModel addOAuthClient(String name);

    OAuthClientModel addOAuthClient(String id, String name);

    OAuthClientModel getOAuthClient(String name);
    OAuthClientModel getOAuthClientById(String id);
    boolean removeOAuthClient(String id);

    List<OAuthClientModel> getOAuthClients();

    Map<String, String> getBrowserSecurityHeaders();
    void setBrowserSecurityHeaders(Map<String, String> headers);

    Map<String, String> getSmtpConfig();

    void setSmtpConfig(Map<String, String> smtpConfig);

    List<IdentityProviderModel> getIdentityProviders();
    IdentityProviderModel getIdentityProviderByAlias(String alias);
    void addIdentityProvider(IdentityProviderModel identityProvider);
    void removeIdentityProviderByAlias(String alias);
    void updateIdentityProvider(IdentityProviderModel identityProvider);

    List<UserFederationProviderModel> getUserFederationProviders();

    UserFederationProviderModel addUserFederationProvider(String providerName, Map<String, String> config, int priority, String displayName, int fullSyncPeriod, int changedSyncPeriod, int lastSync);
    void updateUserFederationProvider(UserFederationProviderModel provider);
    void removeUserFederationProvider(UserFederationProviderModel provider);
    void setUserFederationProviders(List<UserFederationProviderModel> providers);

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

    boolean removeRoleById(String id);

    boolean isEventsEnabled();

    void setEventsEnabled(boolean enabled);

    long getEventsExpiration();

    void setEventsExpiration(long expiration);

    Set<String> getEventsListeners();

    void setEventsListeners(Set<String> listeners);

    ApplicationModel getMasterAdminApp();

    void setMasterAdminApp(ApplicationModel app);

    ClientModel findClientById(String id);

    boolean isIdentityFederationEnabled();

    boolean isInternationalizationEnabled();
    void setInternationalizationEnabled(boolean enabled);
    Set<String> getSupportedLocales();
    void setSupportedLocales(Set<String> locales);
    String getDefaultLocale();
    void setDefaultLocale(String locale);
}
