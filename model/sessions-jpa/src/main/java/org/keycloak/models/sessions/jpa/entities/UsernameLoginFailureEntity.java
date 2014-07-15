package org.keycloak.models.sessions.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@NamedQueries({
        @NamedQuery(name="getAllFailures", query="select failure from UsernameLoginFailureEntity failure"),
        @NamedQuery(name = "removeLoginFailuresByRealm", query = "delete from UsernameLoginFailureEntity f where f.realmId = :realmId"),
        @NamedQuery(name = "removeLoginFailuresByUser", query = "delete from UsernameLoginFailureEntity f where f.realmId = :realmId and f.username = :username")
})
@IdClass(UsernameLoginFailureEntity.Key.class)
public class UsernameLoginFailureEntity {

    @Id
    protected String username;

    @Id
    protected String realmId;

    protected int failedLoginNotBefore;
    protected int numFailures;
    protected long lastFailure;
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

    }

}
