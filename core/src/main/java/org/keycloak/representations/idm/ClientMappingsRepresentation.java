package org.keycloak.representations.idm;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientMappingsRepresentation {
    protected String id;
    protected String client;

    protected List<RoleRepresentation> mappings;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public List<RoleRepresentation> getMappings() {
        return mappings;
    }

    public void setMappings(List<RoleRepresentation> mappings) {
        this.mappings = mappings;
    }
}
