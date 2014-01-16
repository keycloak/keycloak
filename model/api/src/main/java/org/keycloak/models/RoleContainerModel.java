package org.keycloak.models;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleContainerModel {
    RoleModel getRole(String name);

    RoleModel addRole(String name);

    boolean removeRoleById(String id);

    List<RoleModel> getRoles();

    RoleModel getRoleById(String id);
}
