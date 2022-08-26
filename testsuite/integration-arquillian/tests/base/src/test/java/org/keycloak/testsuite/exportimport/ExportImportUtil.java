/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite.exportimport;

import org.junit.Assert;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.models.Constants;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.protocol.saml.SamlProtocolFactory;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientMappingsRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.RealmRepUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.keycloak.util.JsonSerialization;

import static org.junit.Assert.assertThat;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ExportImportUtil {

    // In the old testsuite, this method exists as a public method of ImportTest from the model package.
    // However, model package is not ready to be migrated yet.
    public static void assertDataImportedInRealm(Keycloak adminClient, KeycloakTestingClient testingClient, RealmRepresentation realm) throws IOException {
        Assert.assertTrue(realm.isVerifyEmail());
        Assert.assertEquals((Integer)3600000, realm.getOfflineSessionIdleTimeout());
        Assert.assertEquals((Integer)1500, realm.getAccessTokenLifespanForImplicitFlow());
        Assert.assertEquals((Integer)1800, realm.getSsoSessionIdleTimeout());
        Assert.assertEquals((Integer)36000, realm.getSsoSessionMaxLifespan());
        Assert.assertEquals((Integer)3600, realm.getSsoSessionIdleTimeoutRememberMe());
        Assert.assertEquals((Integer)172800, realm.getSsoSessionMaxLifespanRememberMe());

        Set<String> creds = realm.getRequiredCredentials();
        Assert.assertEquals(1, creds.size());
        String cred = (String)creds.iterator().next();
        Assert.assertEquals("password", cred);

        RealmResource realmRsc = adminClient.realm(realm.getRealm());

        /* See KEYCLOAK-3104*/
        UserRepresentation user = findByUsername(realmRsc, "loginclient");
        Assert.assertNotNull(user);

        UserResource userRsc = realmRsc.users().get(user.getId());
        Assert.assertEquals(0, userRsc.getFederatedIdentity().size());

        List<ClientRepresentation> resources = realmRsc.clients().findAll();
        Assert.assertEquals(10, resources.size());

        // Test applications imported
        ClientRepresentation application = ApiUtil.findClientByClientId(realmRsc, "Application").toRepresentation();
        ClientRepresentation otherApp = ApiUtil.findClientByClientId(realmRsc, "OtherApp").toRepresentation();
        ClientRepresentation accountApp = ApiUtil.findClientByClientId(realmRsc, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).toRepresentation();
        ClientRepresentation testAppAuthzApp = ApiUtil.findClientByClientId(realmRsc, "test-app-authz").toRepresentation();
        ClientResource nonExisting = ApiUtil.findClientByClientId(realmRsc, "NonExisting");
        Assert.assertNotNull(application);
        Assert.assertNotNull(otherApp);
        Assert.assertNull(nonExisting);
        List<ClientRepresentation> clients = realmRsc.clients().findAll();
        Assert.assertEquals(10, clients.size());
        Assert.assertTrue(hasClient(clients, application));
        Assert.assertTrue(hasClient(clients, otherApp));
        Assert.assertTrue(hasClient(clients, accountApp));

        Assert.assertEquals("Applicationn", application.getName());
        Assert.assertEquals((Integer)50, application.getNodeReRegistrationTimeout());
        Map<String, Integer> appRegisteredNodes = application.getRegisteredNodes();
        Assert.assertEquals(2, appRegisteredNodes.size());
        Assert.assertTrue(10 == appRegisteredNodes.get("node1"));
        Assert.assertTrue(20 == appRegisteredNodes.get("172.10.15.20"));

        // test clientAuthenticatorType
        Assert.assertEquals("client-secret", application.getClientAuthenticatorType());
        Assert.assertEquals("client-jwt", otherApp.getClientAuthenticatorType());

        // test authenticationFlowBindingOverrides
        Map<String, String> flowMap = otherApp.getAuthenticationFlowBindingOverrides();
        Assert.assertNotNull(flowMap);
        Assert.assertEquals(1, flowMap.size());
        Assert.assertTrue(flowMap.containsKey("browser"));
        // if the authentication flows were correctly imported there must be a flow whose id matches the one in the authenticationFlowBindingOverrides
        AuthenticationFlowRepresentation flowRep = realmRsc.flows().getFlow(flowMap.get("browser"));
        Assert.assertNotNull(flowRep);
        Assert.assertEquals("browser", flowRep.getAlias());

        // Test finding applications by ID
        Assert.assertNull(ApiUtil.findClientResourceById(realmRsc, "982734"));
        Assert.assertEquals(application.getId(), ApiUtil.findClientResourceById(realmRsc, application.getId()).toRepresentation().getId());


        // Test role mappings
        UserRepresentation admin = findByUsername(realmRsc, "admin");
        // user without creation timestamp in import
        Assert.assertNull(admin.getCreatedTimestamp());
        Set<RoleRepresentation> allRoles = allRoles(realmRsc, admin);
        Assert.assertEquals(3, allRoles.size());
        Assert.assertTrue(containsRole(allRoles, findRealmRole(realmRsc, "admin")));
        Assert.assertTrue(containsRole(allRoles, findClientRole(realmRsc, application.getId(), "app-admin")));
        Assert.assertTrue(containsRole(allRoles, findClientRole(realmRsc, otherApp.getId(), "otherapp-admin")));

        UserRepresentation wburke = findByUsername(realmRsc, "wburke");
        // user with creation timestamp in import
        Assert.assertEquals(new Long(123654), wburke.getCreatedTimestamp());
        allRoles = allRoles(realmRsc, wburke);
        Assert.assertEquals(2, allRoles.size());
        Assert.assertFalse(containsRole(allRoles, findRealmRole(realmRsc, "admin")));
        Assert.assertTrue(containsRole(allRoles, findClientRole(realmRsc, application.getId(), "app-user")));
        Assert.assertTrue(containsRole(allRoles, findClientRole(realmRsc, otherApp.getId(), "otherapp-user")));

        Assert.assertNull(realmRsc.users().get(wburke.getId()).roles().getAll().getRealmMappings());

        Assert.assertEquals((Object) 159, wburke.getNotBefore());

        UserRepresentation loginclient = findByUsername(realmRsc, "loginclient");
        // user with creation timestamp as string in import
        Assert.assertEquals(new Long(123655), loginclient.getCreatedTimestamp());

        UserRepresentation hashedPasswordUser = findByUsername(realmRsc, "hashedpassworduser");
        CredentialRepresentation password = realmRsc.users().get(hashedPasswordUser.getId()).credentials().stream()
                .filter(credential -> PasswordCredentialModel.TYPE.equals(credential.getType()))
                .findFirst().get();
        PasswordCredentialData credentialData = JsonSerialization.readValue(password.getCredentialData(), PasswordCredentialData.class);
        Assert.assertEquals(1234, credentialData.getHashIterations());

        List<RoleRepresentation> realmRoles = realmRolesForUser(realmRsc, admin);
        Assert.assertEquals(1, realmRoles.size());
        Assert.assertEquals("admin", realmRoles.iterator().next().getName());

        List<RoleRepresentation> appRoles = clientRolesForUser(realmRsc, application, admin);
        Assert.assertEquals(1, appRoles.size());
        Assert.assertEquals("app-admin", appRoles.iterator().next().getName());

        // Test attributes
        Map<String, List<String>> attrs = wburke.getAttributes();
        Assert.assertEquals(1, attrs.size());
        List<String> attrVals = attrs.get("old-email");
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
        ClientResource oauthClient = ApiUtil.findClientResourceByClientId(realmRsc, "oauthclient");
        ClientRepresentation oauthClientRep = oauthClient.toRepresentation();
        Assert.assertEquals("clientpassword", oauthClient.getSecret().getValue());
        Assert.assertTrue(oauthClientRep.isEnabled());
        Assert.assertNotNull(oauthClientRep);

        // Test scope relationship
        Set<RoleRepresentation> allScopes = allScopeMappings(oauthClient);
        Assert.assertEquals(2, allScopes.size());
        Assert.assertTrue(containsRole(allScopes, findRealmRole(realmRsc, "admin")));
        Assert.assertTrue(containsRole(allScopes, findClientRole(realmRsc, application.getId(), "app-user")));

        List<RoleRepresentation> realmScopes = realmScopeMappings(oauthClient);
        Assert.assertTrue(containsRole(realmScopes, findRealmRole(realmRsc, "admin")));

        List<RoleRepresentation> appScopes = clientScopeMappings(oauthClient);
        Assert.assertTrue(containsRole(appScopes, findClientRole(realmRsc, application.getId(), "app-user")));

        // Test social linking
        UserResource socialUser = realmRsc.users().get(findByUsername(realmRsc, "mySocialUser").getId());
        List<FederatedIdentityRepresentation> socialLinks = socialUser.getFederatedIdentity();
        Assert.assertEquals(3, socialLinks.size());
        boolean facebookFound = false;
        boolean googleFound = false;
        boolean twitterFound = false;
        FederatedIdentityRepresentation facebookIdentityRep = null;
        for (FederatedIdentityRepresentation federatedIdentityRep : socialLinks) {
            if ("facebook1".equals(federatedIdentityRep.getIdentityProvider())) {
                facebookFound = true;
                facebookIdentityRep = federatedIdentityRep;
                Assert.assertEquals("facebook1",federatedIdentityRep.getUserId());
                Assert.assertEquals("fbuser1", federatedIdentityRep.getUserName());
            } else if ("google1".equals(federatedIdentityRep.getIdentityProvider())) {
                googleFound = true;
                Assert.assertEquals("google1", federatedIdentityRep.getUserId());
                Assert.assertEquals("mysocialuser@gmail.com", federatedIdentityRep.getUserName());
            } else if ("twitter1".equals(federatedIdentityRep.getIdentityProvider())) {
                twitterFound = true;
                Assert.assertEquals("twitter1", federatedIdentityRep.getUserId());
                Assert.assertEquals("twuser1", federatedIdentityRep.getUserName());
            }
        }
        Assert.assertTrue(facebookFound && twitterFound && googleFound);

        UserRepresentation foundSocialUser =  testingClient.testing().getUserByFederatedIdentity(realm.getRealm(), "facebook1", "facebook1", "fbuser1");
        Assert.assertEquals(foundSocialUser.getUsername(), socialUser.toRepresentation().getUsername());
        Assert.assertNull(testingClient.testing().getUserByFederatedIdentity(realm.getRealm(), "facebook", "not-existing", "not-existing"));

        Assert.assertEquals("facebook1", facebookIdentityRep.getUserId());
        Assert.assertEquals("fbuser1", facebookIdentityRep.getUserName());
        Assert.assertEquals("facebook1", facebookIdentityRep.getIdentityProvider());

        // Test remove/add social link
        socialUser.removeFederatedIdentity("facebook1");
        Assert.assertEquals(2, socialUser.getFederatedIdentity().size());
        socialUser.addFederatedIdentity("facebook1", facebookIdentityRep);
        Assert.assertEquals(3, socialUser.getFederatedIdentity().size());

        // Test smtp config
        Map<String, String> smtpConfig = realm.getSmtpServer();
        Assert.assertTrue(smtpConfig.size() == 3);
        Assert.assertEquals("auto@keycloak.org", smtpConfig.get("from"));
        Assert.assertEquals("localhost", smtpConfig.get("host"));
        Assert.assertEquals("3025", smtpConfig.get("port"));

        // Test identity providers
        List<IdentityProviderRepresentation> identityProviders = realm.getIdentityProviders();
        Assert.assertEquals(3, identityProviders.size());
        IdentityProviderRepresentation google = null;
        for (IdentityProviderRepresentation idpRep : identityProviders) {
            if (idpRep.getAlias().equals("google1")) google = idpRep;
        }
        Assert.assertNotNull(google);
        Assert.assertEquals("google1", google.getAlias());
        Assert.assertEquals("google", google.getProviderId());
        Assert.assertTrue(google.isEnabled());
        Assert.assertEquals("googleId", google.getConfig().get("clientId"));
        Assert.assertEquals("googleSecret", google.getConfig().get("clientSecret"));

        //////////////////
        // Test federation providers
        // on import should convert UserfederationProviderRepresentation to Component model
        List<UserFederationProviderRepresentation> fedProviders = realm.getUserFederationProviders();
        Assert.assertTrue(fedProviders == null || fedProviders.size() == 0);
        List<ComponentRepresentation> storageProviders = realmRsc.components().query(realm.getId(), UserStorageProvider.class.getName());
        Assert.assertTrue(storageProviders.size() == 2);
        ComponentRepresentation ldap1 = storageProviders.get(0);
        ComponentRepresentation ldap2 = storageProviders.get(1);
        if (!"MyLDAPProvider1".equals(ldap1.getName())) {
            ldap2 = ldap1;
            ldap1 = storageProviders.get(1);
        }
        Assert.assertEquals("MyLDAPProvider1", ldap1.getName());
        Assert.assertEquals("ldap", ldap1.getProviderId());
        Assert.assertEquals("1", ldap1.getConfig().getFirst("priority"));
        Assert.assertEquals("ldap://foo", ldap1.getConfig().getFirst(LDAPConstants.CONNECTION_URL));

        Assert.assertEquals("MyLDAPProvider2", ldap2.getName());
        Assert.assertEquals("ldap://bar", ldap2.getConfig().getFirst(LDAPConstants.CONNECTION_URL));

        // Test federation mappers
        List<ComponentRepresentation> fedMappers1 = realmRsc.components().query(ldap1.getId(), LDAPStorageMapper.class.getName());
        ComponentRepresentation fullNameMapper = fedMappers1.iterator().next();
        Assert.assertEquals("FullNameMapper", fullNameMapper.getName());
        Assert.assertEquals(FullNameLDAPStorageMapperFactory.PROVIDER_ID, fullNameMapper.getProviderId());
        Assert.assertEquals("cn", fullNameMapper.getConfig().getFirst(FullNameLDAPStorageMapper.LDAP_FULL_NAME_ATTRIBUTE));
        /////////////////

        // Assert that federation link wasn't created during import
        Assert.assertNull(testingClient.testing().getUserByUsernameFromFedProviderFactory(realm.getRealm(), "wburke"));

        // Test builtin authentication flows
        AuthenticationFlowRepresentation clientFlow = testingClient.testing().getClientAuthFlow(realm.getRealm());
        Assert.assertEquals(DefaultAuthenticationFlows.CLIENT_AUTHENTICATION_FLOW, clientFlow.getAlias());
        Assert.assertNotNull(realmRsc.flows().getFlow(clientFlow.getId()));
        Assert.assertTrue(realmRsc.flows().getExecutions(clientFlow.getAlias()).size() > 0);

        AuthenticationFlowRepresentation resetFlow = testingClient.testing().getResetCredFlow(realm.getRealm());
        Assert.assertEquals(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, resetFlow.getAlias());
        Assert.assertNotNull(realmRsc.flows().getFlow(resetFlow.getId()));
        Assert.assertTrue(realmRsc.flows().getExecutions(resetFlow.getAlias()).size() > 0);

        // Test protocol mappers. Default application doesn't have any builtin protocol mappers. OtherApp just gss credential
        List<ProtocolMapperRepresentation> applicationMappers = application.getProtocolMappers();
        Assert.assertNull(findMapperByName(applicationMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));//application.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));
        Assert.assertNull(findMapperByName(applicationMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "email"));
        Assert.assertNull(findMapperByName(applicationMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "given name"));
        Assert.assertNull(findMapperByName(applicationMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));

        Assert.assertEquals(1, otherApp.getProtocolMappers().size());
        List<ProtocolMapperRepresentation> otherAppMappers = otherApp.getProtocolMappers();
        Assert.assertNull(findMapperByName(otherAppMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));
        ProtocolMapperRepresentation gssCredentialMapper = findMapperByName(otherAppMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME);
        assertGssProtocolMapper(gssCredentialMapper);

        // Test clientScopes
        List<ClientScopeRepresentation> clientScopes = realmRsc.clientScopes().findAll();
        ClientScopeRepresentation clientScope = clientScopes.stream().filter((ClientScopeRepresentation clientScope1) -> {

            return "foo_scope".equals(clientScope1.getName());

        }).findFirst().get();
        Assert.assertEquals("foo_scope", clientScope.getName());
        Assert.assertEquals("foo scope-desc", clientScope.getDescription());
        Assert.assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, clientScope.getProtocol());
        Assert.assertEquals(1, clientScope.getProtocolMappers().size());
        List<ProtocolMapperRepresentation> clientScopeMappers = clientScope.getProtocolMappers();
        ProtocolMapperRepresentation scopeGssCredentialMapper = findMapperByName(clientScopeMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME);
        assertGssProtocolMapper(scopeGssCredentialMapper);

        // Test client scope - scopes
        Set<RoleRepresentation> allClientScopeScopes = allScopeMappings(realmRsc.clientScopes().get(clientScope.getId()));
        Assert.assertEquals(3, allClientScopeScopes.size());
        Assert.assertTrue(containsRole(allClientScopeScopes, findRealmRole(realmRsc, "admin")));
        Assert.assertTrue(containsRole(allClientScopeScopes, findClientRole(realmRsc, application.getId(), "app-user")));
        Assert.assertTrue(containsRole(allClientScopeScopes, findClientRole(realmRsc, application.getId(), "app-admin")));

        List<RoleRepresentation> clientScopeRealmScopes = realmScopeMappings(realmRsc.clientScopes().get(clientScope.getId()));
        Assert.assertTrue(containsRole(clientScopeRealmScopes, findRealmRole(realmRsc, "admin")));

        List<RoleRepresentation> clientScopeAppScopes = clientScopeMappings(realmRsc.clientScopes().get(clientScope.getId()));
        Assert.assertTrue(containsRole(clientScopeAppScopes, findClientRole(realmRsc, application.getId(), "app-user")));
        Assert.assertTrue(containsRole(clientScopeAppScopes, findClientRole(realmRsc, application.getId(), "app-admin")));

        // Test client scopes assignment
        Assert.assertTrue(otherApp.getDefaultClientScopes().contains("foo_scope"));
        Assert.assertFalse(application.getDefaultClientScopes().contains("foo_scope"));

        // Test builtin client scopes
        testRealmDefaultClientScopes(realmRsc);

        // Test user consents
        UserResource adminRsc = realmRsc.users().get(admin.getId());
        List<Map<String, Object>> consents = adminRsc.getConsents();
        Assert.assertEquals(2, consents.size());//.getConsents().size());

        Map<String, Object> appAdminConsent = findConsentByClientId(consents, application.getClientId());
        Assert.assertNotNull(appAdminConsent);
        Assert.assertTrue(isClientScopeGranted(appAdminConsent, OAuth2Constants.OFFLINE_ACCESS, "roles", "profile", "email", "account", "web-origins"));

        Map<String, Object> otherAppAdminConsent = findConsentByClientId(consents, otherApp.getClientId());//admin.getConsentByClient(otherApp.getId());
        Assert.assertFalse(isClientScopeGranted(otherAppAdminConsent, OAuth2Constants.OFFLINE_ACCESS));

        Assert.assertTrue(application.isStandardFlowEnabled());
        Assert.assertTrue(application.isImplicitFlowEnabled());
        Assert.assertTrue(application.isDirectAccessGrantsEnabled());
        Assert.assertFalse(otherApp.isStandardFlowEnabled());
        Assert.assertFalse(otherApp.isImplicitFlowEnabled());
        Assert.assertFalse(otherApp.isDirectAccessGrantsEnabled());

        // Test service accounts
        Assert.assertFalse(application.isServiceAccountsEnabled());
        Assert.assertTrue(otherApp.isServiceAccountsEnabled());

        if (ProfileAssume.isFeatureEnabled(Profile.Feature.AUTHORIZATION)) { 
            Assert.assertTrue(testAppAuthzApp.isServiceAccountsEnabled());
            Assert.assertNull(testingClient.testing().getUserByServiceAccountClient(realm.getRealm(), application.getClientId()));//session.users().getUserByServiceAccountClient(application));
            UserRepresentation otherAppSA = testingClient.testing().getUserByServiceAccountClient(realm.getRealm(), otherApp.getClientId());//session.users().getUserByServiceAccountClient(otherApp);
            Assert.assertNotNull(otherAppSA);
            Assert.assertEquals("service-account-otherapp", otherAppSA.getUsername());
            UserRepresentation testAppAuthzSA = testingClient.testing().getUserByServiceAccountClient(realm.getRealm(), testAppAuthzApp.getClientId());
            Assert.assertNotNull(testAppAuthzSA);
            Assert.assertEquals("service-account-test-app-authz", testAppAuthzSA.getUsername());

            // test service account maintains the roles in OtherApp
            allRoles = allRoles(realmRsc, otherAppSA);
            Assert.assertEquals(3, allRoles.size());
            Assert.assertTrue(containsRole(allRoles, findRealmRole(realmRsc, "user")));
            Assert.assertTrue(containsRole(allRoles, findClientRole(realmRsc, otherApp.getId(), "otherapp-user")));
            Assert.assertTrue(containsRole(allRoles, findClientRole(realmRsc, otherApp.getId(), "otherapp-admin")));

            assertAuthorizationSettingsOtherApp(realmRsc);
            assertAuthorizationSettingsTestAppAuthz(realmRsc);
        }
    }


    private static boolean isClientScopeGranted(Map<String, Object> consent, String... clientScopeNames) {
        if (consent.get("grantedClientScopes") == null) return false;
        return ((List)consent.get("grantedClientScopes")).containsAll(Arrays.asList(clientScopeNames));
    }


    private static Map<String, Object> findConsentByClientId(List<Map<String, Object>> consents, String clientId) {
        for (Map<String, Object> consent : consents) {
            if (clientId.equals(consent.get("clientId"))) return consent;
        }
        return null;
    }

    private static void assertGssProtocolMapper(ProtocolMapperRepresentation gssCredentialMapper) {
        Assert.assertEquals(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME, gssCredentialMapper.getName());
        Assert.assertEquals( OIDCLoginProtocol.LOGIN_PROTOCOL, gssCredentialMapper.getProtocol());
        Assert.assertEquals(UserSessionNoteMapper.PROVIDER_ID, gssCredentialMapper.getProtocolMapper());
        String includeInAccessToken = gssCredentialMapper.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN);
        String includeInIdToken = gssCredentialMapper.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN);
        Assert.assertTrue(includeInAccessToken.equalsIgnoreCase("true"));
        Assert.assertTrue(includeInIdToken == null || Boolean.parseBoolean(includeInIdToken) == false);
    }

    private static ProtocolMapperRepresentation findMapperByName(List<ProtocolMapperRepresentation> mappers, String type, String name) {
        if (mappers == null) {
            return null;
        }
        
        for (ProtocolMapperRepresentation mapper : mappers) {
            if (mapper.getProtocol().equals(type) &&
                mapper.getName().equals(name)) {
                return mapper;
            }
        }
        return null;
    }

    private static boolean hasClient(List<ClientRepresentation> clients, ClientRepresentation client) {
        for (ClientRepresentation clientRep : clients) {
            if (client.getId().equals(clientRep.getId())) return true;
        }
        return false;
    }

    // Workaround for KEYCLOAK-3104.  For this realm, search() only works if username is null.
    private static UserRepresentation findByUsername(RealmResource realmRsc, String username) {
        for (UserRepresentation user : realmRsc.users().search(null, 0, -1)) {
            if (user.getUsername().equalsIgnoreCase(username)) return user;
        }
        return null;
    }

    private static Set<RoleRepresentation> allScopeMappings(ClientResource client) {
        Set<RoleRepresentation> allRoles = new HashSet<>();
        List<RoleRepresentation> realmRoles = realmScopeMappings(client);
        if (realmRoles != null) allRoles.addAll(realmRoles);

        allRoles.addAll(clientScopeMappings(client));

        return allRoles;
    }

    private static Set<RoleRepresentation> allScopeMappings(ClientScopeResource client) {
        Set<RoleRepresentation> allRoles = new HashSet<>();
        List<RoleRepresentation> realmRoles = realmScopeMappings(client);
        if (realmRoles != null) allRoles.addAll(realmRoles);

        allRoles.addAll(clientScopeMappings(client));

        return allRoles;
    }

    private static List<RoleRepresentation> clientScopeMappings(ClientResource client) {
        List<RoleRepresentation> clientScopeMappings = new LinkedList<>();
        Map<String, ClientMappingsRepresentation> clientRoles = client.getScopeMappings().getAll().getClientMappings();
        if (clientRoles == null) return clientScopeMappings;

        for (String clientKey : clientRoles.keySet()) {
            List<RoleRepresentation> clientRoleScopeMappings = clientRoles.get(clientKey).getMappings();
            if (clientRoleScopeMappings != null) clientScopeMappings.addAll(clientRoleScopeMappings);
        }

        return clientScopeMappings;
    }

    private static List<RoleRepresentation> clientScopeMappings(ClientScopeResource client) {
        List<RoleRepresentation> clientScopeMappings = new LinkedList<>();
        Map<String, ClientMappingsRepresentation> clientRoles = client.getScopeMappings().getAll().getClientMappings();
        if (clientRoles == null) return clientScopeMappings;

        for (String clientKey : clientRoles.keySet()) {
            List<RoleRepresentation> clientRoleScopeMappings = clientRoles.get(clientKey).getMappings();
            if (clientRoleScopeMappings != null) clientScopeMappings.addAll(clientRoleScopeMappings);
        }

        return clientScopeMappings;
    }

    private static List<RoleRepresentation> realmScopeMappings(ClientResource client) {
        return client.getScopeMappings().realmLevel().listAll();
    }

    private static List<RoleRepresentation> realmScopeMappings(ClientScopeResource client) {
        return client.getScopeMappings().realmLevel().listAll();
    }

    private static Set<RoleRepresentation> allRoles(RealmResource realmRsc, UserRepresentation user) {
        UserResource userRsc = realmRsc.users().get(user.getId());
        Set<RoleRepresentation> roles = new HashSet<>();

        List<RoleRepresentation> realmRoles = userRsc.roles().getAll().getRealmMappings();
        if (realmRoles != null) roles.addAll(realmRoles);

        roles.addAll(allClientRolesForUser(realmRsc, user));

        return roles;
    }

    private static List<RoleRepresentation> realmRolesForUser(RealmResource realmRsc, UserRepresentation user) {
        return realmRsc.users().get(user.getId()).roles().getAll().getRealmMappings();
    }

    private static List<RoleRepresentation> allClientRolesForUser(RealmResource realmRsc, UserRepresentation user) {
        UserResource userRsc = realmRsc.users().get(user.getId());
        List<RoleRepresentation> roles = new LinkedList<>();
        for(String client : userRsc.roles().getAll().getClientMappings().keySet()) {
            List<RoleRepresentation> clientRoles = userRsc.roles().getAll().getClientMappings().get(client).getMappings();
            if (clientRoles != null) roles.addAll(clientRoles);
        }
        return roles;
    }

    private static List<RoleRepresentation> clientRolesForUser(RealmResource realmRsc, ClientRepresentation client, UserRepresentation user) {
        UserResource userRsc = realmRsc.users().get(user.getId());
        return userRsc.roles().clientLevel(client.getId()).listAll();
    }

    private static RoleRepresentation findRealmRole(RealmResource realmRsc, String roleName) {
        return realmRsc.roles().get(roleName).toRepresentation();
    }

    private static RoleRepresentation findClientRole(RealmResource realmRsc, String clientDbId, String roleName) {
        return realmRsc.clients().get(clientDbId).roles().get(roleName).toRepresentation();
    }

    private static boolean containsRole(Collection<RoleRepresentation> roles, RoleRepresentation role) {
        for (RoleRepresentation setRole : roles) {
            if (setRole.getId().equals(role.getId())) return true;
        }
        return false;
    }

    private static void assertAuthorizationSettingsOtherApp(RealmResource realmRsc) {
        AuthorizationResource authzResource = ApiUtil.findAuthorizationSettings(realmRsc, "OtherApp");
        Assert.assertNotNull(authzResource);

        List<ResourceRepresentation> resources = authzResource.resources().resources();
        Assert.assertThat(resources.stream().map(ResourceRepresentation::getName).collect(Collectors.toList()),
                Matchers.containsInAnyOrder("Default Resource", "test"));

        List<PolicyRepresentation> policies = authzResource.policies().policies();
        Assert.assertThat(policies.stream().map(PolicyRepresentation::getName).collect(Collectors.toList()),
                Matchers.containsInAnyOrder("User Policy", "Default Permission", "test-permission"));
    }

    private static void assertAuthorizationSettingsTestAppAuthz(RealmResource realmRsc) {
        AuthorizationResource authzResource = ApiUtil.findAuthorizationSettings(realmRsc, "test-app-authz");

        Assert.assertNotNull(authzResource);

        List<ResourceRepresentation> resources = authzResource.resources().resources();
        Assert.assertEquals(4, resources.size());
        ResourceServerRepresentation authzSettings = authzResource.getSettings();
        List<Predicate<ResourceRepresentation>> resourcePredicates = new ArrayList<>();
        resourcePredicates.add(resourceRep -> {
            if ("Admin Resource".equals(resourceRep.getName())) {
                Assert.assertEquals(authzSettings.getClientId(), resourceRep.getOwner().getId());
                Assert.assertEquals("/protected/admin/*", resourceRep.getUri());
                Assert.assertEquals("http://test-app-authz/protected/admin", resourceRep.getType());
                Assert.assertEquals("http://icons.com/icon-admin", resourceRep.getIconUri());
                Assert.assertEquals(1, resourceRep.getScopes().size());
                return true;
            }
            return false;
        });
        resourcePredicates.add(resourceRep -> {
            if ("Protected Resource".equals(resourceRep.getName())) {
                Assert.assertEquals(authzSettings.getClientId(), resourceRep.getOwner().getId());
                Assert.assertEquals("/*", resourceRep.getUri());
                Assert.assertEquals("http://test-app-authz/protected/resource", resourceRep.getType());
                Assert.assertEquals("http://icons.com/icon-resource", resourceRep.getIconUri());
                Assert.assertEquals(1, resourceRep.getScopes().size());
                return true;
            }
            return false;
        });
        resourcePredicates.add(resourceRep -> {
            if ("Premium Resource".equals(resourceRep.getName())) {
                Assert.assertEquals(authzSettings.getClientId(), resourceRep.getOwner().getId());
                Assert.assertEquals("/protected/premium/*", resourceRep.getUri());
                Assert.assertEquals("urn:test-app-authz:protected:resource", resourceRep.getType());
                Assert.assertEquals("http://icons.com/icon-premium", resourceRep.getIconUri());
                Assert.assertEquals(1, resourceRep.getScopes().size());
                return true;
            }
            return false;
        });
        resourcePredicates.add(resourceRep -> {
            if ("Main Page".equals(resourceRep.getName())) {
                Assert.assertEquals(authzSettings.getClientId(), resourceRep.getOwner().getId());
                Assert.assertNull(resourceRep.getUri());
                Assert.assertEquals("urn:test-app-authz:protected:resource", resourceRep.getType());
                Assert.assertEquals("http://icons.com/icon-main-page", resourceRep.getIconUri());
                Assert.assertEquals(3, resourceRep.getScopes().size());
                return true;
            }
            return false;
        });
        assertPredicate(resources, resourcePredicates);

        List<ScopeRepresentation> scopes = authzResource.scopes().scopes();
        Assert.assertEquals(6, scopes.size());
        List<Predicate<ScopeRepresentation>> scopePredicates = new ArrayList<>();
        scopePredicates.add(scopeRepresentation -> "admin-access".equals(scopeRepresentation.getName()));
        scopePredicates.add(scopeRepresentation -> "resource-access".equals(scopeRepresentation.getName()));
        scopePredicates.add(scopeRepresentation -> "premium-access".equals(scopeRepresentation.getName()));
        scopePredicates.add(scopeRepresentation -> "urn:test-app-authz:page:main:actionForAdmin".equals(scopeRepresentation.getName()));
        scopePredicates.add(scopeRepresentation -> "urn:test-app-authz:page:main:actionForUser".equals(scopeRepresentation.getName()));
        scopePredicates.add(scopeRepresentation -> "urn:test-app-authz:page:main:actionForPremiumUser".equals(scopeRepresentation.getName()));
        assertPredicate(scopes, scopePredicates);

        List<PolicyRepresentation> policies = authzResource.policies().policies();
        Assert.assertEquals(14, policies.size());
        List<Predicate<PolicyRepresentation>> policyPredicates = new ArrayList<>();
        policyPredicates.add(policyRepresentation -> "Any Admin Policy".equals(policyRepresentation.getName()));
        policyPredicates.add(policyRepresentation -> "Any User Policy".equals(policyRepresentation.getName()));
        policyPredicates.add(representation -> "Client and Realm Role Policy".equals(representation.getName()));
        policyPredicates.add(representation -> "Client Test Policy".equals(representation.getName()));
        policyPredicates.add(representation -> "Group Policy Test".equals(representation.getName()));
        policyPredicates.add(policyRepresentation -> "Only Premium User Policy".equals(policyRepresentation.getName()));
        policyPredicates.add(policyRepresentation -> "wburke policy".equals(policyRepresentation.getName()));
        policyPredicates.add(policyRepresentation -> "All Users Policy".equals(policyRepresentation.getName()));
        policyPredicates.add(policyRepresentation -> "Premium Resource Permission".equals(policyRepresentation.getName()));
        policyPredicates.add(policyRepresentation -> "Administrative Resource Permission".equals(policyRepresentation.getName()));
        policyPredicates.add(policyRepresentation -> "Protected Resource Permission".equals(policyRepresentation.getName()));
        policyPredicates.add(policyRepresentation -> "Action 1 on Main Page Resource Permission".equals(policyRepresentation.getName()));
        policyPredicates.add(policyRepresentation -> "Action 2 on Main Page Resource Permission".equals(policyRepresentation.getName()));
        policyPredicates.add(policyRepresentation -> "Action 3 on Main Page Resource Permission".equals(policyRepresentation.getName()));
        assertPredicate(policies, policyPredicates);
    }

    private static <D> void assertPredicate(List<D> source, List<Predicate<D>> predicate) {
        Assert.assertTrue(!source.stream().filter(object -> !predicate.stream().filter(predicate1 -> predicate1.test(object)).findFirst().isPresent()).findAny().isPresent());
    }

    private static Matcher<Iterable<? super String>> getDefaultClientScopeNameMatcher(ClientRepresentation rep) {
        switch (rep.getClientId()) {
            case "client-with-template":
                return Matchers.hasItem("Default_test_template");
            default:
                return Matchers.not(Matchers.hasItem("Default_test_template"));
        }
    }

    public static void testClientDefaultClientScopes(RealmResource realm) {
        for (ClientRepresentation rep : realm.clients().findAll(true)) {
            Matcher<Iterable<? super String>> expectedDefaultClientScopeNames = getDefaultClientScopeNameMatcher(rep);

            assertThat("Default client scopes for " + rep.getClientId(), rep.getDefaultClientScopes(), expectedDefaultClientScopeNames);
        }
    }

    public static void testRealmDefaultClientScopes(RealmResource realm) {
        // Assert built-in scopes were created in realm
        List<ClientScopeRepresentation> clientScopes = realm.clientScopes().findAll();
        Map<String, ClientScopeRepresentation> clientScopesMap = clientScopes.stream()
          .collect(Collectors.toMap(ClientScopeRepresentation::getName, Function.identity()));

        assertThat(clientScopesMap.keySet(), Matchers.hasItems(
          OAuth2Constants.SCOPE_PROFILE,
          OAuth2Constants.SCOPE_EMAIL,
          OAuth2Constants.SCOPE_ADDRESS,
          OAuth2Constants.SCOPE_PHONE,
          OAuth2Constants.OFFLINE_ACCESS,
          OIDCLoginProtocolFactory.ROLES_SCOPE,
          OIDCLoginProtocolFactory.WEB_ORIGINS_SCOPE,
          OIDCLoginProtocolFactory.MICROPROFILE_JWT_SCOPE,
          OIDCLoginProtocolFactory.ACR_SCOPE,
          SamlProtocolFactory.SCOPE_ROLE_LIST
        ));

        // Check content of some client scopes
        Map<String, ProtocolMapperRepresentation> protocolMappers = clientScopesMap.get(OAuth2Constants.SCOPE_EMAIL).getProtocolMappers()
                .stream().collect(Collectors.toMap(protocolMapper -> protocolMapper.getName(), protocolMapper -> protocolMapper));
        org.keycloak.testsuite.Assert.assertNames(protocolMappers.keySet(), OIDCLoginProtocolFactory.EMAIL, OIDCLoginProtocolFactory.EMAIL_VERIFIED);

        ClientScopeRepresentation offlineScope = clientScopesMap.get(OAuth2Constants.OFFLINE_ACCESS);
        org.keycloak.testsuite.Assert.assertTrue(offlineScope.getProtocolMappers()==null || offlineScope.getProtocolMappers().isEmpty());
        List<RoleRepresentation> offlineRoleScopes = realm.clientScopes().get(offlineScope.getId()).getScopeMappings().realmLevel().listAll();
        org.keycloak.testsuite.Assert.assertNames(offlineRoleScopes, OAuth2Constants.OFFLINE_ACCESS);

        // Check default client scopes and optional client scopes expected
        Set<String> defaultClientScopes = realm.getDefaultDefaultClientScopes()
                .stream().map(ClientScopeRepresentation::getName).collect(Collectors.toSet());
        assertThat(defaultClientScopes, Matchers.hasItems(
          OAuth2Constants.SCOPE_PROFILE,
          OAuth2Constants.SCOPE_EMAIL,
          OIDCLoginProtocolFactory.ROLES_SCOPE,
          OIDCLoginProtocolFactory.WEB_ORIGINS_SCOPE,
          OIDCLoginProtocolFactory.ACR_SCOPE
        ));

        Set<String> optionalClientScopes = realm.getDefaultOptionalClientScopes()
                .stream().map(ClientScopeRepresentation::getName).collect(Collectors.toSet());
        assertThat(optionalClientScopes, Matchers.hasItems(
          OAuth2Constants.SCOPE_ADDRESS,
          OAuth2Constants.SCOPE_PHONE,
          OAuth2Constants.OFFLINE_ACCESS,
          OIDCLoginProtocolFactory.MICROPROFILE_JWT_SCOPE
        ));
    }

    public static void testDefaultPostLogoutRedirectUris(RealmResource realm) {
        for (ClientRepresentation client : realm.clients().findAll()) {
            List<String> redirectUris = client.getRedirectUris();
            if(redirectUris != null && !redirectUris.isEmpty()) {
                String postLogoutRedirectUris = client.getAttributes().get(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS);
                Assert.assertEquals("+", postLogoutRedirectUris);
            }
        }
    }
}
