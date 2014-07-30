package org.keycloak.models.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RealmEntity extends AbstractIdentifiableEntity {

    private String name;
    private boolean enabled;
    private boolean sslNotRequired;
    private boolean registrationAllowed;
    private boolean rememberMe;
    private boolean verifyEmail;
    private boolean passwordCredentialGrantAllowed;
    private boolean resetPasswordAllowed;
    private boolean social;
    private boolean updateProfileOnInitialSocialLogin;
    private String passwordPolicy;
    //--- brute force settings
    private boolean bruteForceProtected;
    private int maxFailureWaitSeconds;
    private int minimumQuickLoginWaitSeconds;
    private int waitIncrementSeconds;
    private long quickLoginCheckMilliSeconds;
    private int maxDeltaTimeSeconds;
    private int failureFactor;
    //--- end brute force settings

    private int ssoSessionIdleTimeout;
    private int ssoSessionMaxLifespan;
    private int accessTokenLifespan;
    private int accessCodeLifespan;
    private int accessCodeLifespanUserAction;
    private int notBefore;

    private String publicKeyPem;
    private String privateKeyPem;

    private String loginTheme;
    private String accountTheme;
    private String adminTheme;
    private String emailTheme;

    // We are using names of defaultRoles (not ids)
    private List<String> defaultRoles = new ArrayList<String>();

    private List<RequiredCredentialEntity> requiredCredentials = new ArrayList<RequiredCredentialEntity>();
    private List<AuthenticationProviderEntity> authenticationProviders = new ArrayList<AuthenticationProviderEntity>();
    private List<UserFederationProviderEntity> userFederationProviders = new ArrayList<UserFederationProviderEntity>();

    private Map<String, String> smtpConfig = new HashMap<String, String>();
    private Map<String, String> socialConfig = new HashMap<String, String>();
    private Map<String, String> ldapServerConfig = new HashMap<String, String>();

    private boolean auditEnabled;
    private long auditExpiration;
    private List<String> auditListeners = new ArrayList<String>();

    private String adminAppId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSslNotRequired() {
        return sslNotRequired;
    }

    public void setSslNotRequired(boolean sslNotRequired) {
        this.sslNotRequired = sslNotRequired;
    }

    public boolean isPasswordCredentialGrantAllowed() {
        return passwordCredentialGrantAllowed;
    }

    public void setPasswordCredentialGrantAllowed(boolean passwordCredentialGrantAllowed) {
        this.passwordCredentialGrantAllowed = passwordCredentialGrantAllowed;
    }

    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public boolean isVerifyEmail() {
        return verifyEmail;
    }

    public void setVerifyEmail(boolean verifyEmail) {
        this.verifyEmail = verifyEmail;
    }

    public boolean isResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        this.resetPasswordAllowed = resetPasswordAllowed;
    }

    public boolean isSocial() {
        return social;
    }

    public void setSocial(boolean social) {
        this.social = social;
    }

    public boolean isUpdateProfileOnInitialSocialLogin() {
        return updateProfileOnInitialSocialLogin;
    }

    public void setUpdateProfileOnInitialSocialLogin(boolean updateProfileOnInitialSocialLogin) {
        this.updateProfileOnInitialSocialLogin = updateProfileOnInitialSocialLogin;
    }

    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(String passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public boolean isBruteForceProtected() {
        return bruteForceProtected;
    }

    public void setBruteForceProtected(boolean bruteForceProtected) {
        this.bruteForceProtected = bruteForceProtected;
    }

    public int getMaxFailureWaitSeconds() {
        return maxFailureWaitSeconds;
    }

    public void setMaxFailureWaitSeconds(int maxFailureWaitSeconds) {
        this.maxFailureWaitSeconds = maxFailureWaitSeconds;
    }

    public int getMinimumQuickLoginWaitSeconds() {
        return minimumQuickLoginWaitSeconds;
    }

    public void setMinimumQuickLoginWaitSeconds(int minimumQuickLoginWaitSeconds) {
        this.minimumQuickLoginWaitSeconds = minimumQuickLoginWaitSeconds;
    }

    public int getWaitIncrementSeconds() {
        return waitIncrementSeconds;
    }

    public void setWaitIncrementSeconds(int waitIncrementSeconds) {
        this.waitIncrementSeconds = waitIncrementSeconds;
    }

    public long getQuickLoginCheckMilliSeconds() {
        return quickLoginCheckMilliSeconds;
    }

    public void setQuickLoginCheckMilliSeconds(long quickLoginCheckMilliSeconds) {
        this.quickLoginCheckMilliSeconds = quickLoginCheckMilliSeconds;
    }

    public int getMaxDeltaTimeSeconds() {
        return maxDeltaTimeSeconds;
    }

    public void setMaxDeltaTimeSeconds(int maxDeltaTimeSeconds) {
        this.maxDeltaTimeSeconds = maxDeltaTimeSeconds;
    }

    public int getFailureFactor() {
        return failureFactor;
    }

    public void setFailureFactor(int failureFactor) {
        this.failureFactor = failureFactor;
    }

    public int getSsoSessionIdleTimeout() {
        return ssoSessionIdleTimeout;
    }

    public void setSsoSessionIdleTimeout(int ssoSessionIdleTimeout) {
        this.ssoSessionIdleTimeout = ssoSessionIdleTimeout;
    }

    public int getSsoSessionMaxLifespan() {
        return ssoSessionMaxLifespan;
    }

    public void setSsoSessionMaxLifespan(int ssoSessionMaxLifespan) {
        this.ssoSessionMaxLifespan = ssoSessionMaxLifespan;
    }

    public int getAccessTokenLifespan() {
        return accessTokenLifespan;
    }

    public void setAccessTokenLifespan(int accessTokenLifespan) {
        this.accessTokenLifespan = accessTokenLifespan;
    }

    public int getAccessCodeLifespan() {
        return accessCodeLifespan;
    }

    public void setAccessCodeLifespan(int accessCodeLifespan) {
        this.accessCodeLifespan = accessCodeLifespan;
    }

    public int getAccessCodeLifespanUserAction() {
        return accessCodeLifespanUserAction;
    }

    public void setAccessCodeLifespanUserAction(int accessCodeLifespanUserAction) {
        this.accessCodeLifespanUserAction = accessCodeLifespanUserAction;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    public String getLoginTheme() {
        return loginTheme;
    }

    public void setLoginTheme(String loginTheme) {
        this.loginTheme = loginTheme;
    }

    public String getAccountTheme() {
        return accountTheme;
    }

    public void setAccountTheme(String accountTheme) {
        this.accountTheme = accountTheme;
    }

    public String getAdminTheme() {
        return adminTheme;
    }

    public void setAdminTheme(String adminTheme) {
        this.adminTheme = adminTheme;
    }

    public String getEmailTheme() {
        return emailTheme;
    }

    public void setEmailTheme(String emailTheme) {
        this.emailTheme = emailTheme;
    }

    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public List<RequiredCredentialEntity> getRequiredCredentials() {
        return requiredCredentials;
    }

    public void setRequiredCredentials(List<RequiredCredentialEntity> requiredCredentials) {
        this.requiredCredentials = requiredCredentials;
    }

    public List<AuthenticationProviderEntity> getAuthenticationProviders() {
        return authenticationProviders;
    }

    public void setAuthenticationProviders(List<AuthenticationProviderEntity> authenticationProviders) {
        this.authenticationProviders = authenticationProviders;
    }

    public Map<String, String> getSmtpConfig() {
        return smtpConfig;
    }

    public void setSmtpConfig(Map<String, String> smtpConfig) {
        this.smtpConfig = smtpConfig;
    }

    public Map<String, String> getSocialConfig() {
        return socialConfig;
    }

    public void setSocialConfig(Map<String, String> socialConfig) {
        this.socialConfig = socialConfig;
    }

    public Map<String, String> getLdapServerConfig() {
        return ldapServerConfig;
    }

    public void setLdapServerConfig(Map<String, String> ldapServerConfig) {
        this.ldapServerConfig = ldapServerConfig;
    }

    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    public void setAuditEnabled(boolean auditEnabled) {
        this.auditEnabled = auditEnabled;
    }

    public long getAuditExpiration() {
        return auditExpiration;
    }

    public void setAuditExpiration(long auditExpiration) {
        this.auditExpiration = auditExpiration;
    }

    public List<String> getAuditListeners() {
        return auditListeners;
    }

    public void setAuditListeners(List<String> auditListeners) {
        this.auditListeners = auditListeners;
    }

    public String getAdminAppId() {
        return adminAppId;
    }

    public void setAdminAppId(String adminAppId) {
        this.adminAppId = adminAppId;
    }

    public List<UserFederationProviderEntity> getUserFederationProviders() {
        return userFederationProviders;
    }

    public void setUserFederationProviders(List<UserFederationProviderEntity> userFederationProviders) {
        this.userFederationProviders = userFederationProviders;
    }
}
