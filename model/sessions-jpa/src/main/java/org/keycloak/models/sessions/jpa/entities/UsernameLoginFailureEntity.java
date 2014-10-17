package org.keycloak.models.sessions.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@Table(name="USERNAME_LOGIN_FAILURE")
@NamedQueries({
        @NamedQuery(name="getAllFailures", query="select failure from UsernameLoginFailureEntity failure"),
        @NamedQuery(name = "removeLoginFailuresByRealm", query = "delete from UsernameLoginFailureEntity f where f.realmId = :realmId"),
        @NamedQuery(name = "removeLoginFailuresByUser", query = "delete from UsernameLoginFailureEntity f where f.realmId = :realmId and (f.username = :username or f.username = :email)")
})
@IdClass(UsernameLoginFailureEntity.Key.class)
public class UsernameLoginFailureEntity {

    @Id
    @Column(name="USERNAME",length = 200)
    protected String username;

    @Id
    @Column(name="REALM_ID",length = 36)
    protected String realmId;

    @Column(name="FAILED_LOGIN_NOT_BEFORE")
    protected int failedLoginNotBefore;

    @Column(name="NUM_FAILURES")
    protected int numFailures;

    @Column(name="LAST_FAILURE")
    protected long lastFailure;

    @Column(name="LAST_IP_FAILURE")
    protected String lastIPFailure;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getFailedLoginNotBefore() {
        return failedLoginNotBefore;
    }

    public void setFailedLoginNotBefore(int failedLoginNotBefore) {
        this.failedLoginNotBefore = failedLoginNotBefore;
    }

    public int getNumFailures() {
        return numFailures;
    }

    public void setNumFailures(int numFailures) {
        this.numFailures = numFailures;
    }

    public long getLastFailure() {
        return lastFailure;
    }

    public void setLastFailure(long lastFailure) {
        this.lastFailure = lastFailure;
    }

    public String getLastIPFailure() {
        return lastIPFailure;
    }

    public void setLastIPFailure(String lastIPFailure) {
        this.lastIPFailure = lastIPFailure;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public static class Key implements Serializable {

        private String realmId;

        private String username;

        public Key() {
        }

        public Key(String realmId, String username) {
            this.realmId = realmId;
            this.username = username;
        }

        public String getRealmId() {
            return realmId;
        }

        public String getUsername() {
            return username;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (realmId != null ? !realmId.equals(key.realmId) : key.realmId != null) return false;
            if (username != null ? !username.equals(key.username) : key.username != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = realmId != null ? realmId.hashCode() : 0;
            result = 31 * result + (username != null ? username.hashCode() : 0);
            return result;
        }
    }

}
