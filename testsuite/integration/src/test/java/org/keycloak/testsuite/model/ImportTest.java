/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapperFactory;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.KeycloakModelUtils;
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
    /*
    BIG HELPFUL HINT!!!!
    WHEN YOU MIGRATE THIS CLASS YOU DO NOT NEED TO MIGRATE THIS METHOD.
    IT HAS ALREADY BEEN IMPLEMENTED IN THE NEW ARQUILLIAN TESTSUTE.
    SEE org.keycloak.testsuite.exportimport.ExportImportUtil
    */
    public static void assertDataImportedInRealm(KeycloakSession session, RealmModel realm) {
        Assert.assertTrue(realm.isVerifyEmail());
        Assert.assertEquals(3600000, realm.getOfflineSessionIdleTimeout());
        Assert.assertEquals(1500, realm.getAccessTokenLifespanForImplicitFlow());

        List<RequiredCredentialModel> creds = realm.getRequiredCredentials();
        Assert.assertEquals(1, creds.size());
        RequiredCredentialModel cred = creds.get(0);
        Assert.assertEquals("password", cred.getFormLabel());
        Assert.assertEquals(4, realm.getDefaultRoles().size());

        Assert.assertNotNull(realm.getRole("foo"));
        Assert.assertNotNull(realm.getRole("bar"));

        UserModel user = session.users().getUserByUsername("loginclient", realm);
        Assert.assertNotNull(user);
        Assert.assertEquals(0,  session.users().getFederatedIdentities(user, realm).size());

        List<ClientModel> resources = realm.getClients();
        Assert.assertEquals(8, resources.size());

        // Test applications imported
        ClientModel application = realm.getClientByClientId("Application");
        ClientModel otherApp = realm.getClientByClientId("OtherApp");
        ClientModel accountApp = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        ClientModel nonExisting = realm.getClientByClientId("NonExisting");
        Assert.assertNotNull(application);
        Assert.assertNotNull(otherApp);
        Assert.assertNull(nonExisting);
        List<ClientModel> clients = realm.getClients();
        Assert.assertEquals(8, clients.size());
        Assert.assertTrue(clients.contains(application));
        Assert.assertTrue(clients.contains(otherApp));
        Assert.assertTrue(clients.contains(accountApp));
        realm.getClients().containsAll(clients);

        Assert.assertEquals("Applicationn", application.getName());
        Assert.assertEquals(50, application.getNodeReRegistrationTimeout());
        Map<String, Integer> appRegisteredNodes = application.getRegisteredNodes();
        Assert.assertEquals(2, appRegisteredNodes.size());
        Assert.assertTrue(10 == appRegisteredNodes.get("node1"));
        Assert.assertTrue(20 == appRegisteredNodes.get("172.10.15.20"));

        // test clientAuthenticatorType
        Assert.assertEquals(application.getClientAuthenticatorType(), "client-secret");
        Assert.assertEquals(otherApp.getClientAuthenticatorType(), "client-jwt");

        // Test finding applications by ID
        Assert.assertNull(realm.getClientById("982734"));
        Assert.assertEquals(application, realm.getClientById(application.getId()));


        // Test role mappings
        UserModel admin =  session.users().getUserByUsername("admin", realm);
        // user without creation timestamp in import
        Assert.assertNull(admin.getCreatedTimestamp());
        Set<RoleModel> allRoles = admin.getRoleMappings();
        Assert.assertEquals(3, allRoles.size());
        Assert.assertTrue(allRoles.contains(realm.getRole("admin")));
        Assert.assertTrue(allRoles.contains(application.getRole("app-admin")));
        Assert.assertTrue(allRoles.contains(otherApp.getRole("otherapp-admin")));

        Assert.assertTrue(application.getRole("app-admin").isScopeParamRequired());
        Assert.assertFalse(otherApp.getRole("otherapp-admin").isScopeParamRequired());
        Assert.assertFalse(otherApp.getRole("otherapp-user").isScopeParamRequired());

        UserModel wburke =  session.users().getUserByUsername("wburke", realm);
        // user with creation timestamp in import
        Assert.assertEquals(new Long(123654), wburke.getCreatedTimestamp());
        allRoles = wburke.getRoleMappings();
        Assert.assertEquals(2, allRoles.size());
        Assert.assertFalse(allRoles.contains(realm.getRole("admin")));
        Assert.assertTrue(allRoles.contains(application.getRole("app-user")));
        Assert.assertTrue(allRoles.contains(otherApp.getRole("otherapp-user")));

        Assert.assertEquals(0, wburke.getRealmRoleMappings().size());

        UserModel loginclient = session.users().getUserByUsername("loginclient", realm);
        // user with creation timestamp as string in import
        Assert.assertEquals(new Long(123655), loginclient.getCreatedTimestamp());

        Set<RoleModel> realmRoles = admin.getRealmRoleMappings();
        Assert.assertEquals(1, realmRoles.size());
        Assert.assertEquals("admin", realmRoles.iterator().next().getName());

        Set<RoleModel> appRoles = admin.getClientRoleMappings(application);
        Assert.assertEquals(1, appRoles.size());
        Assert.assertEquals("app-admin", appRoles.iterator().next().getName());

        // Test attributes
        Map<String, List<String>> attrs = wburke.getAttributes();
        Assert.assertEquals(1, attrs.size());
        List<String> attrVals = attrs.get("email");
        Assert.assertEquals(1, attrVals.size());
        Assert.assertEquals("bburke@redhat.com", attrVals.get(0));

        attrs = admin.getAttributes();
        Assert.assertEquals(2, attrs.size());
        attrVals = attrs.get("key1");
        Assert.assertEquals(1, attrVals.size());
        Assert.assertEquals("val1", attrVals.get(0));
        attrVals = attrs.get("key2");
        Assert.assertEquals(2, attrVals.size());
        Assert.assertTrue(attrVals.contains("val21") && attrVals.contains("val22"));

        // Test client
        ClientModel oauthClient = realm.getClientByClientId("oauthclient");
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

        Set<RoleModel> appScopes = KeycloakModelUtils.getClientScopeMappings(application, oauthClient);//application.getClientScopeMappings(oauthClient);
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
        Assert.assertTrue(fedProviders.size() == 2);
        UserFederationProviderModel ldap1 = fedProviders.get(0);
        Assert.assertEquals("MyLDAPProvider1", ldap1.getDisplayName());
        Assert.assertEquals("ldap", ldap1.getProviderName());
        Assert.assertEquals(1, ldap1.getPriority());
        Assert.assertEquals("ldap://foo", ldap1.getConfig().get(LDAPConstants.CONNECTION_URL));

        UserFederationProviderModel ldap2 = fedProviders.get(1);
        Assert.assertEquals("MyLDAPProvider2", ldap2.getDisplayName());
        Assert.assertEquals("ldap://bar", ldap2.getConfig().get(LDAPConstants.CONNECTION_URL));

        // Test federation mappers
        Set<UserFederationMapperModel> fedMappers1 = realm.getUserFederationMappersByFederationProvider(ldap1.getId());
        Assert.assertTrue(fedMappers1.size() == 1);
        UserFederationMapperModel fullNameMapper = fedMappers1.iterator().next();
        Assert.assertEquals("FullNameMapper", fullNameMapper.getName());
        Assert.assertEquals(FullNameLDAPFederationMapperFactory.PROVIDER_ID, fullNameMapper.getFederationMapperType());
        Assert.assertEquals(ldap1.getId(), fullNameMapper.getFederationProviderId());
        Assert.assertEquals("cn", fullNameMapper.getConfig().get(FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE));

        // All builtin LDAP mappers should be here
        Set<UserFederationMapperModel> fedMappers2 = realm.getUserFederationMappersByFederationProvider(ldap2.getId());
        Assert.assertTrue(fedMappers2.size() > 3);
        Set<UserFederationMapperModel> allMappers = realm.getUserFederationMappers();
        Assert.assertEquals(allMappers.size(), fedMappers1.size() + fedMappers2.size());

        // Assert that federation link wasn't created during import
        UserFederationProviderFactory factory = (UserFederationProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(UserFederationProvider.class, "dummy");
        Assert.assertNull(factory.getInstance(session, null).getUserByUsername(realm, "wburke"));

        // Test builtin authentication flows
        AuthenticationFlowModel clientFlow = realm.getClientAuthenticationFlow();
        Assert.assertEquals(DefaultAuthenticationFlows.CLIENT_AUTHENTICATION_FLOW, clientFlow.getAlias());
        Assert.assertNotNull(realm.getAuthenticationFlowById(clientFlow.getId()));
        Assert.assertTrue(realm.getAuthenticationExecutions(clientFlow.getId()).size() > 0);

        AuthenticationFlowModel resetFlow = realm.getResetCredentialsFlow();
        Assert.assertEquals(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, resetFlow.getAlias());
        Assert.assertNotNull(realm.getAuthenticationFlowById(resetFlow.getId()));
        Assert.assertTrue(realm.getAuthenticationExecutions(resetFlow.getId()).size() > 0);

        // Test protocol mappers. Default application has all the builtin protocol mappers. OtherApp just gss credential
        Assert.assertNotNull(application.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));
        Assert.assertNotNull(application.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "email"));
        Assert.assertNotNull(application.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "given name"));
        Assert.assertNull(application.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));

        Assert.assertEquals(1, otherApp.getProtocolMappers().size());
        Assert.assertNull(otherApp.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));
        ProtocolMapperModel gssCredentialMapper = otherApp.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME);
        assertGssProtocolMapper(gssCredentialMapper);

        // Test clientTemplates
        List<ClientTemplateModel> clientTemplates = realm.getClientTemplates();
        Assert.assertEquals(1, clientTemplates.size());
        ClientTemplateModel clientTemplate = clientTemplates.get(0);
        Assert.assertEquals("foo-template", clientTemplate.getName());
        Assert.assertEquals("foo-template-desc", clientTemplate.getDescription());
        Assert.assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, clientTemplate.getProtocol());
        Assert.assertEquals(1, clientTemplate.getProtocolMappers().size());
        ProtocolMapperModel templateGssCredentialMapper = clientTemplate.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME);
        assertGssProtocolMapper(templateGssCredentialMapper);

        // Test client template scopes
        Set<RoleModel> allClientTemplateScopes = clientTemplate.getScopeMappings();
        Assert.assertEquals(3, allClientTemplateScopes.size());
        Assert.assertTrue(allClientTemplateScopes.contains(realm.getRole("admin")));
        Assert.assertTrue(allClientTemplateScopes.contains(application.getRole("app-user")));
        Assert.assertTrue(allClientTemplateScopes.contains(application.getRole("app-admin")));

        Set<RoleModel> clientTemplateRealmScopes = clientTemplate.getRealmScopeMappings();
        Assert.assertTrue(clientTemplateRealmScopes.contains(realm.getRole("admin")));

        Set<RoleModel> clientTemplateAppScopes = KeycloakModelUtils.getClientScopeMappings(application, clientTemplate);//application.getClientScopeMappings(oauthClient);
        Assert.assertTrue(clientTemplateAppScopes.contains(application.getRole("app-user")));
        Assert.assertTrue(clientTemplateAppScopes.contains(application.getRole("app-admin")));

        // Test user consents
        admin =  session.users().getUserByUsername("admin", realm);
        Assert.assertEquals(2, session.users().getConsents(realm, admin).size());

        UserConsentModel appAdminConsent = session.users().getConsentByClient(realm, admin, application.getId());
        Assert.assertEquals(2, appAdminConsent.getGrantedRoles().size());
        Assert.assertTrue(appAdminConsent.getGrantedProtocolMappers() == null || appAdminConsent.getGrantedProtocolMappers().isEmpty());
        Assert.assertTrue(appAdminConsent.isRoleGranted(realm.getRole("admin")));
        Assert.assertTrue(appAdminConsent.isRoleGranted(application.getRole("app-admin")));

        UserConsentModel otherAppAdminConsent = session.users().getConsentByClient(realm, admin, otherApp.getId());
        Assert.assertEquals(1, otherAppAdminConsent.getGrantedRoles().size());
        Assert.assertEquals(1, otherAppAdminConsent.getGrantedProtocolMappers().size());
        Assert.assertTrue(otherAppAdminConsent.isRoleGranted(realm.getRole("admin")));
        Assert.assertFalse(otherAppAdminConsent.isRoleGranted(application.getRole("app-admin")));
        Assert.assertTrue(otherAppAdminConsent.isProtocolMapperGranted(gssCredentialMapper));

        Assert.assertTrue(application.isStandardFlowEnabled());
        Assert.assertTrue(application.isImplicitFlowEnabled());
        Assert.assertTrue(application.isDirectAccessGrantsEnabled());
        Assert.assertFalse(otherApp.isStandardFlowEnabled());
        Assert.assertFalse(otherApp.isImplicitFlowEnabled());
        Assert.assertFalse(otherApp.isDirectAccessGrantsEnabled());

        // Test service accounts
        Assert.assertFalse(application.isServiceAccountsEnabled());
        Assert.assertTrue(otherApp.isServiceAccountsEnabled());
        Assert.assertNull(session.users().getServiceAccount(application));
        UserModel linked = session.users().getServiceAccount(otherApp);
        Assert.assertNotNull(linked);
        Assert.assertEquals("my-service-user", linked.getUsername());
    }

    @Test
    public void install2() throws Exception {
        RealmManager manager = realmManager;
        RealmRepresentation rep = AbstractModelTest.loadJson("model/testrealm-demo.json");
        rep.setId("demo");
        RealmModel realm =manager.importRealm(rep);

        Assert.assertEquals(600, realm.getAccessCodeLifespanUserAction());
        Assert.assertEquals(Constants.DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT, realm.getAccessTokenLifespanForImplicitFlow());
        Assert.assertEquals(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT, realm.getOfflineSessionIdleTimeout());
        verifyRequiredCredentials(realm.getRequiredCredentials(), "password");
    }

    private void verifyRequiredCredentials(List<RequiredCredentialModel> requiredCreds, String expectedType) {
        Assert.assertEquals(1, requiredCreds.size());
        Assert.assertEquals(expectedType, requiredCreds.get(0).getType());
    }

    private static void assertGssProtocolMapper(ProtocolMapperModel gssCredentialMapper) {
        Assert.assertEquals(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME, gssCredentialMapper.getName());
        Assert.assertEquals( OIDCLoginProtocol.LOGIN_PROTOCOL, gssCredentialMapper.getProtocol());
        Assert.assertEquals(UserSessionNoteMapper.PROVIDER_ID, gssCredentialMapper.getProtocolMapper());
        String includeInAccessToken = gssCredentialMapper.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN);
        String includeInIdToken = gssCredentialMapper.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN);
        Assert.assertTrue(includeInAccessToken.equalsIgnoreCase("true"));
        Assert.assertTrue(includeInIdToken == null || Boolean.parseBoolean(includeInIdToken) == false);
    }

}
