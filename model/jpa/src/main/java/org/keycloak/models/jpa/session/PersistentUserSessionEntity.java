package org.keycloak.models.jpa.session;

import java.io.Serializable;
import java.util.Collection;

import javax.persistence.CascadeType;
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

import org.keycloak.models.jpa.entities.UserEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteUserSessionsByRealm", query="delete from PersistentUserSessionEntity sess where sess.realmId=:realmId"),
        @NamedQuery(name="deleteUserSessionsByUser", query="delete from PersistentUserSessionEntity sess where sess.userId=:userId"),
        @NamedQuery(name="deleteDetachedUserSessions", query="delete from PersistentUserSessionEntity sess where sess.userSessionId NOT IN (select c.userSessionId from PersistentClientSessionEntity c)"),
        @NamedQuery(name="findUserSessionsCount", query="select count(sess) from PersistentUserSessionEntity sess where offline=:offline"),
        @NamedQuery(name="findUserSessions", query="select sess from PersistentUserSessionEntity sess where offline=:offline order by sess.userSessionId")

})
@Table(name="OFFLINE_USER_SESSION")
@Entity
@IdClass(PersistentUserSessionEntity.Key.class)
public class PersistentUserSessionEntity {

    @Id
    @Column(name="USER_SESSION_ID", length = 36)
    protected String userSessionId;

    @Column(name = "REALM_ID", length = 36)
    protected String realmId;

    @Column(name="USER_ID", length = 36)
    protected String userId;

    @Column(name = "LAST_SESSION_REFRESH")
    protected int lastSessionRefresh;

    @Id
    @Column(name = "OFFLINE")
    protected boolean offline;

    @Column(name="DATA")
    protected String data;

    public String getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(String userSessionId) {
        this.userSessionId = userSessionId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        this.lastSessionRefresh = lastSessionRefresh;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public static class Key implements Serializable {

        protected String userSessionId;

        protected boolean offline;

        public Key() {
        }

        public Key(String userSessionId, boolean offline) {
            this.userSessionId = userSessionId;
            this.offline = offline;
        }

        public String getUserSessionId() {
            return userSessionId;
        }

        public boolean isOffline() {
            return offline;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (this.userSessionId != null ? !this.userSessionId.equals(key.userSessionId) : key.userSessionId != null) return false;
            if (offline != key.offline) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = this.userSessionId != null ? this.userSessionId.hashCode() : 0;
            result = 31 * result + (offline ? 1 : 0);
            return result;
        }
    }
}
