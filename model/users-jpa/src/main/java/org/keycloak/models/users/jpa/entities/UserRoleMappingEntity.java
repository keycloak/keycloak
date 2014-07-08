package org.keycloak.models.users.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Entity
@IdClass(UserRoleMappingEntity.Key.class)
public class UserRoleMappingEntity {

    @Id
    protected String role;

    @Id
    @ManyToOne
    protected UserEntity user;

    public UserRoleMappingEntity() {
    }

    public UserRoleMappingEntity(UserEntity user, String role) {
        this.user = user;
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public static class Key implements Serializable {
        private String role;
        private UserEntity user;

        public Key() {
        }

        public Key(String role, UserEntity user) {
            this.role = role;
            this.user = user;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (role != null ? !role.equals(key.role) : key.role != null) return false;
            if (user != null ? !user.equals(key.user) : key.user != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = role != null ? role.hashCode() : 0;
            result = 31 * result + (user != null ? user.hashCode() : 0);
            return result;
        }
    }

}
