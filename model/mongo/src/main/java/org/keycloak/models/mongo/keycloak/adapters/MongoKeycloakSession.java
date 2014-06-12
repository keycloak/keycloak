package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.entities.SocialLinkEntity;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.impl.context.TransactionMongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.MongoApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoOAuthClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserSessionEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUsernameLoginFailureEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.Time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoKeycloakSession implements KeycloakSession {

    private final MongoStoreInvocationContext invocationContext;
    private final MongoKeycloakTransaction transaction;

    public MongoKeycloakSession(MongoStore mongoStore) {
        // this.invocationContext = new SimpleMongoStoreInvocationContext(mongoStore);
        this.invocationContext = new TransactionMongoStoreInvocationContext(mongoStore);
        this.transaction = new MongoKeycloakTransaction(invocationContext);
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return transaction;
    }

    @Override
    public void close() {
        // TODO
    }

    @Override
    public void removeAllData() {
        getMongoStore().removeAllEntities();
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        MongoRealmEntity newRealm = new MongoRealmEntity();
        newRealm.setId(id);
        newRealm.setName(name);

        getMongoStore().insertEntity(newRealm, invocationContext);

        return new RealmAdapter(this, newRealm, invocationContext);
    }

    @Override
    public RealmModel getRealm(String id) {
        MongoRealmEntity realmEntity = getMongoStore().loadEntity(MongoRealmEntity.class, id, invocationContext);
        return realmEntity != null ? new RealmAdapter(this, realmEntity, invocationContext) : null;
    }

    @Override
    public List<RealmModel> getRealms() {
        DBObject query = new BasicDBObject();
        List<MongoRealmEntity> realms = getMongoStore().loadEntities(MongoRealmEntity.class, query, invocationContext);

        List<RealmModel> results = new ArrayList<RealmModel>();
        for (MongoRealmEntity realmEntity : realms) {
            results.add(new RealmAdapter(this, realmEntity, invocationContext));
        }
        return results;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .get();
        MongoRealmEntity realm = getMongoStore().loadSingleEntity(MongoRealmEntity.class, query, invocationContext);

        if (realm == null) return null;
        return new RealmAdapter(this, realm, invocationContext);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        MongoUserEntity user = getMongoStore().loadEntity(MongoUserEntity.class, id, invocationContext);

        // Check that it's user from this realm
        if (user == null || !realm.getId().equals(user.getRealmId())) {
            return null;
        } else {
            return new UserAdapter(this, realm, user, invocationContext);
        }
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("loginName").is(username)
                .and("realmId").is(realm.getId())
                .get();
        MongoUserEntity user = getMongoStore().loadSingleEntity(MongoUserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(this, realm, user, invocationContext);
        }
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("email").is(email)
                .and("realmId").is(realm.getId())
                .get();
        MongoUserEntity user = getMongoStore().loadSingleEntity(MongoUserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(this, realm, user, invocationContext);
        }
    }

    @Override
    public boolean removeRealm(String id) {
        return getMongoStore().removeEntity(MongoRealmEntity.class, id, invocationContext);
    }

    protected MongoStore getMongoStore() {
        return invocationContext.getMongoStore();
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("socialLinks.socialProvider").is(socialLink.getSocialProvider())
                .and("socialLinks.socialUserId").is(socialLink.getSocialUserId())
                .and("realmId").is(realm.getId())
                .get();
        MongoUserEntity userEntity = getMongoStore().loadSingleEntity(MongoUserEntity.class, query, invocationContext);
        return userEntity == null ? null : new UserAdapter(this, realm, userEntity, invocationContext);
    }

    protected List<UserModel> convertUserEntities(RealmModel realm, List<MongoUserEntity> userEntities) {
        List<UserModel> userModels = new ArrayList<UserModel>();
        for (MongoUserEntity user : userEntities) {
            userModels.add(new UserAdapter(this, realm, user, invocationContext));
        }
        return userModels;
    }


    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        List<MongoUserEntity> users = getMongoStore().loadEntities(MongoUserEntity.class, query, invocationContext);
        return convertUserEntities(realm, users);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        search = search.trim();
        Pattern caseInsensitivePattern = Pattern.compile("(?i:" + search + ")");

        QueryBuilder nameBuilder;
        int spaceInd = search.lastIndexOf(" ");

        // Case when we have search string like "ohn Bow". Then firstName must end with "ohn" AND lastName must start with "bow" (everything case-insensitive)
        if (spaceInd != -1) {
            String firstName = search.substring(0, spaceInd);
            String lastName = search.substring(spaceInd + 1);
            Pattern firstNamePattern = Pattern.compile("(?i:" + firstName + "$)");
            Pattern lastNamePattern = Pattern.compile("(?i:^" + lastName + ")");
            nameBuilder = new QueryBuilder().and(
                    new QueryBuilder().put("firstName").regex(firstNamePattern).get(),
                    new QueryBuilder().put("lastName").regex(lastNamePattern).get()
            );
        } else {
            // Case when we have search without spaces like "foo". The firstName OR lastName could be "foo" (everything case-insensitive)
            nameBuilder = new QueryBuilder().or(
                    new QueryBuilder().put("firstName").regex(caseInsensitivePattern).get(),
                    new QueryBuilder().put("lastName").regex(caseInsensitivePattern).get()
            );
        }

        QueryBuilder builder = new QueryBuilder().and(
                new QueryBuilder().and("realmId").is(realm.getId()).get(),
                new QueryBuilder().or(
                        new QueryBuilder().put("loginName").regex(caseInsensitivePattern).get(),
                        new QueryBuilder().put("email").regex(caseInsensitivePattern).get(),
                        nameBuilder.get()

                ).get()
        );

        List<MongoUserEntity> users = getMongoStore().loadEntities(MongoUserEntity.class, builder.get(), invocationContext);
        return convertUserEntities(realm, users);    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("realmId").is(realm.getId());

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().equals(UserModel.LOGIN_NAME)) {
                queryBuilder.and("loginName").regex(Pattern.compile("(?i:" + entry.getValue() + "$)"));
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                queryBuilder.and(UserModel.FIRST_NAME).regex(Pattern.compile("(?i:" + entry.getValue() + "$)"));

            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                queryBuilder.and(UserModel.LAST_NAME).regex(Pattern.compile("(?i:" + entry.getValue() + "$)"));

            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                queryBuilder.and(UserModel.EMAIL).regex(Pattern.compile("(?i:" + entry.getValue() + "$)"));
            }
        }
        List<MongoUserEntity> users = getMongoStore().loadEntities(MongoUserEntity.class, queryBuilder.get(), invocationContext);
        return convertUserEntities(realm, users);
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user, RealmModel realm) {
        MongoUserEntity userEntity = ((UserAdapter) user).getUser();
        List<SocialLinkEntity> linkEntities = userEntity.getSocialLinks();

        if (linkEntities == null) {
            return Collections.EMPTY_SET;
        }

        Set<SocialLinkModel> result = new HashSet<SocialLinkModel>();
        for (SocialLinkEntity socialLinkEntity : linkEntities) {
            SocialLinkModel model = new SocialLinkModel(socialLinkEntity.getSocialProvider(),
                    socialLinkEntity.getSocialUserId(), socialLinkEntity.getSocialUsername());
            result.add(model);
        }
        return result;
    }

    private SocialLinkEntity findSocialLink(UserModel user, String socialProvider) {
        MongoUserEntity userEntity = ((UserAdapter) user).getUser();
        List<SocialLinkEntity> linkEntities = userEntity.getSocialLinks();
        if (linkEntities == null) {
            return null;
        }

        for (SocialLinkEntity socialLinkEntity : linkEntities) {
            if (socialLinkEntity.getSocialProvider().equals(socialProvider)) {
                return socialLinkEntity;
            }
        }
        return null;
    }


    @Override
    public SocialLinkModel getSocialLink(UserModel user, String socialProvider, RealmModel realm) {
        SocialLinkEntity socialLinkEntity = findSocialLink(user, socialProvider);
        return socialLinkEntity != null ? new SocialLinkModel(socialLinkEntity.getSocialProvider(), socialLinkEntity.getSocialUserId(), socialLinkEntity.getSocialUsername()) : null;
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        MongoRoleEntity role = getMongoStore().loadEntity(MongoRoleEntity.class, id, invocationContext);
        if (role == null) return null;
        if (role.getRealmId() != null && !role.getRealmId().equals(realm.getId())) return null;
        if (role.getApplicationId() != null && realm.getApplicationById(role.getApplicationId()) == null) return null;
        return new RoleAdapter(this, realm, role, null, invocationContext);
    }

    @Override
    public ApplicationModel getApplicationById(String id, RealmModel realm) {
        MongoApplicationEntity appData = getMongoStore().loadEntity(MongoApplicationEntity.class, id, invocationContext);

        // Check if application belongs to this realm
        if (appData == null || !realm.getId().equals(appData.getRealmId())) {
            return null;
        }

        return new ApplicationAdapter(this, realm, appData, invocationContext);
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id, RealmModel realm) {
        MongoOAuthClientEntity clientEntity = getMongoStore().loadEntity(MongoOAuthClientEntity.class, id, invocationContext);

        // Check if client belongs to this realm
        if (clientEntity == null || !realm.getId().equals(clientEntity.getRealmId())) return null;

        return new OAuthClientAdapter(this, realm, clientEntity, invocationContext);
    }

    @Override
    public UsernameLoginFailureModel getUserLoginFailure(String username, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("username").is(username)
                .and("realmId").is(realm.getId())
                .get();
        MongoUsernameLoginFailureEntity user = getMongoStore().loadSingleEntity(MongoUsernameLoginFailureEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UsernameLoginFailureAdapter(invocationContext, user);
        }
    }

    @Override
    public UsernameLoginFailureModel addUserLoginFailure(String username, RealmModel realm) {
        UsernameLoginFailureModel userLoginFailure = getUserLoginFailure(username, realm);
        if (userLoginFailure != null) {
            return userLoginFailure;
        }

        MongoUsernameLoginFailureEntity userEntity = new MongoUsernameLoginFailureEntity();
        userEntity.setUsername(username);
        userEntity.setRealmId(realm.getId());

        getMongoStore().insertEntity(userEntity, invocationContext);
        return new UsernameLoginFailureAdapter(invocationContext, userEntity);
    }

    @Override
    public List<UsernameLoginFailureModel> getAllUserLoginFailures(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        List<MongoUsernameLoginFailureEntity> failures = getMongoStore().loadEntities(MongoUsernameLoginFailureEntity.class, query, invocationContext);

        List<UsernameLoginFailureModel> result = new ArrayList<UsernameLoginFailureModel>();

        if (failures == null) return result;
        for (MongoUsernameLoginFailureEntity failure : failures) {
            result.add(new UsernameLoginFailureAdapter(invocationContext, failure));
        }

        return result;
    }

    @Override
    public UserSessionModel createUserSession(RealmModel realm, UserModel user, String ipAddress) {
        MongoUserSessionEntity entity = new MongoUserSessionEntity();
        entity.setRealmId(realm.getId());
        entity.setUser(user.getId());
        entity.setIpAddress(ipAddress);

        int currentTime = Time.currentTime();

        entity.setStarted(currentTime);
        entity.setLastSessionRefresh(currentTime);

        getMongoStore().insertEntity(entity, invocationContext);
        return new UserSessionAdapter(entity, realm, invocationContext);
    }

    @Override
    public UserSessionModel getUserSession(String id, RealmModel realm) {
        MongoUserSessionEntity entity = getMongoStore().loadEntity(MongoUserSessionEntity.class, id, invocationContext);
        if (entity == null) {
            return null;
        } else {
            return new UserSessionAdapter(entity, realm, invocationContext);
        }
    }

    @Override
    public List<UserSessionModel> getUserSessions(UserModel user, RealmModel realm) {
        DBObject query = new BasicDBObject("user", user.getId());
        List<UserSessionModel> sessions = new LinkedList<UserSessionModel>();
        for (MongoUserSessionEntity e : getMongoStore().loadEntities(MongoUserSessionEntity.class, query, invocationContext)) {
            sessions.add(new UserSessionAdapter(e, realm, invocationContext));
        }
        return sessions;
    }

    @Override
    public Set<UserSessionModel> getUserSessions(RealmModel realm, ClientModel client) {
        DBObject query = new QueryBuilder()
                .and("associatedClientIds").is(client.getId())
                .get();
        List<MongoUserSessionEntity> sessions = getMongoStore().loadEntities(MongoUserSessionEntity.class, query, invocationContext);

        Set<UserSessionModel> result = new HashSet<UserSessionModel>();
        for (MongoUserSessionEntity session : sessions) {
            result.add(new UserSessionAdapter(session, realm, invocationContext));
        }
        return result;
    }

    @Override
    public int getActiveUserSessions(RealmModel realm, ClientModel client) {
        return getUserSessions(realm, client).size();
    }

    @Override
    public void removeUserSession(UserSessionModel session) {
        getMongoStore().removeEntity(((UserSessionAdapter) session).getMongoEntity(), invocationContext);
    }

    @Override
    public void removeUserSessions(RealmModel realm, UserModel user) {
        DBObject query = new BasicDBObject("user", user.getId());
        getMongoStore().removeEntities(MongoUserSessionEntity.class, query, invocationContext);
    }

    @Override
    public void removeUserSessions(RealmModel realm) {
        DBObject query = new BasicDBObject("realmId", realm.getId());
        getMongoStore().removeEntities(MongoUserSessionEntity.class, query, invocationContext);
    }

    @Override
    public void removeExpiredUserSessions(RealmModel realm) {
        int currentTime = Time.currentTime();
        DBObject query = new QueryBuilder()
                .and("started").lessThan(currentTime - realm.getSsoSessionMaxLifespan())
                .get();

        getMongoStore().removeEntities(MongoUserSessionEntity.class, query, invocationContext);
        query = new QueryBuilder()
                .and("lastSessionRefresh").lessThan(currentTime - realm.getSsoSessionIdleTimeout())
                .get();

        getMongoStore().removeEntities(MongoUserSessionEntity.class, query, invocationContext);
    }
}
