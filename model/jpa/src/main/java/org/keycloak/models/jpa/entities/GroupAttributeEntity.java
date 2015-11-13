package org.keycloak.models.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getGroupAttributesByNameAndValue", query="select attr from GroupAttributeEntity attr where attr.name = :name and attr.value = :value"),
        @NamedQuery(name="deleteGroupAttributesByGroup", query="delete from  GroupAttributeEntity attr where attr.group = :group"),
        @NamedQuery(name="deleteGroupAttributesByRealm", query="delete from  GroupAttributeEntity attr where attr.group IN (select u from GroupEntity u where u.realm=:realm)")
})
@Table(name="GROUP_ATTRIBUTE")
@Entity
public class GroupAttributeEntity {

    @Id
    @Column(name="ID", length = 36)
    protected String id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "GROUP_ID")
    protected GroupEntity group;

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

    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(GroupEntity group) {
        this.group = group;
    }
}
