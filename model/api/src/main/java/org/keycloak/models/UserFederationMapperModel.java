package org.keycloak.models;

import java.io.Serializable;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationMapperModel implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected String name;

    // Refers to DB ID of federation provider
    protected String federationProviderId;

    // Refers to ID of UserFederationMapper implementation ( UserFederationMapperFactory.getId )
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

    public String getFederationProviderId() {
        return federationProviderId;
    }

    public void setFederationProviderId(String federationProviderId) {
        this.federationProviderId = federationProviderId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserFederationMapperModel that = (UserFederationMapperModel) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder(" { name=" + name)
                .append(", federationMapperType=" + federationMapperType)
                .append(", config=" + config)
                .append(" } ").toString();
    }
}
