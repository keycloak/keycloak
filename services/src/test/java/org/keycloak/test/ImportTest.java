package org.keycloak.test;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.test.common.AbstractKeycloakTest;
import org.keycloak.test.common.SessionFactoryTestContext;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ImportTest extends AbstractKeycloakTest {

    public ImportTest(SessionFactoryTestContext testContext) {
        super(testContext);
    }

    @Test
    public void install() throws Exception {
        RealmManager manager = getRealmManager();
        RealmRepresentation rep = AbstractKeycloakServerTest.loadJson("testrealm.json");
        RealmModel realm = manager.createRealm("demo", rep.getRealm());
        manager.importRealm(rep, realm);

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
        Set<RoleModel> scopes = realm.getRealmScopeMappings(user);
        Assert.assertEquals(0, scopes.size());
        Assert.assertEquals(0, realm.getSocialLinks(user).size());

        List<ApplicationModel> resources = realm.getApplications();
        Assert.assertEquals(3, resources.size());

        // Test scope relationship
        ApplicationModel application = realm.getApplicationNameMap().get("Application");
        UserModel oauthClient = realm.getUser("oauthclient");
        Assert.assertNotNull(application);
        Assert.assertNotNull(oauthClient);
        Set<RoleModel> appScopes = application.getApplicationScopeMappings(oauthClient);
        RoleModel appUserRole = application.getRole("user");
        Assert.assertTrue(appScopes.contains(appUserRole));

        // Test social linking
        UserModel socialUser = realm.getUser("mySocialUser");
        Set<SocialLinkModel> socialLinks = realm.getSocialLinks(socialUser);
        Assert.assertEquals(3, socialLinks.size());
        int facebookCount = 0;
        int googleCount = 0;
        for (SocialLinkModel socialLinkModel : socialLinks) {
            if ("facebook".equals(socialLinkModel.getSocialProvider())) {
                facebookCount++;
            } else if ("google".equals(socialLinkModel.getSocialProvider())) {
                googleCount++;
                Assert.assertEquals(socialLinkModel.getSocialUsername(), "mySocialUser@gmail.com");
            }
        }
        Assert.assertEquals(2, facebookCount);
        Assert.assertEquals(1, googleCount);

        UserModel foundSocialUser = realm.getUserBySocialLink(new SocialLinkModel("facebook", "fbuser1"));
        Assert.assertEquals(foundSocialUser.getLoginName(), socialUser.getLoginName());
        Assert.assertNull(realm.getUserBySocialLink(new SocialLinkModel("facebook", "not-existing")));

    }

    @Test
    public void install2() throws Exception {
        RealmManager manager = getRealmManager();
        RealmRepresentation rep = AbstractKeycloakServerTest.loadJson("testrealm-demo.json");
        RealmModel realm = manager.createRealm("demo", rep.getRealm());
        manager.importRealm(rep, realm);

        Assert.assertFalse(realm.isUpdateProfileOnInitialSocialLogin());
        Assert.assertEquals(600, realm.getAccessCodeLifespanUserAction());
        verifyRequiredCredentials(realm.getRequiredCredentials(), "password");
        verifyRequiredCredentials(realm.getRequiredApplicationCredentials(), "totp");
        verifyRequiredCredentials(realm.getRequiredOAuthClientCredentials(), "cert");
    }

    private void verifyRequiredCredentials(List<RequiredCredentialModel> requiredCreds, String expectedType) {
        Assert.assertEquals(1, requiredCreds.size());
        Assert.assertEquals(expectedType, requiredCreds.get(0).getType());
    }

}
