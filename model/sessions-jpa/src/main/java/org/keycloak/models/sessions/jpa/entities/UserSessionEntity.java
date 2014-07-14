package org.keycloak.models.sessions.jpa.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "getUserSessionByUser", query = "select s from UserSessionEntity s where s.realmId = :realmId and s.userId = :userId"),
        @NamedQuery(name = "getUserSessionByClient", query = "select s from UserSessionEntity s join s.clients c where s.realmId = :realmId and c.clientId = :clientId"),
        @NamedQuery(name = "getActiveUserSessionByClient", query = "select count(s) from UserSessionEntity s join s.clients c where s.realmId = :realmId and c.clientId = :clientId"),
        @NamedQuery(name = "removeUserSessionByRealm", query = "delete from UserSessionEntity s where s.realmId = :realmId"),
        @NamedQuery(name = "removeUserSessionByUser", query = "delete from UserSessionEntity s where s.realmId = :realmId and s.userId = :userId"),
        @NamedQuery(name = "removeUserSessionByExpired", query = "delete from UserSessionEntity s where s.realmId = :realmId and (s.started < :maxTime or s.lastSessionRefresh < :idleTime)")
})
public class UserSessionEntity {

    @Id
    @GenericGenerator(name="uuid_generator", strategy="org.keycloak.models.sessions.jpa.utils.JpaIdGenerator")
    @GeneratedValue(generator = "uuid_generator")
    protected String id;

    protected String userId;
    protected String realmId;

    protected String ipAddress;

    protected int started;

    protected int lastSessionRefresh;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="session")
    protected Collection<ClientUserSessionAssociationEntity> clients = new ArrayList<ClientUserSessionAssociationEntity>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getStarted() {
        return started;
    }

    public void setStarted(int started) {
        this.started = started;
    }

    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        this.lastSessionRefresh = lastSessionRefresh;
    }

    public Collection<ClientUserSessionAssociationEntity> getClients() {
        return clients;
    }

}
