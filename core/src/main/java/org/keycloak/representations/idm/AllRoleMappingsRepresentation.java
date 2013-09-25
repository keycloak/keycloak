package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AllRoleMappingsRepresentation {
    protected String realmId;
    protected String realmName;
    protected String username;

    protected List<RoleRepresentation> realmMappings;
    protected Map<String, ApplicationRoleMappings> applicationMappings;

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<RoleRepresentation> getRealmMappings() {
        return realmMappings;
    }

    public void setRealmMappings(List<RoleRepresentation> realmMappings) {
        this.realmMappings = realmMappings;
    }

    public Map<String,ApplicationRoleMappings> getApplicationMappings() {
        return applicationMappings;
    }

    public void setApplicationMappings(Map<String, ApplicationRoleMappings> applicationMappings) {
        this.applicationMappings = applicationMappings;
    }
}
