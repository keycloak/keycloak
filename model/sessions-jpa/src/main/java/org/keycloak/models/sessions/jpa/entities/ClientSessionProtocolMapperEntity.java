package org.keycloak.models.sessions.jpa.entities;

import java.io.Serializable;

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

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name = "removeClientSessionProtMapperByUser", query="delete from ClientSessionProtocolMapperEntity pm where pm.clientSession IN (select c from ClientSessionEntity c where c.session IN (select s from UserSessionEntity s where s.realmId = :realmId and s.userId = :userId))"),
        @NamedQuery(name = "removeClientSessionProtMapperByClient", query="delete from ClientSessionProtocolMapperEntity pm where pm.clientSession IN (select c from ClientSessionEntity c where c.clientId = :clientId and c.realmId = :realmId)"),
        @NamedQuery(name = "removeClientSessionProtMapperByRealm", query="delete from ClientSessionProtocolMapperEntity pm where pm.clientSession IN (select c from ClientSessionEntity c where c.realmId = :realmId)"),
        @NamedQuery(name = "removeClientSessionProtMapperByExpired", query = "delete from ClientSessionProtocolMapperEntity pm where pm.clientSession IN (select c from ClientSessionEntity c where c.session IN (select s from UserSessionEntity s where s.realmId = :realmId and (s.started < :maxTime or s.lastSessionRefresh < :idleTime)))"),
        @NamedQuery(name = "removeDetachedClientSessionProtMapperByExpired", query = "delete from ClientSessionProtocolMapperEntity pm where pm.clientSession IN (select c from ClientSessionEntity c where c.session IS NULL and c.realmId = :realmId and c.timestamp < :maxTime )")
})
@Table(name="CLIENT_SESSION_PROT_MAPPER")
@Entity
@IdClass(ClientSessionProtocolMapperEntity.Key.class)
public class ClientSessionProtocolMapperEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="CLIENT_SESSION")
    protected ClientSessionEntity clientSession;

    @Id
    @Column(name="PROTOCOL_MAPPER_ID")
    protected String protocolMapperId;

    public ClientSessionEntity getClientSession() {
        return clientSession;
    }

    public void setClientSession(ClientSessionEntity clientSession) {
        this.clientSession = clientSession;
    }

    public String getProtocolMapperId() {
        return protocolMapperId;
    }

    public void setProtocolMapperId(String protocolMapperId) {
        this.protocolMapperId = protocolMapperId;
    }

    public static class Key implements Serializable {

        protected ClientSessionEntity clientSession;

        protected String protocolMapperId;

        public Key() {
        }

        public Key(ClientSessionEntity clientSession, String protocolMapperId) {
            this.clientSession = clientSession;
            this.protocolMapperId = protocolMapperId;
        }

        public ClientSessionEntity getClientSession() {
            return clientSession;
        }

        public String getProtocolMapperId() {
            return protocolMapperId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!protocolMapperId.equals(key.protocolMapperId)) return false;
            if (!clientSession.getId().equals(key.clientSession.getId())) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = clientSession.getId().hashCode();
            result = 31 * result + protocolMapperId.hashCode();
            return result;
        }
    }
}
