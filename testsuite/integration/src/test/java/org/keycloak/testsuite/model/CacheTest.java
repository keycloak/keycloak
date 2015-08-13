package org.keycloak.testsuite.model;

import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.*;
import org.keycloak.models.cache.infinispan.RealmAdapter;
import org.keycloak.testsuite.rule.KeycloakRule;

import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CacheTest {
    @ClassRule
    public static KeycloakRule kc = new KeycloakRule();

    @Test
    public void testStaleCache() throws Exception {
        String appId = null;
        {
            // load up cache
            KeycloakSession session = kc.startSession();
            RealmModel realm = session.realms().getRealmByName("test");
            ClientModel testApp = realm.getClientByClientId("test-app");
            assertNotNull(testApp);
            appId = testApp.getId();
            Assert.assertTrue(testApp.isEnabled());
            kc.stopSession(session, true);
        }
        {
            // update realm, then get an AppModel and change it.  The AppModel would not be a cache adapter
            KeycloakSession session = kc.startSession();

            // KEYCLOAK-1240 - obtain the realm via session.realms().getRealms()
            RealmModel realm = null;
            List<RealmModel> realms = session.realms().getRealms();
            for (RealmModel current : realms) {
                if ("test".equals(current.getName())) {
                    realm = current;
                    break;
                }
            }

            Assert.assertTrue(realm instanceof RealmAdapter);
            realm.setAccessCodeLifespanLogin(200);
            ClientModel testApp = realm.getClientByClientId("test-app");
            assertNotNull(testApp);
            testApp.setEnabled(false);
            kc.stopSession(session, true);
        }
        // make sure that app cache was flushed and enabled changed
        {
            KeycloakSession session = kc.startSession();
            RealmModel realm = session.realms().getRealmByName("test");
            Assert.assertEquals(200, realm.getAccessCodeLifespanLogin());
            ClientModel testApp = session.realms().getClientById(appId, realm);
            Assert.assertFalse(testApp.isEnabled());
            kc.stopSession(session, true);

        }


    }

    @Test
    public void testAddUserNotAddedToCache() {
        KeycloakSession session = kc.startSession();
        try {
            RealmModel realm = session.realms().getRealmByName("test");

            UserModel user = session.users().addUser(realm, "testAddUserNotAddedToCache");
            user.setFirstName("firstName");
            user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);

            UserSessionModel userSession = session.sessions().createUserSession(realm, user, "testAddUserNotAddedToCache", "127.0.0.1", "auth", false, null, null);
            UserModel user2 = userSession.getUser();

            user.setLastName("lastName");

            assertNotNull(user2.getLastName());
        } finally {
            session.getTransaction().commit();
            session.close();
        }
    }

}
