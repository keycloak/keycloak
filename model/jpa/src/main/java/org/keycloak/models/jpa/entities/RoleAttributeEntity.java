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
 * @author <a href="mailto:jli@vizuri.com">Jiehuan Li</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="deleteRoleAttributesByRealm", query="delete from  RoleAttributeEntity attr where attr.role IN (select r from RoleEntity r where r.realmId=:realmId)"),
        @NamedQuery(name="deleteRoleAttributesByRealmAndLink", query="delete from  RoleAttributeEntity attr where attr.role IN (select r from RoleEntity r where r.realmId=:realmId and r.federationLink=:link)")
})
@Table(name="ROLE_ATTRIBUTE")
@Entity
@IdClass(RoleAttributeEntity.Key.class)
public class RoleAttributeEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID")
    protected RoleEntity role;

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

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public static class Key implements Serializable {

        protected RoleEntity role;

        protected String name;

        public Key() {
        }

        public Key(RoleEntity role, String name) {
            this.role = role;
            this.name = name;
        }

        public RoleEntity getRole() {
            return role;
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
            if (role != null ? !role.getId().equals(key.role != null ? key.role.getId() : null) : key.role != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = role != null ? role.getId().hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

}
