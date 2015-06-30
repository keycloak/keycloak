package org.keycloak.models.jpa.entities;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
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
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="deleteUserAttributesByRealm", query="delete from  UserAttributeEntity attr where attr.user IN (select u from UserEntity u where u.realmId=:realmId)"),
        @NamedQuery(name="deleteUserAttributesByRealmAndLink", query="delete from  UserAttributeEntity attr where attr.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link)")
})
@Table(name="USER_ATTRIBUTE")
@Entity
public class UserAttributeEntity {

    @Id
    @Column(name="ID", length = 36)
    protected String id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    protected UserEntity user;

    @Column(name = "NAME")
    protected String name;
    @Column(name = "VALUE")
    protected String value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

}
