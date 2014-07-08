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
@IdClass(UserAttributeEntity.Key.class)
public class UserAttributeEntity {

    @Id
    protected String name;

    @Id
    @ManyToOne
    protected UserEntity user;

    protected String value;

    public UserAttributeEntity() {
    }

    public UserAttributeEntity(UserEntity user, String name, String value) {
        this.user = user;
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static class Key implements Serializable {
        private String name;
        private UserEntity user;

        public Key() {
        }

        public Key(String name, UserEntity user) {
            this.name = name;
            this.user = user;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (name != null ? !name.equals(key.name) : key.name != null) return false;
            if (user != null ? !user.equals(key.user) : key.user != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (user != null ? user.hashCode() : 0);
            return result;
        }
    }

}
