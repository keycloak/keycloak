package org.keycloak.models.jpa.entities;

import org.hibernate.annotations.GenericGenerator;
import org.keycloak.models.UserModel;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
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
        @NamedQuery(name = "getUserSessionByUser", query = "select s from UserSessionEntity s where s.user = :user"),
        @NamedQuery(name = "removeRealmUserSessions", query = "delete from UserSessionEntity s where s.realm = :realm"),
        @NamedQuery(name = "removeUserSessionByUser", query = "delete from UserSessionEntity s where s.user = :user"),
        @NamedQuery(name = "removeUserSessionExpired", query = "delete from UserSessionEntity s where s.started < :maxTime or s.lastSessionRefresh < :idleTime")
})
public class UserSessionEntity {

    @Id
    @GenericGenerator(name="uuid_generator", strategy="org.keycloak.models.jpa.utils.JpaIdGenerator")
    @GeneratedValue(generator = "uuid_generator")
    private String id;

    @ManyToOne
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    private RealmEntity realm;

    String ipAddress;

    int started;

    int lastSessionRefresh;

    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy="session")
    Collection<ClientUserSessionAssociationEntity> clients = new ArrayList<ClientUserSessionAssociationEntity>();


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
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

    public void setClients(Collection<ClientUserSessionAssociationEntity> clients) {
        this.clients = clients;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }
}
