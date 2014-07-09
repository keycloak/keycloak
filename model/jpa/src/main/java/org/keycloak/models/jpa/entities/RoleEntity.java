package org.keycloak.models.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "appRealmConstraint" })
})
@NamedQueries({
        @NamedQuery(name="getAppRoleByName", query="select role from RoleEntity role where role.name = :name and role.application = :application"),
        @NamedQuery(name="getRealmRoleByName", query="select role from RoleEntity role where role.applicationRole = false and role.name = :name and role.realm = :realm")
})

public class RoleEntity {
    @Id
    @Column(name="id")
    private String id;

    private String name;
    private String description;

    // hax! couldn't get constraint to work properly
    private String realmId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realm")
    private RealmEntity realm;

    @Column(name="applicationRole")
    private boolean applicationRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application")
    private ApplicationEntity application;

    // Hack to ensure that either name+application or name+realm are unique. Needed due to MS-SQL as it don't allow multiple NULL values in the column, which is part of constraint
    private String appRealmConstraint;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "CompositeRole", joinColumns = @JoinColumn(name = "composite"), inverseJoinColumns = @JoinColumn(name = "childRole"))
    private Collection<RoleEntity> compositeRoles = new ArrayList<RoleEntity>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
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

    public boolean isApplicationRole() {
        return applicationRole;
    }

    public void setApplicationRole(boolean applicationRole) {
        this.applicationRole = applicationRole;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
        this.appRealmConstraint = realm.getId();
    }

    public ApplicationEntity getApplication() {
        return application;
    }

    public void setApplication(ApplicationEntity application) {
        this.application = application;
        if (application != null) {
            this.appRealmConstraint = application.getId();
        }
    }

    public String getAppRealmConstraint() {
        return appRealmConstraint;
    }

    public void setAppRealmConstraint(String appRealmConstraint) {
        this.appRealmConstraint = appRealmConstraint;
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
