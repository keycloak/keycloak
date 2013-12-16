package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MappingsRepresentation {
    protected List<RoleRepresentation> realmMappings;
    protected Map<String, ApplicationMappingsRepresentation> applicationMappings;

    public List<RoleRepresentation> getRealmMappings() {
        return realmMappings;
    }

    public void setRealmMappings(List<RoleRepresentation> realmMappings) {
        this.realmMappings = realmMappings;
    }

    public Map<String, ApplicationMappingsRepresentation> getApplicationMappings() {
        return applicationMappings;
    }

    public void setApplicationMappings(Map<String, ApplicationMappingsRepresentation> applicationMappings) {
        this.applicationMappings = applicationMappings;
    }
}
