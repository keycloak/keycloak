package org.keycloak.models.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteOfflineClientSessionsByRealm", query="delete from OfflineClientSessionEntity sess where sess.user IN (select u from UserEntity u where u.realmId=:realmId)"),
        @NamedQuery(name="deleteOfflineClientSessionsByRealmAndLink", query="delete from OfflineClientSessionEntity sess where sess.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link)"),
        @NamedQuery(name="deleteOfflineClientSessionsByClient", query="delete from OfflineClientSessionEntity sess where sess.clientId=:clientId")
})
@Table(name="OFFLINE_CLIENT_SESSION")
@Entity
public class OfflineClientSessionEntity {

    @Id
    @Column(name="CLIENT_SESSION_ID", length = 36)
    protected String clientSessionId;

    @Column(name="USER_SESSION_ID", length = 36)
    protected String userSessionId;

    @Column(name="CLIENT_ID", length = 36)
    protected String clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    protected UserEntity user;

    @Column(name="DATA")
    protected String data;

    public String getClientSessionId() {
        return clientSessionId;
    }

    public void setClientSessionId(String clientSessionId) {
        this.clientSessionId = clientSessionId;
    }

    public String getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(String userSessionId) {
        this.userSessionId = userSessionId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
