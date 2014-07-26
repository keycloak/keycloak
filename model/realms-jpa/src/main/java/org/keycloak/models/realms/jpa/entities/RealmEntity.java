package org.keycloak.models.realms.jpa.entities;


import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@NamedQueries({
        @NamedQuery(name="getAllRealms", query="select realm from RealmEntity realm"),
        @NamedQuery(name="getRealmByName", query="select realm from RealmEntity realm where realm.name = :name"),
})
public class RealmEntity {
    @Id
    protected String id;

    @Column(unique = true)
    protected String name;

    protected boolean enabled;
    protected boolean sslNotRequired;
    protected boolean registrationAllowed;
    protected boolean passwordCredentialGrantAllowed;
    protected boolean verifyEmail;
    protected boolean resetPasswordAllowed;
    protected boolean social;
    protected boolean rememberMe;
    //--- brute force settings
    protected boolean bruteForceProtected;
    protected int maxFailureWaitSeconds;
    protected int minimumQuickLoginWaitSeconds;
    protected int waitIncrementSeconds;
    protected long quickLoginCheckMilliSeconds;
    protected int maxDeltaTimeSeconds;
    protected int failureFactor;
    //--- end brute force settings


    @Column(name="updateProfileOnInitSocLogin")
    protected boolean updateProfileOnInitialSocialLogin;
    protected String passwordPolicy;

    private int ssoSessionIdleTimeout;
    private int ssoSessionMaxLifespan;
    protected int accessTokenLifespan;
    protected int accessCodeLifespan;
    protected int accessCodeLifespanUserAction;
    protected int notBefore;

    @Column(length = 2048)
    protected String publicKeyPem;
    @Column(length = 2048)
    protected String privateKeyPem;

    protected String loginTheme;
    protected String accountTheme;
    protected String adminTheme;
    protected String emailTheme;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name="User_RequiredCreds")
    Collection<RequiredCredentialEntity> requiredCredentials = new ArrayList<RequiredCredentialEntity>();


    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name="AuthProviders")
    List<AuthenticationProviderEntity> authenticationProviders = new ArrayList<AuthenticationProviderEntity>();

    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    Collection<ApplicationEntity> applications = new ArrayList<ApplicationEntity>();

    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realm")
    Collection<RoleEntity> roles = new ArrayList<RoleEntity>();

    @ElementCollection
    @MapKeyColumn(name="name")
    @Column(name="value")
    @CollectionTable
    protected Map<String, String> smtpConfig = new HashMap<String, String>();

    @ElementCollection
    @MapKeyColumn(name="name")
    @Column(name="value")
    @CollectionTable
    protected Map<String, String> socialConfig = new HashMap<String, String>();

    @ElementCollection
    @MapKeyColumn(name="name")
    @Column(name="value")
    @CollectionTable
    protected Map<String, String> ldapServerConfig = new HashMap<String, String>();

    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name="RealmDefaultRoles")
    protected Collection<RoleEntity> defaultRoles = new ArrayList<RoleEntity>();

    protected boolean auditEnabled;
    protected long auditExpiration;

    @ElementCollection
    protected Set<String> auditListeners= new HashSet<String>();

    @OneToOne
    protected ApplicationEntity masterAdminApp;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Collection<RequiredCredentialEntity> getRequiredCredentials() {
        return requiredCredentials;
    }

    public void setRequiredCredentials(Collection<RequiredCredentialEntity> requiredCredentials) {
        this.requiredCredentials = requiredCredentials;
    }

    public List<AuthenticationProviderEntity> getAuthenticationProviders() {
        return authenticationProviders;
    }

    public void setAuthenticationProviders(List<AuthenticationProviderEntity> authenticationProviders) {
        this.authenticationProviders = authenticationProviders;
    }

    public Collection<ApplicationEntity> getApplications() {
        return applications;
    }

    public void setApplications(Collection<ApplicationEntity> applications) {
        this.applications = applications;
    }

    public Collection<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Collection<RoleEntity> roles) {
        this.roles = roles;
    }

    public void addRole(RoleEntity role) {
        if (roles == null) {
            roles = new ArrayList<RoleEntity>();
        }
        roles.add(role);
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

    public Collection<RoleEntity> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(Collection<RoleEntity> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(String passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public String getLoginTheme() {
        return loginTheme;
    }

    public void setLoginTheme(String theme) {
        this.loginTheme = theme;
    }

    public String getAccountTheme() {
        return accountTheme;
    }

    public void setAccountTheme(String theme) {
        this.accountTheme = theme;
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

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
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

    public Set<String> getAuditListeners() {
        return auditListeners;
    }

    public void setAuditListeners(Set<String> auditListeners) {
        this.auditListeners = auditListeners;
    }

    public ApplicationEntity getMasterAdminApp() {
        return masterAdminApp;
    }

    public void setMasterAdminApp(ApplicationEntity masterAdminApp) {
        this.masterAdminApp = masterAdminApp;
    }

}

