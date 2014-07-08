package org.keycloak.models.users.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@IdClass(UserCredentialEntity.Key.class)
public class UserCredentialEntity {

    @Id
    protected String type;

    protected String value;
    protected String device;
    protected byte[] salt;
    protected int hashIterations;

    @Id
    @ManyToOne
    protected UserEntity user;

    public UserCredentialEntity() {
    }

    public UserCredentialEntity(UserEntity user, String type) {
        this.user = user;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public int getHashIterations() {
        return hashIterations;
    }

    public void setHashIterations(int hashIterations) {
        this.hashIterations = hashIterations;
    }

    public static class Key implements Serializable {
        private String type;
        private UserEntity user;

        public Key() {
        }

        public Key(String type, UserEntity user) {
            this.type = type;
            this.user = user;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (type != null ? !type.equals(key.type) : key.type != null) return false;
            if (user != null ? !user.equals(key.user) : key.user != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (user != null ? user.hashCode() : 0);
            return result;
        }
    }

}
