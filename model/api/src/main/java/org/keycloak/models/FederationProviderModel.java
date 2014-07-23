package org.keycloak.models;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class FederationProviderModel {

    private String id;
    private String providerName;
    private Map<String, String> config = new HashMap<String, String>();

    public FederationProviderModel() {};

    public FederationProviderModel(String id, String providerName, Map<String, String> config) {
        this.id = id;
        this.providerName = providerName;
        if (config != null) {
           this.config.putAll(config);
        }
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
}
