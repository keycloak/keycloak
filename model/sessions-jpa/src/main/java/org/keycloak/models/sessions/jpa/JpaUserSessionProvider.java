package org.keycloak.models.sessions.jpa;

import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.sessions.LoginFailure;
import org.keycloak.models.sessions.Session;
import org.keycloak.models.sessions.SessionProvider;
import org.keycloak.models.sessions.jpa.entities.ClientUserSessionAssociationEntity;
import org.keycloak.models.sessions.jpa.entities.UserSessionEntity;
import org.keycloak.models.sessions.jpa.entities.UsernameLoginFailureEntity;
import org.keycloak.util.Time;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaUserSessionProvider implements SessionProvider {

    protected final EntityManager em;

    public JpaUserSessionProvider(EntityManager em) {
        this.em = PersistenceExceptionConverter.create(em);
    }

    @Override
    public LoginFailure getUserLoginFailure(String username, String realm) {
        String id = username + "-" + realm;
        UsernameLoginFailureEntity entity = em.find(UsernameLoginFailureEntity.class, id);
        if (entity == null) return null;
        return new UsernameLoginFailureAdapter(entity);
    }

    @Override
    public LoginFailure addUserLoginFailure(String username, String realm) {
        LoginFailure model = getUserLoginFailure(username, realm);
        if (model != null) return model;
        String id = username + "-" + realm;
        UsernameLoginFailureEntity entity = new UsernameLoginFailureEntity();
        entity.setId(id);
        entity.setUsername(username);
        entity.setRealm(realm);
        em.persist(entity);
        return new UsernameLoginFailureAdapter(entity);
    }

    @Override
    public List<LoginFailure> getAllUserLoginFailures(String realm) {
        TypedQuery<UsernameLoginFailureEntity> query = em.createNamedQuery("getAllFailures", UsernameLoginFailureEntity.class);
        List<UsernameLoginFailureEntity> entities = query.getResultList();
        List<LoginFailure> models = new ArrayList<LoginFailure>();
        for (UsernameLoginFailureEntity entity : entities) {
            models.add(new UsernameLoginFailureAdapter(entity));
        }
        return models;
    }

    @Override
    public Session createUserSession(String realm, String id, String user, String ipAddress) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.setRealmId(realm);
        entity.setUserId(user);
        entity.setIpAddress(ipAddress);

        int currentTime = Time.currentTime();

        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);

        em.persist(entity);
        return new UserSessionAdapter(em, realm, entity);
    }

    @Override
    public Session getUserSession(String id, String realm) {
        UserSessionEntity entity = em.find(UserSessionEntity.class, id);
        return entity != null ? new UserSessionAdapter(em, realm, entity) : null;
    }

    @Override
    public List<Session> getUserSessionsByUser(String user, String realm) {
        List<Session> sessions = new LinkedList<Session>();
        for (UserSessionEntity e : em.createNamedQuery("getUserSessionByUser", UserSessionEntity.class)
                .setParameter("userId", user).getResultList()) {
            sessions.add(new UserSessionAdapter(em, realm, e));
        }
        return sessions;
    }

    @Override
    public Set<Session> getUserSessionsByClient(String realm, String client) {
        Set<Session> list = new HashSet<Session>();
        TypedQuery<ClientUserSessionAssociationEntity> query = em.createNamedQuery("getClientUserSessionByClient", ClientUserSessionAssociationEntity.class);
        query.setParameter("clientId", client);
        List<ClientUserSessionAssociationEntity> results = query.getResultList();
        for (ClientUserSessionAssociationEntity entity : results) {
            list.add(new UserSessionAdapter(em, realm, entity.getSession()));
        }
        return list;
    }

    @Override
    public int getActiveUserSessions(String realm, String client) {
        Query query = em.createNamedQuery("getActiveClientSessions");
        query.setParameter("clientId", client);
        Object count = query.getSingleResult();
        return ((Number)count).intValue();
    }

    @Override
    public void removeUserSession(Session session) {
        em.remove(((UserSessionAdapter) session).getEntity());
    }

    @Override
    public void removeUserSessions(String realm, String user) {
        em.createNamedQuery("removeClientUserSessionByUser").setParameter("userId", user).executeUpdate();
        em.createNamedQuery("removeUserSessionByUser").setParameter("userId", user).executeUpdate();
    }

    @Override
    public void removeExpiredUserSessions(String realm, long refreshTimeout, long sessionTimeout) {
        TypedQuery<UserSessionEntity> query = em.createNamedQuery("getUserSessionExpired", UserSessionEntity.class)
                .setParameter("maxTime", sessionTimeout)
                .setParameter("idleTime", refreshTimeout);
        List<UserSessionEntity> results = query.getResultList();
        for (UserSessionEntity entity : results) {
            em.remove(entity);
        }
    }

    @Override
    public void removeUserSessions(String realm) {
        em.createNamedQuery("removeClientUserSessionByRealm").setParameter("realmId", realm).executeUpdate();
        em.createNamedQuery("removeRealmUserSessions").setParameter("realmId", realm).executeUpdate();
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return new JpaKeycloakTransaction(em);
    }

    @Override
    public void close() {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        if (em.isOpen()) em.close();
    }

}
