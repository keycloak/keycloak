package org.keycloak.protocol.oid4vc.model;

import java.util.Objects;
import java.util.Set;

/**
 * Pojo representation of a role to be added by the {@link org.keycloak.protocol.oid4vc.issuance.mappers.OID4VPTargetRoleMapper}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class Role {

    private Set<String> names;
    private String target;

    public Role() {
    }

    public Role(Set<String> names, String target) {
        this.names = names;
        this.target = target;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(names, role.names) && Objects.equals(target, role.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(names, target);
    }
}