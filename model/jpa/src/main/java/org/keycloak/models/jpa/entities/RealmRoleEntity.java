package org.keycloak.models.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getRealmRoleByName", query="select role from RealmRoleEntity role where role.name = :name and role.realm = :realm")
})
@Entity
public class RealmRoleEntity extends RoleEntity {

    private String name;

    @ManyToOne
    @JoinColumn(name = "realm")
    private RealmEntity realm;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }
}
