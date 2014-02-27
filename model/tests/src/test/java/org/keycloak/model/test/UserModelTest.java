package org.keycloak.model.test;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserModelTest extends AbstractModelTest {

    @Test
    public void persistUser() {
        RealmModel realm = realmManager.createRealm("original");
        UserModel user = realm.addUser("user");
        user.setFirstName("first-name");
        user.setLastName("last-name");
        user.setEmail("email");

        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        user.addRequiredAction(RequiredAction.UPDATE_PASSWORD);

        UserModel persisted = realmManager.getRealm(realm.getId()).getUser("user");

        assertEquals(user, persisted);

        UserModel persisted2 = realmManager.getRealm(realm.getId()).getUserById(user.getId());
        assertEquals(user, persisted2);
    }
    
    @Test
    public void webOriginSetTest() {
        RealmModel realm = realmManager.createRealm("original");
        ClientModel client = realm.addApplication("user");

        Assert.assertTrue(client.getWebOrigins().isEmpty());

        client.addWebOrigin("origin-1");
        Assert.assertEquals(1, client.getWebOrigins().size());

        client.addWebOrigin("origin-2");
        Assert.assertEquals(2, client.getWebOrigins().size());

        client.removeWebOrigin("origin-2");
        Assert.assertEquals(1, client.getWebOrigins().size());

        client.removeWebOrigin("origin-1");
        Assert.assertTrue(client.getWebOrigins().isEmpty());

        client = realm.addOAuthClient("oauthclient2");

        Assert.assertTrue(client.getWebOrigins().isEmpty());

        client.addWebOrigin("origin-1");
        Assert.assertEquals(1, client.getWebOrigins().size());

        client.addWebOrigin("origin-2");
        Assert.assertEquals(2, client.getWebOrigins().size());

        client.removeWebOrigin("origin-2");
        Assert.assertEquals(1, client.getWebOrigins().size());

        client.removeWebOrigin("origin-1");
        Assert.assertTrue(client.getWebOrigins().isEmpty());

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

    public static void assertEquals(UserModel expected, UserModel actual) {
        Assert.assertEquals(expected.getLoginName(), actual.getLoginName());
        Assert.assertEquals(expected.getFirstName(), actual.getFirstName());
        Assert.assertEquals(expected.getLastName(), actual.getLastName());
        Assert.assertArrayEquals(expected.getRequiredActions().toArray(), actual.getRequiredActions().toArray());
    }

}

