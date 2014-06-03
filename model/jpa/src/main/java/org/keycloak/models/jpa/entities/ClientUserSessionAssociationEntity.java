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
        @NamedQuery(name = "removeClientUserSessionByUser", query = "delete from ClientUserSessionAssociationEntity s where s.userId = :userId"),
        @NamedQuery(name = "removeClientUserSessionByRealm", query = "delete from ClientUserSessionAssociationEntity s where s.realmId = :realmId")})
public class ClientUserSessionAssociationEntity {
    @Id
    @GenericGenerator(name="uuid_generator", strategy="org.keycloak.models.jpa.utils.JpaIdGenerator")
    @GeneratedValue(generator = "uuid_generator")
    private String id;

    // we use ids to avoid select for update contention
    private String userId;
    private String realmId;

    @ManyToOne(fetch= FetchType.LAZY)
    private UserSessionEntity session;



    private String clientId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
}
