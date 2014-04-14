package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmRepresentation {
    protected String self; // link
    protected String id;
    protected String realm;
    protected Integer notBefore;
    protected Integer accessTokenLifespan;
    protected Integer refreshTokenLifespan;
    protected Integer centralLoginLifespan;
    protected Integer accessCodeLifespan;
    protected Integer accessCodeLifespanUserAction;
    protected Boolean enabled;
    protected Boolean sslNotRequired;
    protected Boolean registrationAllowed;
    protected Boolean rememberMe;
    protected Boolean verifyEmail;
    protected Boolean resetPasswordAllowed;
    protected Boolean social;
    protected Boolean updateProfileOnInitialSocialLogin;
    protected Boolean bruteForceProtected;
    protected String privateKey;
    protected String publicKey;
    protected RolesRepresentation roles;
    protected List<String> defaultRoles;
    protected Set<String> requiredCredentials;
    protected String passwordPolicy;
    protected List<UserRepresentation> users;
    protected List<UserRoleMappingRepresentation> roleMappings;
    protected List<ScopeMappingRepresentation> scopeMappings;
    protected Map<String, List<UserRoleMappingRepresentation>> applicationRoleMappings;
    protected Map<String, List<ScopeMappingRepresentation>> applicationScopeMappings;
    protected List<SocialMappingRepresentation> socialMappings;
    protected List<ApplicationRepresentation> applications;
    protected List<OAuthClientRepresentation> oauthClients;
    protected Map<String, String> socialProviders;
    protected Map<String, String> smtpServer;
    protected Map<String, String> ldapServer;
    protected List<AuthenticationProviderRepresentation> authenticationProviders;
    protected String loginTheme;
    protected String accountTheme;
    protected boolean auditEnabled;
    protected long auditExpiration;
    protected List<String> auditListeners;

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

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

    public Boolean isSslNotRequired() {
        return sslNotRequired;
    }

    public void setSslNotRequired(Boolean sslNotRequired) {
        this.sslNotRequired = sslNotRequired;
    }

    public Integer getAccessTokenLifespan() {
        return accessTokenLifespan;
    }

    public void setAccessTokenLifespan(Integer accessTokenLifespan) {
        this.accessTokenLifespan = accessTokenLifespan;
    }

    public Integer getRefreshTokenLifespan() {
        return refreshTokenLifespan;
    }

    public Integer getCentralLoginLifespan() {
        return centralLoginLifespan;
    }

    public void setCentralLoginLifespan(Integer centralLoginLifespan) {
        this.centralLoginLifespan = centralLoginLifespan;
    }

    public void setRefreshTokenLifespan(Integer refreshTokenLifespan) {
        this.refreshTokenLifespan = refreshTokenLifespan;
    }

    public List<UserRoleMappingRepresentation> getRoleMappings() {
        return roleMappings;
    }

    public UserRoleMappingRepresentation roleMapping(String username) {
        UserRoleMappingRepresentation mapping = new UserRoleMappingRepresentation();
        mapping.setUsername(username);
        if (roleMappings == null) roleMappings = new ArrayList<UserRoleMappingRepresentation>();
        roleMappings.add(mapping);
        return mapping;
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

    public List<SocialMappingRepresentation> getSocialMappings() {
        return socialMappings;
    }

    public SocialMappingRepresentation socialMapping(String username) {
        SocialMappingRepresentation mapping = new SocialMappingRepresentation();
        mapping.setUsername(username);
        if (socialMappings == null) socialMappings = new ArrayList<SocialMappingRepresentation>();
        socialMappings.add(mapping);
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

    public Boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(Boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    public Boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
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

    public Map<String, String> getLdapServer() {
        return ldapServer;
    }

    public void setLdapServer(Map<String, String> ldapServer) {
        this.ldapServer = ldapServer;
    }

    public List<AuthenticationProviderRepresentation> getAuthenticationProviders() {
        return authenticationProviders;
    }

    public void setAuthenticationProviders(List<AuthenticationProviderRepresentation> authenticationProviders) {
        this.authenticationProviders = authenticationProviders;
    }

    public List<OAuthClientRepresentation> getOauthClients() {
        return oauthClients;
    }

    public void setOauthClients(List<OAuthClientRepresentation> oauthClients) {
        this.oauthClients = oauthClients;
    }

    public Map<String, List<UserRoleMappingRepresentation>> getApplicationRoleMappings() {
        return applicationRoleMappings;
    }

    public void setApplicationRoleMappings(Map<String, List<UserRoleMappingRepresentation>> applicationRoleMappings) {
        this.applicationRoleMappings = applicationRoleMappings;
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
}
