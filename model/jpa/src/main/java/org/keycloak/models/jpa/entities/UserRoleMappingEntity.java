package org.keycloak.models.jpa.entities;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@MappedSuperclass
public abstract class UserRoleMappingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected long id;
    @ManyToOne
    protected UserEntity user;
    @ManyToOne
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
