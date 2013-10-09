package org.keycloak.models;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleMapperModel {
    boolean hasRole(UserModel user, RoleModel role);

    void grantRole(UserModel user, RoleModel role);

    Set<String> getRoleMappingValues(UserModel user);

    List<RoleModel> getRoleMappings(UserModel user);

    void deleteRoleMapping(UserModel user, RoleModel role);

    boolean hasRole(UserModel user, String role);
}
