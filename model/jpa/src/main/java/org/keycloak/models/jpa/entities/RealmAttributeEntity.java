package org.keycloak.models.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="deleteRealmAttributesByRealm", query="delete from RealmAttributeEntity attr where attr.realm = :realm")
})
@Table(name="REALM_ATTRIBUTE")
@Entity
@IdClass(RealmAttributeEntity.Key.class)
public class RealmAttributeEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Id
    @Column(name = "NAME")
    protected String name;
    @Column(name = "VALUE")
    protected String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public static class Key implements Serializable {

        protected RealmEntity realm;

        protected String name;

        public Key() {
        }

        public Key(RealmEntity user, String name) {
            this.realm = user;
            this.name = name;
        }

        public RealmEntity getRealm() {
            return realm;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (name != null ? !name.equals(key.name) : key.name != null) return false;
            if (realm != null ? !realm.getId().equals(key.realm != null ? key.realm.getId() : null) : key.realm != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = realm != null ? realm.getId().hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

}
