package org.keycloak.model.test;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.OAuthClientManager;
import org.keycloak.services.managers.RealmManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdapterTest extends AbstractModelTest {
    private RealmModel realmModel;

    @Test
    public void installTest() throws Exception {
        new ApplianceBootstrap().bootstrap(identitySession);

    }

    @Test
    public void test1CreateRealm() throws Exception {
        realmModel = realmManager.createRealm("JUGGLER");
        realmModel.setAccessCodeLifespan(100);
        realmModel.setAccessCodeLifespanUserAction(600);
        realmModel.setEnabled(true);
        realmModel.setName("JUGGLER");
        realmModel.setPrivateKeyPem("0234234");
        realmModel.setPublicKeyPem("0234234");
        realmModel.setTokenLifespan(1000);
        realmModel.setUpdateProfileOnInitialSocialLogin(true);
        realmModel.addDefaultRole("foo");

        System.out.println(realmModel.getId());
        realmModel = realmManager.getRealm(realmModel.getId());
        Assert.assertNotNull(realmModel);
        Assert.assertEquals(realmModel.getAccessCodeLifespan(), 100);
        Assert.assertEquals(600, realmModel.getAccessCodeLifespanUserAction());
        Assert.assertEquals(realmModel.getTokenLifespan(), 1000);
        Assert.assertEquals(realmModel.isEnabled(), true);
        Assert.assertEquals(realmModel.getName(), "JUGGLER");
        Assert.assertEquals(realmModel.getPrivateKeyPem(), "0234234");
        Assert.assertEquals(realmModel.getPublicKeyPem(), "0234234");
        Assert.assertEquals(realmModel.isUpdateProfileOnInitialSocialLogin(), true);
        Assert.assertEquals(1, realmModel.getDefaultRoles().size());
        Assert.assertEquals("foo", realmModel.getDefaultRoles().get(0));
    }

    @Test
    public void testRealmListing() throws Exception {
        realmModel = realmManager.createRealm("JUGGLER");
        realmModel.setAccessCodeLifespan(100);
        realmModel.setAccessCodeLifespanUserAction(600);
        realmModel.setEnabled(true);
        realmModel.setName("JUGGLER");
        realmModel.setPrivateKeyPem("0234234");
        realmModel.setPublicKeyPem("0234234");
        realmModel.setTokenLifespan(1000);
        realmModel.setUpdateProfileOnInitialSocialLogin(true);
        realmModel.addDefaultRole("foo");

        System.out.println(realmModel.getId());
        realmModel = realmManager.getRealm(realmModel.getId());
        Assert.assertNotNull(realmModel);
        Assert.assertEquals(realmModel.getAccessCodeLifespan(), 100);
        Assert.assertEquals(600, realmModel.getAccessCodeLifespanUserAction());
        Assert.assertEquals(realmModel.getTokenLifespan(), 1000);
        Assert.assertEquals(realmModel.isEnabled(), true);
        Assert.assertEquals(realmModel.getName(), "JUGGLER");
        Assert.assertEquals(realmModel.getPrivateKeyPem(), "0234234");
        Assert.assertEquals(realmModel.getPublicKeyPem(), "0234234");
        Assert.assertEquals(realmModel.isUpdateProfileOnInitialSocialLogin(), true);
        Assert.assertEquals(1, realmModel.getDefaultRoles().size());
        Assert.assertEquals("foo", realmModel.getDefaultRoles().get(0));

        String id = realmModel.getId();
        System.out.println("id: " + id);

        commit();
        List<RealmModel> realms = identitySession.getRealms(null);
        System.out.println("num realms: " + realms.size());
        Assert.assertEquals(realms.size(), 1);
    }


    @Test
    public void test2RequiredCredential() throws Exception {
        test1CreateRealm();
        realmModel.addRequiredCredential(CredentialRepresentation.PASSWORD);
        List<RequiredCredentialModel> storedCreds = realmModel.getRequiredCredentials();
        Assert.assertEquals(1, storedCreds.size());

        Set<String> creds = new HashSet<String>();
        creds.add(CredentialRepresentation.PASSWORD);
        creds.add(CredentialRepresentation.TOTP);
        realmModel.updateRequiredCredentials(creds);
        storedCreds = realmModel.getRequiredCredentials();
        Assert.assertEquals(2, storedCreds.size());
        boolean totp = false;
        boolean password = false;
        for (RequiredCredentialModel cred : storedCreds) {
            Assert.assertTrue(cred.isInput());
            if (cred.getType().equals(CredentialRepresentation.PASSWORD)) {
                password = true;
                Assert.assertTrue(cred.isSecret());
            } else if (cred.getType().equals(CredentialRepresentation.TOTP)) {
                totp = true;
                Assert.assertFalse(cred.isSecret());
            }
        }
        Assert.assertTrue(totp);
        Assert.assertTrue(password);
    }

    @Test
    public void testCredentialValidation() throws Exception {
        test1CreateRealm();
        UserModel user = realmModel.addUser("bburke");
        UserCredentialModel cred = new UserCredentialModel();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue("geheim");
        realmModel.updateCredential(user, cred);
        Assert.assertTrue(realmModel.validatePassword(user, "geheim"));
    }

    @Test
    public void testOAuthClient() throws Exception {
        test1CreateRealm();

        OAuthClientModel oauth = new OAuthClientManager(realmModel).create("oauth-client");
        oauth = realmModel.getOAuthClient("oauth-client");
        Assert.assertTrue(realmModel.hasRole(oauth.getOAuthAgent(), realmModel.getRole(Constants.IDENTITY_REQUESTER_ROLE)));


    }

    @Test
    public void testDeleteUser() throws Exception {
        test1CreateRealm();

        UserModel user = realmModel.addUser("bburke");
        user.setAttribute("attr1", "val1");
        user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

        RoleModel testRole = realmModel.addRole("test");
        realmModel.grantRole(user, testRole);

        ApplicationModel app = realmModel.addApplication("test-app");
        RoleModel appRole = app.addRole("test");
        realmModel.grantRole(user, appRole);

        SocialLinkModel socialLink = new SocialLinkModel("google", user.getLoginName());
        realmModel.addSocialLink(user, socialLink);

        UserCredentialModel cred = new UserCredentialModel();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue("password");
        realmModel.updateCredential(user, cred);

        Assert.assertTrue(realmModel.removeUser("bburke"));
        Assert.assertFalse(realmModel.removeUser("bburke"));
        Assert.assertNull(realmModel.getUser("bburke"));
    }

    @Test
    public void testRemoveApplication() throws Exception {
        test1CreateRealm();

        UserModel user = realmModel.addUser("bburke");

        OAuthClientModel client = realmModel.addOAuthClient("client");

        ApplicationModel app = realmModel.addApplication("test-app");

        RoleModel appRole = app.addRole("test");
        realmModel.grantRole(user, appRole);
        realmModel.addScopeMapping(client.getOAuthAgent(), appRole);

        RoleModel realmRole = realmModel.addRole("test");
        realmModel.addScopeMapping(app.getApplicationUser(), realmRole);

        Assert.assertTrue(realmModel.removeApplication(app.getId()));
        Assert.assertFalse(realmModel.removeApplication(app.getId()));
        Assert.assertNull(realmModel.getApplicationById(app.getId()));
    }


    @Test
    public void testRemoveRealm() throws Exception {
        test1CreateRealm();

        UserModel user = realmModel.addUser("bburke");

        UserCredentialModel cred = new UserCredentialModel();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue("password");
        realmModel.updateCredential(user, cred);

        OAuthClientModel client = realmModel.addOAuthClient("client");

        ApplicationModel app = realmModel.addApplication("test-app");

        RoleModel appRole = app.addRole("test");
        realmModel.grantRole(user, appRole);
        realmModel.addScopeMapping(client.getOAuthAgent(), appRole);

        RoleModel realmRole = realmModel.addRole("test");
        realmModel.addScopeMapping(app.getApplicationUser(), realmRole);

        Assert.assertTrue(identitySession.removeRealm(realmModel.getId()));
        Assert.assertFalse(identitySession.removeRealm(realmModel.getId()));
        Assert.assertNull(identitySession.getRealm(realmModel.getId()));
    }


    @Test
    public void testRemoveRole() throws Exception {
        test1CreateRealm();

        UserModel user = realmModel.addUser("bburke");

        OAuthClientModel client = realmModel.addOAuthClient("client");

        ApplicationModel app = realmModel.addApplication("test-app");

        RoleModel appRole = app.addRole("test");
        realmModel.grantRole(user, appRole);
        realmModel.addScopeMapping(client.getOAuthAgent(), appRole);

        RoleModel realmRole = realmModel.addRole("test");
        realmModel.addScopeMapping(app.getApplicationUser(), realmRole);

        Assert.assertTrue(realmModel.removeRoleById(realmRole.getId()));
        Assert.assertFalse(realmModel.removeRoleById(realmRole.getId()));
        Assert.assertNull(realmModel.getRole(realmRole.getName()));

        Assert.assertTrue(app.removeRoleById(appRole.getId()));
        Assert.assertFalse(app.removeRoleById(appRole.getId()));
        Assert.assertNull(app.getRole(appRole.getName()));
    }

    @Test
    public void testUserSearch() throws Exception {
        test1CreateRealm();
        {
            UserModel user = realmModel.addUser("bburke");
            user.setLastName("Burke");
            user.setFirstName("Bill");
            user.setEmail("bburke@redhat.com");

            UserModel user2 = realmModel.addUser("doublefirst");
            user2.setFirstName("Knut Ole");
            user2.setLastName("Alver");
            user2.setEmail("knut@redhat.com");

            UserModel user3 = realmModel.addUser("doublelast");
            user3.setFirstName("Ole");
            user3.setLastName("Alver Veland");
            user3.setEmail("knut@redhat.com");
        }

        RealmManager adapter = realmManager;

        {
            List<UserModel> userModels = adapter.searchUsers("total junk query", realmModel);
            Assert.assertEquals(userModels.size(), 0);
        }

        {
            List<UserModel> userModels = adapter.searchUsers("Bill Burke", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("bill burk", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            ArrayList<String> users = new ArrayList<String>();
            for (UserModel u : adapter.searchUsers("ole alver", realmModel)) {
                users.add(u.getLoginName());
            }
            String[] usernames = users.toArray(new String[users.size()]);
            Arrays.sort(usernames);
            Assert.assertArrayEquals(new String[] { "doublefirst", "doublelast"}, usernames);
        }

        {
            List<UserModel> userModels = adapter.searchUsers("bburke@redhat.com", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("rke@redhat.com", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("bburke", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("BurK", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("Burke", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Bill");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "bburke@redhat.com");
        }

        {
            UserModel user = realmModel.addUser("mburke");
            user.setLastName("Burke");
            user.setFirstName("Monica");
            user.setEmail("mburke@redhat.com");
        }

        {
            UserModel user = realmModel.addUser("thor");
            user.setLastName("Thorgersen");
            user.setFirstName("Stian");
            user.setEmail("thor@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("Monica Burke", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Monica");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "mburke@redhat.com");
        }


        {
            List<UserModel> userModels = adapter.searchUsers("mburke@redhat.com", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Monica");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "mburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("mburke", realmModel);
            Assert.assertEquals(userModels.size(), 1);
            UserModel bburke = userModels.get(0);
            Assert.assertEquals(bburke.getFirstName(), "Monica");
            Assert.assertEquals(bburke.getLastName(), "Burke");
            Assert.assertEquals(bburke.getEmail(), "mburke@redhat.com");
        }

        {
            List<UserModel> userModels = adapter.searchUsers("Burke", realmModel);
            Assert.assertEquals(userModels.size(), 2);
            UserModel first = userModels.get(0);
            UserModel second = userModels.get(1);
            if (!first.getEmail().equals("bburke@redhat.com") && !second.getEmail().equals("bburke@redhat.com")) {
                Assert.fail();
            }
            if (!first.getEmail().equals("mburke@redhat.com") && !second.getEmail().equals("mburke@redhat.com")) {
                Assert.fail();
            }
        }

        RealmModel otherRealm = adapter.createRealm("other");
        otherRealm.addUser("bburke");

        Assert.assertEquals(2, otherRealm.getUsers().size());
        Assert.assertEquals(1, otherRealm.searchForUser("bu").size());
    }


    @Test
    public void testRoles() throws Exception {
        test1CreateRealm();
        realmModel.addRole("admin");
        realmModel.addRole("user");
        Set<RoleModel> roles = realmModel.getRoles();
        Assert.assertEquals(5, roles.size());
        UserModel user = realmModel.addUser("bburke");
        RoleModel role = realmModel.getRole("user");
        realmModel.grantRole(user, role);
        Assert.assertTrue(realmModel.hasRole(user, role));
        System.out.println("Role id: " + role.getId());
        role = realmModel.getRoleById(role.getId());
        Assert.assertNotNull(role);
        Assert.assertEquals("user", role.getName());
    }

}
