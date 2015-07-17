package org.keycloak.models.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
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

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@Table(name="KEYCLOAK_ROLE", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "NAME", "CLIENT_REALM_CONSTRAINT" })
})
@NamedQueries({
        @NamedQuery(name="getClientRoleByName", query="select role from RoleEntity role where role.name = :name and role.client = :client"),
        @NamedQuery(name="getRealmRoleByName", query="select role from RoleEntity role where role.clientRole = false and role.name = :name and role.realm = :realm")
})

public class RoleEntity {
    @Id
    @Column(name="id", length = 36)
    private String id;

    @Column(name = "NAME")
    private String name;
    @Column(name = "DESCRIPTION")
    private String description;

    // hax! couldn't get constraint to work properly
    @Column(name = "REALM_ID")
    private String realmId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM")
    private RealmEntity realm;

    @Column(name="CLIENT_ROLE")
    private boolean clientRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT")
    private ClientEntity client;

    // Hack to ensure that either name+client or name+realm are unique. Needed due to MS-SQL as it don't allow multiple NULL values in the column, which is part of constraint
    @Column(name="CLIENT_REALM_CONSTRAINT", length = 36)
    private String clientRealmConstraint;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "COMPOSITE_ROLE", joinColumns = @JoinColumn(name = "COMPOSITE"), inverseJoinColumns = @JoinColumn(name = "CHILD_ROLE"))
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

    public boolean isClientRole() {
        return clientRole;
    }

    public void setClientRole(boolean clientRole) {
        this.clientRole = clientRole;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
        this.clientRealmConstraint = realm.getId();
    }

    public ClientEntity getClient() {
        return client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
        if (client != null) {
            this.clientRealmConstraint = client.getId();
        }
    }

    public String getClientRealmConstraint() {
        return clientRealmConstraint;
    }

    public void setClientRealmConstraint(String clientRealmConstraint) {
        this.clientRealmConstraint = clientRealmConstraint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleEntity)) return false;

        RoleEntity that = (RoleEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
