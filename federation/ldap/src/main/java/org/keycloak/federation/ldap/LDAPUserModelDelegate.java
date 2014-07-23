package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
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
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.TOTPCredential;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LDAPUserModelDelegate implements UserModel {
    private static final Logger logger = Logger.getLogger(LDAPUserModelDelegate.class);

    protected UserModel delegate;
    protected LDAPFederationProvider provider;

    public LDAPUserModelDelegate(UserModel delegate, LDAPFederationProvider provider) {
        this.delegate = delegate;
        this.provider = provider;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public void setAttribute(String name, String value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public boolean isEmailVerified() {
        return delegate.isEmailVerified();
    }

    @Override
    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    @Override
    public String getLastName() {
        return delegate.getLastName();
    }

    @Override
    public void setFederationLink(String link) {
        delegate.setFederationLink(link);
    }

    @Override
    public AuthenticationLinkModel getAuthenticationLink() {
        return delegate.getAuthenticationLink();
    }

    @Override
    public Map<String, String> getAttributes() {
        return delegate.getAttributes();
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
    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    @Override
    public void removeRequiredAction(UserModel.RequiredAction action) {
        delegate.removeRequiredAction(action);
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        delegate.deleteRoleMapping(role);
    }

    @Override
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

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public String getFirstName() {
        return delegate.getFirstName();
    }

    @Override
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

    @Override
    public void setEmailVerified(boolean verified) {
        delegate.setEmailVerified(verified);
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        if (!provider.getSupportedCredentialTypes().contains(cred.getType())) {
            delegate.updateCredential(cred);
            return;
        }
        IdentityManager identityManager = provider.getIdentityManager();

        try {
            User picketlinkUser = BasicModel.getUser(identityManager, getUsername());
            if (picketlinkUser == null) {
                logger.debugf("User '%s' doesn't exists. Skip password update", getUsername());
                throw new IllegalStateException("User doesn't exist in LDAP storage");
            }
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                identityManager.updateCredential(picketlinkUser, new Password(cred.getValue().toCharArray()));
            } else if (cred.getType().equals(UserCredentialModel.TOTP)) {
                TOTPCredential credential = new TOTPCredential(cred.getValue());
                credential.setDevice(cred.getDevice());
                identityManager.updateCredential(picketlinkUser, credential);
            }
        } catch (IdentityManagementException ie) {
            throw new ModelException(ie);
        }

    }

    @Override
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

    @Override
    public void addRequiredAction(UserModel.RequiredAction action) {
        delegate.addRequiredAction(action);
    }

    @Override
    public List<UserCredentialValueModel> getCredentialsDirectly() {
        return delegate.getCredentialsDirectly();
    }

    @Override
    public boolean isTotp() {
        return delegate.isTotp();
    }

    @Override
    public void setFirstName(String firstName) {
        delegate.setFirstName(firstName);
    }

    @Override
    public Set<UserModel.RequiredAction> getRequiredActions() {
        return delegate.getRequiredActions();
    }

    @Override
    public String getEmail() {
        return delegate.getEmail();
    }

    @Override
    public void setTotp(boolean totp) {
        delegate.setTotp(totp);
    }

    @Override
    public void setAuthenticationLink(AuthenticationLinkModel authenticationLink) {
        delegate.setAuthenticationLink(authenticationLink);
    }

    @Override
    public String getUsername() {
        return delegate.getUsername();
    }

    @Override
    public String getFederationLink() {
        return delegate.getFederationLink();
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        return delegate.getRealmRoleMappings();
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        return delegate.getRoleMappings();
    }

    @Override
    public Set<RoleModel> getApplicationRoleMappings(ApplicationModel app) {
        return delegate.getApplicationRoleMappings(app);
    }

    @Override
    public String getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel cred) {
        delegate.updateCredentialDirectly(cred);
    }
}
