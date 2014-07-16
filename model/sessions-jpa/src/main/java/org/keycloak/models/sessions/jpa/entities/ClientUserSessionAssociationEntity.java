package org.keycloak.models.sessions.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@Table(name = "ClientUserSessionAscEntity")
@NamedQueries({
        @NamedQuery(name = "removeClientUserSessionByRealm", query = "delete from ClientUserSessionAssociationEntity a where a.session IN (select s from UserSessionEntity s where s.realmId = :realmId)"),
        @NamedQuery(name = "removeClientUserSessionByUser", query = "delete from ClientUserSessionAssociationEntity a where a.session IN (select s from UserSessionEntity s where s.realmId = :realmId and s.userId = :userId)"),
        @NamedQuery(name = "removeClientUserSessionByClient", query = "delete from ClientUserSessionAssociationEntity a where a.clientId = :clientId and a.session IN (select s from UserSessionEntity s where s.realmId = :realmId)"),
        @NamedQuery(name = "removeClientUserSessionByExpired", query = "delete from ClientUserSessionAssociationEntity a where a.session IN (select s from UserSessionEntity s where s.realmId = :realmId and (s.started < :maxTime or s.lastSessionRefresh < :idleTime))")
})
@IdClass(ClientUserSessionAssociationEntity.Key.class)
public class ClientUserSessionAssociationEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    protected UserSessionEntity session;

    @Id
    @Column(length = 36)
    protected String clientId;

    public UserSessionEntity getSession() {
        return session;
    }

    public void setSession(UserSessionEntity session) {
        this.session = session;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public static class Key implements Serializable {

        private String clientId;
        private UserSessionEntity session;

        public Key() {
        }

        public Key(String clientId, UserSessionEntity session) {
            this.clientId = clientId;
            this.session = session;
        }

        public String getClientId() {
            return clientId;
        }

        public UserSessionEntity getSession() {
            return session;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (clientId != null ? !clientId.equals(key.clientId) : key.clientId != null) return false;
            if (session != null ? !session.equals(key.session) : key.session != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = clientId != null ? clientId.hashCode() : 0;
            result = 31 * result + (session != null ? session.hashCode() : 0);
            return result;
        }
    }

}
