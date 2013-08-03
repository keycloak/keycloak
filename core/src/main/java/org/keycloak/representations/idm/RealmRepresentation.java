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
    protected String privateKey;
    protected String publicKey;
    protected List<RoleRepresentation> roles;
    protected List<RequiredCredentialRepresentation> requiredCredentials;
    protected List<UserRepresentation> users;
    protected List<RoleMappingRepresentation> roleMappings;
    protected List<ScopeMappingRepresentation> scopeMappings;
    protected List<ResourceRepresentation> resources;


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

    public List<ResourceRepresentation> getResources() {
        return resources;
    }

    public ResourceRepresentation resource(String name) {
        ResourceRepresentation resource = new ResourceRepresentation();
        if (resources == null) resources = new ArrayList<ResourceRepresentation>();
        resources.add(resource);
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

    public void setResources(List<ResourceRepresentation> resources) {
        this.resources = resources;
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

    public List<RequiredCredentialRepresentation> getRequiredCredentials() {
        return requiredCredentials;
    }

    public void setRequiredCredentials(List<RequiredCredentialRepresentation> requiredCredentials) {
        this.requiredCredentials = requiredCredentials;
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
}
