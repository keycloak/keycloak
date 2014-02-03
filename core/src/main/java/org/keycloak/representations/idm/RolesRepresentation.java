package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RolesRepresentation {
    protected List<RoleRepresentation> realm;
    protected Map<String, List<RoleRepresentation>> application;

    public List<RoleRepresentation> getRealm() {
        return realm;
    }

    public void setRealm(List<RoleRepresentation> realm) {
        this.realm = realm;
    }

    public Map<String, List<RoleRepresentation>> getApplication() {
        return application;
    }

    public void setApplication(Map<String, List<RoleRepresentation>> application) {
        this.application = application;
    }
}
