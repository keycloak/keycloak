package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.models.ModelException;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.RoleModelDelegate;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Role;

/**
 * @author <a href="mailto:jli@vizuri.com">Jiehuan Li</a>
 * @version $Revision: 1 $
 */
public class WritableLDAPRoleModelDelegate extends RoleModelDelegate implements RoleModel {
    private static final Logger logger = Logger.getLogger(WritableLDAPRoleModelDelegate.class);

    protected LDAPFederationProvider provider;

    public WritableLDAPRoleModelDelegate(RoleModel delegate, LDAPFederationProvider provider) {
        super(delegate);
        this.provider = provider;
    }

    @Override
    public void setName(String rolename) {
        IdentityManager identityManager = provider.getIdentityManager();

        try {
            Role picketlinkRole = BasicModel.getRole(identityManager, delegate.getName());
            if (picketlinkRole == null) {
                throw new IllegalStateException("Role not found in LDAP storage!");
            }
            picketlinkRole.setName(rolename);
            identityManager.update(picketlinkRole);
        } catch (IdentityManagementException ie) {
            throw new ModelException(ie);
        }
        delegate.setName(rolename);
    }

}
