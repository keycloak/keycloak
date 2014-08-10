package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UnsyncedLDAPUserModelDelegate extends UserModelDelegate implements UserModel {
    private static final Logger logger = Logger.getLogger(UnsyncedLDAPUserModelDelegate.class);

    protected LDAPFederationProvider provider;

    public UnsyncedLDAPUserModelDelegate(UserModel delegate, LDAPFederationProvider provider) {
        super(delegate);
        this.provider = provider;
    }
}
