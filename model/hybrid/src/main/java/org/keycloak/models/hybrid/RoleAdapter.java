package org.keycloak.models.hybrid;

import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.realms.Application;
import org.keycloak.models.realms.Realm;
import org.keycloak.models.realms.Role;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RoleAdapter implements RoleModel {

    private HybridModelProvider provider;

    private Role role;

    RoleAdapter(HybridModelProvider provider, Role role) {
        this.provider = provider;
        this.role = role;
    }

    Role getRole() {
        return role;
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
    }

    @Override
    public String getId() {
        return role.getId();
    }

    @Override
    public void setName(String name) {
        role.setName(name);
    }

    @Override
    public boolean isComposite() {
        return role.isComposite();
    }

    @Override
    public void addCompositeRole(RoleModel role) {
        this.role.addCompositeRole(provider.mappings().unwrap(role));
    }

    @Override
    public void removeCompositeRole(RoleModel role) {
        this.role.removeCompositeRole(provider.mappings().unwrap(role));
    }

    @Override
    public Set<RoleModel> getComposites() {
        return provider.mappings().wrap(role.getComposites());
    }

    @Override
    public RoleContainerModel getContainer() {
        if (role.getContainer() instanceof Application) {
             return provider.mappings().wrap((Application) role.getContainer());
        } else if (role.getContainer() instanceof Realm) {
            return provider.mappings().wrap((Realm) role.getContainer());
        } else {
            throw new IllegalArgumentException("Unsupported role container");
        }
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (this.equals(role)) {
            return true;
        }
        if (!isComposite()) {
            return false;
        }

        Set<RoleModel> visited = new HashSet<RoleModel>();
        return KeycloakModelUtils.searchFor(role, this, visited);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof RoleModel)) return false;

        RoleModel that = (RoleModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
