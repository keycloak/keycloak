package org.keycloak.models.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ApplicationEntity extends ClientEntity {

    private boolean surrogateAuthRequired;
    private String managementUrl;
    private String baseUrl;
    private boolean bearerOnly;
    private int nodeReRegistrationTimeout;

    // We are using names of defaultRoles (not ids)
    private List<String> defaultRoles = new ArrayList<String>();

    private Map<String, Integer> registeredNodes;

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    public String getManagementUrl() {
        return managementUrl;
    }

    public void setManagementUrl(String managementUrl) {
        this.managementUrl = managementUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(boolean bearerOnly) {
        this.bearerOnly = bearerOnly;
    }

    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public int getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    public void setNodeReRegistrationTimeout(int nodeReRegistrationTimeout) {
        this.nodeReRegistrationTimeout = nodeReRegistrationTimeout;
    }

    public Map<String, Integer> getRegisteredNodes() {
        return registeredNodes;
    }

    public void setRegisteredNodes(Map<String, Integer> registeredNodes) {
        this.registeredNodes = registeredNodes;
    }
}

