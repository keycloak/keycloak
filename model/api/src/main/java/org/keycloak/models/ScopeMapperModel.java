package org.keycloak.models;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ScopeMapperModel {
    void addScopeMapping(UserModel agent, String roleName);

    Set<String> getScopeMappingValues(UserModel agent);

    List<RoleModel> getScopeMappings(UserModel agent);

    void addScopeMapping(UserModel agent, RoleModel role);

    void deleteScopeMapping(UserModel user, RoleModel role);
}
