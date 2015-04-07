package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.idm.model.LDAPUser;
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

    public WritableLDAPUserModelDelegate(UserModel delegate, LDAPFederationProvider provider) {
        super(delegate);
        this.provider = provider;
    }

    @Override
    public void setUsername(String username) {
        LDAPIdentityStore ldapIdentityStore = provider.getLdapIdentityStore();

        LDAPUser ldapUser = LDAPUtils.getUser(ldapIdentityStore, delegate.getUsername());
        if (ldapUser == null) {
            throw new IllegalStateException("User not found in LDAP storage!");
        }
        ldapUser.setLoginName(username);
        ldapIdentityStore.update(ldapUser);

        delegate.setUsername(username);
    }

    @Override
    public void setLastName(String lastName) {
        LDAPIdentityStore ldapIdentityStore = provider.getLdapIdentityStore();

        LDAPUser ldapUser = LDAPUtils.getUser(ldapIdentityStore, delegate.getUsername());
        if (ldapUser == null) {
            throw new IllegalStateException("User not found in LDAP storage!");
        }
        ldapUser.setLastName(lastName);
        ldapIdentityStore.update(ldapUser);

        delegate.setLastName(lastName);
    }

    @Override
    public void setFirstName(String first) {
        LDAPIdentityStore ldapIdentityStore = provider.getLdapIdentityStore();

        LDAPUser ldapUser = LDAPUtils.getUser(ldapIdentityStore, delegate.getUsername());
        if (ldapUser == null) {
            throw new IllegalStateException("User not found in LDAP storage!");
        }
        ldapUser.setFirstName(first);
        ldapIdentityStore.update(ldapUser);

        delegate.setFirstName(first);
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        if (!provider.getSupportedCredentialTypes(delegate).contains(cred.getType())) {
            delegate.updateCredential(cred);
            return;
        }

        LDAPIdentityStore ldapIdentityStore = provider.getLdapIdentityStore();
        LDAPUser ldapUser = LDAPUtils.getUser(ldapIdentityStore, delegate.getUsername());
        if (ldapUser == null) {
            throw new IllegalStateException("User " + delegate.getUsername() + " not found in LDAP storage!");
        }

        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            LDAPUtils.updatePassword(ldapIdentityStore, delegate, cred.getValue());
        } else {
            logger.warnf("Don't know how to update credential of type [%s] for user [%s]", cred.getType(), delegate.getUsername());
        }
    }

    @Override
    public void setEmail(String email) {
        LDAPIdentityStore ldapIdentityStore = provider.getLdapIdentityStore();

        LDAPUser ldapUser = LDAPUtils.getUser(ldapIdentityStore, delegate.getUsername());
        if (ldapUser == null) {
            throw new IllegalStateException("User not found in LDAP storage!");
        }
        ldapUser.setEmail(email);
        ldapIdentityStore.update(ldapUser);

        delegate.setEmail(email);
    }

}
