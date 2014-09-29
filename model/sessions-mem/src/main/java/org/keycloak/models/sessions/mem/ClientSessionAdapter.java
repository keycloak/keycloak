package org.keycloak.models.sessions.mem;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.mem.entities.ClientSessionEntity;
import org.keycloak.models.sessions.mem.entities.UserSessionEntity;

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



    @Override
    public ClientModel getClient() {
        return realm.findClientById(entity.getClientId());
    }

    @Override
    public UserSessionModel getUserSession() {
        if (entity.getSession() == null) return null;
        return new UserSessionAdapter(session, provider, realm, entity.getSession());
    }

    @Override
    public void setUserSession(UserSessionModel userSession) {
        UserSessionAdapter adapter = (UserSessionAdapter)userSession;
        UserSessionEntity userSessionEntity = adapter.getEntity();
        entity.setSession(userSessionEntity);
        userSessionEntity.getClientSessions().add(entity);
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
    public ClientSessionModel.Action getAction() {
        return entity.getAction();
    }

    @Override
    public void setAction(ClientSessionModel.Action action) {
        entity.setAction(action);
    }

    @Override
    public Set<String> getRoles() {
        return entity.getRoles();
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
    public String getAuthMethod() {
        return entity.getAuthMethod();
    }

    @Override
    public void setAuthMethod(String method) {
        entity.setAuthMethod(method);
    }
}
