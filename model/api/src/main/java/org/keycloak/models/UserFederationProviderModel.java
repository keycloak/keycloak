package org.keycloak.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Stored configuration of a User Federation provider instance.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class UserFederationProviderModel {

    private String id;
    private String providerName;
    private Map<String, String> config = new HashMap<String, String>();
    private int priority;
    private String displayName;

    public UserFederationProviderModel() {};

    public UserFederationProviderModel(String id, String providerName, Map<String, String> config, int priority, String displayName) {
        this.id = id;
        this.providerName = providerName;
        if (config != null) {
           this.config.putAll(config);
        }
        this.priority = priority;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
