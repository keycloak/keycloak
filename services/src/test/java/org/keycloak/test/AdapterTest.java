package org.keycloak.test;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.models.*;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.OAuthClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.test.common.AbstractKeycloakTest;
import org.keycloak.test.common.SessionFactoryTestContext;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdapterTest extends AbstractKeycloakTest {
    private RealmModel realmModel;

    public AdapterTest(SessionFactoryTestContext testContext) {
        super(testContext);
    }

    @Test
    public void installTest() throws Exception {
        new ApplianceBootstrap().bootstrap(identitySession);

    }

    @Test
    public void testMe() {
        String hello = "Bill     Burke";
        StringTokenizer tokenizer = new StringTokenizer(hello, " ");
        while (tokenizer.hasMoreTokens()) {
            System.out.println("token: " + tokenizer.nextToken());
        }
    }


    @Test
    public void test1CreateRealm() throws Exception {
        realmModel = getRealmManager().createRealm("JUGGLER");
        realmModel.setAccessCodeLifespan(100);
        realmModel.setAccessCodeLifespanUserAction(600);
        realmModel.setCookieLoginAllowed(true);
        realmModel.setEnabled(true);
        realmModel.setName("JUGGLER");
        realmModel.setPrivateKeyPem("0234234");
        realmModel.setPublicKeyPem("0234234");
        realmModel.setTokenLifespan(1000);
        realmModel.setAutomaticRegistrationAfterSocialLogin(true);
        realmModel.addDefaultRole("foo");

        System.out.println(realmModel.getId());
        realmModel = getRealmManager().getRealm(realmModel.getId());
        Assert.assertNotNull(realmModel);
        Assert.assertEquals(realmModel.getAccessCodeLifespan(), 100);
        Assert.assertEquals(600, realmModel.getAccessCodeLifespanUserAction());
        Assert.assertEquals(realmModel.getTokenLifespan(), 1000);
        Assert.assertEquals(realmModel.isEnabled(), true);
        Assert.assertEquals(realmModel.getName(), "JUGGLER");
        Assert.assertEquals(realmModel.getPrivateKeyPem(), "0234234");
        Assert.assertEquals(realmModel.getPublicKeyPem(), "0234234");
        Assert.assertEquals(realmModel.isAutomaticRegistrationAfterSocialLogin(), true);
        Assert.assertEquals(1, realmModel.getDefaultRoles().size());
        Assert.assertEquals("foo", realmModel.getDefaultRoles().get(0).getName());
    }

    @Test
    public void testRealmListing() throws Exception {
        realmModel = getRealmManager().createRealm("JUGGLER");
        realmModel.setAccessCodeLifespan(100);
        realmModel.setAccessCodeLifespanUserAction(600);
        realmModel.setCookieLoginAllowed(true);
        realmModel.setEnabled(true);
        realmModel.setName("JUGGLER");
        realmModel.setPrivateKeyPem("0234234");
        realmModel.setPublicKeyPem("0234234");
        realmModel.setTokenLifespan(1000);
        realmModel.setAutomaticRegistrationAfterSocialLogin(true);
        realmModel.addDefaultRole("foo");

        System.out.println(realmModel.getId());
        realmModel = getRealmManager().getRealm(realmModel.getId());
        Assert.assertNotNull(realmModel);
        Assert.assertEquals(realmModel.getAccessCodeLifespan(), 100);
        Assert.assertEquals(600, realmModel.getAccessCodeLifespanUserAction());
        Assert.assertEquals(realmModel.getTokenLifespan(), 1000);
        Assert.assertEquals(realmModel.isEnabled(), true);
        Assert.assertEquals(realmModel.getName(), "JUGGLER");
        Assert.assertEquals(realmModel.getPrivateKeyPem(), "0234234");
        Assert.assertEquals(realmModel.getPublicKeyPem(), "0234234");
        Assert.assertEquals(realmModel.isAutomaticRegistrationAfterSocialLogin(), true);
        Assert.assertEquals(1, realmModel.getDefaultRoles().size());
        Assert.assertEquals("foo", realmModel.getDefaultRoles().get(0).getName());

        String id = realmModel.getId();
        System.out.println("id: " + id);

        identitySession.getTransaction().commit();
        identitySession.close();
        identitySession = factory.createSession();
        identitySession.getTransaction().begin();
        realmManager = new RealmManager(identitySession);
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
        oauth.setBaseUrl("/foo/bar");
        oauth = realmModel.getOAuthClient("oauth-client");
        Assert.assertEquals("/foo/bar", oauth.getBaseUrl());
        Assert.assertTrue(realmModel.hasRole(oauth.getOAuthAgent(), Constants.IDENTITY_REQUESTER_ROLE));


    }

    @Test
    public void testUserSearch() throws Exception {
        test1CreateRealm();
        {
            UserModel user = realmModel.addUser("bburke");
            user.setLastName("Burke");
            user.setFirstName("Bill");
            user.setEmail("bburke@redhat.com");
        }

        RealmManager adapter = getRealmManager();

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
            List<UserModel> userModels = adapter.searchUsers("bburke@redhat.com", realmModel);
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



    }


    @Test
    public void testRoles() throws Exception {
        test1CreateRealm();
        realmModel.addRole("admin");
        realmModel.addRole("user");
        List<RoleModel> roles = realmModel.getRoles();
        Assert.assertEquals(6, roles.size());
        UserModel user = realmModel.addUser("bburke");
        RoleModel role = realmModel.getRole("user");
        realmModel.grantRole(user, role);
        Assert.assertTrue(realmModel.hasRole(user, role));
        System.out.println("Role id: " + role.getId());
        role = realmModel.getRoleById(role.getId());
        Assert.assertNotNull(role);
        Assert.assertEquals("user", role.getName());
    }

    @Test
    public void testUserRequiredActions() throws Exception {
        test1CreateRealm();

        UserModel user = realmModel.addUser("bburke");

        Assert.assertTrue(user.getRequiredActions().isEmpty());

        user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        user = realmModel.getUser("bburke");

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP));

        user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        user = realmModel.getUser("bburke");

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP));

        user.addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
        user = realmModel.getUser("bburke");

        Assert.assertEquals(2, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP));
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL));

        user.removeRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        user = realmModel.getUser("bburke");

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL));

        user.removeRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
        user = realmModel.getUser("bburke");

        Assert.assertTrue(user.getRequiredActions().isEmpty());
    }
}
