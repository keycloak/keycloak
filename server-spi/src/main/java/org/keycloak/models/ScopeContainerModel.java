package org.keycloak.models;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ScopeContainerModel {
    boolean isFullScopeAllowed();

    void setFullScopeAllowed(boolean value);

    Set<RoleModel> getScopeMappings();

    void addScopeMapping(RoleModel role);

    void deleteScopeMapping(RoleModel role);

    Set<RoleModel> getRealmScopeMappings();

    boolean hasScope(RoleModel role);

}
