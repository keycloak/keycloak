package org.keycloak.model.test;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.authentication.AuthProviderConstants;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ImportTest extends AbstractModelTest {

    @Test
    public void demoDelete() throws Exception {
        // was having trouble deleting this realm from admin console
        RealmRepresentation rep = AbstractModelTest.loadJson("testrealm2.json");
        RealmModel realm = realmManager.importRealm(rep);
        commit();
        realm = realmManager.getRealmByName("demo-delete");
        realmManager.removeRealm(realm);
    }

    @Test
    public void install() throws Exception {
        RealmRepresentation rep = AbstractModelTest.loadJson("testrealm.json");
        RealmModel realm = realmManager.createRealm("demo", rep.getRealm());
        realmManager.importRealm(rep, realm);

        // Commit after import
        commit();

        realm = realmManager.getRealm("demo");
        assertDataImportedInRealm(realm);

        commit();

        realm = realmManager.getRealm("demo");
        realmManager.removeRealm(realm);
    }

    // Moved to static method, so it's possible to test this from other places too (for example export-import tests)
    public static void assertDataImportedInRealm(RealmModel realm) {
        Assert.assertTrue(realm.isVerifyEmail());

        Assert.assertFalse(realm.isUpdateProfileOnInitialSocialLogin());
        List<RequiredCredentialModel> creds = realm.getRequiredCredentials();
        Assert.assertEquals(1, creds.size());
        RequiredCredentialModel cred = creds.get(0);
        Assert.assertEquals("password", cred.getFormLabel());
        Assert.assertEquals(2, realm.getDefaultRoles().size());

        Assert.assertNotNull(realm.getRole("foo"));
        Assert.assertNotNull(realm.getRole("bar"));

        UserModel user = realm.getUser("loginclient");
        Assert.assertNotNull(user);
        Assert.assertEquals(0, realm.getSocialLinks(user).size());

        List<ApplicationModel> resources = realm.getApplications();
        for (ApplicationModel app : resources) {
            System.out.println("app: " + app.getName());
        }
        Assert.assertEquals(5, resources.size());

        // Test applications imported
        ApplicationModel application = realm.getApplicationByName("Application");
        ApplicationModel otherApp = realm.getApplicationByName("OtherApp");
        ApplicationModel accountApp = realm.getApplicationByName(Constants.ACCOUNT_MANAGEMENT_APP);
        ApplicationModel nonExisting = realm.getApplicationByName("NonExisting");
        Assert.assertNotNull(application);
        Assert.assertNotNull(otherApp);
        Assert.assertNull(nonExisting);
        Map<String, ApplicationModel> apps = realm.getApplicationNameMap();
        Assert.assertEquals(5, apps.size());
        Assert.assertTrue(apps.values().contains(application));
        Assert.assertTrue(apps.values().contains(otherApp));
        Assert.assertTrue(apps.values().contains(accountApp));
        realm.getApplications().containsAll(apps.values());

        // Test finding applications by ID
        Assert.assertNull(realm.getApplicationById("982734"));
        Assert.assertEquals(application, realm.getApplicationById(application.getId()));


        // Test role mappings
        UserModel admin = realm.getUser("admin");
        Set<RoleModel> allRoles = realm.getRoleMappings(admin);
        Assert.assertEquals(5, allRoles.size());
        Assert.assertTrue(allRoles.contains(realm.getRole("admin")));
        Assert.assertTrue(allRoles.contains(application.getRole("app-admin")));
        Assert.assertTrue(allRoles.contains(otherApp.getRole("otherapp-admin")));
        Assert.assertTrue(allRoles.contains(accountApp.getRole(AccountRoles.VIEW_PROFILE)));
        Assert.assertTrue(allRoles.contains(accountApp.getRole(AccountRoles.MANAGE_ACCOUNT)));

        UserModel wburke = realm.getUser("wburke");
        allRoles = realm.getRoleMappings(wburke);
        Assert.assertEquals(4, allRoles.size());
        Assert.assertFalse(allRoles.contains(realm.getRole("admin")));
        Assert.assertTrue(allRoles.contains(application.getRole("app-user")));
        Assert.assertTrue(allRoles.contains(otherApp.getRole("otherapp-user")));

        Assert.assertEquals(0, realm.getRealmRoleMappings(wburke).size());

        Set<RoleModel> realmRoles = realm.getRealmRoleMappings(admin);
        Assert.assertEquals(1, realmRoles.size());
        Assert.assertEquals("admin", realmRoles.iterator().next().getName());

        Set<RoleModel> appRoles = application.getApplicationRoleMappings(admin);
        Assert.assertEquals(1, appRoles.size());
        Assert.assertEquals("app-admin", appRoles.iterator().next().getName());

        // Test client
        ClientModel oauthClient = realm.findClient("oauthclient");
        Assert.assertEquals("clientpassword", oauthClient.getSecret());
        Assert.assertEquals(true, oauthClient.isEnabled());
        Assert.assertNotNull(oauthClient);

        // Test scope relationship
        Set<RoleModel> allScopes = realm.getScopeMappings(oauthClient);
        Assert.assertEquals(2, allScopes.size());
        Assert.assertTrue(allScopes.contains(realm.getRole("admin")));
        Assert.assertTrue(allScopes.contains(application.getRole("app-user")));

        Set<RoleModel> realmScopes = realm.getRealmScopeMappings(oauthClient);
        Assert.assertTrue(realmScopes.contains(realm.getRole("admin")));

        Set<RoleModel> appScopes = application.getApplicationScopeMappings(oauthClient);
        Assert.assertTrue(appScopes.contains(application.getRole("app-user")));


        // Test social linking
        UserModel socialUser = realm.getUser("mySocialUser");
        Set<SocialLinkModel> socialLinks = realm.getSocialLinks(socialUser);
        Assert.assertEquals(3, socialLinks.size());
        boolean facebookFound = false;
        boolean googleFound = false;
        boolean twitterFound = false;
        for (SocialLinkModel socialLinkModel : socialLinks) {
            if ("facebook".equals(socialLinkModel.getSocialProvider())) {
                facebookFound = true;
                Assert.assertEquals(socialLinkModel.getSocialUserId(), "facebook1");
                Assert.assertEquals(socialLinkModel.getSocialUsername(), "fbuser1");
            } else if ("google".equals(socialLinkModel.getSocialProvider())) {
                googleFound = true;
                Assert.assertEquals(socialLinkModel.getSocialUserId(), "google1");
                Assert.assertEquals(socialLinkModel.getSocialUsername(), "mySocialUser@gmail.com");
            } else if ("twitter".equals(socialLinkModel.getSocialProvider())) {
                twitterFound = true;
                Assert.assertEquals(socialLinkModel.getSocialUserId(), "twitter1");
                Assert.assertEquals(socialLinkModel.getSocialUsername(), "twuser1");
            }
        }
        Assert.assertTrue(facebookFound && twitterFound && googleFound);

        UserModel foundSocialUser = realm.getUserBySocialLink(new SocialLinkModel("facebook", "facebook1", "fbuser1"));
        Assert.assertEquals(foundSocialUser.getLoginName(), socialUser.getLoginName());
        Assert.assertNull(realm.getUserBySocialLink(new SocialLinkModel("facebook", "not-existing", "not-existing")));

        SocialLinkModel foundSocialLink = realm.getSocialLink(socialUser, "facebook");
        Assert.assertEquals("facebook1", foundSocialLink.getSocialUserId());
        Assert.assertEquals("fbuser1", foundSocialLink.getSocialUsername());
        Assert.assertEquals("facebook", foundSocialLink.getSocialProvider());

        // Test removing social link
        Assert.assertTrue(realm.removeSocialLink(socialUser, "facebook"));
        Assert.assertNull(realm.getSocialLink(socialUser, "facebook"));
        Assert.assertFalse(realm.removeSocialLink(socialUser, "facebook"));

        // Test smtp config
        Map<String, String> smtpConfig = realm.getSmtpConfig();
        Assert.assertTrue(smtpConfig.size() == 3);
        Assert.assertEquals("auto@keycloak.org", smtpConfig.get("from"));
        Assert.assertEquals("localhost", smtpConfig.get("host"));
        Assert.assertEquals("3025", smtpConfig.get("port"));

        // Test social config
        Map<String, String> socialConfig = realm.getSocialConfig();
        Assert.assertTrue(socialConfig.size() == 2);
        Assert.assertEquals("abc", socialConfig.get("google.key"));
        Assert.assertEquals("def", socialConfig.get("google.secret"));

        // Test ldap config
        Map<String, String> ldapConfig = realm.getLdapServerConfig();
        Assert.assertTrue(ldapConfig.size() == 6);
        Assert.assertEquals("ldap://localhost:10389", ldapConfig.get("connectionUrl"));
        Assert.assertEquals("dc=keycloak,dc=org", ldapConfig.get("baseDn"));
        Assert.assertEquals("ou=People,dc=keycloak,dc=org", ldapConfig.get("userDnSuffix"));
        Assert.assertEquals("other", ldapConfig.get("vendor"));

        // Test authentication providers
        List<AuthenticationProviderModel> authProviderModels = realm.getAuthenticationProviders();
        Assert.assertTrue(authProviderModels.size() == 3);
        AuthenticationProviderModel authProv1 = authProviderModels.get(0);
        AuthenticationProviderModel authProv2 = authProviderModels.get(1);
        AuthenticationProviderModel authProv3 = authProviderModels.get(2);
        Assert.assertEquals(AuthProviderConstants.PROVIDER_NAME_MODEL, authProv1.getProviderName());
        Assert.assertTrue(authProv1.isPasswordUpdateSupported());
        Assert.assertEquals(AuthProviderConstants.PROVIDER_NAME_EXTERNAL_MODEL, authProv2.getProviderName());
        Assert.assertFalse(authProv2.isPasswordUpdateSupported());
        Assert.assertEquals("trustedRealm", authProv2.getConfig().get("externalRealmId"));
        Assert.assertEquals(AuthProviderConstants.PROVIDER_NAME_PICKETLINK, authProv3.getProviderName());
        Assert.assertTrue(authProv3.isPasswordUpdateSupported());

        // Test authentication linking
        AuthenticationLinkModel authLink = realm.getAuthenticationLink(socialUser);
        Assert.assertEquals(AuthProviderConstants.PROVIDER_NAME_PICKETLINK, authLink.getAuthProvider());
        Assert.assertEquals("myUser1", authLink.getAuthUserId());
    }

    @Test
    public void install2() throws Exception {
        RealmManager manager = realmManager;
        RealmRepresentation rep = AbstractModelTest.loadJson("testrealm-demo.json");
        RealmModel realm = manager.createRealm("demo", rep.getRealm());
        manager.importRealm(rep, realm);

        Assert.assertFalse(realm.isUpdateProfileOnInitialSocialLogin());
        Assert.assertEquals(600, realm.getAccessCodeLifespanUserAction());
        verifyRequiredCredentials(realm.getRequiredCredentials(), "password");
    }

    private void verifyRequiredCredentials(List<RequiredCredentialModel> requiredCreds, String expectedType) {
        Assert.assertEquals(1, requiredCreds.size());
        Assert.assertEquals(expectedType, requiredCreds.get(0).getType());
    }

}
