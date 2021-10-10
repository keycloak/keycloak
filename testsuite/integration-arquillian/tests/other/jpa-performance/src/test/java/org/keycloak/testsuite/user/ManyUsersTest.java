package org.keycloak.testsuite.user;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.Timer;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.Response;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.fail;
import static org.keycloak.testsuite.util.IOUtil.PROJECT_BUILD_DIRECTORY;

/**
 *
 * @author tkyjovsk
 */
public class ManyUsersTest extends AbstractUserTest {

    private static final int COUNT = Integer.parseInt(System.getProperty("many.users.count", "10000"));
    private static final int BATCH = Integer.parseInt(System.getProperty("many.users.batch", "1000"));

    // When true, then it will always send another request to GET user after he is created (this trigger some DB queries and cache user on Keycloak side)
    private static final boolean READ_USER_AFTER_CREATE = Boolean.parseBoolean(System.getProperty("many.users.read.after.create", "false"));

    // When true, then each user will be updated with password, 2 additional attributes, 2 default groups and some required action
    private static final boolean CREATE_OBJECTS = Boolean.parseBoolean(System.getProperty("many.users.create.objects", "false"));

    // When true, then each user will be updated with 2 federated identity links
    private static final boolean CREATE_SOCIAL_LINKS = Boolean.parseBoolean(System.getProperty("many.users.create.social.links", "false"));

    private static final boolean REIMPORT = Boolean.parseBoolean(System.getProperty("many.users.reimport", "false"));

    private static final String REALM = "realm_with_many_users";

    private List<UserRepresentation> users;

    private final Timer realmTimer = new Timer();
    private final Timer usersTimer = new Timer();

    private static final long MIN_TOKEN_VALIDITY = Long.parseLong(System.getProperty("many.users.minTokenValidity", "10000"));
    long tokenExpirationTime = 0;

    protected boolean tokenMinValidityExpired() {
        return System.currentTimeMillis() >= tokenExpirationTime - MIN_TOKEN_VALIDITY;
    }

    protected void refreshToken() {
        long requestTime = System.currentTimeMillis();
        adminClient.tokenManager().refreshToken();
        tokenExpirationTime = requestTime + adminClient.tokenManager().getAccessToken().getExpiresIn() * 1000;
    }

    protected void refreshTokenIfMinValidityExpired() {
        if (tokenMinValidityExpired()) {
            log.info(String.format("Minimum access token validity (%s ms) expired --> refreshing", MIN_TOKEN_VALIDITY));
            refreshToken();
        }
    }

    protected RealmResource realmResource() {
        return realmsResouce().realm(REALM);
    }

    @Before
    public void before() {
        log.infof("Reading users after create is %s", READ_USER_AFTER_CREATE ? "ENABLED" : "DISABLED");

        users = new LinkedList<>();
        for (int i = 0; i < COUNT; i++) {
            users.add(createUserRep("user" + i));
        }

        realmTimer.reset("create realm before test");
        createRealm(REALM);

        if (CREATE_OBJECTS) {

            // Assuming default groups and required action already created
            if (realmResource().getDefaultGroups().isEmpty()) {
                log.infof("Creating default groups 'group1' and 'group2'.");
                setDefaultGroup("group1");
                setDefaultGroup("group2");

                RequiredActionProviderRepresentation updatePassword = realmResource().flows().getRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
                updatePassword.setDefaultAction(true);
                realmResource().flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString(), updatePassword);
            }
        }

