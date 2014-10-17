package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;

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
        RealmRepresentation rep = AbstractModelTest.loadJson("model/testrealm2.json");
        RealmModel realm = realmManager.importRealm(rep);
        commit();
        realm = realmManager.getRealmByName("demo-delete");
        realmManager.removeRealm(realm);
    }

    @Test
    public void install() throws Exception {
        RealmRepresentation rep = AbstractModelTest.loadJson("model/testrealm.json");
        rep.setId("demo");
        RealmModel realm = realmManager.importRealm(rep);

        // Commit after import
        commit();

        realm = realmManager.getRealm("demo");
        assertDataImportedInRealm(realmManager.getSession(), realm);

        commit();

        realm = realmManager.getRealm("demo");
        realmManager.removeRealm(realm);
    }

    // Moved to static method, so it's possible to test this from other places too (for example export-import tests)
    public static void assertDataImportedInRealm(KeycloakSession session, RealmModel realm) {
        Assert.assertTrue(realm.isVerifyEmail());

        Assert.assertFalse(realm.isUpdateProfileOnInitialSocialLogin());
        List<RequiredCredentialModel> creds = realm.getRequiredCredentials();
        Assert.assertEquals(1, creds.size());
        RequiredCredentialModel cred = creds.get(0);
        Assert.assertEquals("password", cred.getFormLabel());
        Assert.assertEquals(2, realm.getDefaultRoles().size());

        Assert.assertNotNull(realm.getRole("foo"));
        Assert.assertNotNull(realm.getRole("bar"));

        UserModel user = session.users().getUserByUsername("loginclient", realm);
        Assert.assertNotNull(user);
        Assert.assertEquals(0,  session.users().getSocialLinks(user, realm).size());

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

        Assert.assertEquals(50, application.getNodeReRegistrationTimeout());
        Map<String, Integer> appRegisteredNodes = application.getRegisteredNodes();
        Assert.assertEquals(2, appRegisteredNodes.size());
        Assert.assertTrue(10 == appRegisteredNodes.get("node1"));
        Assert.assertTrue(20 == appRegisteredNodes.get("172.10.15.20"));

        // Test finding applications by ID
        Assert.assertNull(realm.getApplicationById("982734"));
        Assert.assertEquals(application, realm.getApplicationById(application.getId()));


        // Test role mappings
        UserModel admin =  session.users().getUserByUsername("admin", realm);
        Set<RoleModel> allRoles = admin.getRoleMappings();
        Assert.assertEquals(3, allRoles.size());
        Assert.assertTrue(allRoles.contains(realm.getRole("admin")));
        Assert.assertTrue(allRoles.contains(application.getRole("app-admin")));
        Assert.assertTrue(allRoles.contains(otherApp.getRole("otherapp-admin")));

        UserModel wburke =  session.users().getUserByUsername("wburke", realm);
        allRoles = wburke.getRoleMappings();
        Assert.assertEquals(2, allRoles.size());
        Assert.assertFalse(allRoles.contains(realm.getRole("admin")));
        Assert.assertTrue(allRoles.contains(application.getRole("app-user")));
        Assert.assertTrue(allRoles.contains(otherApp.getRole("otherapp-user")));

        Assert.assertEquals(0, wburke.getRealmRoleMappings().size());

        Set<RoleModel> realmRoles = admin.getRealmRoleMappings();
        Assert.assertEquals(1, realmRoles.size());
        Assert.assertEquals("admin", realmRoles.iterator().next().getName());

        Set<RoleModel> appRoles = admin.getApplicationRoleMappings(application);
        Assert.assertEquals(1, appRoles.size());
        Assert.assertEquals("app-admin", appRoles.iterator().next().getName());

        // Test client
        ClientModel oauthClient = realm.findClient("oauthclient");
        Assert.assertEquals("clientpassword", oauthClient.getSecret());
        Assert.assertEquals(true, oauthClient.isEnabled());
        Assert.assertNotNull(oauthClient);

        // Test scope relationship
        Set<RoleModel> allScopes = oauthClient.getScopeMappings();
        Assert.assertEquals(2, allScopes.size());
        Assert.assertTrue(allScopes.contains(realm.getRole("admin")));
        Assert.assertTrue(allScopes.contains(application.getRole("app-user")));

        Set<RoleModel> realmScopes = oauthClient.getRealmScopeMappings();
        Assert.assertTrue(realmScopes.contains(realm.getRole("admin")));

        Set<RoleModel> appScopes = application.getApplicationScopeMappings(oauthClient);
        Assert.assertTrue(appScopes.contains(application.getRole("app-user")));


        // Test social linking
        UserModel socialUser = session.users().getUserByUsername("mySocialUser", realm);
        Set<SocialLinkModel> socialLinks = session.users().getSocialLinks(socialUser, realm);
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

        UserModel foundSocialUser = session.users().getUserBySocialLink(new SocialLinkModel("facebook", "facebook1", "fbuser1"), realm);
        Assert.assertEquals(foundSocialUser.getUsername(), socialUser.getUsername());
        Assert.assertNull(session.users().getUserBySocialLink(new SocialLinkModel("facebook", "not-existing", "not-existing"), realm));

        SocialLinkModel foundSocialLink = session.users().getSocialLink(socialUser, "facebook", realm);
        Assert.assertEquals("facebook1", foundSocialLink.getSocialUserId());
        Assert.assertEquals("fbuser1", foundSocialLink.getSocialUsername());
        Assert.assertEquals("facebook", foundSocialLink.getSocialProvider());

        // Test removing social link
        Assert.assertTrue(session.users().removeSocialLink(realm, socialUser, "facebook"));
        Assert.assertNull(session.users().getSocialLink(socialUser, "facebook", realm));
        Assert.assertFalse(session.users().removeSocialLink(realm, socialUser, "facebook"));
        session.users().addSocialLink(realm, socialUser, new SocialLinkModel("facebook", "facebook1", "fbuser1"));

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

        // Test federation providers
        List<UserFederationProviderModel> fedProviders = realm.getUserFederationProviders();
        Assert.assertTrue(fedProviders.size() == 1);
        UserFederationProviderModel ldap = fedProviders.get(0);
        Assert.assertEquals("MyLDAPProvider", ldap.getDisplayName());
        Assert.assertEquals("dummy", ldap.getProviderName());
        Assert.assertEquals(1, ldap.getPriority());
        Assert.assertEquals("ldap://foo", ldap.getConfig().get("important.config"));

        // Assert that federation link wasn't created during import
        UserFederationProviderFactory factory = (UserFederationProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(UserFederationProvider.class, "dummy");
        Assert.assertNull(factory.getInstance(session, null).getUserByUsername(realm, "wburke"));
    }

    @Test
    public void install2() throws Exception {
        RealmManager manager = realmManager;
        RealmRepresentation rep = AbstractModelTest.loadJson("model/testrealm-demo.json");
        rep.setId("demo");
        RealmModel realm =manager.importRealm(rep);

        Assert.assertFalse(realm.isUpdateProfileOnInitialSocialLogin());
        Assert.assertEquals(600, realm.getAccessCodeLifespanUserAction());
        verifyRequiredCredentials(realm.getRequiredCredentials(), "password");
    }

    private void verifyRequiredCredentials(List<RequiredCredentialModel> requiredCreds, String expectedType) {
        Assert.assertEquals(1, requiredCreds.size());
        Assert.assertEquals(expectedType, requiredCreds.get(0).getType());
    }

}
