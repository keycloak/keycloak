package org.keycloak.models.realms;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Application extends RoleContainer, Client {
    void updateApplication();

    String getName();

    void setName(String name);

    boolean isSurrogateAuthRequired();

    void setSurrogateAuthRequired(boolean surrogateAuthRequired);

    String getManagementUrl();

    void setManagementUrl(String url);

    String getBaseUrl();

    void setBaseUrl(String url);

    List<String> getDefaultRoles();

    void addDefaultRole(String name);

    void updateDefaultRoles(String[] defaultRoles);

    Set<Role> getApplicationScopeMappings(Client client);

    boolean isBearerOnly();
    void setBearerOnly(boolean only);

}
