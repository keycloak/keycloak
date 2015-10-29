package org.keycloak.models.jpa.entities;

import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getAllGroupsByRealm", query="select u from GroupEntity u where u.realmId = :realmId order by u.name"),
        @NamedQuery(name="getGroupById", query="select u from GroupEntity u where u.id = :id and u.realmId = :realmId"),
        @NamedQuery(name="getGroupIdsByParent", query="select u.id from GroupEntity u where u.parent = :parent"),
        @NamedQuery(name="getGroupByName", query="select u from GroupEntity u where u.name = :name and u.realmId = :realmId"),
        @NamedQuery(name="getGroupCount", query="select count(u) from GroupEntity u where u.realmId = :realmId"),
        @NamedQuery(name="deleteGroupsByRealm", query="delete from GroupEntity u where u.realmId = :realmId")
})
@Entity
@Table(name="GROUP_ENTITY")
public class GroupEntity {
    @Id
    @Column(name="ID", length = 36)
    protected String id;

    @Column(name = "NAME")
    protected String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_GROUP")
    private GroupEntity parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM")
    private RealmEntity realm;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="group")
    protected Collection<GroupAttributeEntity> attributes = new ArrayList<GroupAttributeEntity>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Collection<GroupAttributeEntity> getAttributes() {
        return attributes;
    }

    public void setAttributes(Collection<GroupAttributeEntity> attributes) {
        this.attributes = attributes;
    }

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

    public GroupEntity getParent() {
        return parent;
    }

    public void setParent(GroupEntity parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroupEntity that = (GroupEntity) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
