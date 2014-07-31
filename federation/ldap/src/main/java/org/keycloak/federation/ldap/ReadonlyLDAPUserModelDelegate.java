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
public class ReadonlyLDAPUserModelDelegate extends UserModelDelegate implements UserModel {
    private static final Logger logger = Logger.getLogger(ReadonlyLDAPUserModelDelegate.class);

    protected LDAPFederationProvider provider;

    public ReadonlyLDAPUserModelDelegate(UserModel delegate, LDAPFederationProvider provider) {
        super(delegate);
        this.provider = provider;
    }

    @Override
    public void setUsername(String username) {
        throw new IllegalStateException("Federated storage is not writable");
    }

    @Override
    public void setLastName(String lastName) {
        throw new IllegalStateException("Federated storage is not writable");
    }

    @Override
    public void setFirstName(String first) {
        throw new IllegalStateException("Federated storage is not writable");
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        if (provider.getSupportedCredentialTypes(delegate).contains(cred.getType())) {
            throw new IllegalStateException("Federated storage is not writable");
        }
        delegate.updateCredential(cred);
    }

    @Override
    public void setEmail(String email) {
        throw new IllegalStateException("Federated storage is not writable");
    }

}
