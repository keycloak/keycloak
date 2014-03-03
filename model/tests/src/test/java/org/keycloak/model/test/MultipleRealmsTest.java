package org.keycloak.model.test;

import java.util.List;

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
    public void before() throws Exception {
        super.before();
        realm1 = identitySession.createRealm("id1", "realm1");
        realm2 = identitySession.createRealm("id2", "realm2");

        createObjects(realm1);
        createObjects(realm2);
    }

    @Test
    public void testUsers() {
        UserModel r1user1 = realm1.getUser("user1");
        UserModel r2user1 = realm2.getUser("user1");
        Assert.assertEquals(r1user1.getLoginName(), r2user1.getLoginName());
        Assert.assertNotEquals(r1user1.getId(), r2user1.getId());

        // Test password
        realm1.updateCredential(r1user1, UserCredentialModel.password("pass1"));
        realm2.updateCredential(r2user1, UserCredentialModel.password("pass2"));

        Assert.assertTrue(realm1.validatePassword(r1user1, "pass1"));
        Assert.assertFalse(realm1.validatePassword(r1user1, "pass2"));
        Assert.assertFalse(realm2.validatePassword(r2user1, "pass1"));
        Assert.assertTrue(realm2.validatePassword(r2user1, "pass2"));

        // Test searching
        Assert.assertEquals(2, realm1.searchForUser("user").size());

        realm1.removeUser("user1");
        realm1.removeUser("user2");
        Assert.assertEquals(0, realm1.searchForUser("user").size());
        Assert.assertEquals(2, realm2.searchForUser("user").size());
    }

    @Test
    public void testGetById() {
        Assert.assertEquals(realm1, identitySession.getRealm("id1"));
        Assert.assertEquals(realm1, identitySession.getRealmByName("realm1"));
        Assert.assertEquals(realm2, identitySession.getRealm("id2"));
        Assert.assertEquals(realm2, identitySession.getRealmByName("realm2"));

        ApplicationModel r1app1 = realm1.getApplicationByName("app1");
        ApplicationModel r1app2 = realm1.getApplicationByName("app2");
        ApplicationModel r2app1 = realm2.getApplicationByName("app1");
        ApplicationModel r2app2 = realm2.getApplicationByName("app2");

        Assert.assertEquals(r1app1, realm1.getApplicationById(r1app1.getId()));
        Assert.assertNull(realm2.getApplicationById(r1app1.getId()));

        OAuthClientModel r2cl1 = realm2.getOAuthClient("cl1");
        Assert.assertNull(realm1.getOAuthClientById(r2cl1.getId()));
        Assert.assertEquals(r2cl1.getId(), realm2.getOAuthClientById(r2cl1.getId()).getId());

        RoleModel r1App1Role = r1app1.getRole("app1Role1");
        Assert.assertEquals(r1App1Role, realm1.getRoleById(r1App1Role.getId()));

        RoleModel r2Role1 = realm2.getRole("role2");
        Assert.assertNull(realm1.getRoleById(r2Role1.getId()));
        Assert.assertEquals(r2Role1, realm2.getRoleById(r2Role1.getId()));
    }

    private void createObjects(RealmModel realm) {
        ApplicationModel app1 = realm.addApplication("app1");
        realm.addApplication("app2");

        realm.addUser("user1");
        realm.addUser("user2");

        realm.addRole("role1");
        realm.addRole("role2");

        app1.addRole("app1Role1");
        app1.addScope(realm.getRole("role1"));

        realm.addOAuthClient("cl1");
    }

}
