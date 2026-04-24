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

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.constants.ServiceAccountConstants;
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
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.util.JsonSerialization;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ExportImportUtil {

    // In the old testsuite, this method exists as a public method of ImportTest from the model package.
    // However, model package is not ready to be migrated yet.
    public static void assertDataImportedInRealm(Keycloak adminClient, KeycloakTestingClient testingClient, RealmRepresentation realm) throws IOException {
        Assertions.assertTrue(realm.isVerifyEmail());
        Assertions.assertEquals((Integer)3600000, realm.getOfflineSessionIdleTimeout());
        Assertions.assertEquals((Integer)1500, realm.getAccessTokenLifespanForImplicitFlow());
        Assertions.assertEquals((Integer)1800, realm.getSsoSessionIdleTimeout());
        Assertions.assertEquals((Integer)36000, realm.getSsoSessionMaxLifespan());
        Assertions.assertEquals((Integer)3600, realm.getSsoSessionIdleTimeoutRememberMe());
        Assertions.assertEquals((Integer)172800, realm.getSsoSessionMaxLifespanRememberMe());

        Set<String> creds = realm.getRequiredCredentials();
        Assertions.assertEquals(1, creds.size());
        String cred = (String)creds.iterator().next();
        Assertions.assertEquals("password", cred);

        RealmResource realmRsc = adminClient.realm(realm.getRealm());

        UPConfig upConfig = realmRsc.users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        realmRsc.users().userProfile().update(upConfig);

        UserRepresentation user = findByUsername(realmRsc, "loginclient");
        Assertions.assertNotNull(user);

        UserResource userRsc = realmRsc.users().get(user.getId());
        Assertions.assertEquals(0, userRsc.getFederatedIdentity().size());

        List<ClientRepresentation> resources = realmRsc.clients().findAll();
        Assertions.assertEquals(10, resources.size());

        // Test applications imported
        ClientRepresentation application = AdminApiUtil.findClientByClientId(realmRsc, "Application").toRepresentation();
        ClientRepresentation otherApp = AdminApiUtil.findClientByClientId(realmRsc, "OtherApp").toRepresentation();
        ClientRepresentation accountApp = AdminApiUtil.findClientByClientId(realmRsc, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).toRepresentation();
        ClientRepresentation testAppAuthzApp = AdminApiUtil.findClientByClientId(realmRsc, "test-app-authz").toRepresentation();
        ClientResource nonExisting = AdminApiUtil.findClientByClientId(realmRsc, "NonExisting");
        Assertions.assertNotNull(application);
        Assertions.assertNotNull(otherApp);
        Assertions.assertNull(nonExisting);
        List<ClientRepresentation> clients = realmRsc.clients().findAll();
        Assertions.assertEquals(10, clients.size());
        Assertions.assertTrue(hasClient(clients, application));
        Assertions.assertTrue(hasClient(clients, otherApp));
        Assertions.assertTrue(hasClient(clients, accountApp));

        Assertions.assertEquals("Applicationn", application.getName());
        Assertions.assertEquals((Integer)50, application.getNodeReRegistrationTimeout());
        Map<String, Integer> appRegisteredNodes = application.getRegisteredNodes();
        Assertions.assertEquals(2, appRegisteredNodes.size());
        Assertions.assertTrue(10 == appRegisteredNodes.get("node1"));
        Assertions.assertTrue(20 == appRegisteredNodes.get("172.10.15.20"));

        // test clientAuthenticatorType
        Assertions.assertEquals("client-secret", application.getClientAuthenticatorType());
        Assertions.assertEquals("client-jwt", otherApp.getClientAuthenticatorType());

        // test authenticationFlowBindingOverrides
        Map<String, String> flowMap = otherApp.getAuthenticationFlowBindingOverrides();
        Assertions.assertNotNull(flowMap);
        Assertions.assertEquals(1, flowMap.size());
        Assertions.assertTrue(flowMap.containsKey("browser"));
        // if the authentication flows were correctly imported there must be a flow whose id matches the one in the authenticationFlowBindingOverrides
        AuthenticationFlowRepresentation flowRep = realmRsc.flows().getFlow(flowMap.get("browser"));
        Assertions.assertNotNull(flowRep);
        Assertions.assertEquals("browser", flowRep.getAlias());

        // Test finding applications by ID
        Assertions.assertNull(AdminApiUtil.findClientResourceById(realmRsc, "982734"));
        Assertions.assertEquals(application.getId(), AdminApiUtil.findClientResourceById(realmRsc, application.getId()).toRepresentation().getId());


        // Test role mappings
        UserRepresentation admin = findByUsername(realmRsc, "admin");

        Assertions.assertNotNull(admin.getCreatedTimestamp());
        Set<RoleRepresentation> allRoles = allRoles(realmRsc, admin);
        Assertions.assertEquals(3, allRoles.size());
        Assertions.assertTrue(containsRole(allRoles, findRealmRole(realmRsc, "admin")));
        Assertions.assertTrue(containsRole(allRoles, findClientRole(realmRsc, application.getId(), "app-admin")));
        Assertions.assertTrue(containsRole(allRoles, findClientRole(realmRsc, otherApp.getId(), "otherapp-admin")));

        UserRepresentation wburke = findByUsername(realmRsc, "wburke");
        // user with creation timestamp in import
        Assertions.assertEquals(Long.valueOf(123654), wburke.getCreatedTimestamp());
        allRoles = allRoles(realmRsc, wburke);
        Assertions.assertEquals(2, allRoles.size());
        Assertions.assertFalse(containsRole(allRoles, findRealmRole(realmRsc, "admin")));
        Assertions.assertTrue(containsRole(allRoles, findClientRole(realmRsc, application.getId(), "app-user")));
        Assertions.assertTrue(containsRole(allRoles, findClientRole(realmRsc, otherApp.getId(), "otherapp-user")));

        Assertions.assertNull(realmRsc.users().get(wburke.getId()).roles().getAll().getRealmMappings());

        Assertions.assertEquals((Object) 159, wburke.getNotBefore());

        UserRepresentation loginclient = findByUsername(realmRsc, "loginclient");
        // user with creation timestamp as string in import
        Assertions.assertEquals(Long.valueOf(123655), loginclient.getCreatedTimestamp());

        UserRepresentation hashedPasswordUser = findByUsername(realmRsc, "hashedpassworduser");
        CredentialRepresentation password = realmRsc.users().get(hashedPasswordUser.getId()).credentials().stream()
                .filter(credential -> PasswordCredentialModel.TYPE.equals(credential.getType()))
                .findFirst().get();
        PasswordCredentialData credentialData = JsonSerialization.readValue(password.getCredentialData(), PasswordCredentialData.class);
        Assertions.assertEquals(1234, credentialData.getHashIterations());

        List<RoleRepresentation> realmRoles = realmRolesForUser(realmRsc, admin);
        Assertions.assertEquals(1, realmRoles.size());
        Assertions.assertEquals("admin", realmRoles.iterator().next().getName());

        List<RoleRepresentation> appRoles = clientRolesForUser(realmRsc, application, admin);
        Assertions.assertEquals(1, appRoles.size());
        Assertions.assertEquals("app-admin", appRoles.iterator().next().getName());

        // Test attributes
        Map<String, List<String>> attrs = wburke.getAttributes();
        Assertions.assertEquals(1, attrs.size());
        List<String> attrVals = attrs.get("old-email");
        Assertions.assertEquals(1, attrVals.size());
        Assertions.assertEquals("bburke@redhat.com", attrVals.get(0));

        attrs = admin.getAttributes();
        Assertions.assertEquals(2, attrs.size());
        attrVals = attrs.get("key1");
        Assertions.assertEquals(1, attrVals.size());
        Assertions.assertEquals("val1", attrVals.get(0));
        attrVals = attrs.get("key2");
        Assertions.assertEquals(2, attrVals.size());
        Assertions.assertTrue(attrVals.contains("val21") && attrVals.contains("val22"));

        // Test client
        ClientResource oauthClient = AdminApiUtil.findClientResourceByClientId(realmRsc, "oauthclient");
        ClientRepresentation oauthClientRep = oauthClient.toRepresentation();
        Assertions.assertEquals("clientpassword", oauthClient.getSecret().getValue());
        Assertions.assertTrue(oauthClientRep.isEnabled());
        Assertions.assertNotNull(oauthClientRep);

        // Test scope relationship
        Set<RoleRepresentation> allScopes = allScopeMappings(oauthClient);
        Assertions.assertEquals(2, allScopes.size());
        Assertions.assertTrue(containsRole(allScopes, findRealmRole(realmRsc, "admin")));
        Assertions.assertTrue(containsRole(allScopes, findClientRole(realmRsc, application.getId(), "app-user")));

        List<RoleRepresentation> realmScopes = realmScopeMappings(oauthClient);
        Assertions.assertTrue(containsRole(realmScopes, findRealmRole(realmRsc, "admin")));

        List<RoleRepresentation> appScopes = clientScopeMappings(oauthClient);
        Assertions.assertTrue(containsRole(appScopes, findClientRole(realmRsc, application.getId(), "app-user")));

        // Test social linking
        UserResource socialUser = realmRsc.users().get(findByUsername(realmRsc, "mySocialUser").getId());
        List<FederatedIdentityRepresentation> socialLinks = socialUser.getFederatedIdentity();
        Assertions.assertEquals(3, socialLinks.size());
        boolean facebookFound = false;
        boolean googleFound = false;
        boolean twitterFound = false;
        FederatedIdentityRepresentation facebookIdentityRep = null;
        for (FederatedIdentityRepresentation federatedIdentityRep : socialLinks) {
            if ("facebook1".equals(federatedIdentityRep.getIdentityProvider())) {
                facebookFound = true;
                facebookIdentityRep = federatedIdentityRep;
                Assertions.assertEquals("facebook1",federatedIdentityRep.getUserId());
                Assertions.assertEquals("fbuser1", federatedIdentityRep.getUserName());
            } else if ("google1".equals(federatedIdentityRep.getIdentityProvider())) {
                googleFound = true;
                Assertions.assertEquals("google1", federatedIdentityRep.getUserId());
                Assertions.assertEquals("mySocialUser@gmail.com", federatedIdentityRep.getUserName());
            } else if ("twitter1".equals(federatedIdentityRep.getIdentityProvider())) {
                twitterFound = true;
                Assertions.assertEquals("twitter1", federatedIdentityRep.getUserId());
                Assertions.assertEquals("twuser1", federatedIdentityRep.getUserName());
            }
        }
        Assertions.assertTrue(facebookFound && twitterFound && googleFound);

        // make sure the username format is the same when importing
        UserResource socialUserLowercase = realmRsc.users().get(findByUsername(realmRsc, "lowercasesocialuser").getId());
        List<FederatedIdentityRepresentation> socialLowercaseLinks = socialUserLowercase.getFederatedIdentity();
        Assertions.assertEquals(1, socialLowercaseLinks.size());
        Assertions.assertEquals("lowercasesocialuser@gmail.com", socialLowercaseLinks.get(0).getUserName());

        UserRepresentation foundSocialUser =  testingClient.testing(realm.getRealm()).getUserByFederatedIdentity(realm.getRealm(), "facebook1", "facebook1", "fbuser1");
        Assertions.assertEquals(foundSocialUser.getUsername(), socialUser.toRepresentation().getUsername());
        Assertions.assertNull(testingClient.testing(realm.getRealm()).getUserByFederatedIdentity(realm.getRealm(), "facebook", "not-existing", "not-existing"));

        Assertions.assertEquals("facebook1", facebookIdentityRep.getUserId());
        Assertions.assertEquals("fbuser1", facebookIdentityRep.getUserName());
        Assertions.assertEquals("facebook1", facebookIdentityRep.getIdentityProvider());

        // Test remove/add social link
        socialUser.removeFederatedIdentity("facebook1");
        Assertions.assertEquals(2, socialUser.getFederatedIdentity().size());
        socialUser.addFederatedIdentity("facebook1", facebookIdentityRep);
        Assertions.assertEquals(3, socialUser.getFederatedIdentity().size());

        // Test smtp config
        Map<String, String> smtpConfig = realm.getSmtpServer();
        Assertions.assertTrue(smtpConfig.size() == 3);
        Assertions.assertEquals("auto@keycloak.org", smtpConfig.get("from"));
        Assertions.assertEquals("localhost", smtpConfig.get("host"));
        Assertions.assertEquals("3025", smtpConfig.get("port"));

        // Test identity providers
        List<IdentityProviderRepresentation> identityProviders = realmRsc.identityProviders().findAll();
        Assertions.assertEquals(4, identityProviders.size());
        IdentityProviderRepresentation google = null;
        for (IdentityProviderRepresentation idpRep : identityProviders) {
            if (idpRep.getAlias().equals("google1")) google = idpRep;
        }
        Assertions.assertNotNull(google);
        Assertions.assertEquals("google1", google.getAlias());
        Assertions.assertEquals("google", google.getProviderId());
        Assertions.assertTrue(google.isEnabled());
        Assertions.assertEquals("googleId", google.getConfig().get("clientId"));
        Assertions.assertEquals("**********", google.getConfig().get("clientSecret")); // secret is masked in GET call

        //////////////////
        // Test federation providers
        // on import should convert UserfederationProviderRepresentation to Component model
        List<UserFederationProviderRepresentation> fedProviders = realm.getUserFederationProviders();
        Assertions.assertTrue(fedProviders == null || fedProviders.size() == 0);
        List<ComponentRepresentation> storageProviders = realmRsc.components().query(realm.getId(), UserStorageProvider.class.getName());
        Assertions.assertTrue(storageProviders.size() == 2);
        ComponentRepresentation ldap1 = storageProviders.get(0);
        ComponentRepresentation ldap2 = storageProviders.get(1);
        if (!"MyLDAPProvider1".equals(ldap1.getName())) {
            ldap2 = ldap1;
            ldap1 = storageProviders.get(1);
        }
        Assertions.assertEquals("MyLDAPProvider1", ldap1.getName());
        Assertions.assertEquals("ldap", ldap1.getProviderId());
        Assertions.assertEquals("1", ldap1.getConfig().getFirst("priority"));
        Assertions.assertEquals("ldap://foo", ldap1.getConfig().getFirst(LDAPConstants.CONNECTION_URL));

        Assertions.assertEquals("MyLDAPProvider2", ldap2.getName());
        Assertions.assertEquals("ldap://bar", ldap2.getConfig().getFirst(LDAPConstants.CONNECTION_URL));

        // Test federation mappers
        List<ComponentRepresentation> fedMappers1 = realmRsc.components().query(ldap1.getId(), LDAPStorageMapper.class.getName());
        ComponentRepresentation fullNameMapper = fedMappers1.iterator().next();
        Assertions.assertEquals("FullNameMapper", fullNameMapper.getName());
        Assertions.assertEquals(FullNameLDAPStorageMapperFactory.PROVIDER_ID, fullNameMapper.getProviderId());
        Assertions.assertEquals("cn", fullNameMapper.getConfig().getFirst(FullNameLDAPStorageMapper.LDAP_FULL_NAME_ATTRIBUTE));
        /////////////////

        // Assert that federation link wasn't created during import
        Assertions.assertNull(testingClient.testing(realm.getRealm()).getUserByUsernameFromFedProviderFactory(realm.getRealm(), "wburke"));

        // Test builtin authentication flows
        AuthenticationFlowRepresentation clientFlow = testingClient.testing(realm.getRealm()).getClientAuthFlow(realm.getRealm());
        Assertions.assertEquals(DefaultAuthenticationFlows.CLIENT_AUTHENTICATION_FLOW, clientFlow.getAlias());
        Assertions.assertNotNull(realmRsc.flows().getFlow(clientFlow.getId()));
        Assertions.assertTrue(realmRsc.flows().getExecutions(clientFlow.getAlias()).size() > 0);

        AuthenticationFlowRepresentation resetFlow = testingClient.testing(realm.getRealm()).getResetCredFlow(realm.getRealm());
        Assertions.assertEquals(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, resetFlow.getAlias());
        Assertions.assertNotNull(realmRsc.flows().getFlow(resetFlow.getId()));
        Assertions.assertTrue(realmRsc.flows().getExecutions(resetFlow.getAlias()).size() > 0);

        // Test protocol mappers. Default application doesn't have any builtin protocol mappers. OtherApp just gss credential
        List<ProtocolMapperRepresentation> applicationMappers = application.getProtocolMappers();
        Assertions.assertNull(findMapperByName(applicationMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));//application.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));
        Assertions.assertNull(findMapperByName(applicationMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "email"));
        Assertions.assertNull(findMapperByName(applicationMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "given name"));
        Assertions.assertNull(findMapperByName(applicationMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));

        Assertions.assertEquals(1, otherApp.getProtocolMappers().size());
        List<ProtocolMapperRepresentation> otherAppMappers = otherApp.getProtocolMappers();
        Assertions.assertNull(findMapperByName(otherAppMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));
        ProtocolMapperRepresentation gssCredentialMapper = findMapperByName(otherAppMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME);
        assertGssProtocolMapper(gssCredentialMapper);

        // Test clientScopes
        List<ClientScopeRepresentation> clientScopes = realmRsc.clientScopes().findAll();
        ClientScopeRepresentation clientScope = clientScopes.stream().filter((ClientScopeRepresentation clientScope1) -> {

            return "foo_scope".equals(clientScope1.getName());

        }).findFirst().get();
        Assertions.assertEquals("foo_scope", clientScope.getName());
        Assertions.assertEquals("foo scope-desc", clientScope.getDescription());
        Assertions.assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, clientScope.getProtocol());
        Assertions.assertEquals(1, clientScope.getProtocolMappers().size());
        List<ProtocolMapperRepresentation> clientScopeMappers = clientScope.getProtocolMappers();
        ProtocolMapperRepresentation scopeGssCredentialMapper = findMapperByName(clientScopeMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME);
        assertGssProtocolMapper(scopeGssCredentialMapper);

        // Test client scope - scopes
        Set<RoleRepresentation> allClientScopeScopes = allScopeMappings(realmRsc.clientScopes().get(clientScope.getId()));
        Assertions.assertEquals(3, allClientScopeScopes.size());
        Assertions.assertTrue(containsRole(allClientScopeScopes, findRealmRole(realmRsc, "admin")));
        Assertions.assertTrue(containsRole(allClientScopeScopes, findClientRole(realmRsc, application.getId(), "app-user")));
        Assertions.assertTrue(containsRole(allClientScopeScopes, findClientRole(realmRsc, application.getId(), "app-admin")));

        List<RoleRepresentation> clientScopeRealmScopes = realmScopeMappings(realmRsc.clientScopes().get(clientScope.getId()));
        Assertions.assertTrue(containsRole(clientScopeRealmScopes, findRealmRole(realmRsc, "admin")));

        List<RoleRepresentation> clientScopeAppScopes = clientScopeMappings(realmRsc.clientScopes().get(clientScope.getId()));
        Assertions.assertTrue(containsRole(clientScopeAppScopes, findClientRole(realmRsc, application.getId(), "app-user")));
        Assertions.assertTrue(containsRole(clientScopeAppScopes, findClientRole(realmRsc, application.getId(), "app-admin")));

        // Test client scopes assignment
        Assertions.assertTrue(otherApp.getDefaultClientScopes().contains("foo_scope"));
        Assertions.assertFalse(application.getDefaultClientScopes().contains("foo_scope"));

        // Test builtin client scopes
        testRealmDefaultClientScopes(realmRsc);

        // Test user consents
        UserResource adminRsc = realmRsc.users().get(admin.getId());
        List<Map<String, Object>> consents = adminRsc.getConsents();
        Assertions.assertEquals(2, consents.size());//.getConsents().size());

        Map<String, Object> appAdminConsent = findConsentByClientId(consents, application.getClientId());
        Assertions.assertNotNull(appAdminConsent);
        Assertions.assertTrue(isClientScopeGranted(appAdminConsent, OAuth2Constants.OFFLINE_ACCESS, "roles", "profile", "email", "account", "web-origins"));

        Map<String, Object> otherAppAdminConsent = findConsentByClientId(consents, otherApp.getClientId());//admin.getConsentByClient(otherApp.getId());
        Assertions.assertFalse(isClientScopeGranted(otherAppAdminConsent, OAuth2Constants.OFFLINE_ACCESS));

        Assertions.assertTrue(application.isStandardFlowEnabled());
        Assertions.assertTrue(application.isImplicitFlowEnabled());
        Assertions.assertTrue(application.isDirectAccessGrantsEnabled());
        Assertions.assertFalse(otherApp.isStandardFlowEnabled());
        Assertions.assertFalse(otherApp.isImplicitFlowEnabled());
        Assertions.assertFalse(otherApp.isDirectAccessGrantsEnabled());

        // Test service accounts
        Assertions.assertFalse(application.isServiceAccountsEnabled());
        Assertions.assertTrue(otherApp.isServiceAccountsEnabled());

        if (ProfileAssume.isFeatureEnabled(Profile.Feature.AUTHORIZATION)) {
            Assertions.assertTrue(testAppAuthzApp.isServiceAccountsEnabled());
            Assertions.assertNull(testingClient.testing(realm.getRealm()).getUserByServiceAccountClient(realm.getRealm(), application.getClientId()));//session.users().getUserByServiceAccountClient(application));
            UserRepresentation otherAppSA = testingClient.testing(realm.getRealm()).getUserByServiceAccountClient(realm.getRealm(), otherApp.getClientId());//session.users().getUserByServiceAccountClient(otherApp);
            Assertions.assertNotNull(otherAppSA);
            Assertions.assertEquals("service-account-otherapp", otherAppSA.getUsername());
            UserRepresentation testAppAuthzSA = testingClient.testing(realm.getRealm()).getUserByServiceAccountClient(realm.getRealm(), testAppAuthzApp.getClientId());
            Assertions.assertNotNull(testAppAuthzSA);
            Assertions.assertEquals("service-account-test-app-authz", testAppAuthzSA.getUsername());

            // test service account maintains the roles in OtherApp
            allRoles = allRoles(realmRsc, otherAppSA);
            Assertions.assertEquals(3, allRoles.size());
            Assertions.assertTrue(containsRole(allRoles, findRealmRole(realmRsc, "user")));
            Assertions.assertTrue(containsRole(allRoles, findClientRole(realmRsc, otherApp.getId(), "otherapp-user")));
            Assertions.assertTrue(containsRole(allRoles, findClientRole(realmRsc, otherApp.getId(), "otherapp-admin")));

            assertAuthorizationSettingsOtherApp(realmRsc);
            assertAuthorizationSettingsTestAppAuthz(realmRsc);
        }

        // Test Message Bundle
        Map<String, String> localizations = adminClient.realm(realm.getRealm()).localization().getRealmLocalizationTexts("en");
        Assertions.assertEquals("value1", localizations.get("key1"));
        Assertions.assertEquals("value2", localizations.get("key2"));
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
        Assertions.assertEquals(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME, gssCredentialMapper.getName());
        Assertions.assertEquals( OIDCLoginProtocol.LOGIN_PROTOCOL, gssCredentialMapper.getProtocol());
        Assertions.assertEquals(UserSessionNoteMapper.PROVIDER_ID, gssCredentialMapper.getProtocolMapper());
        String includeInAccessToken = gssCredentialMapper.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN);
        String includeInIdToken = gssCredentialMapper.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN);
        Assertions.assertTrue(includeInAccessToken.equalsIgnoreCase("true"));
        Assertions.assertTrue(includeInIdToken == null || Boolean.parseBoolean(includeInIdToken) == false);
    }

    public static ProtocolMapperRepresentation findMapperByName(List<ProtocolMapperRepresentation> mappers, String type, String name) {
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

    private static UserRepresentation findByUsername(RealmResource realmRsc, String username) {
        List<UserRepresentation> usersByUsername = realmRsc.users().search(username);
        MatcherAssert.assertThat(usersByUsername, Matchers.hasSize(1));
        return usersByUsername.get(0);
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

        for (ClientMappingsRepresentation clientMappingsRepresentation : clientRoles.values()) {
            List<RoleRepresentation> clientRoleScopeMappings = clientMappingsRepresentation.getMappings();
            if (clientRoleScopeMappings != null) clientScopeMappings.addAll(clientRoleScopeMappings);
        }

        return clientScopeMappings;
    }

    private static List<RoleRepresentation> clientScopeMappings(ClientScopeResource client) {
        List<RoleRepresentation> clientScopeMappings = new LinkedList<>();
        Map<String, ClientMappingsRepresentation> clientRoles = client.getScopeMappings().getAll().getClientMappings();
        if (clientRoles == null) return clientScopeMappings;

        for (ClientMappingsRepresentation clientMappingsRepresentation : clientRoles.values()) {
            List<RoleRepresentation> clientRoleScopeMappings = clientMappingsRepresentation.getMappings();
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
        AuthorizationResource authzResource = AdminApiUtil.findAuthorizationSettings(realmRsc, "OtherApp");
        Assertions.assertNotNull(authzResource);

        List<ResourceRepresentation> resources = authzResource.resources().resources();
        assertThat(resources.stream().map(ResourceRepresentation::getName).collect(Collectors.toList()),
                Matchers.containsInAnyOrder("Default Resource", "test"));

        List<PolicyRepresentation> policies = authzResource.policies().policies();
        assertThat(policies.stream().map(PolicyRepresentation::getName).collect(Collectors.toList()),
                Matchers.containsInAnyOrder("User Policy", "Default Permission", "test-permission"));
    }

    private static void assertAuthorizationSettingsTestAppAuthz(RealmResource realmRsc) {
        AuthorizationResource authzResource = AdminApiUtil.findAuthorizationSettings(realmRsc, "test-app-authz");

        Assertions.assertNotNull(authzResource);

        List<ResourceRepresentation> resources = authzResource.resources().resources();
        Assertions.assertEquals(4, resources.size());
        ResourceServerRepresentation authzSettings = authzResource.getSettings();
        List<Predicate<ResourceRepresentation>> resourcePredicates = new ArrayList<>();
        resourcePredicates.add(resourceRep -> {
            if ("Admin Resource".equals(resourceRep.getName())) {
                Assertions.assertEquals(authzSettings.getClientId(), resourceRep.getOwner().getId());
                Assertions.assertEquals("/protected/admin/*", resourceRep.getUri());
                Assertions.assertEquals("http://test-app-authz/protected/admin", resourceRep.getType());
                Assertions.assertEquals("http://icons.com/icon-admin", resourceRep.getIconUri());
                Assertions.assertEquals(1, resourceRep.getScopes().size());
                return true;
            }
            return false;
        });
        resourcePredicates.add(resourceRep -> {
            if ("Protected Resource".equals(resourceRep.getName())) {
                Assertions.assertEquals(authzSettings.getClientId(), resourceRep.getOwner().getId());
                Assertions.assertEquals("/*", resourceRep.getUri());
                Assertions.assertEquals("http://test-app-authz/protected/resource", resourceRep.getType());
                Assertions.assertEquals("http://icons.com/icon-resource", resourceRep.getIconUri());
                Assertions.assertEquals(1, resourceRep.getScopes().size());
                return true;
            }
            return false;
        });
        resourcePredicates.add(resourceRep -> {
            if ("Premium Resource".equals(resourceRep.getName())) {
                Assertions.assertEquals(authzSettings.getClientId(), resourceRep.getOwner().getId());
                Assertions.assertEquals("/protected/premium/*", resourceRep.getUri());
                Assertions.assertEquals("urn:test-app-authz:protected:resource", resourceRep.getType());
                Assertions.assertEquals("http://icons.com/icon-premium", resourceRep.getIconUri());
                Assertions.assertEquals(1, resourceRep.getScopes().size());
                return true;
            }
            return false;
        });
        resourcePredicates.add(resourceRep -> {
            if ("Main Page".equals(resourceRep.getName())) {
                Assertions.assertEquals(authzSettings.getClientId(), resourceRep.getOwner().getId());
                Assertions.assertNull(resourceRep.getUri());
                Assertions.assertEquals("urn:test-app-authz:protected:resource", resourceRep.getType());
                Assertions.assertEquals("http://icons.com/icon-main-page", resourceRep.getIconUri());
                Assertions.assertEquals(3, resourceRep.getScopes().size());
                return true;
            }
            return false;
        });
        assertPredicate(resources, resourcePredicates);

        List<ScopeRepresentation> scopes = authzResource.scopes().scopes();
        Assertions.assertEquals(6, scopes.size());
        List<Predicate<ScopeRepresentation>> scopePredicates = new ArrayList<>();
        scopePredicates.add(scopeRepresentation -> "admin-access".equals(scopeRepresentation.getName()));
        scopePredicates.add(scopeRepresentation -> "resource-access".equals(scopeRepresentation.getName()));
        scopePredicates.add(scopeRepresentation -> "premium-access".equals(scopeRepresentation.getName()));
        scopePredicates.add(scopeRepresentation -> "urn:test-app-authz:page:main:actionForAdmin".equals(scopeRepresentation.getName()));
        scopePredicates.add(scopeRepresentation -> "urn:test-app-authz:page:main:actionForUser".equals(scopeRepresentation.getName()));
        scopePredicates.add(scopeRepresentation -> "urn:test-app-authz:page:main:actionForPremiumUser".equals(scopeRepresentation.getName()));
        assertPredicate(scopes, scopePredicates);

        List<PolicyRepresentation> policies = authzResource.policies().policies();
        Assertions.assertEquals(14, policies.size());
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
        Assertions.assertTrue(!source.stream().filter(object -> !predicate.stream().filter(predicate1 -> predicate1.test(object)).findFirst().isPresent()).findAny().isPresent());
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
          OIDCLoginProtocolFactory.BASIC_SCOPE,
          ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE,
          SamlProtocolFactory.SCOPE_ROLE_LIST
        ));

        // Check content of some client scopes
        Map<String, ProtocolMapperRepresentation> protocolMappers = clientScopesMap.get(OAuth2Constants.SCOPE_EMAIL).getProtocolMappers()
                .stream().collect(Collectors.toMap(protocolMapper -> protocolMapper.getName(), protocolMapper -> protocolMapper));
        org.keycloak.testsuite.Assert.assertNames(protocolMappers.keySet(), OIDCLoginProtocolFactory.EMAIL, OIDCLoginProtocolFactory.EMAIL_VERIFIED);

        ClientScopeRepresentation offlineScope = clientScopesMap.get(OAuth2Constants.OFFLINE_ACCESS);
        Assertions.assertTrue(offlineScope.getProtocolMappers()==null || offlineScope.getProtocolMappers().isEmpty());
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
          OIDCLoginProtocolFactory.ACR_SCOPE,
          OIDCLoginProtocolFactory.BASIC_SCOPE
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
                Assertions.assertEquals("+", postLogoutRedirectUris);
            }
        }
    }
}
