package org.keycloak.models.sessions.mongo;

import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.mongo.entities.MongoClientSessionEntity;
import org.keycloak.models.sessions.mongo.entities.MongoUserSessionEntity;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientSessionAdapter implements ClientSessionModel {

    private KeycloakSession session;
    private MongoUserSessionProvider provider;
    private RealmModel realm;
    private MongoClientSessionEntity entity;
    private MongoStoreInvocationContext invContext;

    public ClientSessionAdapter(KeycloakSession session, MongoUserSessionProvider provider, RealmModel realm, MongoClientSessionEntity entity, MongoStoreInvocationContext invContext) {
        this.session = session;
        this.provider = provider;
        this.realm = realm;
        this.entity = entity;
        this.invContext = invContext;
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
        if (entity.getSessionId() == null) return null;
        return provider.getUserSession(realm, entity.getSessionId());
    }

    @Override
    public void setUserSession(UserSessionModel userSession) {
        MongoUserSessionEntity userSessionEntity = provider.getUserSessionEntity(realm, userSession.getId());
        entity.setSessionId(userSessionEntity.getId());
        provider.getMongoStore().pushItemToList(userSessionEntity, "clientSessions", entity.getId(), true, invContext);
    }

    @Override
    public void setRedirectUri(String uri) {
        entity.setRedirectUri(uri);

    }

    @Override
    public void setRoles(Set<String> roles) {
        List<String> list = new LinkedList<String>();
        list.addAll(roles);
        entity.setRoles(list);
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
    public Action getAction() {
        return entity.getAction();
    }

    @Override
    public void setAction(Action action) {
        entity.setAction(action);
    }

    @Override
    public Set<String> getRoles() {
        return entity.getRoles() != null ? new HashSet<String>(entity.getRoles()) : null;
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
