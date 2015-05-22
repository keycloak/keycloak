package org.keycloak.representations.idm;

import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationMapperRepresentation {

    protected String id;
    protected String name;
    protected String federationProviderDisplayName;
    protected String federationMapperType;
    protected Map<String, String> config;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFederationProviderDisplayName() {
        return federationProviderDisplayName;
    }

    public void setFederationProviderDisplayName(String federationProviderDisplayName) {
        this.federationProviderDisplayName = federationProviderDisplayName;
    }

    public String getFederationMapperType() {
        return federationMapperType;
    }

    public void setFederationMapperType(String federationMapperType) {
        this.federationMapperType = federationMapperType;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}

