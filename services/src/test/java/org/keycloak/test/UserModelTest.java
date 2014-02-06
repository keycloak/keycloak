package org.keycloak.test;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.services.managers.RealmManager;

import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserModelTest extends AbstractKeycloakTest {

    public UserModelTest(String providerId) {
        super(providerId);
    }

    @Test
    public void persistUser() {
        RealmModel realm = realmManager.createRealm("original");
        UserModel user = realm.addUser("user");
        user.setFirstName("first-name");
        user.setLastName("last-name");
        user.setEmail("email");

        user.addRedirectUri("redirect-1");
        user.addRedirectUri("redirect-2");

        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        user.addRequiredAction(RequiredAction.UPDATE_PASSWORD);

        user.addWebOrigin("origin-1");
        user.addWebOrigin("origin-2");

        UserModel persisted = realmManager.getRealm(realm.getId()).getUser("user");

        assertEquals(user, persisted);
    }
    
    @Test
    public void webOriginSetTest() {
        RealmModel realm = realmManager.createRealm("original");
        UserModel user = realm.addUser("user");

        Assert.assertTrue(user.getWebOrigins().isEmpty());

        user.addWebOrigin("origin-1");
        Assert.assertEquals(1, user.getWebOrigins().size());

        user.addWebOrigin("origin-2");
        Assert.assertEquals(2, user.getWebOrigins().size());

        user.removeWebOrigin("origin-2");
        Assert.assertEquals(1, user.getWebOrigins().size());

        user.removeWebOrigin("origin-1");
        Assert.assertTrue(user.getWebOrigins().isEmpty());
    }

    @Test
    public void testUserRequiredActions() throws Exception {
        RealmModel realm = realmManager.createRealm("original");
        UserModel user = realm.addUser("user");

        Assert.assertTrue(user.getRequiredActions().isEmpty());

        user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        String id = realm.getId();
        commit();
        realm = realmManager.getRealm(id);
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

    protected void commit() {
        identitySession.getTransaction().commit();
        identitySession.close();
        identitySession = factory.createSession();
        identitySession.getTransaction().begin();
        realmManager = new RealmManager(identitySession);
    }

    public static void assertEquals(UserModel expected, UserModel actual) {
        Assert.assertEquals(expected.getLoginName(), actual.getLoginName());
        Assert.assertEquals(expected.getFirstName(), actual.getFirstName());
        Assert.assertEquals(expected.getLastName(), actual.getLastName());
        Assert.assertArrayEquals(expected.getRedirectUris().toArray(), actual.getRedirectUris().toArray());
        Assert.assertArrayEquals(expected.getRequiredActions().toArray(), actual.getRequiredActions().toArray());
        Assert.assertArrayEquals(expected.getWebOrigins().toArray(), actual.getWebOrigins().toArray());
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

