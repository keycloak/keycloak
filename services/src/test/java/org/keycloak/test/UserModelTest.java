package org.keycloak.test;

import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserModelTest extends AbstractKeycloakServerTest {
    private KeycloakSessionFactory factory;
    private KeycloakSession identitySession;
    private RealmManager manager;

    @Before
    public void before() throws Exception {
        factory = KeycloakApplication.buildSessionFactory();
        identitySession = factory.createSession();
        identitySession.getTransaction().begin();
        manager = new RealmManager(identitySession);
    }

    @After
    public void after() throws Exception {
        identitySession.getTransaction().commit();
        identitySession.close();
        factory.close();
    }

    @Test
    public void persistUser() {
        RealmModel realm = manager.createRealm("original");
        UserModel user = realm.addUser("user");
        user.setFirstName("first-name");
        user.setLastName("last-name");
        user.setEmail("email");

        user.addRedirectUri("redirect-1");
        user.addRedirectUri("redirect-2");

        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        user.addRequiredAction(RequiredAction.UPDATE_PASSWORD);

        UserModel persisted = manager.getRealm(realm.getId()).getUser("user");

        assertEquals(user, persisted);
    }

    @Test
    public void testUserRequiredActions() throws Exception {
        RealmModel realm = manager.createRealm("original");
        UserModel user = realm.addUser("user");

        Assert.assertTrue(user.getRequiredActions().isEmpty());

        user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        user = realm.getUser("user");

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP));

        user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        user = realm.getUser("user");

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP));

        user.addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
        user = realm.getUser("user");

        Assert.assertEquals(2, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP));
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL));

        user.removeRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        user = realm.getUser("user");

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL));

        user.removeRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
        user = realm.getUser("user");

        Assert.assertTrue(user.getRequiredActions().isEmpty());
    }

    public static void assertEquals(UserModel expected, UserModel actual) {
        Assert.assertEquals(expected.getLoginName(), actual.getLoginName());
        Assert.assertEquals(expected.getFirstName(), actual.getFirstName());
        Assert.assertEquals(expected.getLastName(), actual.getLastName());
        Assert.assertArrayEquals(expected.getRedirectUris().toArray(), actual.getRedirectUris().toArray());
        Assert.assertArrayEquals(expected.getRequiredActions().toArray(), actual.getRequiredActions().toArray());

    }

    public static void assertEquals(List<RoleModel> expected, List<RoleModel> actual) {
        Assert.assertEquals(expected.size(), actual.size());
        Iterator<RoleModel> exp = expected.iterator();
        Iterator<RoleModel> act = actual.iterator();
        while (exp.hasNext()) {
            Assert.assertEquals(exp.next().getName(), act.next().getName());
        }
    }

}

