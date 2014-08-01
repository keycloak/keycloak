package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.models.ModelException;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.TOTPCredential;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WritableLDAPUserModelDelegate extends UserModelDelegate implements UserModel {
    private static final Logger logger = Logger.getLogger(WritableLDAPUserModelDelegate.class);

    protected LDAPFederationProvider provider;

    public WritableLDAPUserModelDelegate(UserModel delegate, LDAPFederationProvider provider) {
        super(delegate);
        this.provider = provider;
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
    public void setFirstName(String first) {
        IdentityManager identityManager = provider.getIdentityManager();

        try {
            User picketlinkUser = BasicModel.getUser(identityManager, delegate.getUsername());
            if (picketlinkUser == null) {
                throw new IllegalStateException("User not found in LDAP storage!");
            }
            picketlinkUser.setFirstName(first);
            identityManager.update(picketlinkUser);
        } catch (IdentityManagementException ie) {
            throw new ModelException(ie);
        }
        delegate.setFirstName(first);
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        if (!provider.getSupportedCredentialTypes(delegate).contains(cred.getType())) {
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

}
