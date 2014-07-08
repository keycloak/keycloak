package org.keycloak.models.realms;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleContainer {

    Role getRole(String name);

    Set<Role> getRoles();

    Role addRole(String id, String name);

    boolean removeRole(Role role);

}
