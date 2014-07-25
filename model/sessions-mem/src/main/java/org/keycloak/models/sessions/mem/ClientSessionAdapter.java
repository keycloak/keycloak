package org.keycloak.models.sessions.mem;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.mem.entities.ClientSessionEntity;

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
    public ClientModel getClient() {
        return realm.findClientById(entity.getClientId());
    }

    @Override
    public String getState() {
        return entity.getState();
    }

    @Override
    public UserSessionModel getUserSession() {
        return new UserSessionAdapter(session, provider, realm, entity.getSession());
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

}
