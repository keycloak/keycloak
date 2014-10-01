package org.keycloak.models.sessions.jpa;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.jpa.entities.ClientSessionEntity;
import org.keycloak.models.sessions.jpa.entities.ClientSessionNoteEntity;
import org.keycloak.models.sessions.jpa.entities.ClientSessionRoleEntity;
import org.keycloak.models.sessions.jpa.entities.UserSessionEntity;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientSessionAdapter implements ClientSessionModel {

    private KeycloakSession session;
    private ClientSessionEntity entity;
    private EntityManager em;
    private RealmModel realm;

    public ClientSessionAdapter(KeycloakSession session, EntityManager em, RealmModel realm, ClientSessionEntity entity) {
        this.session = session;
        this.em = em;
        this.realm = realm;
        this.entity = entity;
    }

    @Override
    public RealmModel getRealm() {
        return session.realms().getRealm(entity.getRealmId());
    }

    @Override
    public void setNote(String name, String value) {
        for (ClientSessionNoteEntity attr : entity.getNotes()) {
            if (attr.getName().equals(name)) {
                attr.setValue(value);
                return;
            }
        }
        ClientSessionNoteEntity attr = new ClientSessionNoteEntity();
        attr.setName(name);
        attr.setValue(value);
        attr.setClientSession(entity);
        em.persist(attr);
        entity.getNotes().add(attr);
    }

    @Override
    public void removeNote(String name) {
        Iterator<ClientSessionNoteEntity> it = entity.getNotes().iterator();
        while (it.hasNext()) {
            ClientSessionNoteEntity attr = it.next();
            if (attr.getName().equals(name)) {
                it.remove();
                em.remove(attr);
            }
        }
    }

    @Override
    public String getNote(String name) {
        for (ClientSessionNoteEntity attr : entity.getNotes()) {
            if (attr.getName().equals(name)) {
                return attr.getValue();
            }
        }
        return null;
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
        if (roles != null) {
            for (String r : roles) {
                ClientSessionRoleEntity roleEntity = new ClientSessionRoleEntity();
                roleEntity.setClientSession(entity);
                roleEntity.setRoleId(r);
                em.persist(roleEntity);

                entity.getRoles().add(roleEntity);
            }
        }
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
    public UserSessionModel getUserSession() {
        if (entity.getSession() == null) return null;
        return new UserSessionAdapter(session, em, realm, entity.getSession());
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
        Set<String> roles = new HashSet<String>();
        if (entity.getRoles() != null) {
            for (ClientSessionRoleEntity e : entity.getRoles()) {
                roles.add(e.getRoleId());
            }
        }
        return roles;
    }
}
