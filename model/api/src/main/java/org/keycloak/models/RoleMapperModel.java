package org.keycloak.models;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleMapperModel {
    Set<RoleModel> getRealmRoleMappings();

    Set<RoleModel> getClientRoleMappings(ClientModel app);

    boolean hasRole(RoleModel role);

    void grantRole(RoleModel role);

    Set<RoleModel> getRoleMappings();

    void deleteRoleMapping(RoleModel role);
}
