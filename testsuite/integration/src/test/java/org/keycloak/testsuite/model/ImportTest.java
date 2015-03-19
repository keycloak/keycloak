package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.constants.KerberosConstants;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
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

        List<RequiredCredentialModel> creds = realm.getRequiredCredentials();
        Assert.assertEquals(1, creds.size());
        RequiredCredentialModel cred = creds.get(0);
        Assert.assertEquals("password", cred.getFormLabel());
        Assert.assertEquals(2, realm.getDefaultRoles().size());

        Assert.assertNotNull(realm.getRole("foo"));
        Assert.assertNotNull(realm.getRole("bar"));

        UserModel user = session.users().getUserByUsername("loginclient", realm);
        Assert.assertNotNull(user);
        Assert.assertEquals(0,  session.users().getFederatedIdentities(user, realm).size());

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
        Set<FederatedIdentityModel> socialLinks = session.users().getFederatedIdentities(socialUser, realm);
        Assert.assertEquals(3, socialLinks.size());
        boolean facebookFound = false;
        boolean googleFound = false;
        boolean twitterFound = false;
        for (FederatedIdentityModel federatedIdentityModel : socialLinks) {
            if ("facebook".equals(federatedIdentityModel.getIdentityProvider())) {
                facebookFound = true;
                Assert.assertEquals(federatedIdentityModel.getUserId(), "facebook1");
                Assert.assertEquals(federatedIdentityModel.getUserName(), "fbuser1");
            } else if ("google".equals(federatedIdentityModel.getIdentityProvider())) {
                googleFound = true;
                Assert.assertEquals(federatedIdentityModel.getUserId(), "google1");
                Assert.assertEquals(federatedIdentityModel.getUserName(), "mysocialuser@gmail.com");
            } else if ("twitter".equals(federatedIdentityModel.getIdentityProvider())) {
                twitterFound = true;
                Assert.assertEquals(federatedIdentityModel.getUserId(), "twitter1");
                Assert.assertEquals(federatedIdentityModel.getUserName(), "twuser1");
            }
        }
        Assert.assertTrue(facebookFound && twitterFound && googleFound);

        UserModel foundSocialUser = session.users().getUserByFederatedIdentity(new FederatedIdentityModel("facebook", "facebook1", "fbuser1"), realm);
        Assert.assertEquals(foundSocialUser.getUsername(), socialUser.getUsername());
        Assert.assertNull(session.users().getUserByFederatedIdentity(new FederatedIdentityModel("facebook", "not-existing", "not-existing"), realm));

        FederatedIdentityModel foundSocialLink = session.users().getFederatedIdentity(socialUser, "facebook", realm);
        Assert.assertEquals("facebook1", foundSocialLink.getUserId());
        Assert.assertEquals("fbuser1", foundSocialLink.getUserName());
        Assert.assertEquals("facebook", foundSocialLink.getIdentityProvider());

        // Test removing social link
        Assert.assertTrue(session.users().removeFederatedIdentity(realm, socialUser, "facebook"));
        Assert.assertNull(session.users().getFederatedIdentity(socialUser, "facebook", realm));
        Assert.assertFalse(session.users().removeFederatedIdentity(realm, socialUser, "facebook"));
        session.users().addFederatedIdentity(realm, socialUser, new FederatedIdentityModel("facebook", "facebook1", "fbuser1"));

        // Test smtp config
        Map<String, String> smtpConfig = realm.getSmtpConfig();
        Assert.assertTrue(smtpConfig.size() == 3);
        Assert.assertEquals("auto@keycloak.org", smtpConfig.get("from"));
        Assert.assertEquals("localhost", smtpConfig.get("host"));
        Assert.assertEquals("3025", smtpConfig.get("port"));

        // Test identity providers
        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();
        Assert.assertEquals(1, identityProviders.size());
        IdentityProviderModel google = identityProviders.get(0);
        Assert.assertEquals("google1", google.getAlias());
        Assert.assertEquals("google", google.getProviderId());
        Assert.assertTrue(google.isEnabled());
        Assert.assertEquals("googleId", google.getConfig().get("clientId"));
        Assert.assertEquals("googleSecret", google.getConfig().get("clientSecret"));

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

        // Test protocol mappers. Default application has all the builtin protocol mappers. OtherApp just gss credential
        Assert.assertNotNull(application.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));
        Assert.assertNotNull(application.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "email"));
        Assert.assertNotNull(application.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "given name"));
        Assert.assertNull(application.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));

        Assert.assertEquals(1, otherApp.getProtocolMappers().size());
        Assert.assertNull(otherApp.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));
        ProtocolMapperModel gssCredentialMapper = otherApp.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME);
        Assert.assertEquals(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME, gssCredentialMapper.getName());
        Assert.assertEquals( OIDCLoginProtocol.LOGIN_PROTOCOL, gssCredentialMapper.getProtocol());
        Assert.assertEquals(UserSessionNoteMapper.PROVIDER_ID, gssCredentialMapper.getProtocolMapper());
        String includeInAccessToken = gssCredentialMapper.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN);
        String includeInIdToken = gssCredentialMapper.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN);
        Assert.assertTrue(includeInAccessToken.equalsIgnoreCase("true"));
        Assert.assertTrue(includeInIdToken == null || Boolean.parseBoolean(includeInIdToken) == false);
    }

    @Test
    public void install2() throws Exception {
        RealmManager manager = realmManager;
        RealmRepresentation rep = AbstractModelTest.loadJson("model/testrealm-demo.json");
        rep.setId("demo");
        RealmModel realm =manager.importRealm(rep);

        Assert.assertEquals(600, realm.getAccessCodeLifespanUserAction());
        verifyRequiredCredentials(realm.getRequiredCredentials(), "password");
    }

    private void verifyRequiredCredentials(List<RequiredCredentialModel> requiredCreds, String expectedType) {
        Assert.assertEquals(1, requiredCreds.size());
        Assert.assertEquals(expectedType, requiredCreds.get(0).getType());
    }

}
