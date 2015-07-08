package org.keycloak.models.sessions.mem;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.mem.entities.ClientSessionEntity;
import org.keycloak.models.sessions.mem.entities.UserSessionEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientSessionAdapter implements ClientSessionModel {

    private KeycloakSession session;
    private MemUserSessionProvider provider;
    private RealmModel realm;
    private ClientSessionEntity entity;

    public ClientSessionAdapter(KeycloakSession session, MemUserSessionProvider provider, RealmModel realm, ClientSessionEntity entity) {
        this.session = session;
        this.provider = provider;
        this.realm = realm;
        this.entity = entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return session.realms().getRealm(entity.getRealmId());
    }

    public ClientSessionEntity getEntity() {
        return entity;
    }

    @Override
    public ClientModel getClient() {
        return realm.getClientById(entity.getClientId());
    }

    @Override
    public UserSessionModel getUserSession() {
        if (entity.getSession() == null) return null;
        return new UserSessionAdapter(session, provider, realm, entity.getSession());
    }

    @Override
    public void setUserSession(UserSessionModel userSession) {
        if (userSession == null) {
            if (entity.getSession() != null) {
                entity.getSession().getClientSessions().remove(entity);
            }
            entity.setSession(null);
        } else {
            UserSessionAdapter adapter = (UserSessionAdapter) userSession;
            UserSessionEntity userSessionEntity = adapter.getEntity();
            entity.setSession(userSessionEntity);
            userSessionEntity.getClientSessions().add(entity);
        }
    }

    @Override
    public void setRedirectUri(String uri) {
        entity.setRedirectUri(uri);
    }

    @Override
    public void setRoles(Set<String> roles) {
        entity.setRoles(roles);
    }

    @Override
    public String getRedirectUri() {
        return entity.getRedirectUri();
    }

    @Override
    public int getTimestamp() {
        return entity.getTimestamp();
    }

    @Override
    public void setTimestamp(int timestamp) {
        entity.setTimestamp(timestamp);
    }

    @Override
    public String getAction() {
        return entity.getAction();
    }

    @Override
    public void setAction(String action) {
        entity.setAction(action);
    }

    @Override
    public Set<String> getRoles() {
        return entity.getRoles();
    }

    @Override
    public Set<String> getProtocolMappers() {
        return entity.getProtocolMappers();
    }

    @Override
    public void setProtocolMappers(Set<String> protocolMappers) {
        entity.setProtocolMappers(protocolMappers);
    }

    @Override
    public String getNote(String name) {
        return entity.getNotes().get(name);
    }

    @Override
    public void setNote(String name, String value) {
        entity.getNotes().put(name, value);

    }

    @Override
    public void removeNote(String name) {
        entity.getNotes().remove(name);

    }

    @Override
    public Map<String, String> getNotes() {
        if (entity.getNotes() == null || entity.getNotes().isEmpty()) return Collections.emptyMap();
        Map<String, String> copy = new HashMap<>();
        copy.putAll(entity.getNotes());
        return copy;
   }

    @Override
    public void setUserSessionNote(String name, String value) {
        entity.getUserSessionNotes().put(name, value);
    }

    @Override
    public Map<String, String> getUserSessionNotes() {
        return entity.getUserSessionNotes();
    }

    @Override
    public String getAuthMethod() {
        return entity.getAuthMethod();
    }

    @Override
    public void setAuthMethod(String method) {
        entity.setAuthMethod(method);
    }

    @Override
    public Map<String, ExecutionStatus> getExecutionStatus() {
        return entity.getAuthenticatorStatus();
    }

    @Override
    public void setExecutionStatus(String authenticator, ExecutionStatus status) {
        entity.getAuthenticatorStatus().put(authenticator, status);

    }

    @Override
    public void clearExecutionStatus() {
        entity.getAuthenticatorStatus().clear();
    }

    @Override
    public void clearUserSessionNotes() {
        entity.getUserSessionNotes().clear();
    }

    @Override
    public UserModel getAuthenticatedUser() {
        return entity.getAuthUserId() == null ? null : session.users().getUserById(entity.getAuthUserId(), realm);    }

    @Override
    public void setAuthenticatedUser(UserModel user) {
        if (user == null) entity.setAuthUserId(null);
        else entity.setAuthUserId(user.getId());

    }
}
