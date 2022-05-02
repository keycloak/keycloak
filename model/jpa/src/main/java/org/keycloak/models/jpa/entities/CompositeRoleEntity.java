package org.keycloak.models.jpa.entities;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.jpa.QueryHints;

@Entity
@IdClass(CompositeRoleEntityKey.class)
@Table(name="COMPOSITE_ROLE", uniqueConstraints = {
      @UniqueConstraint(columnNames = { "COMPOSITE", "CHILD_ROLE" })
})
@NamedQueries({
    @NamedQuery(name="getChildrenRoleIds", hints = @QueryHint(name = QueryHints.HINT_READONLY, value = "true"),
            query="select compositerole.childRoleId from CompositeRoleEntity compositerole where compositerole.compositeId = :roleId"),
    @NamedQuery(name="getCompositeRolesByCompositeIds", hints = @QueryHint(name = QueryHints.HINT_READONLY, value = "true"),
            query="select compositerole from CompositeRoleEntity compositerole where compositerole.compositeId in :roleIds"),
    @NamedQuery(name="getChildRoleIdsForCompositeIds", hints = @QueryHint(name = QueryHints.HINT_READONLY, value = "true"),
            query="select compositerole.childRoleId from CompositeRoleEntity compositerole where compositerole.compositeId in :roleIds"),
    @NamedQuery(name="removeCompositeAndChildRoleEntry",
            query="delete from CompositeRoleEntity compositerole where compositerole.compositeId = :compositeId and compositerole.childRoleId = :childId"),
})
public class CompositeRoleEntity implements Serializable {

    private static final long serialVersionUID = 7375648337789837909L;
    
    @Id
    @Column(name="COMPOSITE", length = 36, nullable = false)
    private String compositeId;

    @Id
    @Column(name="CHILD_ROLE", length = 36, nullable = false)
    private String childRoleId;
    
    public CompositeRoleEntity() {
        super();
    }

    public CompositeRoleEntity(CompositeRoleEntityKey key) {
        super();
        setCompositeId(key.getCompositeId());
        setChildRoleId(key.getChildRoleId());
    }

    public String getCompositeId() {
        return compositeId;
    }

    public void setCompositeId(String compositeId) {
        this.compositeId = compositeId;
    }

    public String getChildRoleId() {
        return childRoleId;
    }

    public void setChildRoleId(String childRoleId) {
        this.childRoleId = childRoleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof CompositeRoleEntity)) return false;

        CompositeRoleEntity that = (CompositeRoleEntity) o;

        if (!compositeId.equals(that.compositeId)) return false;
        if (!childRoleId.equals(that.childRoleId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(compositeId, childRoleId);
    }
    
}
