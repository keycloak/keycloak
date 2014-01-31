package org.keycloak.models;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ApplicationModel extends RoleContainerModel {
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

    List<String> getDefaultRoles();

    void addDefaultRole(String name);

    void updateDefaultRoles(String[] defaultRoles);

    Set<RoleModel> getApplicationRoleMappings(UserModel user);

    Set<RoleModel> getApplicationScopeMappings(UserModel user);

    void addScope(RoleModel role);
}
