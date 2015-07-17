package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RolesRepresentation {
    protected List<RoleRepresentation> realm;
    protected Map<String, List<RoleRepresentation>> client;
    @Deprecated
    protected Map<String, List<RoleRepresentation>> application;

    public List<RoleRepresentation> getRealm() {
        return realm;
    }

    public void setRealm(List<RoleRepresentation> realm) {
        this.realm = realm;
    }

    public Map<String, List<RoleRepresentation>> getClient() {
        return client;
    }

    public void setClient(Map<String, List<RoleRepresentation>> client) {
        this.client = client;
    }

    @Deprecated
    public Map<String, List<RoleRepresentation>> getApplication() {
        return application;
    }
}
