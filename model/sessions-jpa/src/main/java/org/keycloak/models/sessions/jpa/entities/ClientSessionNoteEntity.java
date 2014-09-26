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
        @NamedQuery(name = "removeClientSessionNoteByUser", query="delete from ClientSessionNoteEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.session IN (select s from UserSessionEntity s where s.realmId = :realmId and s.userId = :userId))"),
        @NamedQuery(name = "removeClientSessionNoteByClient", query="delete from ClientSessionNoteEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.clientId = :clientId and c.realmId = :realmId)"),
        @NamedQuery(name = "removeClientSessionNoteByRealm", query="delete from ClientSessionNoteEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.realmId = :realmId)"),
        @NamedQuery(name = "removeClientSessionNoteByExpired", query = "delete from ClientSessionNoteEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.session IN (select s from UserSessionEntity s where s.realmId = :realmId and (s.started < :maxTime or s.lastSessionRefresh < :idleTime)))"),
        @NamedQuery(name = "removeDetachedClientSessionNoteByExpired", query = "delete from ClientSessionNoteEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.session IS NULL and c.realmId = :realmId and c.timestamp < :maxTime )")
})
@Table(name="CLIENT_SESSION_NOTE")
@Entity
@IdClass(ClientSessionNoteEntity.Key.class)
public class ClientSessionNoteEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "CLIENT_SESSION")
    protected ClientSessionEntity clientSession;

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

    public ClientSessionEntity getClientSession() {
        return clientSession;
    }

    public void setClientSession(ClientSessionEntity clientSession) {
        this.clientSession = clientSession;
    }

    public static class Key implements Serializable {

        protected ClientSessionEntity clientSession;

        protected String name;

        public Key() {
        }

        public Key(ClientSessionEntity clientSession, String name) {
            this.clientSession = clientSession;
            this.name = name;
        }

        public ClientSessionEntity getClientSession() {
            return clientSession;
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
            if (clientSession != null ? !clientSession.getId().equals(key.clientSession != null ? key.clientSession.getId() : null) : key.clientSession != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = clientSession != null ? clientSession.getId().hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

}
