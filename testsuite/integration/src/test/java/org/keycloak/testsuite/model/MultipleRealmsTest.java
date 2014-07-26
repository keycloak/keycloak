package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MultipleRealmsTest extends AbstractModelTest {

    private RealmModel realm1;
    private RealmModel realm2;

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        realm1 = realmManager.createRealm("id1", "realm1");
        realm2 = realmManager.createRealm("id2", "realm2");

        createObjects(realm1);
        createObjects(realm2);
    }

    @Test
    public void testUsers() {
        UserModel r1user1 = session.users().getUserByUsername("user1", realm1);
        UserModel r2user1 = session.users().getUserByUsername("user1", realm2);
        Assert.assertEquals(r1user1.getUsername(), r2user1.getUsername());
        Assert.assertNotEquals(r1user1.getId(), r2user1.getId());

        // Test password
        r1user1.updateCredential(UserCredentialModel.password("pass1"));
        r2user1.updateCredential(UserCredentialModel.password("pass2"));

        Assert.assertTrue(session.users().validCredentials(realm1, r1user1, UserCredentialModel.password("pass1")));
        Assert.assertFalse(session.users().validCredentials(realm1, r1user1, UserCredentialModel.password("pass2")));
        Assert.assertFalse(session.users().validCredentials(realm2, r2user1, UserCredentialModel.password("pass1")));
        Assert.assertTrue(session.users().validCredentials(realm2, r2user1, UserCredentialModel.password("pass2")));

        // Test searching
        Assert.assertEquals(2, session.users().searchForUser("user", realm1).size());

        commit();
        realm1 = model.getRealm("id1");
        realm2 = model.getRealm("id2");

        session.users().removeUser(realm1, r1user1);
        UserModel user2 = session.users().getUserByUsername("user2", realm1);
        session.users().removeUser(realm1, user2);
        Assert.assertEquals(0, session.users().searchForUser("user", realm1).size());
        Assert.assertEquals(2, session.users().searchForUser("user", realm2).size());
    }

    @Test
    public void testGetById() {
        Assert.assertEquals(realm1, model.getRealm("id1"));
        Assert.assertEquals(realm1, model.getRealmByName("realm1"));
        Assert.assertEquals(realm2, model.getRealm("id2"));
        Assert.assertEquals(realm2, model.getRealmByName("realm2"));

        ApplicationModel r1app1 = realm1.getApplicationByName("app1");
        ApplicationModel r1app2 = realm1.getApplicationByName("app2");
        ApplicationModel r2app1 = realm2.getApplicationByName("app1");
        ApplicationModel r2app2 = realm2.getApplicationByName("app2");

        Assert.assertEquals(r1app1, realm1.getApplicationById(r1app1.getId()));
        Assert.assertNull(realm2.getApplicationById(r1app1.getId()));

        OAuthClientModel r2cl1 = realm2.getOAuthClient("cl1");
        Assert.assertEquals(r2cl1.getId(), realm2.getOAuthClientById(r2cl1.getId()).getId());
        Assert.assertNull(realm1.getOAuthClientById(r2cl1.getId()));

        RoleModel r1App1Role = r1app1.getRole("app1Role1");
        Assert.assertEquals(r1App1Role, realm1.getRoleById(r1App1Role.getId()));
        Assert.assertNull(realm2.getRoleById(r1App1Role.getId()));

        RoleModel r2Role1 = realm2.getRole("role2");
        Assert.assertNull(realm1.getRoleById(r2Role1.getId()));
        Assert.assertEquals(r2Role1, realm2.getRoleById(r2Role1.getId()));
    }

    private void createObjects(RealmModel realm) {
        ApplicationModel app1 = realm.addApplication("app1");
        realm.addApplication("app2");

        realmManager.getSession().users().addUser(realm, "user1");
        realmManager.getSession().users().addUser(realm, "user2");

        realm.addRole("role1");
        realm.addRole("role2");

        app1.addRole("app1Role1");
        app1.addScopeMapping(realm.getRole("role1"));

        realm.addOAuthClient("cl1");
    }

}
