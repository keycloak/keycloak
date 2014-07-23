package org.keycloak.federation.ldap;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.FederationProviderModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LDAPUserModelDelegate  {
    protected UserModel delegate;
    protected LDAPFederationProvider provider;

    public String getId() {
        return delegate.getId();
    }

    public void setAttribute(String name, String value) {
        delegate.setAttribute(name, value);
    }

    public boolean isEmailVerified() {
        return delegate.isEmailVerified();
    }

    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    public String getLastName() {
        return delegate.getLastName();
    }

    public void setFederationLink(String link) {
        delegate.setFederationLink(link);
    }

    public AuthenticationLinkModel getAuthenticationLink() {
        return delegate.getAuthenticationLink();
    }

    public Map<String, String> getAttributes() {
        return delegate.getAttributes();
    }

    public boolean hasRole(RoleModel role) {
        return delegate.hasRole(role);
    }

    public void grantRole(RoleModel role) {
        delegate.grantRole(role);
    }

    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    public void removeRequiredAction(UserModel.RequiredAction action) {
        delegate.removeRequiredAction(action);
    }

    public void deleteRoleMapping(RoleModel role) {
        delegate.deleteRoleMapping(role);
    }

    public void setUsername(String username) {
        IdentityManager identityManager = provider.getIdentityManager();

        try {
            User picketlinkUser = BasicModel.getUser(identityManager, delegate.getUsername());
            if (picketlinkUser == null) {
                throw new IllegalStateException("User not found in LDAP storage!");
            }
            picketlinkUser.setLoginName(username);
            identityManager.update(picketlinkUser);
        } catch (IdentityManagementException ie) {
            throw new ModelException(ie);
        }
        delegate.setUsername(username);
    }

    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    public String getFirstName() {
        return delegate.getFirstName();
    }

    public void setLastName(String lastName) {
        IdentityManager identityManager = provider.getIdentityManager();

        try {
            User picketlinkUser = BasicModel.getUser(identityManager, delegate.getUsername());
            if (picketlinkUser == null) {
                throw new IllegalStateException("User not found in LDAP storage!");
            }
            picketlinkUser.setLastName(lastName);
            identityManager.update(picketlinkUser);
        } catch (IdentityManagementException ie) {
            throw new ModelException(ie);
        }
        delegate.setLastName(lastName);
    }

    public void setEmailVerified(boolean verified) {
        delegate.setEmailVerified(verified);
    }

    public void updateCredential(UserCredentialModel cred) {
        delegate.updateCredential(cred);
    }

    public void setEmail(String email) {
        IdentityManager identityManager = provider.getIdentityManager();

        try {
            User picketlinkUser = BasicModel.getUser(identityManager, delegate.getUsername());
            if (picketlinkUser == null) {
                throw new IllegalStateException("User not found in LDAP storage!");
            }
            picketlinkUser.setEmail(email);
            identityManager.update(picketlinkUser);
        } catch (IdentityManagementException ie) {
            throw new ModelException(ie);
        }
        delegate.setEmail(email);
    }

    public void addRequiredAction(UserModel.RequiredAction action) {
        delegate.addRequiredAction(action);
    }

    public List<UserCredentialValueModel> getCredentialsDirectly() {
        return delegate.getCredentialsDirectly();
    }

    public boolean isTotp() {
        return delegate.isTotp();
    }

    public void setFirstName(String firstName) {
        delegate.setFirstName(firstName);
    }

    public Set<UserModel.RequiredAction> getRequiredActions() {
        return delegate.getRequiredActions();
    }

    public String getEmail() {
        return delegate.getEmail();
    }

    public void setTotp(boolean totp) {
        delegate.setTotp(totp);
    }

    public void setAuthenticationLink(AuthenticationLinkModel authenticationLink) {
        delegate.setAuthenticationLink(authenticationLink);
    }

    public String getUsername() {
        return delegate.getUsername();
    }

    public String getFederationLink() {
        return delegate.getFederationLink();
    }

    public Set<RoleModel> getRealmRoleMappings() {
        return delegate.getRealmRoleMappings();
    }

    public Set<RoleModel> getRoleMappings() {
        return delegate.getRoleMappings();
    }

    public Set<RoleModel> getApplicationRoleMappings(ApplicationModel app) {
        return delegate.getApplicationRoleMappings(app);
    }

    public String getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    public void updateCredentialDirectly(UserCredentialValueModel cred) {
        delegate.updateCredentialDirectly(cred);
    }
}
