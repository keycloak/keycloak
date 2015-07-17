package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WritableLDAPUserModelDelegate extends UserModelDelegate implements UserModel {
    private static final Logger logger = Logger.getLogger(WritableLDAPUserModelDelegate.class);

    protected LDAPFederationProvider provider;
    protected LDAPObject ldapObject;

    public WritableLDAPUserModelDelegate(UserModel delegate, LDAPFederationProvider provider, LDAPObject ldapObject) {
        super(delegate);
        this.provider = provider;
        this.ldapObject = ldapObject;
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        if (!provider.getSupportedCredentialTypes(delegate).contains(cred.getType())) {
            delegate.updateCredential(cred);
            return;
        }

        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            LDAPIdentityStore ldapIdentityStore = provider.getLdapIdentityStore();
            String password = cred.getValue();
            ldapIdentityStore.updatePassword(ldapObject, password);
        } else {
            logger.warnf("Don't know how to update credential of type [%s] for user [%s]", cred.getType(), delegate.getUsername());
        }
    }

}