        refreshToken();
    }

    private void setDefaultGroup(String groupName) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(groupName);
        Response resp = realmResource().groups().add(group);
        String groupId = ApiUtil.getCreatedId(resp);
        resp.close();
        realmResource().addDefaultGroup(groupId);
    }

    @After
    public void after() {
        realmTimer.clearStats(true, true, false);
        usersTimer.clearStats();
    }

    @Override
    public UserRepresentation createUser(UsersResource users, UserRepresentation user) {
        // Add some additional attributes to user
        if (CREATE_OBJECTS) {
            Map<String, List<String>> attrs = new HashMap<>();
            attrs.put("attr1", Collections.singletonList("val1"));
            attrs.put("attr2", Collections.singletonList("val2"));
            user.setAttributes(attrs);
        }

        UserRepresentation userRep = super.createUser(users, user);

        // Add password
        if (CREATE_OBJECTS) {
            CredentialRepresentation password = new CredentialRepresentation();
            password.setType(CredentialRepresentation.PASSWORD);
            password.setValue("password");
            password.setTemporary(false);
            users.get(userRep.getId()).resetPassword(password);
        }

        // Add social link
        if (CREATE_SOCIAL_LINKS) {
            createSocialLink("facebook", users, userRep.getId());
        }

        return userRep;
    }

    private void createSocialLink(String provider, UsersResource users, String userId) {
        String uuid = UUID.randomUUID().toString();

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setIdentityProvider(provider);
        link.setUserId(uuid);
        link.setUserName(uuid);
        users.get(userId).addFederatedIdentity(provider, link);
    }

    @Test
    public void manyUsers() throws IOException {
        RealmRepresentation realm = realmResource().toRepresentation();
        realm.setUsers(users);

        // CREATE 
        realmTimer.reset("create " + users.size() + " users");
        usersTimer.reset("create " + BATCH + " users");
        int i = 0;
        for (UserRepresentation user : users) {
            refreshTokenIfMinValidityExpired();
            UserRepresentation createdUser = createUser(realmResource().users(), user);

            // Send additional request to read every user after he is created
            if (READ_USER_AFTER_CREATE) {
                UserRepresentation returned = realmResource().users().get(createdUser.getId()).toRepresentation();
                Assert.assertEquals(returned.getId(), createdUser.getId());
            }

            // Send additional request to read social links of user
            if (CREATE_SOCIAL_LINKS) {
                List<FederatedIdentityRepresentation> fedIdentities = realmResource().users().get(createdUser.getId()).getFederatedIdentity();
            }

            if (++i % BATCH == 0) {
                usersTimer.reset();
                log.info("Created users: " + i + " / " + users.size());
            }
        }
        if (i % BATCH != 0) {
            usersTimer.reset();
            log.info("Created users: " + i + " / " + users.size());
        }

        if (REIMPORT) {

            // SAVE REALM
            realmTimer.reset("save realm with " + users.size() + " users");
            File realmFile = new File(PROJECT_BUILD_DIRECTORY, REALM + ".json");
            JsonSerialization.writeValueToStream(new BufferedOutputStream(new FileOutputStream(realmFile)), realm);

            // DELETE REALM
            realmTimer.reset("delete realm with " + users.size() + " users");
            realmResource().remove();
            try {
                realmResource().toRepresentation();
                fail("realm not deleted");
            } catch (Exception ex) {
                log.debug("realm deleted");
            }

            // RE-IMPORT SAVED REALM
            realmTimer.reset("re-import realm with " + realm.getUsers().size() + " users");
            realmsResouce().create(realm);
            realmTimer.reset("load " + realm.getUsers().size() + " users");
            users = realmResource().users().search("", 0, -1);

        }

        // DELETE INDIVIDUAL USERS
        realmTimer.reset("delete " + users.size() + " users");
        usersTimer.reset("delete " + BATCH + " users", false);
        i = 0;
        for (UserRepresentation user : users) {
            refreshTokenIfMinValidityExpired();
            realmResource().users().get(user.getId()).remove();
            if (++i % BATCH == 0) {
                usersTimer.reset();
                log.info("Deleted users: " + i + " / " + users.size());
            }
        }
        if (i % BATCH != 0) {
            usersTimer.reset();
            log.info("Deleted users: " + i + " / " + users.size());
        }
        realmTimer.reset();
    }

    private void createRealm(String REALM) {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm(REALM);
        adminClient.realms().create(rep);
    }

}
