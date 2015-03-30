package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.RoleModelDelegate;

/**
 * @author <a href="mailto:jli@vizuri.com">Jiehuan Li</a>
 * @version $Revision: 1 $
 */
public class UnsyncedLDAPRoleModelDelegate extends RoleModelDelegate implements RoleModel {
    private static final Logger logger = Logger.getLogger(UnsyncedLDAPRoleModelDelegate.class);

    protected LDAPFederationProvider provider;

    public UnsyncedLDAPRoleModelDelegate(RoleModel delegate, LDAPFederationProvider provider) {
        super(delegate);
        this.provider = provider;
    }
}
