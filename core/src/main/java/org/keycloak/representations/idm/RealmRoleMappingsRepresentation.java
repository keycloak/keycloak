package org.keycloak.representations.idm;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmRoleMappingsRepresentation {
    protected String realmId;
    protected String realm;
    protected String username;

    protected List<RoleRepresentation> mappings;

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<RoleRepresentation> getMappings() {
        return mappings;
    }

    public void setMappings(List<RoleRepresentation> mappings) {
        this.mappings = mappings;
    }
}
