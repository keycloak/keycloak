package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MappingsRepresentation {
    protected List<RoleRepresentation> realmMappings;
    protected Map<String, ClientMappingsRepresentation> clientMappings;

    public List<RoleRepresentation> getRealmMappings() {
        return realmMappings;
    }

    public void setRealmMappings(List<RoleRepresentation> realmMappings) {
        this.realmMappings = realmMappings;
    }

    public Map<String, ClientMappingsRepresentation> getClientMappings() {
        return clientMappings;
    }

    public void setClientMappings(Map<String, ClientMappingsRepresentation> clientMappings) {
        this.clientMappings = clientMappings;
    }
}
