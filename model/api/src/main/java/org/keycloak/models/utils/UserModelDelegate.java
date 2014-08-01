package org.keycloak.models.utils;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserModelDelegate implements UserModel {
    protected UserModel delegate;

    public UserModelDelegate(UserModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getUsername() {
        return delegate.getUsername();
    }

    @Override
    public void setUsername(String username) {
        delegate.setUsername(username);
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public boolean isTotp() {
        return delegate.isTotp();
    }

    @Override
    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    @Override
    public void setAttribute(String name, String value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Set<RequiredAction> getRequiredActions() {
        return delegate.getRequiredActions();
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        delegate.addRequiredAction(action);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        delegate.removeRequiredAction(action);
    }

    @Override
    public String getFirstName() {
        return delegate.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        delegate.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        return delegate.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        delegate.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        return delegate.getEmail();
    }

    @Override
    public void setEmail(String email) {
        delegate.setEmail(email);
    }

    @Override
    public boolean isEmailVerified() {
        return delegate.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        delegate.setEmailVerified(verified);
    }

    @Override
    public void setTotp(boolean totp) {
        delegate.setTotp(totp);
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        delegate.updateCredential(cred);
    }

    @Override
    public List<UserCredentialValueModel> getCredentialsDirectly() {
        return delegate.getCredentialsDirectly();
    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel cred) {
        delegate.updateCredentialDirectly(cred);
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        return delegate.getRealmRoleMappings();
    }

    @Override
    public Set<RoleModel> getApplicationRoleMappings(ApplicationModel app) {
        return delegate.getApplicationRoleMappings(app);
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return delegate.hasRole(role);
    }

    @Override
    public void grantRole(RoleModel role) {
        delegate.grantRole(role);
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        return delegate.getRoleMappings();
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        delegate.deleteRoleMapping(role);
    }

    @Override
    public String getFederationLink() {
        return delegate.getFederationLink();
    }

    @Override
    public void setFederationLink(String link) {
        delegate.setFederationLink(link);
    }
}
