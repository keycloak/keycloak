package org.keycloak.models.users.jpa;

import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.users.Credentials;
import org.keycloak.models.users.Feature;
import org.keycloak.models.users.User;
import org.keycloak.models.users.UserProvider;
import org.keycloak.models.users.jpa.entities.UserEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaUserProvider implements UserProvider {

    protected final EntityManager em;

    public JpaUserProvider(EntityManager em) {
        this.em = PersistenceExceptionConverter.create(em);
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return new JpaKeycloakTransaction(em);
    }

    @Override
    public User addUser(String id, String username, Set<String> initialRoles, String realm) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setUsername(username);
        entity.setEmailConstraint(id);
        entity.setRealm(realm);
        em.persist(entity);

        UserAdapter adapter = new UserAdapter(realm, em, entity);

        if (initialRoles != null && !initialRoles.isEmpty()) {
            for (String role : initialRoles) {
                adapter.grantRole(role);
            }
        }

        return adapter;
    }

    @Override
    public boolean removeUser(String name, String realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByLoginName", UserEntity.class);
        query.setParameter("username", name);
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        if (results.size() == 0) return false;
        em.remove(results.get(0));
        return true;
    }

    @Override
    public User getUserById(String id, String realm) {
        UserEntity user = em.find(UserEntity.class, new UserEntity.Key(id, realm));
        return user != null ? new UserAdapter(realm, em, user) : null;
    }

    @Override
    public User getUserByUsername(String username, String realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByUsername", UserEntity.class);
        query.setParameter("username", username);
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        if (results.size() == 0) return null;
        return new UserAdapter(realm, em, results.get(0));
    }

    @Override
    public User getUserByEmail(String email, String realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByEmail", UserEntity.class);
        query.setParameter("email", email);
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        return results.isEmpty() ? null : new UserAdapter(realm, em, results.get(0));
    }

    @Override
    public User getUserByAttribute(String name, String value, String realm) {
        List<UserEntity> results = em.createNamedQuery("getRealmUserByAttribute", UserEntity.class)
                .setParameter("realm", realm)
                .setParameter("name", name)
                .setParameter("value", value)
                .getResultList();
        return results.isEmpty() ? null : new UserAdapter(realm, em, results.get(0));
    }

    @Override
    public void close() {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        if (em.isOpen()) em.close();
    }

    @Override
    public List<User> getUsers(String realm) {
        TypedQuery<UserEntity> query = em.createQuery("select u from UserEntity u where u.realm = :realm", UserEntity.class);
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        List<User> users = new ArrayList<User>();
        for (UserEntity entity : results) users.add(new UserAdapter(realm, em, entity));
        return users;
    }

    @Override
    public List<User> searchForUser(String search, String realm) {
        TypedQuery<UserEntity> query = em.createQuery("select u from UserEntity u where u.realm = :realm and ( lower(u.username) like :search or lower(concat(u.firstName, ' ', u.lastName)) like :search or u.email like :search )", UserEntity.class);
        query.setParameter("realm", realm);
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        List<UserEntity> results = query.getResultList();
        List<User> users = new ArrayList<User>();
        for (UserEntity entity : results) users.add(new UserAdapter(realm, em, entity));
        return users;
    }

    @Override
    public List<User> searchForUserByAttributes(Map<String, String> attributes, String realm) {
        StringBuilder builder = new StringBuilder("select u from UserEntity u");
        boolean first = true;
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String attribute = null;
            if (entry.getKey().equals(User.USERNAME)) {
                attribute = "lower(username)";
            } else if (entry.getKey().equalsIgnoreCase(User.FIRST_NAME)) {
                attribute = "lower(firstName)";
            } else if (entry.getKey().equalsIgnoreCase(User.LAST_NAME)) {
                attribute = "lower(lastName)";
            } else if (entry.getKey().equalsIgnoreCase(User.EMAIL)) {
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
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        List<User> users = new ArrayList<User>();
        for (UserEntity entity : results) users.add(new UserAdapter(realm, em, entity));
        return users;
    }

    @Override
    public boolean supports(Feature feature) {
        return (feature == Feature.READ_CREDENTIALS || feature == Feature.UPDATE_CREDENTIALS);
    }

    @Override
    public boolean verifyCredentials(User user, Credentials... credentials) {
        return false;
    }

    @Override
    public void onRealmRemoved(String realm) {
        TypedQuery<UserEntity> query = em.createQuery("select u from UserEntity u where u.realm = :realm", UserEntity.class);
        query.setParameter("realm", realm);
        for (UserEntity u : query.getResultList()) {
            em.remove(u);
        }
    }

    @Override
    public void onRoleRemoved(String role) {
        em.createQuery("delete from RoleEntity r where r.role = :role").setParameter("role", role).executeUpdate();
    }

}
