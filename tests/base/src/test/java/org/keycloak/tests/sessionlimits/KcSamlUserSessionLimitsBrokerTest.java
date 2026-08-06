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
package org.keycloak.tests.sessionlimits;

import java.util.List;
import java.util.Map;

import org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.ERROR_TO_DISPLAY;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.assertSessionCount;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.configurePostBrokerFlow;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.removePostBrokerFlow;

@KeycloakIntegrationTest
public class KcSamlUserSessionLimitsBrokerTest {

    private static final String PROVIDER_REALM = "provider";
    private static final String CONSUMER_REALM = "consumer";
    private static final String IDP_ALIAS = "kc-saml-idp";
    private static final String USER_LOGIN = "testuser";
    private static final String USER_PASSWORD = "password";
    private static final String USER_EMAIL = "user@localhost.com";
    private static final String CONSUMER_CLIENT_ID = "broker-app";
    private static final String CONSUMER_CLIENT_SECRET = "broker-app-secret";

    @InjectRealm(ref = "provider", config = ProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", config = ConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectRunOnServer(realmRef = "consumer")
    RunOnServerClient runOnServer;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    IdpReviewUserProfilePage idpReviewUserProfilePage;

    @BeforeEach
    public void setup() {
        deleteAllCookies(consumerRealm);
        deleteAllCookies(providerRealm);

        List<UserRepresentation> users = consumerRealm.admin().users().search(USER_LOGIN, true);
        for (UserRepresentation user : users) {
            consumerRealm.admin().users().get(user.getId()).logout();
            consumerRealm.admin().users().get(user.getId()).remove();
        }

        runOnServer.run(removePostBrokerFlow(CONSUMER_REALM));
    }

    @Test
    public void testSessionCountExceededAndNewSessionDeniedFirstBrokerLoginFlow() {
        runOnServer.run(configurePostBrokerFlow(CONSUMER_REALM, IDP_ALIAS,
                UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1"));

        logInAsUserInIDPForFirstTime();
        deleteAllCookies(consumerRealm);
        deleteAllCookies(providerRealm);
        logInAsUserInIDP();

        errorPage.assertCurrent();
        Assertions.assertEquals(ERROR_TO_DISPLAY, errorPage.getError());
    }

    @Test
    public void testSessionCountExceededAndOldestSessionRemovedFirstBrokerLoginFlow() {
        runOnServer.run(configurePostBrokerFlow(CONSUMER_REALM, IDP_ALIAS,
                UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION, "0", "1"));

        logInAsUserInIDPForFirstTime();
        deleteAllCookies(consumerRealm);
        deleteAllCookies(providerRealm);
        logInAsUserInIDP();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        runOnServer.run(assertSessionCount(CONSUMER_REALM, USER_LOGIN, 1));
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedFirstBrokerLoginFlow() {
        runOnServer.run(configurePostBrokerFlow(CONSUMER_REALM, IDP_ALIAS,
                UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "1", "0"));

        logInAsUserInIDPForFirstTime();
        deleteAllCookies(consumerRealm);
        deleteAllCookies(providerRealm);
        logInAsUserInIDP();

        errorPage.assertCurrent();
        Assertions.assertEquals(ERROR_TO_DISPLAY, errorPage.getError());
    }

    @Test
    public void testRealmSessionCountExceededAndOldestSessionRemovedFirstBrokerLoginFlow() {
        runOnServer.run(configurePostBrokerFlow(CONSUMER_REALM, IDP_ALIAS,
                UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION, "1", "0"));

        logInAsUserInIDPForFirstTime();
        deleteAllCookies(consumerRealm);
        deleteAllCookies(providerRealm);
        logInAsUserInIDP();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        runOnServer.run(assertSessionCount(CONSUMER_REALM, USER_LOGIN, 1));
    }

    private void logInAsUserInIDPForFirstTime() {
        logInAsUserInIDP();
        idpReviewUserProfilePage.assertCurrent();
        idpReviewUserProfilePage.update("Firstname", "Lastname");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    private void logInAsUserInIDP() {
        oauth.realm(CONSUMER_REALM).client(CONSUMER_CLIENT_ID, CONSUMER_CLIENT_SECRET).openLoginForm();
        loginPage.assertCurrent();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.assertCurrent();
        loginPage.fillLogin(USER_LOGIN, USER_PASSWORD);
        loginPage.submit();
    }

    private void deleteAllCookies(ManagedRealm realm) {
        driver.driver().navigate().to(realm.getBaseUrl());
        driver.cookies().deleteAll();
    }

    private static ProtocolMapperRepresentation createSamlUserPropertyMapper(
            String name, String userAttribute, String samlAttributeName, String friendlyName) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(name);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        mapper.setProtocolMapper(UserPropertyAttributeStatementMapper.PROVIDER_ID);
        Map<String, String> config = mapper.getConfig();
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE, userAttribute);
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, samlAttributeName);
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
        config.put(AttributeStatementHelper.FRIENDLY_NAME, friendlyName);
        return mapper;
    }

    public static class ProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(PROVIDER_REALM);
            realm.users(UserBuilder.create(USER_LOGIN)
                    .name("Firstname", "Lastname")
                    .email(USER_EMAIL)
                    .emailVerified(true)
                    .password(USER_PASSWORD)
                    .enabled(true));

            ProtocolMapperRepresentation emailMapper = createSamlUserPropertyMapper(
                    "email", "email", "urn:oid:1.2.840.113549.1.9.1", "email");
            ProtocolMapperRepresentation firstNameMapper = createSamlUserPropertyMapper(
                    "firstName", "firstName", "urn:oid:2.5.4.42", "givenName");
            ProtocolMapperRepresentation lastNameMapper = createSamlUserPropertyMapper(
                    "lastName", "lastName", "urn:oid:2.5.4.4", "sn");

            String samlClientId = "http://localhost:8080/realms/" + CONSUMER_REALM;
            realm.clients(ClientBuilder.create(samlClientId)
                    .protocol(SamlProtocol.LOGIN_PROTOCOL)
                    .redirectUris("http://localhost:8080/realms/" + CONSUMER_REALM + "/broker/" + IDP_ALIAS + "/endpoint")
                    .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true")
                    .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE,
                            "http://localhost:8080/realms/" + CONSUMER_REALM + "/broker/" + IDP_ALIAS + "/endpoint")
                    .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                            "http://localhost:8080/realms/" + CONSUMER_REALM + "/broker/" + IDP_ALIAS + "/endpoint")
                    .attribute(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "true")
                    .attribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username")
                    .attribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false")
                    .attribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false")
                    .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
                    .attribute(SamlConfigAttributes.SAML_ENCRYPT, "false")
                    .protocolMappers(emailMapper, firstNameMapper, lastNameMapper));
            return realm;
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(CONSUMER_REALM);
            realm.identityProviders(IdentityProviderBuilder.create()
                    .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .attribute(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL,
                            "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/saml")
                    .attribute(SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL,
                            "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/saml")
                    .attribute(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT,
                            "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress")
                    .attribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, "true")
                    .attribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "true")
                    .attribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "false")
                    .attribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "false")
                    .attribute(SAMLIdentityProviderConfig.BACKCHANNEL_SUPPORTED, "false")
                    .attribute(SAMLIdentityProviderConfig.FORCE_AUTHN, "false")
                    .build());
            realm.clients(ClientBuilder.create(CONSUMER_CLIENT_ID)
                    .secret(CONSUMER_CLIENT_SECRET)
                    .directAccessGrantsEnabled(true)
                    .redirectUris("*"));
            return realm;
        }
    }
}
