package org.keycloak.models.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="userHasRole", query="select m from UserRoleMappingEntity m where m.user = :user and m.role = :role"),
        @NamedQuery(name="userRoleMappings", query="select m from UserRoleMappingEntity m where m.user = :user"),
        @NamedQuery(name="userRoleMappingIds", query="select m.role.id from UserRoleMappingEntity m where m.user = :user")
})
@Entity
public class UserRoleMappingEntity  {
    @Id
    @GeneratedValue
    protected long id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="userId")
    protected UserEntity user;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="roleId")
    protected RoleEntity role;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

}
