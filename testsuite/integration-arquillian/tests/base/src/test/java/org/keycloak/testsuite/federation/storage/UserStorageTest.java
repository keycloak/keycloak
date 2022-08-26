package org.keycloak.testsuite.federation.storage;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.common.util.reflections.Types;
import org.keycloak.credential.CredentialAuthentication;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.CacheableStorageProviderModel.CachePolicy;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.federation.UserMapStorage;
import org.keycloak.testsuite.federation.UserMapStorageFactory;
import org.keycloak.testsuite.federation.UserPropertyFileStorageFactory;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.TestCleanup;

import javax.mail.internet.MimeMessage;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.models.UserModel.RequiredAction.UPDATE_PROFILE;
import static org.keycloak.storage.UserStorageProviderModel.CACHE_POLICY;
import static org.keycloak.storage.UserStorageProviderModel.EVICTION_DAY;
import static org.keycloak.storage.UserStorageProviderModel.EVICTION_HOUR;
import static org.keycloak.storage.UserStorageProviderModel.EVICTION_MINUTE;
import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;
import static org.keycloak.storage.UserStorageProviderModel.MAX_LIFESPAN;
import static org.keycloak.testsuite.actions.RequiredActionEmailVerificationTest.getPasswordResetEmailLink;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author tkyjovsk
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
public class UserStorageTest extends AbstractAuthTest {

    private String memProviderId;
    private String propProviderROId;
    private String propProviderRWId;

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected LoginPage loginPage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected VerifyEmailPage verifyEmailPage;

    private static final File CONFIG_DIR = new File(System.getProperty("auth.server.config.dir", ""));

    @Before
    public void addProvidersBeforeTest() throws URISyntaxException, IOException {
        Assume.assumeTrue("User cache disabled.", isUserCacheEnabled());

        ComponentRepresentation memProvider = new ComponentRepresentation();
        memProvider.setName("memory");
        memProvider.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        memProvider.setProviderType(UserStorageProvider.class.getName());
        memProvider.setConfig(new MultivaluedHashMap<>());
        memProvider.getConfig().putSingle("priority", Integer.toString(0));
        memProvider.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(false));

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
    public void afterTestCleanUp() throws URISyntaxException, IOException {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            if (realm == null) {
                return;
            }

            UserModel user = session.users().getUserByUsername(realm, "thor");
            if (user != null) {
                UserStoragePrivateUtil.userLocalStorage(session).removeUser(realm, user);
                UserStorageUtil.userCache(session).clear();
            }

            //we need to clear userPasswords and userGroups from UserMapStorageFactory
            UserMapStorageFactory userMapStorageFactory = (UserMapStorageFactory) session.getKeycloakSessionFactory().getProviderFactory(UserStorageProvider.class, UserMapStorageFactory.PROVIDER_ID);
            Assert.assertNotNull(userMapStorageFactory);
            userMapStorageFactory.clear();
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

    private String addComponent(ComponentRepresentation component) {
        return addComponent(testRealmResource(), getCleanup(), component);
    }

    static String addComponent(RealmResource realmResource, TestCleanup testCleanup, ComponentRepresentation component) {
        Response resp = realmResource.components().add(component);
        resp.close();
        String id = ApiUtil.getCreatedId(resp);
        testCleanup.addComponentId(id);
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

    /**
     * KEYCLOAK-4013
     *
     * @throws Exception
     */
    @Test
    @ModelTest
    public void testCast(KeycloakSession session) throws Exception {
        session.getKeycloakSessionFactory().getProviderFactoriesStream(CredentialProvider.class)
                .filter(f -> Types.supports(CredentialAuthentication.class, f, CredentialProviderFactory.class))
                .map(f -> (CredentialAuthentication) session.getProvider(CredentialProvider.class, f.getId())).collect(Collectors.toList());
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

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testRegisterWithRequiredEmail() throws Exception {
        try (AutoCloseable c = new RealmAttributeUpdater(testRealmResource())
          .updateWith(r -> {
            Map<String, String> config = new HashMap<>();
            config.put("from", "auto@keycloak.org");
            config.put("host", "localhost");
            config.put("port", "3025");
            r.setSmtpServer(config);
            r.setRegistrationAllowed(true);
            r.setVerifyEmail(true);
          })
          .update()) {

            testRealmAccountPage.navigateTo();
            loginPage.clickRegister();
            registerPage.register("firstName", "lastName", "email@mail.com", "verifyEmail", "password", "password");

            verifyEmailPage.assertCurrent();

            Assert.assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];

            String verificationUrl = getPasswordResetEmailLink(message);

            driver.navigate().to(verificationUrl.trim());

            testRealmAccountPage.assertCurrent();
        }
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
        List<UserRepresentation> users = testRealmResource().users().search("tbrady", 0, -1);
        assertThat(users, hasSize(1));
        assertThat(users.get(0).getUsername(), equalTo("tbrady"));

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
        testingClient.server().run(session -> {
            System.out.println("search by single attribute");

            RealmModel realm = session.realms().getRealmByName("test");
            UserModel userModel = session.users().getUserByUsername(realm, "thor");
            userModel.setSingleAttribute("weapon", "hammer");

            List<UserModel> userModels = session.users().searchForUserByUserAttributeStream(realm, "weapon", "hammer")
                    .peek(System.out::println).collect(Collectors.toList());
            Assert.assertEquals(1, userModels.size());
            Assert.assertEquals("thor", userModels.get(0).getUsername());
        });
    }

    @Test
    public void testQueryExactMatch() {
        Assert.assertThat(testRealmResource().users().search("a", true), hasSize(0));
        Assert.assertThat(testRealmResource().users().search("apollo", true), hasSize(1));
        Assert.assertThat(testRealmResource().users().search("tbrady", true), hasSize(1));
    }

    private void setDailyEvictionTime(int hour, int minutes) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour == " + hour);
        }
        if (minutes < 0 || minutes > 59) {
            throw new IllegalArgumentException("minutes == " + minutes);
        }
        ComponentRepresentation propProviderRW = testRealmResource().components().component(propProviderRWId).toRepresentation();
        propProviderRW.getConfig().putSingle(CACHE_POLICY, CachePolicy.EVICT_DAILY.name());
        propProviderRW.getConfig().putSingle(EVICTION_HOUR, String.valueOf(hour));
        propProviderRW.getConfig().putSingle(EVICTION_MINUTE, String.valueOf(minutes));
        testRealmResource().components().component(propProviderRWId).update(propProviderRW);
    }


