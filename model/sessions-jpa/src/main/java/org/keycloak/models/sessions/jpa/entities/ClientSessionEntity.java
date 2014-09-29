package org.keycloak.models.sessions.jpa.entities;

import org.keycloak.models.ClientSessionModel;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@Table(name = "CLIENT_SESSION")
@NamedQueries({
        @NamedQuery(name = "removeClientSessionByRealm", query = "delete from ClientSessionEntity a where a.realmId = :realmId"),
        @NamedQuery(name = "removeClientSessionByUser", query = "delete from ClientSessionEntity a where a.session IN (select s from UserSessionEntity s where s.realmId = :realmId and s.userId = :userId)"),
        @NamedQuery(name = "removeClientSessionByClient", query = "delete from ClientSessionEntity a where a.clientId = :clientId and a.realmId = :realmId"),
        @NamedQuery(name = "removeClientSessionByExpired", query = "delete from ClientSessionEntity a where a.session IN (select s from UserSessionEntity s where s.realmId = :realmId and (s.started < :maxTime or s.lastSessionRefresh < :idleTime))"),
        @NamedQuery(name = "removeDetachedClientSessionByExpired", query = "delete from ClientSessionEntity a where a.session IS NULL and a.timestamp < :maxTime and a.realmId = :realmId")
})
public class ClientSessionEntity {

    @Id
    @Column(name = "ID", length = 36)
    protected String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SESSION_ID")
    protected UserSessionEntity session;

    @Column(name="CLIENT_ID",length = 36)
    protected String clientId;

    @Column(name="REALM_ID")
    protected String realmId;

    @Column(name="TIMESTAMP")
    protected int timestamp;

    @Column(name="REDIRECT_URI")
    protected String redirectUri;

    @Column(name="AUTH_METHOD")
    protected String authMethod;

    @Column(name="ACTION")
    protected ClientSessionModel.Action action;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="clientSession")
    protected Collection<ClientSessionRoleEntity> roles = new ArrayList<ClientSessionRoleEntity>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="clientSession")
    protected Collection<ClientSessionNoteEntity> notes = new ArrayList<ClientSessionNoteEntity>();

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

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public ClientSessionModel.Action getAction() {
        return action;
    }

    public void setAction(ClientSessionModel.Action action) {
        this.action = action;
    }

    public Collection<ClientSessionRoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Collection<ClientSessionRoleEntity> roles) {
        this.roles = roles;
    }

    public Collection<ClientSessionNoteEntity> getNotes() {
        return notes;
    }

    public void setNotes(Collection<ClientSessionNoteEntity> notes) {
        this.notes = notes;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }
}
