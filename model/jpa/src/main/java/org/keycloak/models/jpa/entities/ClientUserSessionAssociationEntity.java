package org.keycloak.models.jpa.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "getAllClientUserSessions", query = "select s from ClientUserSessionAssociationEntity s"),
        @NamedQuery(name = "getClientUserSessionBySession", query = "select s from ClientUserSessionAssociationEntity s where s.session = :session"),
        @NamedQuery(name = "getClientUserSessionByClient", query = "select s from ClientUserSessionAssociationEntity s where s.clientId = :clientId"),
        @NamedQuery(name = "getActiveClientSessions", query = "select COUNT(s) from ClientUserSessionAssociationEntity s where s.clientId = :clientId"),
        @NamedQuery(name = "removeClientUserSessionByClient", query = "delete from ClientUserSessionAssociationEntity s where s.clientId = :clientId"),
        @NamedQuery(name = "removeClientUserSessionByUser", query = "delete from ClientUserSessionAssociationEntity s where s.user = :user"),
        @NamedQuery(name = "removeClientUserSessionByRealm", query = "delete from ClientUserSessionAssociationEntity s where s.realm = :realm")
})
public class ClientUserSessionAssociationEntity {
    @Id
    @GenericGenerator(name="uuid_generator", strategy="org.keycloak.models.jpa.utils.JpaIdGenerator")
    @GeneratedValue(generator = "uuid_generator")
    private String id;

    @ManyToOne(fetch= FetchType.LAZY)
    private UserSessionEntity session;

    @ManyToOne(fetch= FetchType.LAZY)
    private UserEntity user;

    @ManyToOne(fetch= FetchType.LAZY)
    private RealmEntity realm;

    private String clientId;

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

    public UserSessionEntity getSession() {
        return session;
    }

    public void setSession(UserSessionEntity session) {
        this.session = session;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
