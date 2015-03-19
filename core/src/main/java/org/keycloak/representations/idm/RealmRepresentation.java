package org.keycloak.representations.idm;

import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmRepresentation {
    protected String id;
    protected String realm;
    protected Integer notBefore;
    protected Integer accessTokenLifespan;
    protected Integer ssoSessionIdleTimeout;
    protected Integer ssoSessionMaxLifespan;
    protected Integer accessCodeLifespan;
    protected Integer accessCodeLifespanUserAction;
    protected Integer accessCodeLifespanLogin;
    protected Boolean enabled;
    protected String sslRequired;
    protected Boolean passwordCredentialGrantAllowed;
    protected Boolean registrationAllowed;
    protected Boolean registrationEmailAsUsername;
    protected Boolean rememberMe;
    protected Boolean verifyEmail;
    protected Boolean resetPasswordAllowed;

    @Deprecated
    protected Boolean social;
    @Deprecated
    protected Boolean updateProfileOnInitialSocialLogin;
    @Deprecated
    protected Map<String, String> socialProviders;

    protected Boolean userCacheEnabled;
    protected Boolean realmCacheEnabled;

    //--- brute force settings
    protected Boolean bruteForceProtected;
    protected Integer maxFailureWaitSeconds;
    protected Integer minimumQuickLoginWaitSeconds;
    protected Integer waitIncrementSeconds;
    protected Long quickLoginCheckMilliSeconds;
    protected Integer maxDeltaTimeSeconds;
    protected Integer failureFactor;
    //--- end brute force settings

    protected String privateKey;
    protected String publicKey;
    protected String certificate;
    protected String codeSecret;
    protected RolesRepresentation roles;
    protected List<String> defaultRoles;
    protected Set<String> requiredCredentials;
    protected String passwordPolicy;
    protected List<UserRepresentation> users;
    protected List<ScopeMappingRepresentation> scopeMappings;
    protected Map<String, List<ScopeMappingRepresentation>> applicationScopeMappings;
    protected List<ApplicationRepresentation> applications;
    protected List<OAuthClientRepresentation> oauthClients;
    protected Map<String, String> browserSecurityHeaders;
    protected Map<String, String> smtpServer;
    protected List<UserFederationProviderRepresentation> userFederationProviders;
    protected String loginTheme;
    protected String accountTheme;
    protected String adminTheme;
    protected String emailTheme;
    protected Boolean eventsEnabled;
    protected Long eventsExpiration;
    protected List<String> eventsListeners;
    private List<IdentityProviderRepresentation> identityProviders;
    private List<ProtocolMapperRepresentation> protocolMappers;
    private Boolean identityFederationEnabled;
    protected Boolean internationalizationEnabled;
    protected Set<String> supportedLocales;
    protected String defaultLocale;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public List<UserRepresentation> getUsers() {
        return users;
    }

    public List<ApplicationRepresentation> getApplications() {
        return applications;
    }

    public ApplicationRepresentation resource(String name) {
        ApplicationRepresentation resource = new ApplicationRepresentation();
        if (applications == null) applications = new ArrayList<ApplicationRepresentation>();
        applications.add(resource);
        resource.setName(name);
        return resource;
    }

    public void setUsers(List<UserRepresentation> users) {
        this.users = users;
    }

    public UserRepresentation user(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        if (users == null) users = new ArrayList<UserRepresentation>();
        users.add(user);
        return user;
    }

    public void setApplications(List<ApplicationRepresentation> applications) {
        this.applications = applications;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getSslRequired() {
        return sslRequired;
    }

    public void setSslRequired(String sslRequired) {
        this.sslRequired = sslRequired;
    }

    public Integer getAccessTokenLifespan() {
        return accessTokenLifespan;
    }

    public void setAccessTokenLifespan(Integer accessTokenLifespan) {
        this.accessTokenLifespan = accessTokenLifespan;
    }

    public Integer getSsoSessionIdleTimeout() {
        return ssoSessionIdleTimeout;
    }

    public void setSsoSessionIdleTimeout(Integer ssoSessionIdleTimeout) {
        this.ssoSessionIdleTimeout = ssoSessionIdleTimeout;
    }

    public Integer getSsoSessionMaxLifespan() {
        return ssoSessionMaxLifespan;
    }

    public void setSsoSessionMaxLifespan(Integer ssoSessionMaxLifespan) {
        this.ssoSessionMaxLifespan = ssoSessionMaxLifespan;
    }

    public List<ScopeMappingRepresentation> getScopeMappings() {
        return scopeMappings;
    }

    public ScopeMappingRepresentation scopeMapping(String username) {
        ScopeMappingRepresentation mapping = new ScopeMappingRepresentation();
        mapping.setClient(username);
        if (scopeMappings == null) scopeMappings = new ArrayList<ScopeMappingRepresentation>();
        scopeMappings.add(mapping);
        return mapping;
    }

    public Set<String> getRequiredCredentials() {
        return requiredCredentials;
    }

    public void setRequiredCredentials(Set<String> requiredCredentials) {
        this.requiredCredentials = requiredCredentials;
    }

    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(String passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public Integer getAccessCodeLifespan() {
        return accessCodeLifespan;
    }

    public void setAccessCodeLifespan(Integer accessCodeLifespan) {
        this.accessCodeLifespan = accessCodeLifespan;
    }

    public Integer getAccessCodeLifespanUserAction() {
        return accessCodeLifespanUserAction;
    }

    public void setAccessCodeLifespanUserAction(Integer accessCodeLifespanUserAction) {
        this.accessCodeLifespanUserAction = accessCodeLifespanUserAction;
    }

    public Integer getAccessCodeLifespanLogin() {
        return accessCodeLifespanLogin;
    }

    public void setAccessCodeLifespanLogin(Integer accessCodeLifespanLogin) {
        this.accessCodeLifespanLogin = accessCodeLifespanLogin;
    }

    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getCodeSecret() {
        return codeSecret;
    }

    public void setCodeSecret(String codeSecret) {
        this.codeSecret = codeSecret;
    }

    public Boolean isPasswordCredentialGrantAllowed() {
        return passwordCredentialGrantAllowed;
    }

    public void setPasswordCredentialGrantAllowed(Boolean passwordCredentialGrantAllowed) {
        this.passwordCredentialGrantAllowed = passwordCredentialGrantAllowed;
    }

    public Boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(Boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    public Boolean isRegistrationEmailAsUsername() {
        return registrationEmailAsUsername;
    }

    public void setRegistrationEmailAsUsername(Boolean registrationEmailAsUsername) {
        this.registrationEmailAsUsername = registrationEmailAsUsername;
    }

    public Boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public Boolean isRealmCacheEnabled() {
        return realmCacheEnabled;
    }

    public void setRealmCacheEnabled(Boolean realmCacheEnabled) {
        this.realmCacheEnabled = realmCacheEnabled;
    }

    public Boolean isUserCacheEnabled() {
        return userCacheEnabled;
    }

    public void setUserCacheEnabled(Boolean userCacheEnabled) {
        this.userCacheEnabled = userCacheEnabled;
    }

    public Boolean isVerifyEmail() {
        return verifyEmail;
    }

    public void setVerifyEmail(Boolean verifyEmail) {
        this.verifyEmail = verifyEmail;
    }

    public Boolean isResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public void setResetPasswordAllowed(Boolean resetPassword) {
        this.resetPasswordAllowed = resetPassword;
    }

    public Boolean isSocial() {
        return social;
    }

    public void setSocial(Boolean social) {
        this.social = social;
    }

    public Boolean isUpdateProfileOnInitialSocialLogin() {
        return updateProfileOnInitialSocialLogin;
    }

    public void setUpdateProfileOnInitialSocialLogin(Boolean updateProfileOnInitialSocialLogin) {
        this.updateProfileOnInitialSocialLogin = updateProfileOnInitialSocialLogin;
    }

    public Map<String, String> getBrowserSecurityHeaders() {
        return browserSecurityHeaders;
    }

    public void setBrowserSecurityHeaders(Map<String, String> browserSecurityHeaders) {
        this.browserSecurityHeaders = browserSecurityHeaders;
    }

    public Map<String, String> getSocialProviders() {
        return socialProviders;
    }

    public void setSocialProviders(Map<String, String> socialProviders) {
        this.socialProviders = socialProviders;
    }

    public Map<String, String> getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(Map<String, String> smtpServer) {
        this.smtpServer = smtpServer;
    }

    public List<OAuthClientRepresentation> getOauthClients() {
        return oauthClients;
    }

    public void setOauthClients(List<OAuthClientRepresentation> oauthClients) {
        this.oauthClients = oauthClients;
    }

    public Map<String, List<ScopeMappingRepresentation>> getApplicationScopeMappings() {
        return applicationScopeMappings;
    }

    public void setApplicationScopeMappings(Map<String, List<ScopeMappingRepresentation>> applicationScopeMappings) {
        this.applicationScopeMappings = applicationScopeMappings;
    }

    public RolesRepresentation getRoles() {
        return roles;
    }

    public void setRoles(RolesRepresentation roles) {
        this.roles = roles;
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

    public Integer getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Integer notBefore) {
        this.notBefore = notBefore;
    }

    public Boolean isBruteForceProtected() {
        return bruteForceProtected;
    }

    public void setBruteForceProtected(Boolean bruteForceProtected) {
        this.bruteForceProtected = bruteForceProtected;
    }

    public Integer getMaxFailureWaitSeconds() {
        return maxFailureWaitSeconds;
    }

    public void setMaxFailureWaitSeconds(Integer maxFailureWaitSeconds) {
        this.maxFailureWaitSeconds = maxFailureWaitSeconds;
    }

    public Integer getMinimumQuickLoginWaitSeconds() {
        return minimumQuickLoginWaitSeconds;
    }

    public void setMinimumQuickLoginWaitSeconds(Integer minimumQuickLoginWaitSeconds) {
        this.minimumQuickLoginWaitSeconds = minimumQuickLoginWaitSeconds;
    }

    public Integer getWaitIncrementSeconds() {
        return waitIncrementSeconds;
    }

    public void setWaitIncrementSeconds(Integer waitIncrementSeconds) {
        this.waitIncrementSeconds = waitIncrementSeconds;
    }

    public Long getQuickLoginCheckMilliSeconds() {
        return quickLoginCheckMilliSeconds;
    }

    public void setQuickLoginCheckMilliSeconds(Long quickLoginCheckMilliSeconds) {
        this.quickLoginCheckMilliSeconds = quickLoginCheckMilliSeconds;
    }

    public Integer getMaxDeltaTimeSeconds() {
        return maxDeltaTimeSeconds;
    }

    public void setMaxDeltaTimeSeconds(Integer maxDeltaTimeSeconds) {
        this.maxDeltaTimeSeconds = maxDeltaTimeSeconds;
    }

    public Integer getFailureFactor() {
        return failureFactor;
    }

    public void setFailureFactor(Integer failureFactor) {
        this.failureFactor = failureFactor;
    }

    public Boolean isEventsEnabled() {
        return eventsEnabled;
    }

    public void setEventsEnabled(boolean eventsEnabled) {
        this.eventsEnabled = eventsEnabled;
    }

    public Long getEventsExpiration() {
        return eventsExpiration;
    }

    public void setEventsExpiration(long eventsExpiration) {
        this.eventsExpiration = eventsExpiration;
    }

    public List<String> getEventsListeners() {
        return eventsListeners;
    }

    public void setEventsListeners(List<String> eventsListeners) {
        this.eventsListeners = eventsListeners;
    }

    public List<UserFederationProviderRepresentation> getUserFederationProviders() {
        return userFederationProviders;
    }

    public void setUserFederationProviders(List<UserFederationProviderRepresentation> userFederationProviders) {
        this.userFederationProviders = userFederationProviders;
    }

    public List<IdentityProviderRepresentation> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<IdentityProviderRepresentation> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public void addIdentityProvider(IdentityProviderRepresentation identityProviderRepresentation) {
        if (identityProviders == null) identityProviders = new LinkedList<>();
        identityProviders.add(identityProviderRepresentation);
    }

    public boolean isIdentityFederationEnabled() {
        return identityProviders != null && !identityProviders.isEmpty();
    }

    public List<ProtocolMapperRepresentation> getProtocolMappers() {
        return protocolMappers;
    }

    public void addProtocolMapper(ProtocolMapperRepresentation rep) {
        if (protocolMappers == null) protocolMappers = new LinkedList<ProtocolMapperRepresentation>();
        protocolMappers.add(rep);
    }

    public void setProtocolMappers(List<ProtocolMapperRepresentation> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    public Boolean isInternationalizationEnabled() {
        return internationalizationEnabled;
    }

    public void setInternationalizationEnabled(Boolean internationalizationEnabled) {
        this.internationalizationEnabled = internationalizationEnabled;
    }

    public Set<String> getSupportedLocales() {
        if(supportedLocales == null){
            supportedLocales = new HashSet<String>();
        }
        return supportedLocales;
    }

    public void setSupportedLocales(Set<String> supportedLocales) {
        this.supportedLocales = supportedLocales;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }
}
