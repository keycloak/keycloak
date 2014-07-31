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
public class UnsyncedLDAPUserModelDelegate extends UserModelDelegate implements UserModel {
    private static final Logger logger = Logger.getLogger(UnsyncedLDAPUserModelDelegate.class);

    protected LDAPFederationProvider provider;

    public UnsyncedLDAPUserModelDelegate(UserModel delegate, LDAPFederationProvider provider) {
        super(delegate);
        this.provider = provider;
    }
}
