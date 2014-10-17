package org.keycloak.models;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ApplicationModel extends RoleContainerModel, ClientModel {
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

    Set<RoleModel> getApplicationScopeMappings(ClientModel client);

    boolean isBearerOnly();
    void setBearerOnly(boolean only);

    int getNodeReRegistrationTimeout();

    void setNodeReRegistrationTimeout(int timeout);

    Map<String, Integer> getRegisteredNodes();

    /**
     * Register node or just update the 'lastReRegistration' time if this node is already registered
     *
     * @param nodeHost
     * @param registrationTime
     */
    void registerNode(String nodeHost, int registrationTime);

    void unregisterNode(String nodeHost);

}
