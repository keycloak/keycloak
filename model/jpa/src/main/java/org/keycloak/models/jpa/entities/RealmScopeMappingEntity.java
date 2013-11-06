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
        @NamedQuery(name="userHasRealmScope", query="select m from RealmScopeMappingEntity m where m.user = :user and m.role = :role and m.realm = :realm"),
        @NamedQuery(name="userRealmScopeMappings", query="select m from RealmScopeMappingEntity m where m.user = :user and m.realm = :realm")
})
@Entity
public class RealmScopeMappingEntity extends UserRoleMappingEntity {

    @ManyToOne
    protected RealmEntity realm;

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }
}
