package org.keycloak.models.sessions.jpa;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.jpa.entities.ClientSessionEntity;
import org.keycloak.models.sessions.jpa.entities.UserSessionEntity;
import org.keycloak.models.sessions.jpa.entities.UserSessionNoteEntity;
import org.keycloak.models.sessions.jpa.entities.UsernameLoginFailureEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RealmInfoUtil;
import org.keycloak.util.Time;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaUserSessionProvider implements UserSessionProvider {

    protected final KeycloakSession session;

    protected final EntityManager em;

    public JpaUserSessionProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
    }

    @Override
    public ClientSessionModel createClientSession(RealmModel realm, ClientModel client) {
        ClientSessionEntity entity = new ClientSessionEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setTimestamp(Time.currentTime());
        entity.setClientId(client.getId());
        entity.setRealmId(realm.getId());
        em.persist(entity);

        return new ClientSessionAdapter(session, em, realm, entity);
    }

    @Override
    public void removeClientSession(RealmModel realm, ClientSessionModel clientSession) {
        ClientSessionEntity clientSessionEntity = ((ClientSessionAdapter)clientSession).getEntity();
        em.remove(clientSessionEntity);
        em.flush();
   }

    @Override
    public ClientSessionModel getClientSession(RealmModel realm, String id) {
        ClientSessionEntity clientSession = em.find(ClientSessionEntity.class, id);
        if (clientSession != null && clientSession.getRealmId().equals(realm.getId())) {
            return new ClientSessionAdapter(session, em, realm, clientSession);
        }
        return null;
    }

    @Override
    public ClientSessionModel getClientSession(String id) {
        ClientSessionEntity clientSession = em.find(ClientSessionEntity.class, id);
        if (clientSession != null) {
            RealmModel realm = session.realms().getRealm(clientSession.getRealmId());
            return new ClientSessionAdapter(session, em, realm, clientSession);
        }
        return null;
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(RealmModel realm, String username) {
        UsernameLoginFailureEntity entity = em.find(UsernameLoginFailureEntity.class, new UsernameLoginFailureEntity.Key(realm.getId(), username));
        if (entity == null) return null;
        return new UsernameLoginFailureAdapter(entity);
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(RealmModel realm, String username) {
        UsernameLoginFailureModel model = getUserLoginFailure(realm, username);
        if (model != null) return model;
        UsernameLoginFailureEntity entity = new UsernameLoginFailureEntity();
        entity.setUsername(username);
        entity.setRealmId(realm.getId());
        em.persist(entity);
        return new UsernameLoginFailureAdapter(entity);
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setRealmId(realm.getId());
        entity.setUserId(user.getId());
        entity.setLoginUsername(loginUsername);
        entity.setIpAddress(ipAddress);
        entity.setAuthMethod(authMethod);
        entity.setRememberMe(rememberMe);
        entity.setBrokerSessionId(brokerSessionId);
        entity.setBrokerUserId(brokerUserId);

        int currentTime = Time.currentTime();

        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);

        em.persist(entity);
        return new UserSessionAdapter(session, em, realm, entity);
    }

    @Override
    public UserSessionModel getUserSession(RealmModel realm, String id) {
        UserSessionEntity entity = em.find(UserSessionEntity.class, id);
        return entity != null ? new UserSessionAdapter(session, em, realm, entity) : null;
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, UserModel user) {
        List<UserSessionModel> sessions = new LinkedList<UserSessionModel>();
        TypedQuery<UserSessionEntity> query = em.createNamedQuery("getUserSessionByUser", UserSessionEntity.class)
                .setParameter("realmId", realm.getId())
                .setParameter("userId", user.getId());
        for (UserSessionEntity e : query.getResultList()) {
            sessions.add(new UserSessionAdapter(session, em, realm, e));
        }
        return sessions;
    }

    @Override
    public List<UserSessionModel> getUserSessionByBrokerUserId(RealmModel realm, String brokerUserId) {
        List<UserSessionModel> sessions = new LinkedList<UserSessionModel>();
        TypedQuery<UserSessionEntity> query = em.createNamedQuery("getUserSessionByBrokerUserId", UserSessionEntity.class)
                .setParameter("realmId", realm.getId())
                .setParameter("brokerUserId", brokerUserId);
        for (UserSessionEntity e : query.getResultList()) {
            sessions.add(new UserSessionAdapter(session, em, realm, e));
        }
        return sessions;
    }

    @Override
    public UserSessionModel getUserSessionByBrokerSessionId(RealmModel realm, String brokerSessionId) {
        List<UserSessionModel> sessions = new LinkedList<UserSessionModel>();
        TypedQuery<UserSessionEntity> query = em.createNamedQuery("getUserSessionByBrokerSessionId", UserSessionEntity.class)
                .setParameter("realmId", realm.getId())
                .setParameter("brokerSessionId", brokerSessionId);
        for (UserSessionEntity e : query.getResultList()) {
            sessions.add(new UserSessionAdapter(session, em, realm, e));
        }
        if (sessions.isEmpty()) return null;
        return sessions.get(0);
    }

    @Override
    public List<UserSessionModel> getUserSessionsByNote(RealmModel realm, String noteName, String noteValue) {
        List<UserSessionModel> sessions = new LinkedList<UserSessionModel>();
        TypedQuery<UserSessionNoteEntity> query = em.createNamedQuery("selectNoteByNameValue", UserSessionNoteEntity.class)
                .setParameter("name", noteName)
                .setParameter("value", noteValue);
        for (UserSessionNoteEntity note : query.getResultList()) {
            if (!note.getUserSession().getRealmId().equals(realm.getId())) continue;
            sessions.add(new UserSessionAdapter(session, em, realm, note.getUserSession()));
        }
        return sessions;
    }

    @Override
    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessions(realm, client, -1, -1);
    }

    public List<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client, int firstResult, int maxResults) {
        List<UserSessionModel> list = new LinkedList<UserSessionModel>();
        TypedQuery<UserSessionEntity> query = em.createNamedQuery("getUserSessionByClient", UserSessionEntity.class)
                .setParameter("realmId", realm.getId())
                .setParameter("clientId", client.getId());
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        for (UserSessionEntity entity : query.getResultList()) {
            list.add(new UserSessionAdapter(session, em, realm, entity));
        }
        return list;
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        Object count = em.createNamedQuery("getActiveUserSessionByClient")
                .setParameter("realmId", realm.getId())
                .setParameter("clientId", client.getId())
                .getSingleResult();
        return ((Number)count).intValue();
    }

    @Override
    public void removeUserSession(RealmModel realm, UserSessionModel session) {
        UserSessionEntity entity = em.find(UserSessionEntity.class, session.getId());
        if (entity != null) {
            for (ClientSessionEntity c : entity.getClientSessions()) {
                em.remove(c);
            }
            em.remove(entity);
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        em.createNamedQuery("removeClientSessionNoteByUser")
                .setParameter("realmId", realm.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();
        em.createNamedQuery("removeClientSessionRoleByUser")
                .setParameter("realmId", realm.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();
        em.createNamedQuery("removeClientSessionProtMapperByUser")
                .setParameter("realmId", realm.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();
        em.createNamedQuery("removeClientSessionByUser")
                .setParameter("realmId", realm.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();
        em.createNamedQuery("removeUserSessionNoteByUser")
                .setParameter("realmId", realm.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();
        em.createNamedQuery("removeUserSessionByUser")
                .setParameter("realmId", realm.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        int maxTime = Time.currentTime() - realm.getSsoSessionMaxLifespan();
        int idleTime = Time.currentTime() - realm.getSsoSessionIdleTimeout();
        int dettachedClientSessionExpired = Time.currentTime() - RealmInfoUtil.getDettachedClientSessionLifespan(realm);

        em.createNamedQuery("removeDetachedClientSessionRoleByExpired")
                .setParameter("realmId", realm.getId())
                .setParameter("maxTime", dettachedClientSessionExpired)
                .executeUpdate();
        em.createNamedQuery("removeDetachedClientSessionProtMapperByExpired")
                .setParameter("realmId", realm.getId())
                .setParameter("maxTime", dettachedClientSessionExpired)
                .executeUpdate();
        em.createNamedQuery("removeDetachedClientSessionNoteByExpired")
                .setParameter("realmId", realm.getId())
                .setParameter("maxTime", dettachedClientSessionExpired)
                .executeUpdate();
        em.createNamedQuery("removeDetachedClientSessionByExpired")
                .setParameter("realmId", realm.getId())
                .setParameter("maxTime", dettachedClientSessionExpired)
                .executeUpdate();
        em.createNamedQuery("removeClientSessionRoleByExpired")
                .setParameter("realmId", realm.getId())
                .setParameter("maxTime", maxTime)
                .setParameter("idleTime", idleTime)
                .executeUpdate();
        em.createNamedQuery("removeClientSessionProtMapperByExpired")
                .setParameter("realmId", realm.getId())
                .setParameter("maxTime", maxTime)
                .setParameter("idleTime", idleTime)
                .executeUpdate();
        em.createNamedQuery("removeClientSessionNoteByExpired")
                .setParameter("realmId", realm.getId())
                .setParameter("maxTime", maxTime)
                .setParameter("idleTime", idleTime)
                .executeUpdate();
        em.createNamedQuery("removeClientSessionByExpired")
                .setParameter("realmId", realm.getId())
                .setParameter("maxTime", maxTime)
                .setParameter("idleTime", idleTime)
                .executeUpdate();
        em.createNamedQuery("removeUserSessionNoteByExpired")
                .setParameter("realmId", realm.getId())
                .setParameter("maxTime", maxTime)
                .setParameter("idleTime", idleTime)
                .executeUpdate();
        em.createNamedQuery("removeUserSessionByExpired")
                .setParameter("realmId", realm.getId())
                .setParameter("maxTime", maxTime)
                .setParameter("idleTime", idleTime)
                .executeUpdate();
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        em.createNamedQuery("removeClientSessionNoteByRealm").setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("removeClientSessionRoleByRealm").setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("removeClientSessionProtMapperByRealm").setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("removeClientSessionByRealm").setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("removeUserSessionNoteByRealm").setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("removeUserSessionByRealm").setParameter("realmId", realm.getId()).executeUpdate();
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        removeUserSessions(realm);
        em.createNamedQuery("removeLoginFailuresByRealm").setParameter("realmId", realm.getId()).executeUpdate();
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        em.createNamedQuery("removeClientSessionNoteByClient").setParameter("realmId", realm.getId()).setParameter("clientId", client.getId()).executeUpdate();
        em.createNamedQuery("removeClientSessionRoleByClient").setParameter("realmId", realm.getId()).setParameter("clientId", client.getId()).executeUpdate();
        em.createNamedQuery("removeClientSessionProtMapperByClient").setParameter("realmId", realm.getId()).setParameter("clientId", client.getId()).executeUpdate();
        em.createNamedQuery("removeClientSessionByClient").setParameter("realmId", realm.getId()).setParameter("clientId", client.getId()).executeUpdate();
    }

    @Override
    public void onUserRemoved(RealmModel realm, UserModel user) {
        removeUserSessions(realm, user);
        em.createNamedQuery("removeLoginFailuresByUser").setParameter("realmId", realm.getId()).setParameter("username", user.getUsername()).setParameter("email", user.getEmail()).executeUpdate();
    }

    @Override
    public void close() {
    }

}
