package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationRepresentation {
    protected String self; // link
    protected String id;
    protected String name;
    protected String adminUrl;
    protected boolean surrogateAuthRequired;
    protected boolean enabled;
    protected List<CredentialRepresentation> credentials;
    protected List<RoleRepresentation> roles;
    protected String[] defaultRoles;
    protected List<String> redirectUris;
    protected List<String> webOrigins;

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

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    public List<RoleRepresentation> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleRepresentation> roles) {
        this.roles = roles;
    }

    public ApplicationRepresentation role(RoleRepresentation role) {
        if (this.roles == null) this.roles = new ArrayList<RoleRepresentation>();
        this.roles.add(role);
        return this;
    }


    public ApplicationRepresentation role(String role, String description) {
        if (this.roles == null) this.roles = new ArrayList<RoleRepresentation>();
        this.roles.add(new RoleRepresentation(role, description));
        return this;
    }

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    public List<CredentialRepresentation> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<CredentialRepresentation> credentials) {
        this.credentials = credentials;
    }

    public ApplicationRepresentation credential(String type, String value) {
        if (this.credentials == null) credentials = new ArrayList<CredentialRepresentation>();
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(type);
        cred.setValue(value);
        credentials.add(cred);
        return this;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(List<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    public String[] getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(String[] defaultRoles) {
        this.defaultRoles = defaultRoles;
    }
}
