package org.keycloak.models;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ApplicationModel {
    void updateApplication();

    UserModel getApplicationUser();

    String getId();

    String getName();

    void setName(String name);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isSurrogateAuthRequired();

    void setSurrogateAuthRequired(boolean surrogateAuthRequired);

    String getManagementUrl();

    void setManagementUrl(String url);

    String getBaseUrl();

    void setBaseUrl(String url);

    RoleModel getRole(String name);

    RoleModel addRole(String name);

    List<RoleModel> getRoles();

    Set<String> getRoleMappingValues(UserModel user);

    void addScopeMapping(UserModel agent, String roleName);

    void addScopeMapping(UserModel agent, RoleModel role);

    Set<String> getScopeMapping(UserModel agent);

    List<RoleModel> getRoleMappings(UserModel user);

    void deleteRoleMapping(UserModel user, RoleModel role);

    RoleModel getRoleById(String id);

    void grantRole(UserModel user, RoleModel role);
}
