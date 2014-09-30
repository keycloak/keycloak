package org.keycloak.models.cache.entities;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedUser {
    private String id;
    private String realm;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private boolean emailVerified;
    private List<UserCredentialValueModel> credentials = new LinkedList<UserCredentialValueModel>();
    private boolean enabled;
    private boolean totp;
    private String federationLink;
    private Map<String, String> attributes = new HashMap<String, String>();
    private Set<UserModel.RequiredAction> requiredActions = new HashSet<UserModel.RequiredAction>();
    private Set<String> roleMappings = new HashSet<String>();


    public CachedUser(RealmModel realm, UserModel user) {
        this.id = user.getId();
        this.realm = realm.getId();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.attributes.putAll(user.getAttributes());
        this.email = user.getEmail();
        this.emailVerified = user.isEmailVerified();
        this.credentials.addAll(user.getCredentialsDirectly());
        this.enabled = user.isEnabled();
        this.totp = user.isTotp();
        this.federationLink = user.getFederationLink();
        this.requiredActions.addAll(user.getRequiredActions());
        for (RoleModel role : user.getRoleMappings()) {
            roleMappings.add(role.getId());
        }
    }

    public String getId() {
        return id;
    }

    public String getRealm() {
        return realm;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public List<UserCredentialValueModel> getCredentials() {
        return credentials;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isTotp() {
        return totp;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Set<UserModel.RequiredAction> getRequiredActions() {
        return requiredActions;
    }

    public Set<String> getRoleMappings() {
        return roleMappings;
    }

    public String getFederationLink() {
        return federationLink;
    }
}
