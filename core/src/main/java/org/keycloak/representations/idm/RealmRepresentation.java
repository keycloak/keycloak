package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmRepresentation {
    protected String self; // link
    protected String id;
    protected String realm;
    protected int tokenLifespan;
    protected int accessCodeLifespan;
    protected boolean enabled;
    protected boolean sslNotRequired;
    protected boolean cookieLoginAllowed;
    protected boolean registrationAllowed;
    protected boolean social;
    protected boolean automaticRegistrationAfterSocialLogin;
    protected String privateKey;
    protected String publicKey;
    protected List<RoleRepresentation> roles;
    protected String[] defaultRoles;
    protected Set<String> requiredCredentials;
    protected Set<String> requiredApplicationCredentials;
    protected Set<String> requiredOAuthClientCredentials;
    protected List<UserRepresentation> users;
    protected List<RoleMappingRepresentation> roleMappings;
    protected List<ScopeMappingRepresentation> scopeMappings;
    protected List<SocialMappingRepresentation> socialMappings;
    protected List<ApplicationRepresentation> applications;


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

    public boolean isCookieLoginAllowed() {
        return cookieLoginAllowed;
    }

    public void setCookieLoginAllowed(boolean cookieLoginAllowed) {
        this.cookieLoginAllowed = cookieLoginAllowed;
    }

    public int getTokenLifespan() {
        return tokenLifespan;
    }

    public void setTokenLifespan(int tokenLifespan) {
        this.tokenLifespan = tokenLifespan;
    }

    public List<RoleMappingRepresentation> getRoleMappings() {
        return roleMappings;
    }

    public RoleMappingRepresentation roleMapping(String username) {
        RoleMappingRepresentation mapping = new RoleMappingRepresentation();
        mapping.setUsername(username);
        if (roleMappings == null) roleMappings = new ArrayList<RoleMappingRepresentation>();
        roleMappings.add(mapping);
        return mapping;
    }

    public List<ScopeMappingRepresentation> getScopeMappings() {
        return scopeMappings;
    }

    public ScopeMappingRepresentation scopeMapping(String username) {
        ScopeMappingRepresentation mapping = new ScopeMappingRepresentation();
        mapping.setUsername(username);
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

    public Set<String> getRequiredApplicationCredentials() {
        return requiredApplicationCredentials;
    }

    public void setRequiredApplicationCredentials(Set<String> requiredApplicationCredentials) {
        this.requiredApplicationCredentials = requiredApplicationCredentials;
    }

    public Set<String> getRequiredOAuthClientCredentials() {
        return requiredOAuthClientCredentials;
    }

    public void setRequiredOAuthClientCredentials(Set<String> requiredOAuthClientCredentials) {
        this.requiredOAuthClientCredentials = requiredOAuthClientCredentials;
    }

    public int getAccessCodeLifespan() {
        return accessCodeLifespan;
    }

    public void setAccessCodeLifespan(int accessCodeLifespan) {
        this.accessCodeLifespan = accessCodeLifespan;
    }

    public List<RoleRepresentation> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleRepresentation> roles) {
        this.roles = roles;
    }

    public String[] getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(String[] defaultRoles) {
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

    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    public boolean isSocial() {
        return social;
    }

    public void setSocial(boolean social) {
        this.social = social;
    }

    public boolean isAutomaticRegistrationAfterSocialLogin() {
        return automaticRegistrationAfterSocialLogin;
    }

    public void setAutomaticRegistrationAfterSocialLogin(boolean automaticRegistrationAfterSocialLogin) {
        this.automaticRegistrationAfterSocialLogin = automaticRegistrationAfterSocialLogin;
    }
}
