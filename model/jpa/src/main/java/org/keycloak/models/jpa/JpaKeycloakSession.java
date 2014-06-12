package org.keycloak.models.jpa;

import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
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
public class JpaKeycloakSession implements KeycloakSession {
    protected EntityManager em;

    public JpaKeycloakSession(EntityManager em) {
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
        return new RealmAdapter(this, em, realm);
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) return null;
        return new RealmAdapter(this, em, realm);
    }

    @Override
    public List<RealmModel> getRealms() {
        TypedQuery<RealmEntity> query = em.createNamedQuery("getAllRealms", RealmEntity.class);
        List<RealmEntity> entities = query.getResultList();
        List<RealmModel> realms = new ArrayList<RealmModel>();
        for (RealmEntity entity : entities) {
            realms.add(new RealmAdapter(this, em, entity));
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
        return new RealmAdapter(this, em, realm);
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
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByLoginName", UserEntity.class);
        query.setParameter("loginName", username);
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

        RealmAdapter adapter = new RealmAdapter(this, em, realm);
        adapter.removeUserSessions();
        for (ApplicationEntity a : new LinkedList<ApplicationEntity>(realm.getApplications())) {
            adapter.removeApplication(a.getId());
        }

        for (OAuthClientModel oauth : adapter.getOAuthClients()) {
            adapter.removeOAuthClient(oauth.getId());
        }

        for (UserEntity u : em.createQuery("from UserEntity u where u.realm = :realm", UserEntity.class).setParameter("realm", realm).getResultList()) {
            adapter.removeUser(u.getLoginName());
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
    public void removeAllData() {
        // Should be sufficient to delete all realms. Rest data should be removed in cascade
        List<RealmModel> realms = getRealms();
        for (RealmModel realm : realms) {
            removeRealm(realm.getId());
        }
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings(UserModel user, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
    public AuthenticationLinkModel getAuthenticationLink(UserModel user, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
    public List<UsernameLoginFailureModel> getAllUserLoginFailures() {
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserSessionModel getUserSession(String id, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserSessionModel> getUserSessions(UserModel user, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserSession(UserSessionModel session) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
