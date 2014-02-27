package org.keycloak.models.jpa.entities;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

import org.hibernate.annotations.GenericGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="findOAuthClientByName", query="select o from OAuthClientEntity o where o.name=:name and o.realm = :realm"),
        @NamedQuery(name="findOAuthClientByRealm", query="select o from OAuthClientEntity o where o.realm = :realm")

})
@Entity
public class OAuthClientEntity extends ClientEntity {

    @ManyToOne()
    private RealmEntity realm;

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }


}
