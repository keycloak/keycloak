package org.keycloak.models.sessions.jpa.entities;

import org.keycloak.models.ClientSessionModel;

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
        @NamedQuery(name = "removeClientSessionAuthStatusByUser", query="delete from ClientSessionAuthStatusEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.session IN (select s from UserSessionEntity s where s.realmId = :realmId and s.userId = :userId))"),
        @NamedQuery(name = "removeClientSessionAuthStatusByClient", query="delete from ClientSessionAuthStatusEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.clientId = :clientId and c.realmId = :realmId)"),
        @NamedQuery(name = "removeClientSessionAuthStatusByRealm", query="delete from ClientSessionAuthStatusEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.realmId = :realmId)"),
        @NamedQuery(name = "removeClientSessionAuthStatusByExpired", query = "delete from ClientSessionAuthStatusEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.session IN (select s from UserSessionEntity s where s.realmId = :realmId and (s.started < :maxTime or s.lastSessionRefresh < :idleTime)))"),
        @NamedQuery(name = "removeDetachedClientSessionAuthStatusByExpired", query = "delete from ClientSessionAuthStatusEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.session IS NULL and c.realmId = :realmId and c.timestamp < :maxTime )")
})
@Table(name="CLIENT_SESSION_AUTH_STATUS")
@Entity
@IdClass(ClientSessionAuthStatusEntity.Key.class)
public class ClientSessionAuthStatusEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "CLIENT_SESSION")
    protected ClientSessionEntity clientSession;

    @Id
    @Column(name = "AUTHENTICATOR")
    protected String authenticator;
    @Column(name = "STATUS")
    protected ClientSessionModel.ExecutionStatus status;

    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
    }

    public ClientSessionModel.ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ClientSessionModel.ExecutionStatus status) {
        this.status = status;
    }

    public ClientSessionEntity getClientSession() {
        return clientSession;
    }

    public void setClientSession(ClientSessionEntity clientSession) {
        this.clientSession = clientSession;
    }

    public static class Key implements Serializable {

        protected ClientSessionEntity clientSession;

        protected String authenticator;

        public Key() {
        }

        public Key(ClientSessionEntity clientSession, String authenticator) {
            this.clientSession = clientSession;
            this.authenticator = authenticator;
        }

        public ClientSessionEntity getClientSession() {
            return clientSession;
        }

        public String getAuthenticator() {
            return authenticator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (authenticator != null ? !authenticator.equals(key.authenticator) : key.authenticator != null) return false;
            if (clientSession != null ? !clientSession.getId().equals(key.clientSession != null ? key.clientSession.getId() : null) : key.clientSession != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = clientSession != null ? clientSession.getId().hashCode() : 0;
            result = 31 * result + (authenticator != null ? authenticator.hashCode() : 0);
            return result;
        }
    }

}
