package org.keycloak.models.sessions.jpa;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.jpa.entities.ClientSessionAuthStatusEntity;
import org.keycloak.models.sessions.jpa.entities.ClientSessionEntity;
import org.keycloak.models.sessions.jpa.entities.ClientSessionNoteEntity;
import org.keycloak.models.sessions.jpa.entities.ClientSessionProtocolMapperEntity;
import org.keycloak.models.sessions.jpa.entities.ClientSessionRoleEntity;
import org.keycloak.models.sessions.jpa.entities.ClientUserSessionNoteEntity;
import org.keycloak.models.sessions.jpa.entities.UserSessionEntity;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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
    public Map<String, String> getNotes() {
        Map<String, String> copy = new HashMap<>();
        for (ClientSessionNoteEntity attr : entity.getNotes()) {
            copy.put(attr.getName(), attr.getValue());
        }

        return copy;
    }

    @Override
    public void setUserSessionNote(String name, String value) {
        for (ClientUserSessionNoteEntity attr : entity.getUserSessionNotes()) {
            if (attr.getName().equals(name)) {
                attr.setValue(value);
                return;
            }
        }
        ClientUserSessionNoteEntity attr = new ClientUserSessionNoteEntity();
        attr.setName(name);
        attr.setValue(value);
        attr.setClientSession(entity);
        em.persist(attr);
        entity.getUserSessionNotes().add(attr);

    }

    @Override
    public Map<String, String> getUserSessionNotes() {
        Map<String, String> copy = new HashMap<>();
        for (ClientUserSessionNoteEntity attr : entity.getUserSessionNotes()) {
            copy.put(attr.getName(), attr.getValue());
        }
        return copy;
    }

    @Override
    public void clearUserSessionNotes() {
        Iterator<ClientUserSessionNoteEntity> it = entity.getUserSessionNotes().iterator();
        while (it.hasNext()) {
            ClientUserSessionNoteEntity attr = it.next();
            it.remove();
            em.remove(attr);
        }

    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public ClientModel getClient() {
        return realm.getClientById(entity.getClientId());
    }

    public ClientSessionEntity getEntity() {
        return entity;
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
        if (roles != null) {
            for (String r : roles) {
                ClientSessionRoleEntity roleEntity = new ClientSessionRoleEntity();
                roleEntity.setClientSession(entity);
                roleEntity.setRoleId(r);
                em.persist(roleEntity);

                entity.getRoles().add(roleEntity);
            }
        } else {
            if (entity.getRoles() != null) {
                for (ClientSessionRoleEntity r : entity.getRoles()) {
                    em.remove(r);
                }
                entity.getRoles().clear();
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
    public String getAction() {
        return entity.getAction();
    }

    @Override
    public void setAction(String action) {
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

    @Override
    public Set<String> getProtocolMappers() {
        Set<String> protMappers = new HashSet<String>();
        if (entity.getProtocolMappers() != null) {
            for (ClientSessionProtocolMapperEntity e : entity.getProtocolMappers()) {
                protMappers.add(e.getProtocolMapperId());
            }
        }
        return protMappers;
    }

    @Override
    public void setProtocolMappers(Set<String> protocolMappers) {
        if (protocolMappers != null) {
            for (String pm : protocolMappers) {
                ClientSessionProtocolMapperEntity protMapperEntity = new ClientSessionProtocolMapperEntity();
                protMapperEntity.setClientSession(entity);
                protMapperEntity.setProtocolMapperId(pm);
                em.persist(protMapperEntity);

                entity.getProtocolMappers().add(protMapperEntity);
            }
        } else {
            if (entity.getProtocolMappers() != null) {
                for (ClientSessionProtocolMapperEntity pm : entity.getProtocolMappers()) {
                    em.remove(pm);
                }
                entity.getProtocolMappers().clear();
            }
        }
    }

    @Override
    public Map<String, ExecutionStatus> getExecutionStatus() {
        Map<String, ExecutionStatus> result = new HashMap<>();
        for (ClientSessionAuthStatusEntity status : entity.getAuthanticatorStatus()) {
            result.put(status.getAuthenticator(), status.getStatus());
        }
        return result;
    }

    @Override
    public void setExecutionStatus(String authenticator, ExecutionStatus status) {
        ClientSessionAuthStatusEntity authStatus = new ClientSessionAuthStatusEntity();
        authStatus.setAuthenticator(authenticator);
        authStatus.setClientSession(entity);
        authStatus.setStatus(status);
        em.persist(authStatus);
        entity.getAuthanticatorStatus().add(authStatus);
        em.flush();


    }

    @Override
    public void clearExecutionStatus() {
        Iterator<ClientSessionAuthStatusEntity> iterator = entity.getAuthanticatorStatus().iterator();
        while (iterator.hasNext()) {
            ClientSessionAuthStatusEntity authStatus = iterator.next();
            iterator.remove();
            em.remove(authStatus);
        }
    }

    @Override
    public UserModel getAuthenticatedUser() {
        return entity.getUserId() == null ? null : session.users().getUserById(entity.getUserId(), realm);
    }

    @Override
    public void setAuthenticatedUser(UserModel user) {
        if (user == null) entity.setUserId(null);
        else entity.setUserId(user.getId());
    }
}
