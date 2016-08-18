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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.Assert;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientTemplateResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapperFactory;
import org.keycloak.models.Constants;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientMappingsRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserFederationMapperRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.RealmRepUtil;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ExportImportUtil {

    // In the old testsuite, this method exists as a public method of ImportTest from the model package.
    // However, model package is not ready to be migrated yet.
    public static void assertDataImportedInRealm(Keycloak adminClient, KeycloakTestingClient testingClient, RealmRepresentation realm) {
        Assert.assertTrue(realm.isVerifyEmail());
        Assert.assertEquals((Integer)3600000, realm.getOfflineSessionIdleTimeout());
        Assert.assertEquals((Integer)1500, realm.getAccessTokenLifespanForImplicitFlow());

        Set<String> creds = realm.getRequiredCredentials();
        Assert.assertEquals(1, creds.size());
        String cred = (String)creds.iterator().next();
        Assert.assertEquals("password", cred);
        Assert.assertEquals(4, realm.getDefaultRoles().size());

        Assert.assertNotNull(RealmRepUtil.findDefaultRole(realm, "foo"));
        Assert.assertNotNull(RealmRepUtil.findDefaultRole(realm, "bar"));

        RealmResource realmRsc = adminClient.realm(realm.getRealm());

        /* See KEYCLOAK-3104*/
        UserRepresentation user = findByUsername(realmRsc, "loginclient");
        Assert.assertNotNull(user);

        UserResource userRsc = realmRsc.users().get(user.getId());
        Assert.assertEquals(0, userRsc.getFederatedIdentity().size());

        List<ClientRepresentation> resources = realmRsc.clients().findAll();
        Assert.assertEquals(9, resources.size());

        // Test applications imported
        ClientRepresentation application = ApiUtil.findClientByClientId(realmRsc, "Application").toRepresentation();
        ClientRepresentation otherApp = ApiUtil.findClientByClientId(realmRsc, "OtherApp").toRepresentation();
        ClientRepresentation accountApp = ApiUtil.findClientByClientId(realmRsc, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).toRepresentation();
        ClientResource nonExisting = ApiUtil.findClientByClientId(realmRsc, "NonExisting");
        Assert.assertNotNull(application);
        Assert.assertNotNull(otherApp);
        Assert.assertNull(nonExisting);
        List<ClientRepresentation> clients = realmRsc.clients().findAll();
        Assert.assertEquals(9, clients.size());
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

        Assert.assertTrue(findClientRole(realmRsc, application.getId(), "app-admin").isScopeParamRequired());
        Assert.assertFalse(findClientRole(realmRsc, otherApp.getId(), "otherapp-admin").isScopeParamRequired());
        Assert.assertFalse(findClientRole(realmRsc, otherApp.getId(), "otherapp-user").isScopeParamRequired());

        UserRepresentation wburke = findByUsername(realmRsc, "wburke");
        // user with creation timestamp in import
        Assert.assertEquals(new Long(123654), wburke.getCreatedTimestamp());
        allRoles = allRoles(realmRsc, wburke);
        Assert.assertEquals(2, allRoles.size());
        Assert.assertFalse(containsRole(allRoles, findRealmRole(realmRsc, "admin")));
        Assert.assertTrue(containsRole(allRoles, findClientRole(realmRsc, application.getId(), "app-user")));
        Assert.assertTrue(containsRole(allRoles, findClientRole(realmRsc, otherApp.getId(), "otherapp-user")));

        Assert.assertNull(realmRsc.users().get(wburke.getId()).roles().getAll().getRealmMappings());

        UserRepresentation loginclient = findByUsername(realmRsc, "loginclient");
        // user with creation timestamp as string in import
        Assert.assertEquals(new Long(123655), loginclient.getCreatedTimestamp());

        List<RoleRepresentation> realmRoles = realmRolesForUser(realmRsc, admin);
        Assert.assertEquals(1, realmRoles.size());
        Assert.assertEquals("admin", realmRoles.iterator().next().getName());

        List<RoleRepresentation> appRoles = clientRolesForUser(realmRsc, application, admin);
        Assert.assertEquals(1, appRoles.size());
        Assert.assertEquals("app-admin", appRoles.iterator().next().getName());

        // Test attributes
        Map<String, List<String>> attrs = wburke.getAttributesAsListValues();
        Assert.assertEquals(1, attrs.size());
        List<String> attrVals = attrs.get("email");
        Assert.assertEquals(1, attrVals.size());
        Assert.assertEquals("bburke@redhat.com", attrVals.get(0));

        attrs = admin.getAttributesAsListValues();
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

        // Test federation providers
        List<UserFederationProviderRepresentation> fedProviders = realm.getUserFederationProviders();
        Assert.assertTrue(fedProviders.size() == 2);
        UserFederationProviderRepresentation ldap1 = fedProviders.get(0);
        Assert.assertEquals("MyLDAPProvider1", ldap1.getDisplayName());
        Assert.assertEquals("ldap", ldap1.getProviderName());
        Assert.assertEquals(1, ldap1.getPriority());
        Assert.assertEquals("ldap://foo", ldap1.getConfig().get(LDAPConstants.CONNECTION_URL));

        UserFederationProviderRepresentation ldap2 = fedProviders.get(1);
        Assert.assertEquals("MyLDAPProvider2", ldap2.getDisplayName());
        Assert.assertEquals("ldap://bar", ldap2.getConfig().get(LDAPConstants.CONNECTION_URL));

        // Test federation mappers
        List<UserFederationMapperRepresentation> fedMappers1 = realmRsc.userFederation().get(ldap1.getId()).getMappers();
        Assert.assertTrue(fedMappers1.size() == 1);
        UserFederationMapperRepresentation fullNameMapper = fedMappers1.iterator().next();
        Assert.assertEquals("FullNameMapper", fullNameMapper.getName());
        Assert.assertEquals(FullNameLDAPFederationMapperFactory.PROVIDER_ID, fullNameMapper.getFederationMapperType());
        //Assert.assertEquals(ldap1.getId(), fullNameMapper.getFederationProviderId());
        Assert.assertEquals("cn", fullNameMapper.getConfig().get(FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE));

        // All builtin LDAP mappers should be here
        List<UserFederationMapperRepresentation> fedMappers2 = realmRsc.userFederation().get(ldap2.getId()).getMappers();
        Assert.assertTrue(fedMappers2.size() > 3);
        List<UserFederationMapperRepresentation> allMappers = realm.getUserFederationMappers();
        Assert.assertEquals(allMappers.size(), fedMappers1.size() + fedMappers2.size());

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

        // Test protocol mappers. Default application has all the builtin protocol mappers. OtherApp just gss credential
        List<ProtocolMapperRepresentation> applicationMappers = application.getProtocolMappers();
        Assert.assertNotNull(findMapperByName(applicationMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));//application.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));
        Assert.assertNotNull(findMapperByName(applicationMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "email"));
        Assert.assertNotNull(findMapperByName(applicationMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "given name"));
        Assert.assertNull(findMapperByName(applicationMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));

        Assert.assertEquals(1, otherApp.getProtocolMappers().size());
        List<ProtocolMapperRepresentation> otherAppMappers = otherApp.getProtocolMappers();
        Assert.assertNull(findMapperByName(otherAppMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "username"));
        ProtocolMapperRepresentation gssCredentialMapper = findMapperByName(otherAppMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME);
        assertGssProtocolMapper(gssCredentialMapper);

        // Test clientTemplates
        List<ClientTemplateRepresentation> clientTemplates = realmRsc.clientTemplates().findAll();
        Assert.assertEquals(1, clientTemplates.size());
        ClientTemplateRepresentation clientTemplate = clientTemplates.get(0);
        Assert.assertEquals("foo-template", clientTemplate.getName());
        Assert.assertEquals("foo-template-desc", clientTemplate.getDescription());
        Assert.assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, clientTemplate.getProtocol());
        Assert.assertEquals(1, clientTemplate.getProtocolMappers().size());
        List<ProtocolMapperRepresentation> clientTemplateMappers = clientTemplate.getProtocolMappers();
        ProtocolMapperRepresentation templateGssCredentialMapper = findMapperByName(clientTemplateMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME);
        assertGssProtocolMapper(templateGssCredentialMapper);

        // Test client template scopes
        Set<RoleRepresentation> allClientTemplateScopes = allScopeMappings(realmRsc.clientTemplates().get(clientTemplate.getId()));
        Assert.assertEquals(3, allClientTemplateScopes.size());
        Assert.assertTrue(containsRole(allClientTemplateScopes, findRealmRole(realmRsc, "admin")));//allClientTemplateScopes.contains(realm.getRole("admin")));
        Assert.assertTrue(containsRole(allClientTemplateScopes, findClientRole(realmRsc, application.getId(), "app-user")));//allClientTemplateScopes.contains(application.getRole("app-user")));
        Assert.assertTrue(containsRole(allClientTemplateScopes, findClientRole(realmRsc, application.getId(), "app-admin")));//allClientTemplateScopes.contains(application.getRole("app-admin")));

        List<RoleRepresentation> clientTemplateRealmScopes = realmScopeMappings(realmRsc.clientTemplates().get(clientTemplate.getId()));
        Assert.assertTrue(containsRole(clientTemplateRealmScopes, findRealmRole(realmRsc, "admin")));//clientTemplateRealmScopes.contains(realm.getRole("admin")));

        List<RoleRepresentation> clientTemplateAppScopes = clientScopeMappings(realmRsc.clientTemplates().get(clientTemplate.getId()));//application.getClientScopeMappings(oauthClient);
        Assert.assertTrue(containsRole(clientTemplateAppScopes, findClientRole(realmRsc, application.getId(), "app-user")));//clientTemplateAppScopes.contains(application.getRole("app-user")));
        Assert.assertTrue(containsRole(clientTemplateAppScopes, findClientRole(realmRsc, application.getId(), "app-admin")));//clientTemplateAppScopes.contains(application.getRole("app-admin")));

        // Test user consents
        //admin =  session.users().getUserByUsername("admin", realm);

        UserResource adminRsc = realmRsc.users().get(admin.getId());
        List<Map<String, Object>> consents = adminRsc.getConsents();
        Assert.assertEquals(2, consents.size());//.getConsents().size());

        Map<String, Object> appAdminConsent = findConsentByClientId(consents, application.getClientId());
        Assert.assertEquals(2, calcNumberGrantedRoles(appAdminConsent));
        Assert.assertTrue(getGrantedProtocolMappers(appAdminConsent) == null || getGrantedProtocolMappers(appAdminConsent).isEmpty());
        Assert.assertTrue(isRealmRoleGranted(appAdminConsent, "admin"));//appAdminConsent.isRoleGranted(realm.getRole("admin")));
        Assert.assertTrue(isClientRoleGranted(appAdminConsent, application.getClientId(), "app-admin"));//appAdminConsent.isRoleGranted(application.getRole("app-admin")));

        Map<String, Object> otherAppAdminConsent = findConsentByClientId(consents, otherApp.getClientId());//admin.getConsentByClient(otherApp.getId());
        Assert.assertEquals(1, calcNumberGrantedRoles(otherAppAdminConsent));
        Assert.assertEquals(1, getGrantedProtocolMappers(otherAppAdminConsent).size());//otherAppAdminConsent.getGrantedProtocolMappers().size());
        Assert.assertTrue(isRealmRoleGranted(otherAppAdminConsent, "admin"));//otherAppAdminConsent.isRoleGranted(realm.getRole("admin")));
        Assert.assertFalse(isClientRoleGranted(otherAppAdminConsent, application.getClientId(), "app-admin"));//otherAppAdminConsent.isRoleGranted(application.getRole("app-admin")));
        Assert.assertTrue(isProtocolMapperGranted(otherAppAdminConsent, gssCredentialMapper));

        Assert.assertTrue(application.isStandardFlowEnabled());
        Assert.assertTrue(application.isImplicitFlowEnabled());
        Assert.assertTrue(application.isDirectAccessGrantsEnabled());
        Assert.assertFalse(otherApp.isStandardFlowEnabled());
        Assert.assertFalse(otherApp.isImplicitFlowEnabled());
        Assert.assertFalse(otherApp.isDirectAccessGrantsEnabled());

        // Test service accounts
        Assert.assertFalse(application.isServiceAccountsEnabled());
        Assert.assertTrue(otherApp.isServiceAccountsEnabled());
        Assert.assertNull(testingClient.testing().getUserByServiceAccountClient(realm.getRealm(), application.getClientId()));//session.users().getUserByServiceAccountClient(application));
        UserRepresentation linked = testingClient.testing().getUserByServiceAccountClient(realm.getRealm(), otherApp.getClientId());//session.users().getUserByServiceAccountClient(otherApp);
        Assert.assertNotNull(linked);
        Assert.assertEquals("my-service-user", linked.getUsername());

        assertAuthorizationSettings(realmRsc);
    }

    private static boolean isProtocolMapperGranted(Map<String, Object> consent, ProtocolMapperRepresentation mapperRep) {
        Map<String, List> grantedMappers = (Map<String, List>)consent.get("grantedProtocolMappers");
        if (grantedMappers == null) return false;
        List<String> mappers = grantedMappers.get(mapperRep.getProtocol());
        if (mappers == null) return false;
        return mappers.contains(mapperRep.getName());
    }

    private static boolean isRealmRoleGranted(Map<String, Object> consent, String roleName) {
        if (consent.get("grantedRealmRoles") == null) return false;
        return ((List)consent.get("grantedRealmRoles")).contains(roleName);
    }

    private static boolean isClientRoleGranted(Map<String, Object> consent, String clientId, String roleName) {
        if (consent.get("grantedClientRoles") == null) return false;
        Map<String, List> grantedClientRoles = (Map<String, List>)consent.get("grantedClientRoles");
        List rolesForClient = grantedClientRoles.get(clientId);
        if (rolesForClient == null) return false;
        return rolesForClient.contains(roleName);
    }

    private static Map<String, List<String>> getGrantedProtocolMappers(Map<String, Object> consent) {
        return (Map<String, List<String>>)consent.get("grantedProtocolMappers");
    }

    private static int calcNumberGrantedRoles(Map<String, Object> consent) {
        int numGranted = 0;
        List realmRoles = (List)consent.get("grantedRealmRoles");
        if (realmRoles != null) numGranted += realmRoles.size();
        Map clientRoles = (Map)consent.get("grantedClientRoles");
        if (clientRoles != null) numGranted += clientRoles.size();
        return numGranted;
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
        for (UserRepresentation user : realmRsc.users().search(null, 0, Integer.MAX_VALUE)) {
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

    private static Set<RoleRepresentation> allScopeMappings(ClientTemplateResource client) {
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

    private static List<RoleRepresentation> clientScopeMappings(ClientTemplateResource client) {
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

    private static List<RoleRepresentation> realmScopeMappings(ClientTemplateResource client) {
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

    private static void assertAuthorizationSettings(RealmResource realmRsc) {
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
        Assert.assertEquals(10, policies.size());
        List<Predicate<PolicyRepresentation>> policyPredicates = new ArrayList<>();
        policyPredicates.add(policyRepresentation -> "Any Admin Policy".equals(policyRepresentation.getName()));
        policyPredicates.add(policyRepresentation -> "Any User Policy".equals(policyRepresentation.getName()));
        policyPredicates.add(policyRepresentation -> "Only Premium User Policy".equals(policyRepresentation.getName()));
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
}
