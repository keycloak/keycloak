package org.keycloak.services.models.nosql.keycloak.adapters;

import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.keycloak.data.RoleData;

/**
 * Wrapper around RoleData object, which will persist wrapped object after each set operation (compatibility with picketlink based impl)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleAdapter implements RoleModel {

    private final RoleData role;
    private final NoSQL noSQL;

    public RoleAdapter(RoleData roleData, NoSQL noSQL) {
        this.role = roleData;
        this.noSQL = noSQL;
    }

    @Override
    public String getName() {
        return role.getName();
    }

    @Override
    public String getDescription() {
        return role.getDescription();
    }

    @Override
    public void setDescription(String description) {
        role.setDescription(description);
        noSQL.saveObject(role);
    }

    @Override
    public String getId() {
        return role.getId();
    }

    @Override
    public void setName(String name) {
        role.setName(name);
        noSQL.saveObject(role);
    }

    public RoleData getRole() {
        return role;
    }
}
