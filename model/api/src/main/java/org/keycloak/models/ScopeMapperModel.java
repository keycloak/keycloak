package org.keycloak.models;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ScopeMapperModel {
    Set<RoleModel> getScopeMappings(ClientModel client);
    void addScopeMapping(ClientModel client, RoleModel role);
    void deleteScopeMapping(ClientModel client, RoleModel role);
}
