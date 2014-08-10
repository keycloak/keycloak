package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserModelTest extends AbstractModelTest {

    @Test
    public void persistUser() {
        RealmModel realm = realmManager.createRealm("original");
        KeycloakSession session = realmManager.getSession();
        UserModel user = session.users().addUser(realm, "user");
        user.setFirstName("first-name");
        user.setLastName("last-name");
        user.setEmail("email");

        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        user.addRequiredAction(RequiredAction.UPDATE_PASSWORD);

        RealmModel searchRealm = realmManager.getRealm(realm.getId());
        UserModel persisted = session.users().getUserByUsername("user", searchRealm);

        assertEquals(user, persisted);

        searchRealm = realmManager.getRealm(realm.getId());
        UserModel persisted2 =  session.users().getUserById(user.getId(), searchRealm);
        assertEquals(user, persisted2);

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(UserModel.LAST_NAME, "last-name");
        List<UserModel> search = session.users().searchForUserByAttributes(attributes, realm);
        Assert.assertEquals(search.size(), 1);
        Assert.assertEquals(search.get(0).getUsername(), "user");

        attributes.clear();
        attributes.put(UserModel.EMAIL, "email");
        search = session.users().searchForUserByAttributes(attributes, realm);
        Assert.assertEquals(search.size(), 1);
        Assert.assertEquals(search.get(0).getUsername(), "user");

        attributes.clear();
        attributes.put(UserModel.LAST_NAME, "last-name");
        attributes.put(UserModel.EMAIL, "email");
        search = session.users().searchForUserByAttributes(attributes, realm);
        Assert.assertEquals(search.size(), 1);
        Assert.assertEquals(search.get(0).getUsername(), "user");
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
        UserModel user = session.users().addUser(realm, "user");

        Assert.assertTrue(user.getRequiredActions().isEmpty());

        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        String id = realm.getId();
        commit();
        realm = realmManager.getRealm(id);
        user = session.users().getUserByUsername("user", realm);

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP));

        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        user = session.users().getUserByUsername("user", realm);

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP));

        user.addRequiredAction(RequiredAction.VERIFY_EMAIL);
        user = session.users().getUserByUsername("user", realm);

        Assert.assertEquals(2, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP));
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL));

        user.removeRequiredAction(RequiredAction.CONFIGURE_TOTP);
        user = session.users().getUserByUsername("user", realm);

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL));

        user.removeRequiredAction(RequiredAction.VERIFY_EMAIL);
        user = session.users().getUserByUsername("user", realm);

        Assert.assertTrue(user.getRequiredActions().isEmpty());
    }

    public static void assertEquals(UserModel expected, UserModel actual) {
        Assert.assertEquals(expected.getUsername(), actual.getUsername());
        Assert.assertEquals(expected.getFirstName(), actual.getFirstName());
        Assert.assertEquals(expected.getLastName(), actual.getLastName());

        RequiredAction[] expectedRequiredActions = expected.getRequiredActions().toArray(new RequiredAction[expected.getRequiredActions().size()]);
        Arrays.sort(expectedRequiredActions);
        RequiredAction[] actualRequiredActions = actual.getRequiredActions().toArray(new RequiredAction[actual.getRequiredActions().size()]);
        Arrays.sort(actualRequiredActions);

        Assert.assertArrayEquals(expectedRequiredActions, actualRequiredActions);
    }

}

