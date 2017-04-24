package org.keycloak.testsuite.federation.storage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import static org.keycloak.models.UserModel.RequiredAction.UPDATE_PROFILE;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import static org.keycloak.storage.UserStorageProviderModel.CACHE_POLICY;
import org.keycloak.storage.UserStorageProviderModel.CachePolicy;
import static org.keycloak.storage.UserStorageProviderModel.EVICTION_DAY;
import static org.keycloak.storage.UserStorageProviderModel.EVICTION_HOUR;
import static org.keycloak.storage.UserStorageProviderModel.EVICTION_MINUTE;
import static org.keycloak.storage.UserStorageProviderModel.MAX_LIFESPAN;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.UserMapStorage;
import org.keycloak.testsuite.federation.UserMapStorageFactory;
import org.keycloak.testsuite.federation.UserPropertyFileStorageFactory;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author tkyjovsk
 */
public class UserStorageTest extends AbstractAuthTest {

    private String memProviderId;
    private String propProviderROId;
    private String propProviderRWId;

    private static final File CONFIG_DIR = new File(System.getProperty("auth.server.config.dir", ""));

    @Before
    public void addProvidersBeforeTest() throws URISyntaxException, IOException {
        ComponentRepresentation memProvider = new ComponentRepresentation();
        memProvider.setName("memory");
        memProvider.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        memProvider.setProviderType(UserStorageProvider.class.getName());
        memProvider.setConfig(new MultivaluedHashMap<>());
        memProvider.getConfig().putSingle("priority", Integer.toString(0));

        memProviderId = addComponent(memProvider);

        // copy files used by the following RO/RW user providers
        File stResDir = new File(getClass().getResource("/storage-test").toURI());
        if (stResDir.exists() && stResDir.isDirectory() && CONFIG_DIR.exists() && CONFIG_DIR.isDirectory()) {
            for (File f : stResDir.listFiles()) {
                log.infof("Copying %s to %s", f.getName(), CONFIG_DIR.getAbsolutePath());
                FileUtils.copyFileToDirectory(f, CONFIG_DIR);
            }
        } else {
            throw new RuntimeException("Property `auth.server.config.dir` must be set to run UserStorageTests.");
        }

        ComponentRepresentation propProviderRO = new ComponentRepresentation();
        propProviderRO.setName("read-only-user-props");
        propProviderRO.setProviderId(UserPropertyFileStorageFactory.PROVIDER_ID);
        propProviderRO.setProviderType(UserStorageProvider.class.getName());
        propProviderRO.setConfig(new MultivaluedHashMap<>());
        propProviderRO.getConfig().putSingle("priority", Integer.toString(1));
        propProviderRO.getConfig().putSingle("propertyFile",
                CONFIG_DIR.getAbsolutePath() + File.separator + "read-only-user-password.properties");

        propProviderROId = addComponent(propProviderRO);

        propProviderRWId = addComponent(newPropProviderRW());

    }

    @After
    public void removeTestUser() throws URISyntaxException, IOException {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            if (realm == null) {
                return;
            }

            UserModel user = session.users().getUserByUsername("thor", realm);
            if (user != null) {
                session.userLocalStorage().removeUser(realm, user);
                session.userCache().clear();
            }
        });
    }

    protected ComponentRepresentation newPropProviderRW() {
        ComponentRepresentation propProviderRW = new ComponentRepresentation();
        propProviderRW.setName("user-props");
        propProviderRW.setProviderId(UserPropertyFileStorageFactory.PROVIDER_ID);
        propProviderRW.setProviderType(UserStorageProvider.class.getName());
        propProviderRW.setConfig(new MultivaluedHashMap<>());
        propProviderRW.getConfig().putSingle("priority", Integer.toString(2));
        propProviderRW.getConfig().putSingle("propertyFile", CONFIG_DIR.getAbsolutePath() + File.separator + "user-password.properties");
        propProviderRW.getConfig().putSingle("federatedStorage", "true");
        return propProviderRW;
    }

    protected String addComponent(ComponentRepresentation component) {
        Response resp = testRealmResource().components().add(component);
        resp.close();
        String id = ApiUtil.getCreatedId(resp);
        getCleanup().addComponentId(id);
        return id;
    }

    private void loginSuccessAndLogout(String username, String password) {
        testRealmAccountPage.navigateTo();
        testRealmLoginPage.form().login(username, password);
        assertCurrentUrlStartsWith(testRealmAccountPage);
        testRealmAccountPage.logOut();
    }

    public void loginBadPassword(String username) {
        testRealmAccountPage.navigateTo();
        testRealmLoginPage.form().login(username, "badpassword");
        assertCurrentUrlDoesntStartWith(testRealmAccountPage);
    }

