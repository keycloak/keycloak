package org.keycloak.models.picketlink;

import org.keycloak.models.RoleModel;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.sample.Role;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleAdapter implements RoleModel {
    protected Role role;
    protected IdentityManager idm;

    public RoleAdapter(Role role, IdentityManager idm) {
        this.role = role;
        this.idm = idm;
    }

    protected Role getRole() {
        return role;
    }

    @Override
    public String getId() {
        return role.getId();
    }

    @Override
    public String getName() {
        return role.getName();
    }

    @Override
    public void setName(String name) {
        role.setName(name);
        idm.update(role);
    }

    @Override
    public String getDescription() {
        Attribute<Serializable> description = role.getAttribute("description");
        if (description == null) return null;
        return (String) description.getValue();
    }

    @Override
    public void setDescription(String description) {
        if (description == null) {
            role.removeAttribute("description");
        } else {
            role.setAttribute(new Attribute<String>("description", description));
        }
        idm.update(role);
    }

}
