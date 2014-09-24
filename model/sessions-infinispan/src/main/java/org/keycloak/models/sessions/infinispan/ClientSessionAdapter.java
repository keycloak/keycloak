package org.keycloak.models.sessions.infinispan;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;

import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientSessionAdapter implements ClientSessionModel {

    private KeycloakSession session;
    private InfinispanUserSessionProvider provider;
    private RealmModel realm;
    private ClientSessionEntity entity;

    public ClientSessionAdapter(KeycloakSession session, InfinispanUserSessionProvider provider, RealmModel realm, ClientSessionEntity entity) {
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
        return realm.findClientById(entity.getClient());
    }

    @Override
    public String getState() {
        return entity.getState();
    }

    @Override
    public UserSessionModel getUserSession() {
        return provider.getUserSession(realm, entity.getUserSession());
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
        provider.update(entity);
    }

    @Override
    public Action getAction() {
        return entity.getAction();
    }

    @Override
    public void setAction(Action action) {
        entity.setAction(action);
        provider.update(entity);
    }

    @Override
    public Set<String> getRoles() {
        return entity.getRoles();
    }

}