//    @Test
    public void listComponents() {
        log.info("COMPONENTS:");
        testRealmResource().components().query().forEach((c) -> {
            log.infof("%s - %s - %s", c.getId(), c.getProviderType(), c.getName());
        });
    }

    @Test
    public void testLoginSuccess() {
        loginSuccessAndLogout("tbrady", "goat");
        loginSuccessAndLogout("thor", "hammer");
        loginBadPassword("tbrady");
    }

    @Test
    public void testUpdate() {
        UserRepresentation thor = ApiUtil.findUserByUsername(testRealmResource(), "thor");

        // update entity
        thor.setFirstName("Stian");
        thor.setLastName("Thorgersen");
        thor.setEmailVerified(true);
        long thorCreated = System.currentTimeMillis() - 100;
        thor.setCreatedTimestamp(thorCreated);
        thor.setEmail("thor@hammer.com");
        thor.setAttributes(new HashMap<>());
        thor.getAttributes().put("test-attribute", Arrays.asList("value"));
        thor.setRequiredActions(new ArrayList<>());
        thor.getRequiredActions().add(UPDATE_PROFILE.name());
        testRealmResource().users().get(thor.getId()).update(thor);

        // check entity
        thor = ApiUtil.findUserByUsername(testRealmResource(), "thor");
        Assert.assertEquals("Stian", thor.getFirstName());
        Assert.assertEquals("Thorgersen", thor.getLastName());
        Assert.assertEquals("thor@hammer.com", thor.getEmail());
        Assert.assertTrue(thor.getAttributes().containsKey("test-attribute"));
        Assert.assertEquals(1, thor.getAttributes().get("test-attribute").size());
        Assert.assertEquals("value", thor.getAttributes().get("test-attribute").get(0));
        Assert.assertTrue(thor.isEmailVerified());

        // update group
        GroupRepresentation g = new GroupRepresentation();
        g.setName("my-group");
        String gid = ApiUtil.getCreatedId(testRealmResource().groups().add(g));

        testRealmResource().users().get(thor.getId()).joinGroup(gid);

        // check group
        boolean foundGroup = false;
        for (GroupRepresentation ug : testRealmResource().users().get(thor.getId()).groups()) {
            if (ug.getId().equals(gid)) {
                foundGroup = true;
            }
        }
        Assert.assertTrue(foundGroup);

        // check required actions
        assertTrue(thor.getRequiredActions().contains(UPDATE_PROFILE.name()));
        // remove req. actions
        thor.getRequiredActions().remove(UPDATE_PROFILE.name());
        testRealmResource().users().get(thor.getId()).update(thor);

        // change pass
        ApiUtil.resetUserPassword(testRealmResource().users().get(thor.getId()), "lightning", false);
        loginSuccessAndLogout("thor", "lightning");

        // update role
        RoleRepresentation r = new RoleRepresentation("foo-role", "foo role", false);
        testRealmResource().roles().create(r);
        ApiUtil.assignRealmRoles(testRealmResource(), thor.getId(), "foo-role");

        // check role
        boolean foundRole = false;
        for (RoleRepresentation rr : user(thor.getId()).roles().getAll().getRealmMappings()) {
            if ("foo-role".equals(rr.getName())) {
                foundRole = true;
                break;
            }
        }
        assertTrue(foundRole);

        // test removal of provider
        testRealmResource().components().component(propProviderRWId).remove();
        propProviderRWId = addComponent(newPropProviderRW());
        loginSuccessAndLogout("thor", "hammer");

        thor = ApiUtil.findUserByUsername(testRealmResource(), "thor");

        Assert.assertNull(thor.getFirstName());
        Assert.assertNull(thor.getLastName());
        Assert.assertNull(thor.getEmail());
        Assert.assertNull(thor.getAttributes());
        Assert.assertFalse(thor.isEmailVerified());

        foundGroup = false;
        for (GroupRepresentation ug : testRealmResource().users().get(thor.getId()).groups()) {
            if (ug.getId().equals(gid)) {
                foundGroup = true;
            }
        }
        Assert.assertFalse(foundGroup);

        foundRole = false;
        for (RoleRepresentation rr : user(thor.getId()).roles().getAll().getRealmMappings()) {
            if ("foo-role".equals(rr.getName())) {
                foundRole = true;
                break;
            }
        }
        assertFalse(foundRole);
    }

    public UserResource user(String userId) {
        return testRealmResource().users().get(userId);
    }

    @Test
    public void testRegistration() {
        UserRepresentation memuser = new UserRepresentation();
        memuser.setUsername("memuser");
        String uid = ApiUtil.createUserAndResetPasswordWithAdminClient(testRealmResource(), memuser, "password");
        loginSuccessAndLogout("memuser", "password");
        loginSuccessAndLogout("memuser", "password");
        loginSuccessAndLogout("memuser", "password");

        memuser = user(uid).toRepresentation();
        assertNotNull(memuser);
        assertNotNull(memuser.getOrigin());
        ComponentRepresentation origin = testRealmResource().components().component(memuser.getOrigin()).toRepresentation();
        Assert.assertEquals("memory", origin.getName());

        testRealmResource().users().get(memuser.getId()).remove();
        try {
            user(uid).toRepresentation(); // provider doesn't implement UserQueryProvider --> have to lookup by uid
            fail("`memuser` wasn't removed");
        } catch (NotFoundException nfe) {
            // expected
        }
    }

    @Test
    public void testQuery() {
        Set<UserRepresentation> queried = new HashSet<>();
        int first = 0;
        while (queried.size() < 8) {
            List<UserRepresentation> results = testRealmResource().users().search("", first, 3);
            log.debugf("first=%s, results: %s", first, results.size());
            if (results.isEmpty()) {
                break;
            }
            first += results.size();
            queried.addAll(results);
        }
        Set<String> usernames = new HashSet<>();
        for (UserRepresentation user : queried) {
            usernames.add(user.getUsername());
            log.info(user.getUsername());
        }
        Assert.assertEquals(8, queried.size());
        Assert.assertTrue(usernames.contains("thor"));
        Assert.assertTrue(usernames.contains("zeus"));
        Assert.assertTrue(usernames.contains("apollo"));
        Assert.assertTrue(usernames.contains("perseus"));
        Assert.assertTrue(usernames.contains("tbrady"));
        Assert.assertTrue(usernames.contains("rob"));
        Assert.assertTrue(usernames.contains("jules"));
        Assert.assertTrue(usernames.contains("danny"));

        // test searchForUser
        List<UserRepresentation> users = testRealmResource().users().search("tbrady", 0, Integer.MAX_VALUE);
        Assert.assertTrue(users.size() == 1);
        Assert.assertTrue(users.get(0).getUsername().equals("tbrady"));

        // test getGroupMembers()
        GroupRepresentation g = new GroupRepresentation();
        g.setName("gods");
        String gid = ApiUtil.getCreatedId(testRealmResource().groups().add(g));

        UserRepresentation user = ApiUtil.findUserByUsername(testRealmResource(), "apollo");
        testRealmResource().users().get(user.getId()).joinGroup(gid);
        user = ApiUtil.findUserByUsername(testRealmResource(), "zeus");
        testRealmResource().users().get(user.getId()).joinGroup(gid);
        user = ApiUtil.findUserByUsername(testRealmResource(), "thor");
        testRealmResource().users().get(user.getId()).joinGroup(gid);
        queried.clear();
        usernames.clear();

        first = 0;
        while (queried.size() < 8) {
            List<UserRepresentation> results = testRealmResource().groups().group(gid).members(first, 1);
            log.debugf("first=%s, results: %s", first, results.size());
            if (results.isEmpty()) {
                break;
            }
            first += results.size();
            queried.addAll(results);
        }
        for (UserRepresentation u : queried) {
            usernames.add(u.getUsername());
            log.info(u.getUsername());
        }
        Assert.assertEquals(3, queried.size());
        Assert.assertTrue(usernames.contains("apollo"));
        Assert.assertTrue(usernames.contains("zeus"));
        Assert.assertTrue(usernames.contains("thor"));

        // search by single attribute
        // FIXME - no equivalent for model in REST
    }

    @Deployment
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(UserResource.class)
                .addPackages(true, "org.keycloak.testsuite");
    }

    @Test
    public void testDailyEviction() {
        ApiUtil.findUserByUsername(testRealmResource(), "thor");

        // set eviction to 1 hour from now
        Calendar eviction = Calendar.getInstance();
        eviction.add(Calendar.HOUR, 1);
        ComponentRepresentation propProviderRW = testRealmResource().components().component(propProviderRWId).toRepresentation();
        propProviderRW.getConfig().putSingle(CACHE_POLICY, CachePolicy.EVICT_DAILY.name());
        propProviderRW.getConfig().putSingle(EVICTION_HOUR, Integer.toString(eviction.get(HOUR_OF_DAY)));
        propProviderRW.getConfig().putSingle(EVICTION_MINUTE, Integer.toString(eviction.get(MINUTE)));
        testRealmResource().components().component(propProviderRWId).update(propProviderRW);

        // now
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("thor", realm);
            System.out.println("User class: " + user.getClass());
            Assert.assertTrue(user instanceof CachedUserModel); // should still be cached
        });

        setTimeOffset(2 * 60 * 60); // 2 hours in future

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("thor", realm);
            System.out.println("User class: " + user.getClass());
            Assert.assertFalse(user instanceof CachedUserModel); // should be evicted
        });

    }

    @Test
    public void testWeeklyEviction() {
        ApiUtil.findUserByUsername(testRealmResource(), "thor");

        // set eviction to 4 days from now
        Calendar eviction = Calendar.getInstance();
        eviction.add(Calendar.HOUR, 4 * 24);
        ComponentRepresentation propProviderRW = testRealmResource().components().component(propProviderRWId).toRepresentation();
        propProviderRW.getConfig().putSingle(CACHE_POLICY, CachePolicy.EVICT_WEEKLY.name());
        propProviderRW.getConfig().putSingle(EVICTION_DAY, Integer.toString(eviction.get(DAY_OF_WEEK)));
        propProviderRW.getConfig().putSingle(EVICTION_HOUR, Integer.toString(eviction.get(HOUR_OF_DAY)));
        propProviderRW.getConfig().putSingle(EVICTION_MINUTE, Integer.toString(eviction.get(MINUTE)));
        testRealmResource().components().component(propProviderRWId).update(propProviderRW);

        // now
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("thor", realm);
            System.out.println("User class: " + user.getClass());
            Assert.assertTrue(user instanceof CachedUserModel); // should still be cached
        });

        setTimeOffset(2 * 24 * 60 * 60); // 2 days in future

        // now
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("thor", realm);
            System.out.println("User class: " + user.getClass());
            Assert.assertTrue(user instanceof CachedUserModel); // should still be cached
        });

        setTimeOffset(5 * 24 * 60 * 60); // 5 days in future

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("thor", realm);
            System.out.println("User class: " + user.getClass());
            Assert.assertFalse(user instanceof CachedUserModel); // should be evicted
        });

    }

    @Test
    public void testMaxLifespan() {
        ApiUtil.findUserByUsername(testRealmResource(), "thor");

        // set eviction to 1 hour from now
        ComponentRepresentation propProviderRW = testRealmResource().components().component(propProviderRWId).toRepresentation();
        propProviderRW.getConfig().putSingle(CACHE_POLICY, CachePolicy.MAX_LIFESPAN.name());
        propProviderRW.getConfig().putSingle(MAX_LIFESPAN, Long.toString(1 * 60 * 60 * 1000)); // 1 hour in milliseconds
        testRealmResource().components().component(propProviderRWId).update(propProviderRW);

        // now
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("thor", realm);
            System.out.println("User class: " + user.getClass());
            Assert.assertTrue(user instanceof CachedUserModel); // should still be cached
        });

        setTimeOffset(1/2 * 60 * 60); // 1/2 hour in future
        
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("thor", realm);
            System.out.println("User class: " + user.getClass());
            Assert.assertTrue(user instanceof CachedUserModel); // should still be cached
        });

        setTimeOffset(2 * 60 * 60); // 2 hours in future

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("thor", realm);
            System.out.println("User class: " + user.getClass());
            Assert.assertFalse(user instanceof CachedUserModel); // should be evicted
        });

    }

    @Test
    public void testNoCache() {
        ApiUtil.findUserByUsername(testRealmResource(), "thor");

        // set NO_CACHE policy
        ComponentRepresentation propProviderRW = testRealmResource().components().component(propProviderRWId).toRepresentation();
        propProviderRW.getConfig().putSingle(CACHE_POLICY, CachePolicy.NO_CACHE.name());
        testRealmResource().components().component(propProviderRWId).update(propProviderRW);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("thor", realm);
            System.out.println("User class: " + user.getClass());
            Assert.assertFalse(user instanceof CachedUserModel); // should be evicted
        });
    }

    @Test
    public void testLifecycle() {

        testingClient.server().run(session -> {
            UserMapStorage.allocations.set(0);
            UserMapStorage.closings.set(0);

            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().addUser(realm, "memuser");
            Assert.assertNotNull(user);
            user = session.users().getUserByUsername("nonexistent", realm);
            Assert.assertNull(user);

            Assert.assertEquals(1, UserMapStorage.allocations.get());
            Assert.assertEquals(0, UserMapStorage.closings.get());
        });

        testingClient.server().run(session -> {
            Assert.assertEquals(1, UserMapStorage.allocations.get());
            Assert.assertEquals(1, UserMapStorage.closings.get());
        });

    }

    @Test
    public void testEntityRemovalHooks() {
        testingClient.server().run(session -> {
            UserMapStorage.realmRemovals.set(0);
            UserMapStorage.groupRemovals.set(0);
            UserMapStorage.roleRemovals.set(0);
        });

        // remove group
        GroupRepresentation g1 = new GroupRepresentation();
        g1.setName("group1");
        GroupRepresentation g2 = new GroupRepresentation();
        g2.setName("group2");
        String gid1 = ApiUtil.getCreatedId(testRealmResource().groups().add(g1));
        String gid2 = ApiUtil.getCreatedId(testRealmResource().groups().add(g2));
        testRealmResource().groups().group(gid1).remove();
        testRealmResource().groups().group(gid2).remove();
        testingClient.server().run(session -> {
            Assert.assertEquals(2, UserMapStorage.groupRemovals.get());
            UserMapStorage.realmRemovals.set(0);
        });

        // remove role
        RoleRepresentation role1 = new RoleRepresentation();
        role1.setName("role1");
        RoleRepresentation role2 = new RoleRepresentation();
        role2.setName("role2");
        testRealmResource().roles().create(role1);
        testRealmResource().roles().create(role2);
        testRealmResource().roles().get("role1").remove();
        testRealmResource().roles().get("role2").remove();
        testingClient.server().run(session -> {
            Assert.assertEquals(2, UserMapStorage.roleRemovals.get());
            UserMapStorage.realmRemovals.set(0);
        });

        // remove realm
        RealmRepresentation testRealmRepresentation = testRealmResource().toRepresentation();
        testRealmResource().remove();
        testingClient.server().run(session -> {
            Assert.assertEquals(1, UserMapStorage.realmRemovals.get());
            UserMapStorage.realmRemovals.set(0);
        });

        // Re-create realm
        RealmRepresentation repOrig = testContext.getTestRealmReps().get(0);
        adminClient.realms().create(repOrig);
    }

    @Test
    @Ignore
    public void testEntityRemovalHooksCascade() {
        testingClient.server().run(session -> {
            UserMapStorage.realmRemovals.set(0);
            UserMapStorage.groupRemovals.set(0);
            UserMapStorage.roleRemovals.set(0);
        });

        GroupRepresentation g1 = new GroupRepresentation();
        g1.setName("group1");
        GroupRepresentation g2 = new GroupRepresentation();
        g2.setName("group2");
        String gid1 = ApiUtil.getCreatedId(testRealmResource().groups().add(g1));
        String gid2 = ApiUtil.getCreatedId(testRealmResource().groups().add(g2));

        RoleRepresentation role1 = new RoleRepresentation();
        role1.setName("role1");
        RoleRepresentation role2 = new RoleRepresentation();
        role2.setName("role2");
        testRealmResource().roles().create(role1);
        testRealmResource().roles().create(role2);

        // remove realm with groups and roles in it
        testRealmResource().remove();
        testingClient.server().run(session -> {
            Assert.assertEquals(1, UserMapStorage.realmRemovals.get());
            Assert.assertEquals(2, UserMapStorage.groupRemovals.get()); // check if group removal hooks were called
            Assert.assertEquals(2, UserMapStorage.roleRemovals.get()); // check if role removal hooks were called
        });

    }

}
