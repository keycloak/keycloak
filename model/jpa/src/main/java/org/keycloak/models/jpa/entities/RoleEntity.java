package org.keycloak.models.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class RoleEntity {
    @Id
    @GenericGenerator(name="keycloak_generator", strategy="org.keycloak.models.jpa.utils.JpaIdGenerator")
    @GeneratedValue(generator = "keycloak_generator")
    private String id;

    private String name;
    private String description;
    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "CompositeRole", joinColumns = @JoinColumn(name = "composite"), inverseJoinColumns = @JoinColumn(name = "role"))
    private Collection<RoleEntity> compositeRoles = new ArrayList<RoleEntity>();


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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Collection<RoleEntity> getCompositeRoles() {
        return compositeRoles;
    }

    public void setCompositeRoles(Collection<RoleEntity> compositeRoles) {
        this.compositeRoles = compositeRoles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoleEntity that = (RoleEntity) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
