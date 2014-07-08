package org.keycloak.models.users.jpa.entities;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getRealmUserByUsername", query="select u from UserEntity u where u.username = :username and u.realm = :realm"),
        @NamedQuery(name="getRealmUserByEmail", query="select u from UserEntity u where u.email = :email and u.realm = :realm"),
        @NamedQuery(name="getRealmUserByAttribute", query="select u from UserEntity u join u.attributes a where u.realm = :realm and a.name = :name and a.value = :value")
})
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "realm", "username" }),
        @UniqueConstraint(columnNames = { "realm", "emailConstraint" })
})
@IdClass(UserEntity.Key.class)
public class UserEntity {
    @Id
    protected String id;

    protected boolean enabled;
    protected String username;
    protected String firstName;
    protected String lastName;
    protected String email;

    // Hack just to workaround the fact that on MS-SQL you can't have unique constraint with multiple NULL values TODO: Find better solution (like unique index with 'where' but that's proprietary)
    protected String emailConstraint;

    @Id
    protected String realm;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy="user")
    protected List<UserAttributeEntity> attributes = new LinkedList<UserAttributeEntity>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy="user")
    protected List<UserCredentialEntity> credentials = new LinkedList<UserCredentialEntity>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy="user")
    protected List<UserRoleMappingEntity> roles = new LinkedList<UserRoleMappingEntity>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.emailConstraint = email != null ? email : id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmailConstraint() {
        return emailConstraint;
    }

    public void setEmailConstraint(String emailConstraint) {
        this.emailConstraint = emailConstraint;
    }

    public List<UserAttributeEntity> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<UserAttributeEntity> attributes) {
        this.attributes = attributes;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Collection<UserCredentialEntity> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<UserCredentialEntity> credentials) {
        this.credentials = credentials;
    }

    public List<UserRoleMappingEntity> getRoles() {
        return roles;
    }

    public void setRoles(List<UserRoleMappingEntity> roles) {
        this.roles = roles;
    }

    public static class Key implements Serializable {
        private String id;
        private String realm;

        public Key() {
        }

        public Key(String id, String realm) {
            this.id = id;
            this.realm = realm;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (id != null ? !id.equals(key.id) : key.id != null) return false;
            if (realm != null ? !realm.equals(key.realm) : key.realm != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (realm != null ? realm.hashCode() : 0);
            return result;
        }
    }

}
