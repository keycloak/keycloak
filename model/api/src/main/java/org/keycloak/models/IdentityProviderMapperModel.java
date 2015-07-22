package org.keycloak.models;

import java.io.Serializable;
import java.util.Map;

/**
 * Specifies a mapping from broker login to user data.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class IdentityProviderMapperModel implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected String name;
    protected String identityProviderAlias;
    protected String identityProviderMapper;
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

    public String getIdentityProviderAlias() {
        return identityProviderAlias;
    }

    public void setIdentityProviderAlias(String identityProviderAlias) {
        this.identityProviderAlias = identityProviderAlias;
    }

    public String getIdentityProviderMapper() {
        return identityProviderMapper;
    }

    public void setIdentityProviderMapper(String identityProviderMapper) {
        this.identityProviderMapper = identityProviderMapper;
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

        IdentityProviderMapperModel that = (IdentityProviderMapperModel) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
