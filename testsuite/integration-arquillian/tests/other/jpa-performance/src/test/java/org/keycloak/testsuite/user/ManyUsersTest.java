package org.keycloak.testsuite.user;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.Timer;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;
import org.keycloak.admin.client.resource.RealmResource;
import static org.keycloak.testsuite.util.IOUtil.PROJECT_BUILD_DIRECTORY;
import static org.junit.Assert.fail;

/**
 *
 * @author tkyjovsk
 */
public class ManyUsersTest extends AbstractUserTest {

    private static final int COUNT = Integer.parseInt(System.getProperty("many.users.count", "10000"));
    private static final int BATCH = Integer.parseInt(System.getProperty("many.users.batch", "1000"));
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
        users = new LinkedList<>();
        for (int i = 0; i < COUNT; i++) {
            users.add(createUserRep("user" + i));
        }

        realmTimer.reset("create realm before test");
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM);
        realmsResouce().create(realm);

        refreshToken();
    }

    @After
    public void after() {
        realmTimer.clearStats(true, true, false);
        usersTimer.clearStats();
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
            createUser(realmResource().users(), user);
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
            users = realmResource().users().search("", 0, Integer.MAX_VALUE);

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

}