    /**
     * Test daily eviction behaviour
     */
    @Test
    public void testDailyEviction() {

        // We need to test both cases: eviction the same day, and eviction the next day
        // Simplest is to take full control of the clock

        // set clock to 23:30 of current day
        setTimeOfDay(23, 30, 0);

        // test same day eviction behaviour
        // set eviction at 23:45
        setDailyEvictionTime(23, 45);

        // there are users in cache already from before-test import
        // and they didn't use any time offset clock so they may have timestamps in the 'future'

        // let's clear cache
        testingClient.server().run(session -> {
            UserStorageUtil.userCache(session).clear();
        });


        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertTrue(user instanceof CachedUserModel); // should be newly cached
        });


        setTimeOfDay(23, 40, 0);

        // lookup user again - make sure it's returned from cache
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertTrue(user instanceof CachedUserModel); // should be returned from cache
        });


        setTimeOfDay(23, 50, 0);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertFalse(user instanceof CachedUserModel); // should have been invalidated
        });


        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertTrue(user instanceof CachedUserModel); // should have been newly cached
        });


        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertTrue(user instanceof CachedUserModel); // should be returned from cache
        });


        setTimeOfDay(23, 55, 0);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertTrue(user instanceof CachedUserModel); // should be returned from cache
        });


        // at 00:30
        // it's next day now. the daily eviction time is now in the future
        setTimeOfDay(0, 30, 0, 24 * 60 * 60);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertTrue(user instanceof CachedUserModel); // should be returned from cache - it's still good for almost the whole day
        });


        // at 23:30 next day
        setTimeOfDay(23, 30, 0, 24 * 60 * 60);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertTrue(user instanceof CachedUserModel); // should be returned from cache - it's still good until 23:45
        });

        // at 23:50
        setTimeOfDay(23, 50, 0, 24 * 60 * 60);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertFalse(user instanceof CachedUserModel); // should be invalidated
        });

        setTimeOfDay(23, 55, 0, 24 * 60 * 60);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertTrue(user instanceof CachedUserModel); // should be newly cached
        });


        setTimeOfDay(23, 40, 0, 2 * 24 * 60 * 60);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertTrue(user instanceof CachedUserModel); // should be returned from cache
        });

        setTimeOfDay(23, 50, 0, 2 * 24 * 60 * 60);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertFalse(user instanceof CachedUserModel); // should be invalidated
        });

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertTrue(user instanceof CachedUserModel); // should be newly cached
        });

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertTrue(user instanceof CachedUserModel); // should be returned from cache
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
            UserModel user = session.users().getUserByUsername(realm, "thor");
            System.out.println("User class: " + user.getClass());
            Assert.assertTrue(user instanceof CachedUserModel); // should still be cached
        });

        setTimeOffset(2 * 24 * 60 * 60); // 2 days in future

        // now
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            System.out.println("User class: " + user.getClass());
            Assert.assertTrue(user instanceof CachedUserModel); // should still be cached
        });

        setTimeOffset(5 * 24 * 60 * 60); // 5 days in future

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
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
            UserModel user = session.users().getUserByUsername(realm, "thor");
            System.out.println("User class: " + user.getClass());
            Assert.assertTrue(user instanceof CachedUserModel); // should still be cached
        });

        setTimeOffset(1/2 * 60 * 60); // 1/2 hour in future
        
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
            System.out.println("User class: " + user.getClass());
            Assert.assertTrue(user instanceof CachedUserModel); // should still be cached
        });

        setTimeOffset(2 * 60 * 60); // 2 hours in future

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "thor");
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
            UserModel user = session.users().getUserByUsername(realm, "thor");
            System.out.println("User class: " + user.getClass());
            Assert.assertFalse(user instanceof CachedUserModel); // should be evicted
        });


        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel thor2 = session.users().getUserByUsername(realm, "thor");
            Assert.assertFalse(thor2 instanceof CachedUserModel);
        });

        propProviderRW = testRealmResource().components().component(propProviderRWId).toRepresentation();
        propProviderRW.getConfig().putSingle(CACHE_POLICY, CachePolicy.DEFAULT.name());
        propProviderRW.getConfig().remove("evictionHour");
        propProviderRW.getConfig().remove("evictionMinute");
        propProviderRW.getConfig().remove("evictionDay");
        testRealmResource().components().component(propProviderRWId).update(propProviderRW);

        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel thor = session.users().getUserByUsername(realm, "thor");
            System.out.println("Foo");
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
            user = session.users().getUserByUsername(realm, "nonexistent");
            Assert.assertNull(user);

            Assert.assertEquals(1, UserMapStorage.allocations.get());
            Assert.assertEquals(0, UserMapStorage.closings.get());

            session.users().removeUser(realm,session.users().getUserByUsername(realm, "memuser"));
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


    @Test
    @ModelTest
    public void testCredentialCRUD(KeycloakSession session) throws Exception {
        AtomicReference<String> passwordId = new AtomicReference<>();
        AtomicReference<String> otp1Id = new AtomicReference<>();
        AtomicReference<String> otp2Id = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");

            UserModel user = currentSession.users().getUserByUsername(realm, "thor");
            Assert.assertFalse(StorageId.isLocalStorage(user));

            Stream<CredentialModel> credentials = user.credentialManager().getStoredCredentialsStream();
            org.keycloak.testsuite.Assert.assertEquals(0, credentials.count());

            // Create password
            CredentialModel passwordCred = PasswordCredentialModel.createFromValues("my-algorithm", "theSalt".getBytes(), 22, "ABC");
            passwordCred = user.credentialManager().createStoredCredential(passwordCred);
            passwordId.set(passwordCred.getId());

            // Create Password and 2 OTP credentials (password was already created)
            CredentialModel otp1 = OTPCredentialModel.createFromPolicy(realm, "secret1");
            CredentialModel otp2 = OTPCredentialModel.createFromPolicy(realm, "secret2");
            otp1 = user.credentialManager().createStoredCredential(otp1);
            otp2 = user.credentialManager().createStoredCredential(otp2);
            otp1Id.set(otp1.getId());
            otp2Id.set(otp2.getId());
        });


        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");
            UserModel user = currentSession.users().getUserByUsername(realm, "thor");

            // Assert priorities: password, otp1, otp2
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            assertOrder(list, passwordId.get(), otp1Id.get(), otp2Id.get());

            // Assert can't move password when newPreviousCredential not found
            assertFalse(user.credentialManager().moveStoredCredentialTo(passwordId.get(), "not-known"));

            // Assert can't move credential when not found
            assertFalse(user.credentialManager().moveStoredCredentialTo("not-known", otp2Id.get()));

            // Move otp2 up
            assertTrue(user.credentialManager().moveStoredCredentialTo(otp2Id.get(), passwordId.get()));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");
            UserModel user = currentSession.users().getUserByUsername(realm, "thor");

            // Assert priorities: password, otp2, otp1
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            assertOrder(list, passwordId.get(), otp2Id.get(), otp1Id.get());

            // Move otp2 to the top
            org.keycloak.testsuite.Assert.assertTrue(user.credentialManager().moveStoredCredentialTo(otp2Id.get(), null));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");
            UserModel user = currentSession.users().getUserByUsername(realm, "thor");

            // Assert priorities: otp2, password, otp1
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            assertOrder(list, otp2Id.get(), passwordId.get(), otp1Id.get());

            // Move password down
            assertTrue(user.credentialManager().moveStoredCredentialTo(passwordId.get(), otp1Id.get()));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");
            UserModel user = currentSession.users().getUserByUsername(realm, "thor");

            // Assert priorities: otp2, otp1, password
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            assertOrder(list, otp2Id.get(), otp1Id.get(), passwordId.get());

            // Remove otp2 down two positions
            assertTrue(user.credentialManager().moveStoredCredentialTo(otp2Id.get(), passwordId.get()));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");
            UserModel user = currentSession.users().getUserByUsername(realm, "thor");

            // Assert priorities: otp2, otp1, password
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            assertOrder(list, otp1Id.get(), passwordId.get(), otp2Id.get());

            // Remove password
            assertTrue(user.credentialManager().removeStoredCredentialById(passwordId.get()));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSession) -> {
            RealmModel realm = currentSession.realms().getRealmByName("test");
            UserModel user = currentSession.users().getUserByUsername(realm, "thor");

            // Assert priorities: otp2, password
            List<CredentialModel> list = user.credentialManager().getStoredCredentialsStream()
                    .collect(Collectors.toList());
            assertOrder(list, otp1Id.get(), otp2Id.get());
        });
    }

    @Test
    public void testCRUDCredentialsOfDifferentUser() {
        // Create OTP credential for user1 in the federated storage
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            UserModel user = session.users().getUserByUsername(realm, "thor");
            Assert.assertFalse(StorageId.isLocalStorage(user));

            CredentialModel otp1 = OTPCredentialModel.createFromPolicy(realm, "secret1");
            user.credentialManager().createStoredCredential(otp1);
        });

        UserResource user1 = ApiUtil.findUserByUsernameId(testRealmResource(), "thor");
        CredentialRepresentation otpCredential = user1.credentials().stream()
                .filter(credentialRep -> OTPCredentialModel.TYPE.equals(credentialRep.getType()))
                .findFirst()
                .get();

        // Test that when admin operates on user "user2", who is saved in user-storage, he can't update, move or remove credentials of different user "user1"
        UserResource user2 = ApiUtil.findUserByUsernameId(testRealmResource(), "tbrady");
        try {
            user2.setCredentialUserLabel(otpCredential.getId(), "new-label");
            Assert.fail("Not expected to successfully update user label");
        } catch (NotFoundException nfe) {
            // Expected
        }

        try {
            user2.moveCredentialToFirst(otpCredential.getId());
            Assert.fail("Not expected to successfully move credential");
        } catch (NotFoundException nfe) {
            // Expected
        }

        try {
            user2.removeCredential(otpCredential.getId());
            Assert.fail("Not expected to successfully remove credential");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Assert credential was not removed or updated
        CredentialRepresentation otpCredentialLoaded = user1.credentials().stream()
                .filter(credentialRep -> OTPCredentialModel.TYPE.equals(credentialRep.getType()))
                .findFirst()
                .get();
        Assert.assertTrue(ObjectUtil.isEqualOrBothNull(otpCredential.getUserLabel(), otpCredentialLoaded.getUserLabel()));
        Assert.assertTrue(ObjectUtil.isEqualOrBothNull(otpCredential.getPriority(), otpCredentialLoaded.getPriority()));
    }


    private void assertOrder(List<CredentialModel> creds, String... expectedIds) {
        org.keycloak.testsuite.Assert.assertEquals(expectedIds.length, creds.size());

        if (creds.size() == 0) return;

        for (int i=0 ; i<expectedIds.length ; i++) {
            org.keycloak.testsuite.Assert.assertEquals(creds.get(i).getId(), expectedIds[i]);
        }
    }

}
