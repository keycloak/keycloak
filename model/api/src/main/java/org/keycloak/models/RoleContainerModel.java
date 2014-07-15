package org.keycloak.models;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleContainerModel {
    RoleModel getRole(String name);

    RoleModel addRole(String name);

    RoleModel addRole(String id, String name);

    boolean removeRole(RoleModel role);

    Set<RoleModel> getRoles();

}
