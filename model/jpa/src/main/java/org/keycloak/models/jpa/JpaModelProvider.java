package org.keycloak.models.jpa;

import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.Time;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaModelProvider implements ModelProvider {
    private final KeycloakSession session;
    protected EntityManager em;

    public JpaModelProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
        this.em = PersistenceExceptionConverter.create(em);
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return new JpaKeycloakTransaction(em);
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        RealmEntity realm = new RealmEntity();
        realm.setName(name);
        realm.setId(id);
        em.persist(realm);
        em.flush();
        return new RealmAdapter(session, em, realm);
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) return null;
        return new RealmAdapter(session, em, realm);
    }

    @Override
    public List<RealmModel> getRealms() {
        TypedQuery<RealmEntity> query = em.createNamedQuery("getAllRealms", RealmEntity.class);
        List<RealmEntity> entities = query.getResultList();
        List<RealmModel> realms = new ArrayList<RealmModel>();
        for (RealmEntity entity : entities) {
            realms.add(new RealmAdapter(session, em, entity));
        }
        return realms;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        TypedQuery<RealmEntity> query = em.createNamedQuery("getRealmByName", RealmEntity.class);
        query.setParameter("name", name);
        List<RealmEntity> entities = query.getResultList();
        if (entities.size() == 0) return null;
        if (entities.size() > 1) throw new IllegalStateException("Should not be more than one realm with same name");
        RealmEntity realm = query.getResultList().get(0);
        if (realm == null) return null;
        return new RealmAdapter(session, em, realm);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realmModel) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserById", UserEntity.class);
        query.setParameter("id", id);
        RealmEntity realm = em.getReference(RealmEntity.class, realmModel.getId());
        query.setParameter("realm", realm);
        List<UserEntity> entities = query.getResultList();
        if (entities.size() == 0) return null;
        return new UserAdapter(realmModel, em, entities.get(0));
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realmModel) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByUsername", UserEntity.class);
        query.setParameter("username", username);
        RealmEntity realm = em.getReference(RealmEntity.class, realmModel.getId());
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        if (results.size() == 0) return null;
        return new UserAdapter(realmModel, em, results.get(0));
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realmModel) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByEmail", UserEntity.class);
        query.setParameter("email", email);
        RealmEntity realm = em.getReference(RealmEntity.class, realmModel.getId());
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        return results.isEmpty() ? null : new UserAdapter(realmModel, em, results.get(0));
    }

    @Override
    public boolean removeRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) {
            return false;
        }

        RealmAdapter adapter = new RealmAdapter(session, em, realm);
        adapter.removeUserSessions();
        for (ApplicationEntity a : new LinkedList<ApplicationEntity>(realm.getApplications())) {
            adapter.removeApplication(a.getId());
        }

        for (OAuthClientModel oauth : adapter.getOAuthClients()) {
            adapter.removeOAuthClient(oauth.getId());
        }

        for (UserEntity u : em.createQuery("from UserEntity u where u.realm = :realm", UserEntity.class).setParameter("realm", realm).getResultList()) {
            adapter.removeUser(u.getUsername());
        }

        em.remove(realm);

        return true;
    }

    @Override
    public void close() {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        if (em.isOpen()) em.close();
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("findUserByLinkAndRealm", UserEntity.class);
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        query.setParameter("realm", realmEntity);
        query.setParameter("socialProvider", socialLink.getSocialProvider());
        query.setParameter("socialUserId", socialLink.getSocialUserId());
        List<UserEntity> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new IllegalStateException("More results found for socialProvider=" + socialLink.getSocialProvider() +
                    ", socialUserId=" + socialLink.getSocialUserId() + ", results=" + results);
        } else {
            UserEntity user = results.get(0);
            return new UserAdapter(realm, em, user);
        }
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        TypedQuery<UserEntity> query = em.createQuery("select u from UserEntity u where u.realm = :realm", UserEntity.class);
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        query.setParameter("realm", realmEntity);
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity entity : results) users.add(new UserAdapter(realm, em, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createQuery("select u from UserEntity u where u.realm = :realm and ( lower(u.username) like :search or lower(concat(u.firstName, ' ', u.lastName)) like :search or u.email like :search )", UserEntity.class);
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        query.setParameter("realm", realmEntity);
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity entity : results) users.add(new UserAdapter(realm, em, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        StringBuilder builder = new StringBuilder("select u from UserEntity u");
        boolean first = true;
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String attribute = null;
            if (entry.getKey().equals(UserModel.LOGIN_NAME)) {
                attribute = "lower(username)";
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                attribute = "lower(firstName)";
            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                attribute = "lower(lastName)";
            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                attribute = "lower(email)";
            }
            if (attribute == null) continue;
            if (first) {
                first = false;
                builder.append(" where realm = :realm");
            } else {
                builder.append(" and ");
            }
            builder.append(attribute).append(" like '%").append(entry.getValue().toLowerCase()).append("%'");
        }
        String q = builder.toString();
        TypedQuery<UserEntity> query = em.createQuery(q, UserEntity.class);
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        query.setParameter("realm", realmEntity);
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity entity : results) users.add(new UserAdapter(realm, em, entity));
        return users;
    }

    private SocialLinkEntity findSocialLink(UserModel user, String socialProvider) {
        TypedQuery<SocialLinkEntity> query = em.createNamedQuery("findSocialLinkByUserAndProvider", SocialLinkEntity.class);
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        query.setParameter("user", userEntity);
        query.setParameter("socialProvider", socialProvider);
        List<SocialLinkEntity> results = query.getResultList();
        return results.size() > 0 ? results.get(0) : null;
    }


    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user, RealmModel realm) {
        TypedQuery<SocialLinkEntity> query = em.createNamedQuery("findSocialLinkByUser", SocialLinkEntity.class);
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        query.setParameter("user", userEntity);
        List<SocialLinkEntity> results = query.getResultList();
        Set<SocialLinkModel> set = new HashSet<SocialLinkModel>();
        for (SocialLinkEntity entity : results) {
            set.add(new SocialLinkModel(entity.getSocialProvider(), entity.getSocialUserId(), entity.getSocialUsername()));
        }
        return set;
    }

    @Override
    public SocialLinkModel getSocialLink(UserModel user, String socialProvider, RealmModel realm) {
        SocialLinkEntity entity = findSocialLink(user, socialProvider);
        return (entity != null) ? new SocialLinkModel(entity.getSocialProvider(), entity.getSocialUserId(), entity.getSocialUsername()) : null;
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        RoleEntity entity = em.find(RoleEntity.class, id);
        if (entity == null) return null;
        if (!realm.getId().equals(entity.getRealmId())) return null;
        return new RoleAdapter(realm, em, entity);
    }

    @Override
    public ApplicationModel getApplicationById(String id, RealmModel realm) {
        ApplicationEntity app = em.find(ApplicationEntity.class, id);

        // Check if application belongs to this realm
        if (app == null || !realm.getId().equals(app.getRealm().getId())) return null;
        return new ApplicationAdapter(realm, em, app);
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id, RealmModel realm) {
        OAuthClientEntity client = em.find(OAuthClientEntity.class, id);

        // Check if client belongs to this realm
        if (client == null || !realm.getId().equals(client.getRealm().getId())) return null;
        return new OAuthClientAdapter(realm, client, em);
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(String username, RealmModel realm) {
        String id = username + "-" + realm.getId();
        UsernameLoginFailureEntity entity = em.find(UsernameLoginFailureEntity.class, id);
        if (entity == null) return null;
        return new UsernameLoginFailureAdapter(entity);
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(String username, RealmModel realm) {
        UsernameLoginFailureModel model = getUserLoginFailure(username, realm);
        if (model != null) return model;
        String id = username + "-" + realm.getId();
        UsernameLoginFailureEntity entity = new UsernameLoginFailureEntity();
        entity.setId(id);
        entity.setUsername(username);
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        entity.setRealm(realmEntity);
        em.persist(entity);
        return new UsernameLoginFailureAdapter(entity);
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures(RealmModel realm) {
        TypedQuery<UsernameLoginFailureEntity> query = em.createNamedQuery("getAllFailures", UsernameLoginFailureEntity.class);
        List<UsernameLoginFailureEntity> entities = query.getResultList();
        List<UsernameLoginFailureModel> models = new ArrayList<UsernameLoginFailureModel>();
        for (UsernameLoginFailureEntity entity : entities) {
            models.add(new UsernameLoginFailureAdapter(entity));
        }
        return models;
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String ipAddress) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.setRealmId(realm.getId());
        entity.setUserId(user.getId());
        entity.setIpAddress(ipAddress);

        int currentTime = Time.currentTime();

        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);

        em.persist(entity);
        return new UserSessionAdapter(em, realm, entity);
    }

    @Override
    public UserSessionModel getUserSession(String id, RealmModel realm) {
        UserSessionEntity entity = em.find(UserSessionEntity.class, id);
        return entity != null ? new UserSessionAdapter(em, realm, entity) : null;
    }

    @Override
    public List<UserSessionModel> getUserSessions(UserModel user, RealmModel realm) {
        List<UserSessionModel> sessions = new LinkedList<UserSessionModel>();
        for (UserSessionEntity e : em.createNamedQuery("getUserSessionByUser", UserSessionEntity.class)
                .setParameter("userId", user.getId()).getResultList()) {
            sessions.add(new UserSessionAdapter(em, realm, e));
        }
        return sessions;
    }

    @Override
    public Set<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        Set<UserSessionModel> list = new HashSet<UserSessionModel>();
        TypedQuery<ClientUserSessionAssociationEntity> query = em.createNamedQuery("getClientUserSessionByClient", ClientUserSessionAssociationEntity.class);
        String id = client.getId();
        query.setParameter("clientId", id);
        List<ClientUserSessionAssociationEntity> results = query.getResultList();
        for (ClientUserSessionAssociationEntity entity : results) {
            list.add(new UserSessionAdapter(em, realm, entity.getSession()));
        }
        return list;
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        Query query = em.createNamedQuery("getActiveClientSessions");
        query.setParameter("clientId", client.getId());
        Object count = query.getSingleResult();
        return ((Number)count).intValue();
    }

    @Override
    public void removeUserSession(UserSessionModel session) {
        em.remove(((UserSessionAdapter) session).getEntity());
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        em.createNamedQuery("removeClientUserSessionByUser").setParameter("userId", user.getId()).executeUpdate();
        em.createNamedQuery("removeUserSessionByUser").setParameter("userId", user.getId()).executeUpdate();
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        TypedQuery<UserSessionEntity> query = em.createNamedQuery("getUserSessionExpired", UserSessionEntity.class)
                .setParameter("maxTime", Time.currentTime() - realm.getSsoSessionMaxLifespan())
                .setParameter("idleTime", Time.currentTime() - realm.getSsoSessionIdleTimeout());
        List<UserSessionEntity> results = query.getResultList();
        for (UserSessionEntity entity : results) {
            em.remove(entity);
        }
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        em.createNamedQuery("removeClientUserSessionByRealm").setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("removeRealmUserSessions").setParameter("realmId", realm.getId()).executeUpdate();
    }
}
