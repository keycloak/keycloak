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

    interface UserFederationProviderCreationEvent extends ProviderEvent {
        UserFederationProviderModel getCreatedFederationProvider();
        RealmModel getRealm();
    }

    interface UserFederationMapperEvent extends ProviderEvent {
        UserFederationMapperModel getFederationMapper();
        RealmModel getRealm();
        KeycloakSession getSession();
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

    boolean isRegistrationEmailAsUsername();

    void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername);

    boolean isRememberMe();

    void setRememberMe(boolean rememberMe);

    boolean isEditUsernameAllowed();

    void setEditUsernameAllowed(boolean editUsernameAllowed);

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

    // Key is clientId
    Map<String, ClientModel> getClientNameMap();

    List<ClientModel> getClients();

    ClientModel addClient(String name);

    ClientModel addClient(String id, String clientId);

    boolean removeClient(String id);

    ClientModel getClientById(String id);
    ClientModel getClientByClientId(String clientId);

    void updateRequiredCredentials(Set<String> creds);

    Map<String, String> getBrowserSecurityHeaders();
    void setBrowserSecurityHeaders(Map<String, String> headers);

    Map<String, String> getSmtpConfig();

    void setSmtpConfig(Map<String, String> smtpConfig);

    List<AuthenticationFlowModel> getAuthenticationFlows();
    AuthenticationFlowModel getFlowByAlias(String alias);
    AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model);
    AuthenticationFlowModel getAuthenticationFlowById(String id);
    void removeAuthenticationFlow(AuthenticationFlowModel model);
    void updateAuthenticationFlow(AuthenticationFlowModel model);

    List<AuthenticationExecutionModel> getAuthenticationExecutions(String flowId);
    AuthenticationExecutionModel getAuthenticationExecutionById(String id);
    AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model);
    void updateAuthenticatorExecution(AuthenticationExecutionModel model);
    void removeAuthenticatorExecution(AuthenticationExecutionModel model);


    List<AuthenticatorConfigModel> getAuthenticatorConfigs();
    AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model);
    void updateAuthenticatorConfig(AuthenticatorConfigModel model);
    void removeAuthenticatorConfig(AuthenticatorConfigModel model);
    AuthenticatorConfigModel getAuthenticatorConfigById(String id);
    AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias);

    List<RequiredActionProviderModel> getRequiredActionProviders();
    RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model);
    void updateRequiredActionProvider(RequiredActionProviderModel model);
    void removeRequiredActionProvider(RequiredActionProviderModel model);
    RequiredActionProviderModel getRequiredActionProviderById(String id);
    RequiredActionProviderModel getRequiredActionProviderByAlias(String alias);

    List<IdentityProviderModel> getIdentityProviders();
    IdentityProviderModel getIdentityProviderByAlias(String alias);
    void addIdentityProvider(IdentityProviderModel identityProvider);
    void removeIdentityProviderByAlias(String alias);
    void updateIdentityProvider(IdentityProviderModel identityProvider);
    Set<IdentityProviderMapperModel> getIdentityProviderMappers();
    Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(String brokerAlias);
    IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model);
    void removeIdentityProviderMapper(IdentityProviderMapperModel mapping);
    void updateIdentityProviderMapper(IdentityProviderMapperModel mapping);
    public IdentityProviderMapperModel getIdentityProviderMapperById(String id);
    public IdentityProviderMapperModel getIdentityProviderMapperByName(String brokerAlias, String name);

    // Should return list sorted by UserFederationProviderModel.priority
    List<UserFederationProviderModel> getUserFederationProviders();

    UserFederationProviderModel addUserFederationProvider(String providerName, Map<String, String> config, int priority, String displayName, int fullSyncPeriod, int changedSyncPeriod, int lastSync);
    void updateUserFederationProvider(UserFederationProviderModel provider);
    void removeUserFederationProvider(UserFederationProviderModel provider);
    void setUserFederationProviders(List<UserFederationProviderModel> providers);

    Set<UserFederationMapperModel> getUserFederationMappers();
    Set<UserFederationMapperModel> getUserFederationMappersByFederationProvider(String federationProviderId);
    UserFederationMapperModel addUserFederationMapper(UserFederationMapperModel mapper);
    void removeUserFederationMapper(UserFederationMapperModel mapper);
    void updateUserFederationMapper(UserFederationMapperModel mapper);
    UserFederationMapperModel getUserFederationMapperById(String id);
    UserFederationMapperModel getUserFederationMapperByName(String federationProviderId, String name);

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
    
    Set<String> getEnabledEventTypes();

    void setEnabledEventTypes(Set<String> enabledEventTypes);
    
    boolean isAdminEventsEnabled();

    void setAdminEventsEnabled(boolean enabled);
    
    boolean isAdminEventsDetailsEnabled();

    void setAdminEventsDetailsEnabled(boolean enabled);
    
    ClientModel getMasterAdminClient();

    void setMasterAdminClient(ClientModel client);

    boolean isIdentityFederationEnabled();

    boolean isInternationalizationEnabled();
    void setInternationalizationEnabled(boolean enabled);
    Set<String> getSupportedLocales();
    void setSupportedLocales(Set<String> locales);
    String getDefaultLocale();
    void setDefaultLocale(String locale);
}
