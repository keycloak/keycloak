package org.keycloak.models.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="credentialByUserAndType", query="select cred from CredentialEntity cred where cred.user = :user and cred.type = :type"),
        @NamedQuery(name="deleteCredentialsByRealm", query="delete from CredentialEntity cred where cred.user IN (select u from UserEntity u where u.realmId=:realmId)")

})
@Entity
@IdClass(CredentialEntity.Key.class)
public class CredentialEntity {

    @Id
    protected String type;
    protected String value;
    protected String device;
    protected byte[] salt;
    protected int hashIterations;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="userId")
    protected UserEntity user;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
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

        protected UserEntity user;

        protected String type;

        public Key() {
        }

        public Key(UserEntity user, String type) {
            this.user = user;
            this.type = type;
        }

        public UserEntity getUser() {
            return user;
        }

        public String getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (type != null ? !type.equals(key.type) : key.type != null) return false;
            if (user != null ? !user.getId().equals(key.user != null ? key.user.getId() : null) : key.user != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = user != null ? user.getId().hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }
    }

}
