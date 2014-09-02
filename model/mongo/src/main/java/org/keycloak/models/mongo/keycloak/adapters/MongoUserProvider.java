package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.entities.SocialLinkEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;
import org.keycloak.models.utils.CredentialValidation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoUserProvider implements UserProvider {

    private final MongoStoreInvocationContext invocationContext;
    private final KeycloakSession session;
    private final MongoStore mongoStore;

    public MongoUserProvider(KeycloakSession session, MongoStore mongoStore, MongoStoreInvocationContext invocationContext) {
        this.session = session;
        this.mongoStore = mongoStore;
        this.invocationContext = invocationContext;
    }

    @Override
    public void close() {
        // TODO
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        MongoUserEntity user = getMongoStore().loadEntity(MongoUserEntity.class, id, invocationContext);

        // Check that it's user from this realm
        if (user == null || !realm.getId().equals(user.getRealmId())) {
            return null;
        } else {
            return new UserAdapter(session, realm, user, invocationContext);
        }
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("username").is(username)
                .and("realmId").is(realm.getId())
                .get();
        MongoUserEntity user = getMongoStore().loadSingleEntity(MongoUserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(session, realm, user, invocationContext);
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
            return new UserAdapter(session, realm, user, invocationContext);
        }
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
        return userEntity == null ? null : new UserAdapter(session, realm, userEntity, invocationContext);
    }

    protected List<UserModel> convertUserEntities(RealmModel realm, List<MongoUserEntity> userEntities) {
        List<UserModel> userModels = new ArrayList<UserModel>();
        for (MongoUserEntity user : userEntities) {
            userModels.add(new UserAdapter(session, realm, user, invocationContext));
        }
        return userModels;
    }


    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, -1, -1);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        return getMongoStore().countEntities(MongoUserEntity.class, query, invocationContext);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        DBObject sort = new BasicDBObject("username", 1);
        List<MongoUserEntity> users = getMongoStore().loadEntities(MongoUserEntity.class, query, sort, firstResult, maxResults, invocationContext);
        return convertUserEntities(realm, users);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
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
                        new QueryBuilder().put("username").regex(caseInsensitivePattern).get(),
                        new QueryBuilder().put("email").regex(caseInsensitivePattern).get(),
                        nameBuilder.get()

                ).get()
        );

        DBObject sort = new BasicDBObject("username", 1);

        List<MongoUserEntity> users = getMongoStore().loadEntities(MongoUserEntity.class, builder.get(), sort, firstResult, maxResults, invocationContext);
        return convertUserEntities(realm, users);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return searchForUserByAttributes(attributes, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("realmId").is(realm.getId());

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(UserModel.USERNAME)) {
                queryBuilder.and(UserModel.USERNAME).regex(Pattern.compile(".*" + entry.getValue() + ".*", Pattern.CASE_INSENSITIVE));
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                queryBuilder.and(UserModel.FIRST_NAME).regex(Pattern.compile(".*" + entry.getValue() + ".*", Pattern.CASE_INSENSITIVE));

            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                queryBuilder.and(UserModel.LAST_NAME).regex(Pattern.compile(".*" + entry.getValue() + ".*", Pattern.CASE_INSENSITIVE));

            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                queryBuilder.and(UserModel.EMAIL).regex(Pattern.compile(".*" + entry.getValue() + ".*", Pattern.CASE_INSENSITIVE));
            }
        }

        DBObject sort = new BasicDBObject("username", 1);

        List<MongoUserEntity> users = getMongoStore().loadEntities(MongoUserEntity.class, queryBuilder.get(), sort, firstResult, maxResults, invocationContext);
        return convertUserEntities(realm, users);
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel userModel, RealmModel realm) {
        UserModel user = getUserById(userModel.getId(), realm);
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

    private SocialLinkEntity findSocialLink(UserModel userModel, String socialProvider, RealmModel realm) {
        UserModel user = getUserById(userModel.getId(), realm);
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
        SocialLinkEntity socialLinkEntity = findSocialLink(user, socialProvider, realm);
        return socialLinkEntity != null ? new SocialLinkModel(socialLinkEntity.getSocialProvider(), socialLinkEntity.getSocialUserId(), socialLinkEntity.getSocialUsername()) : null;
    }

    @Override
    public UserAdapter addUser(RealmModel realm, String id, String username, boolean addDefaultRoles) {
        UserAdapter userModel = addUserEntity(realm, id, username);

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

    protected UserAdapter addUserEntity(RealmModel realm, String id, String username) {
        MongoUserEntity userEntity = new MongoUserEntity();
        userEntity.setId(id);
        userEntity.setUsername(username);
        // Compatibility with JPA model, which has user disabled by default
        // userEntity.setEnabled(true);
        userEntity.setRealmId(realm.getId());

        getMongoStore().insertEntity(userEntity, invocationContext);
        return new UserAdapter(session, realm, userEntity, invocationContext);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        DBObject query = new QueryBuilder()
                .and("_id").is(user.getId())
                .and("realmId").is(realm.getId())
                .get();
        return getMongoStore().removeEntities(MongoUserEntity.class, query, invocationContext);
    }


    @Override
    public void addSocialLink(RealmModel realm, UserModel user, SocialLinkModel socialLink) {
        user = getUserById(user.getId(), realm);
        MongoUserEntity userEntity = ((UserAdapter) user).getUser();
        SocialLinkEntity socialLinkEntity = new SocialLinkEntity();
        socialLinkEntity.setSocialProvider(socialLink.getSocialProvider());
        socialLinkEntity.setSocialUserId(socialLink.getSocialUserId());
        socialLinkEntity.setSocialUsername(socialLink.getSocialUsername());

        getMongoStore().pushItemToList(userEntity, "socialLinks", socialLinkEntity, true, invocationContext);
    }

    @Override
    public boolean removeSocialLink(RealmModel realm, UserModel userModel, String socialProvider) {
        UserModel user = getUserById(userModel.getId(), realm);
        MongoUserEntity userEntity = ((UserAdapter) user).getUser();
        SocialLinkEntity socialLinkEntity = findSocialLink(userEntity, socialProvider);
        if (socialLinkEntity == null) {
            return false;
        }
        return getMongoStore().pullItemFromList(userEntity, "socialLinks", socialLinkEntity, invocationContext);
    }

    private SocialLinkEntity findSocialLink(MongoUserEntity userEntity, String socialProvider) {
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
    public UserModel addUser(RealmModel realm, String username) {
        return this.addUser(realm, null, username, true);
    }

    @Override
    public void preRemove(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        getMongoStore().removeEntities(MongoUserEntity.class, query, invocationContext);
    }

    @Override
    public void preRemove(RealmModel realm, UserFederationProviderModel link) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .and("federationLink").is(link.getId())
                .get();
        getMongoStore().removeEntities(MongoUserEntity.class, query, invocationContext);

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        // todo not sure what to do for this
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
