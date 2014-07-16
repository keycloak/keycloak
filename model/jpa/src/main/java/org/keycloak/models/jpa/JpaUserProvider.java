package org.keycloak.models.jpa;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.SocialLinkEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserRoleMappingEntity;
import org.keycloak.models.utils.CredentialValidation;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaUserProvider implements UserProvider {
    private final KeycloakSession session;
    protected EntityManager em;

    public JpaUserProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
    }

    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles) {
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        }

        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setUsername(username);
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        entity.setRealm(realmEntity);
        em.persist(entity);
        em.flush();
        UserModel userModel = new UserAdapter(realm, em, entity);

        if (addDefaultRoles) {
            for (String r : realm.getDefaultRoles()) {
                userModel.grantRole(realm.getRole(r));
            }

            for (ApplicationModel application : realm.getApplications()) {
                for (String r : application.getDefaultRoles()) {
                    userModel.grantRole(application.getRole(r));
                }
            }
        }

        return userModel;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return addUser(realm, KeycloakModelUtils.generateId(), username, true);
    }

    @Override
    public boolean removeUser(RealmModel realm, String name) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByUsername", UserEntity.class);
        query.setParameter("username", name);
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        query.setParameter("realm", realmEntity);
        List<UserEntity> results = query.getResultList();
        if (results.size() == 0) return false;
        removeUser(results.get(0));
        return true;
    }

    private void removeUser(UserEntity user) {
        em.createQuery("delete from " + UserRoleMappingEntity.class.getSimpleName() + " where user = :user").setParameter("user", user).executeUpdate();
        em.createQuery("delete from " + SocialLinkEntity.class.getSimpleName() + " where user = :user").setParameter("user", user).executeUpdate();
        if (user.getAuthenticationLink() != null) {
            em.remove(user.getAuthenticationLink());
        }
        em.remove(user);
    }

    @Override
    public void addSocialLink(RealmModel realm, UserModel user, SocialLinkModel socialLink) {
        SocialLinkEntity entity = new SocialLinkEntity();
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        entity.setRealm(realmEntity);
        entity.setSocialProvider(socialLink.getSocialProvider());
        entity.setSocialUserId(socialLink.getSocialUserId());
        entity.setSocialUsername(socialLink.getSocialUsername());
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        entity.setUser(userEntity);
        em.persist(entity);
        em.flush();
    }

    @Override
    public boolean removeSocialLink(RealmModel realm, UserModel user, String socialProvider) {
        SocialLinkEntity entity = findSocialLink(user, socialProvider);
        if (entity != null) {
            em.remove(entity);
            em.flush();
            return true;
        } else {
            return false;
        }
    }



    @Override
    public void preRemove(RealmModel realm) {
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        int num = em.createQuery("delete from " + UserRoleMappingEntity.class.getSimpleName() + " mapping where mapping.user IN (select u from UserEntity u where realm=:realm)")
                .setParameter("realm", realmEntity).executeUpdate();
        num = em.createQuery("delete from " + SocialLinkEntity.class.getSimpleName() + " socialLink where socialLink.user IN (select u from UserEntity u where realm=:realm)")
                .setParameter("realm", realmEntity).executeUpdate();
        num = em.createQuery("delete from " + CredentialEntity.class.getSimpleName() + " mapping where mapping.user IN (select u from UserEntity u where realm=:realm)")
                .setParameter("realm", realmEntity).executeUpdate();
        num = em.createQuery("delete from UserEntity u where u.realm = :realm")
                .setParameter("realm", realmEntity).executeUpdate();
    }

    @Override
    public void preRemove(RoleModel role) {
        RoleEntity roleEntity = em.getReference(RoleEntity.class, role.getId());
        em.createQuery("delete from " + UserRoleMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", roleEntity).executeUpdate();
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
    public void close() {
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
        return getUsers(realm, -1, -1);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        TypedQuery<UserEntity> query = em.createQuery("select u from UserEntity u where u.realm = :realm order by u.username", UserEntity.class);
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        query.setParameter("realm", realmEntity);
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity entity : results) users.add(new UserAdapter(realm, em, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        TypedQuery<UserEntity> query = em.createQuery("select u from UserEntity u where u.realm = :realm and ( lower(u.username) like :search or lower(concat(u.firstName, ' ', u.lastName)) like :search or u.email like :search ) order by u.username", UserEntity.class);
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        query.setParameter("realm", realmEntity);
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity entity : results) users.add(new UserAdapter(realm, em, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return searchForUserByAttributes(attributes, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
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
        builder.append(" order by u.username");
        String q = builder.toString();
        TypedQuery<UserEntity> query = em.createQuery(q, UserEntity.class);
        RealmEntity realmEntity = em.getReference(RealmEntity.class, realm.getId());
        query.setParameter("realm", realmEntity);
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
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
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        return CredentialValidation.validCredentials(realm, user, input);
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
        return CredentialValidation.validCredentials(realm, user, input);
    }
}
