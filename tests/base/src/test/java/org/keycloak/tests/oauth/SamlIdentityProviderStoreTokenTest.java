/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.oauth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.JpaRealmProviderFactory;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.UserAttributeStatementMapper;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.PlainStringResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class SamlIdentityProviderStoreTokenTest {

    public static String IDP_ALIAS = "saml-idp-alias";
    public static String EXTERNAL_REALM_NAME = "external-realm";

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectRealm(config = IdpRealmConfig.class)
    protected ManagedRealm realm;

    @InjectRealm(ref = "external-realm", config = ExternalRealmConfig.class)
    ManagedRealm externalRealm;

    @InjectPage
    LoginPage loginPage;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @AfterEach
    public void logout() {
        Optional<UserRepresentation> userResult = realm.admin().users().search("testuser", true).stream().findFirst();
        if (userResult.isPresent()) {
            AccountHelper.logout(realm.admin(), "testuser");
            realm.admin().users().delete(userResult.get().getId()).close();
        }

        userResult = externalRealm.admin().users().search("testuser", true).stream().findFirst();
        if (userResult.isPresent()) {
            AccountHelper.logout(externalRealm.admin(), "testuser");
        }
    }

    @Test
    public void testIdentityProviderStoreTokenManualRoleGrant() throws Exception {
        realm.updateIdentityProvider(IDP_ALIAS, idp-> {
            idp.setAddReadTokenRoleOnCreate(false);
        });

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        //user without the role tries to read the stored token
        PlainStringResponse response = oauth.doFetchExternalIdpTokenString(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(403, response.getStatusCode());

        AccountHelper.logout(realm.admin(), "testuser");

        //grant the role to the user and repeat the login
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel brokerClient = realm.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID);
            RoleModel readTokenRole = brokerClient.getRole(Constants.READ_TOKEN_ROLE);
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            user.grantRole(readTokenRole);
        });

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        response = oauth.doFetchExternalIdpTokenString(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertNotNull(response.getResponse());
    }

    @Test
    public void testIdentityProviderStoreTokenRoleGrantOnUserCreation() throws Exception {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        PlainStringResponse response = oauth.doFetchExternalIdpTokenString(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertNotNull(response.getResponse());
    }

    @Test
    public void testStoreTokenDisabled() {
        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setStoreToken(false);
        });

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        PlainStringResponse response = oauth.doFetchExternalIdpTokenString(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertNotNull(response.getResponse());

        String realmName = realm.getName();
        String oldTokenFromDatabase = runOnServer.fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realm, "testuser");
            return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(realm, user, IDP_ALIAS).getToken();
        }, String.class);

        //Ensure that the token is null in the db
        Assertions.assertNull(oldTokenFromDatabase);
    }

    public static class IdpRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.identityProvider(IdentityProviderBuilder.create()
                    .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .setAttribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .setAttribute(SAMLIdentityProviderConfig.ENTITY_ID, "http://localhost:8080/realms/default")
                    .setAttribute(SAMLIdentityProviderConfig.IDP_ENTITY_ID, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME)
                    .setAttribute(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/saml")
                    .setAttribute(SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/saml")
                    .setAttribute(SAMLIdentityProviderConfig.USE_METADATA_DESCRIPTOR_URL, Boolean.TRUE.toString())
                    .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, Boolean.TRUE.toString())
                    .setAttribute(SAMLIdentityProviderConfig.BACKCHANNEL_SUPPORTED, Boolean.TRUE.toString())
                    .setAttribute(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get())
                    .setAttribute(SAMLIdentityProviderConfig.SIGNATURE_ALGORITHM, SignatureAlgorithm.RSA_SHA256.name())
                    .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.TRUE.toString())
                    .setAttribute(IdentityProviderModel.METADATA_DESCRIPTOR_URL, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/saml/descriptor")
                    .storeToken(true)
                    .addReadTokenRoleOnCreate(true)
                    .build());
            realm.identityProviderMapper(createMapper("email"))
                    .identityProviderMapper(createMapper("firstName"))
                    .identityProviderMapper(createMapper("lastName"));
            return realm;
        }

        private IdentityProviderMapperRepresentation createMapper(String name) {
            IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
            mapper.setName(name);
            mapper.setIdentityProviderAlias(IDP_ALIAS);
            mapper.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
            Map<String, String> config = new HashMap<>();
            config.put(IdentityProviderModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.name());
            config.put(UserAttributeMapper.USER_ATTRIBUTE, name);
            config.put(UserAttributeMapper.ATTRIBUTE_NAME, name);
            config.put(UserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME, name);
            config.put(UserAttributeMapper.ATTRIBUTE_NAME_FORMAT, JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.name());
            mapper.setConfig(config);
            return mapper;
        }
    }

    public static class ExternalRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient("http://localhost:8080/realms/default")
                    .name("saml-client")
                    .protocol(SamlProtocol.LOGIN_PROTOCOL)
                    .adminUrl("http://localhost:8080/realms/default/broker/" + IDP_ALIAS + "/endpoint")
                    .redirectUris("http://localhost:8080/realms/default/broker/" + IDP_ALIAS + "/endpoint/*")
                    .attribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, Boolean.TRUE.toString())
                    .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, Boolean.TRUE.toString())
                    .attribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, Boolean.TRUE.toString())
                    .attribute(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, SignatureAlgorithm.RSA_SHA256.name())
                    .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.TRUE.toString())
                    .attribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username")
                    .attribute(SamlConfigAttributes.SAML_USE_METADATA_DESCRIPTOR_URL, Boolean.TRUE.toString())
                    .attribute(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, "http://localhost:8080/realms/default/broker/" + IDP_ALIAS + "/endpoint/descriptor")
                    .protocolMappers(List.of(createMapper("email"), createMapper("firstName"), createMapper("lastName")));
            realm.addUser("testuser")
                    .name("Test", "User")
                    .email("test@localhost")
                    .emailVerified(Boolean.TRUE)
                    .password("password");
            return realm;
        }

        private ProtocolMapperRepresentation createMapper(String name) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName(name);
            mapper.setProtocolMapper(UserAttributeStatementMapper.PROVIDER_ID);
            mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
            Map<String, String> config = new HashMap<>();
            config.put(ProtocolMapperUtils.USER_ATTRIBUTE, name);
            config.put(AttributeStatementHelper.FRIENDLY_NAME, name);
            config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, name);
            config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC);
            mapper.setConfig(config);
            return mapper;
        }
    }
}
