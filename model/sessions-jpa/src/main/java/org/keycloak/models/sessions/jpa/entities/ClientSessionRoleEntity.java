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
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@NamedQueries({
        @NamedQuery(name = "removeClientSessionRoleByUser", query="delete from ClientSessionRoleEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.session IN (select s from UserSessionEntity s where s.realmId = :realmId and s.userId = :userId))"),
        @NamedQuery(name = "removeClientSessionRoleByClient", query="delete from ClientSessionRoleEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.clientId = :clientId and c.realmId = :realmId)"),
        @NamedQuery(name = "removeClientSessionRoleByRealm", query="delete from ClientSessionRoleEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.realmId = :realmId)"),
        @NamedQuery(name = "removeClientSessionRoleByExpired", query = "delete from ClientSessionRoleEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.session IN (select s from UserSessionEntity s where s.realmId = :realmId and (s.started < :maxTime or s.lastSessionRefresh < :idleTime)))"),
        @NamedQuery(name = "removeDetachedClientSessionRoleByExpired", query = "delete from ClientSessionRoleEntity r where r.clientSession IN (select c from ClientSessionEntity c where c.session IS NULL and c.realmId = :realmId and c.timestamp < :maxTime )")
})
@Table(name="CLIENT_SESSION_ROLE")
@Entity
@IdClass(ClientSessionRoleEntity.Key.class)
public class ClientSessionRoleEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="CLIENT_SESSION")
    protected ClientSessionEntity clientSession;

    @Id
    @Column(name = "ROLE_ID")
    protected String roleId;

    public ClientSessionEntity getClientSession() {
        return clientSession;
    }

    public void setClientSession(ClientSessionEntity clientSession) {
        this.clientSession = clientSession;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }


    public static class Key implements Serializable {

        protected ClientSessionEntity clientSession;

        protected String roleId;

        public Key() {
        }

        public Key(ClientSessionEntity clientSession, String roleId) {
            this.clientSession = clientSession;
            this.roleId = roleId;
        }

        public ClientSessionEntity getClientSession() {
            return clientSession;
        }

        public String getRoleId() {
            return roleId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!roleId.equals(key.roleId)) return false;
            if (!clientSession.getId().equals(key.clientSession.getId())) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = clientSession.getId().hashCode();
            result = 31 * result + roleId.hashCode();
            return result;
        }
    }
}
