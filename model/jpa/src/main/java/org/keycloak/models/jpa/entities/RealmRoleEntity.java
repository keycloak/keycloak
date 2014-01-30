package org.keycloak.models.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getRealmRoleByName", query="select role from RealmRoleEntity role where role.name = :name and role.realm = :realm")
})
@Entity
public class RealmRoleEntity extends RoleEntity {
    @ManyToOne
    private RealmEntity realm;

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }
}
