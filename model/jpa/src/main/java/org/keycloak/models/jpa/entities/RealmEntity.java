package org.keycloak.models.jpa.entities;


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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    protected String name;
    protected boolean enabled;
    protected boolean sslNotRequired;
    protected boolean registrationAllowed;
    protected boolean verifyEmail;
    protected boolean resetPasswordAllowed;
    protected boolean social;
    protected boolean updateProfileOnInitialSocialLogin;
    protected String passwordPolicy;

    protected int tokenLifespan;
    protected int accessCodeLifespan;
    protected int accessCodeLifespanUserAction;

    @Column(length = 2048)
    protected String publicKeyPem;
    @Column(length = 2048)
    protected String privateKeyPem;

    protected String loginTheme;
    protected String accountTheme;

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name="USER_REQUIRED_CREDENTIALS")
    Collection<RequiredCredentialEntity> requiredCredentials = new ArrayList<RequiredCredentialEntity>();

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name="APPLICATION_REQUIRED_CREDENTIALS")
    Collection<RequiredCredentialEntity> requiredApplicationCredentials = new ArrayList<RequiredCredentialEntity>();

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name="OAUTH_CLIENT_REQUIRED_CREDENTIALS")
    Collection<RequiredCredentialEntity> requiredOAuthClientCredentials = new ArrayList<RequiredCredentialEntity>();

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    Collection<ApplicationEntity> applications = new ArrayList<ApplicationEntity>();

    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "realm")
    Collection<RealmRoleEntity> roles = new ArrayList<RealmRoleEntity>();

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

    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name="REALM_DEFAULT_ROLES")
    Collection<RoleEntity> defaultRoles = new ArrayList<RoleEntity>();

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

    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
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

    public int getTokenLifespan() {
        return tokenLifespan;
    }

    public void setTokenLifespan(int tokenLifespan) {
        this.tokenLifespan = tokenLifespan;
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

    public Collection<RequiredCredentialEntity> getRequiredApplicationCredentials() {
        return requiredApplicationCredentials;
    }

    public void setRequiredApplicationCredentials(Collection<RequiredCredentialEntity> requiredApplicationCredentials) {
        this.requiredApplicationCredentials = requiredApplicationCredentials;
    }

    public Collection<RequiredCredentialEntity> getRequiredOAuthClientCredentials() {
        return requiredOAuthClientCredentials;
    }

    public void setRequiredOAuthClientCredentials(Collection<RequiredCredentialEntity> requiredOAuthClientCredentials) {
        this.requiredOAuthClientCredentials = requiredOAuthClientCredentials;
    }

    public Collection<ApplicationEntity> getApplications() {
        return applications;
    }

    public void setApplications(Collection<ApplicationEntity> applications) {
        this.applications = applications;
    }

    public Collection<RealmRoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Collection<RealmRoleEntity> roles) {
        this.roles = roles;
    }

    public void addRole(RealmRoleEntity role) {
        if (roles == null) {
            roles = new ArrayList<RealmRoleEntity>();
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
}

