package org.keycloak.models.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteAuthenticationLinksByRealm", query="delete from AuthenticationLinkEntity authLink where authLink.user IN (select u from UserEntity u where u.realmId=:realmId)")
})
@Table(name="AUTHENTICATION_LINK")
@Entity
@IdClass(AuthenticationLinkEntity.Key.class)
public class AuthenticationLinkEntity {

    @Id
    @Column(name="AUTH_PROVIDER")
    protected String authProvider;
    @Column(name="AUTH_USER_ID")
    protected String authUserId;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    protected UserEntity user;

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public static class Key implements Serializable {

        protected UserEntity user;

        protected String authProvider;

        public Key() {
        }

        public Key(UserEntity user, String authProvider) {
            this.user = user;
            this.authProvider = authProvider;
        }

        public UserEntity getUser() {
            return user;
        }

        public String getAuthProvider() {
            return authProvider;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (authProvider != null ? !authProvider.equals(key.authProvider) : key.authProvider != null) return false;
            if (user != null ? !user.getId().equals(key.user != null ? key.user.getId() : null) : key.user != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = user != null ? user.getId().hashCode() : 0;
            result = 31 * result + (authProvider != null ? authProvider.hashCode() : 0);
            return result;
        }
    }

}
