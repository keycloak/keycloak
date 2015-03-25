package org.keycloak.models.sessions.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name = "selectNoteByNameValue", query="select r from UserSessionNoteEntity r where r.name = :name and r.value = :value"),
        @NamedQuery(name = "removeUserSessionNoteByUser", query="delete from UserSessionNoteEntity r where r.userSession IN (select s from UserSessionEntity s where s.realmId = :realmId and s.userId = :userId)"),
        @NamedQuery(name = "removeUserSessionNoteByRealm", query="delete from UserSessionNoteEntity r where r.userSession IN (select c from UserSessionEntity c where c.realmId = :realmId)"),
        @NamedQuery(name = "removeUserSessionNoteByExpired", query = "delete from UserSessionNoteEntity r where r.userSession IN (select s from UserSessionEntity s where s.realmId = :realmId and (s.started < :maxTime or s.lastSessionRefresh < :idleTime))")
})
@Table(name="USER_SESSION_NOTE")
@Entity
@IdClass(UserSessionNoteEntity.Key.class)
public class UserSessionNoteEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "USER_SESSION")
    protected UserSessionEntity userSession;

    @Id
    @Column(name = "NAME")
    protected String name;
    @Column(name = "VALUE")
    protected String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public UserSessionEntity getUserSession() {
        return userSession;
    }

    public void setUserSession(UserSessionEntity userSession) {
        this.userSession = userSession;
    }

    public static class Key implements Serializable {

        protected UserSessionEntity userSession;

        protected String name;

        public Key() {
        }

        public Key(UserSessionEntity clientSession, String name) {
            this.userSession = clientSession;
            this.name = name;
        }

        public UserSessionEntity getUserSession() {
            return userSession;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (name != null ? !name.equals(key.name) : key.name != null) return false;
            if (userSession != null ? !userSession.getId().equals(key.userSession != null ? key.userSession.getId() : null) : key.userSession != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = userSession != null ? userSession.getId().hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

}
