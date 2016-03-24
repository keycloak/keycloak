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

package org.keycloak.testsuite.broker;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;

import javax.mail.MessagingException;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author pedroigor
 */
public class SAMLBrokerUserPropertyTest extends AbstractKeycloakIdentityProviderTest {

    @ClassRule
    public static AbstractKeycloakRule samlServerRule = new AbstractKeycloakRule() {

        @Override
        protected void configureServer(KeycloakServer server) {
            server.getConfig().setPort(8082);
        }

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/broker-test/realm-with-saml-property-mappers.json"));
        }

        @Override
        protected String[] getTestRealms() {
            return new String[] { "realm-with-saml-idp-property-mappers" };
        }
    };

    @Override
    protected String getProviderId() {
        return "kc-saml-idp-property-mappers";
    }

    @Override
    protected void doAssertFederatedUser(UserModel federatedUser, IdentityProviderModel identityProviderModel, String expectedEmail, boolean isProfileUpdateExpected) {
        if (isProfileUpdateExpected) {
            super.doAssertFederatedUser(federatedUser, identityProviderModel, expectedEmail, isProfileUpdateExpected);
        } else {
            assertEquals(expectedEmail, federatedUser.getEmail());
            assertNotNull(federatedUser.getFirstName());
            assertNotNull(federatedUser.getLastName());
        }
    }

    @Override
    protected void doAssertFederatedUserNoEmail(UserModel federatedUser) {
        assertEquals("kc-saml-idp-basic.test-user-noemail", federatedUser.getUsername());
        //assertEquals("", federatedUser.getEmail());
        assertEquals(null, federatedUser.getFirstName());
        assertEquals(null, federatedUser.getLastName());
    }

    @Override
    protected void doAssertTokenRetrieval(String pageSource) {
        try {
            SAML2Request saml2Request = new SAML2Request();
            ResponseType responseType = (ResponseType) saml2Request
                    .getSAML2ObjectFromStream(PostBindingUtil.base64DecodeAsStream(pageSource));
                    //.getSAML2ObjectFromStream(PostBindingUtil.base64DecodeAsStream(URLDecoder.decode(pageSource, "UTF-8")));

            assertNotNull(responseType);
            assertFalse(responseType.getAssertions().isEmpty());
        } catch (Exception e) {
            fail("Could not parse token.");
        }
    }

    @Override
    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile() {
        super.testSuccessfulAuthenticationWithoutUpdateProfile();
    }

    @Test
    @Ignore
    @Override
    public void testSuccessfulAuthentication() {
        // ignore
    }

    @Override
    @Ignore
    @Test
   public void testSuccessfulAuthenticationUpdateProfileOnMissing_nothingMissing() {
        // ignore
    }

    @Override
    @Ignore
    @Test
    public void testSuccessfulAuthenticationUpdateProfileOnMissing_missingEmail() {
        // ignore
    }

    @Override
    @Ignore
    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_emailProvided_emailVerifyEnabled() throws IOException, MessagingException {
        // ignore
    }

    @Override
    @Ignore
    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_emailNotProvided_emailVerifyEnabled() {
        // ignore
    }

    @Override
    @Ignore
    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_emailProvided_emailVerifyEnabled_emailTrustEnabled() {
        // ignore
    }

    @Override
    @Ignore
    @Test
    public void testSuccessfulAuthentication_emailTrustEnabled_emailVerifyEnabled_emailUpdatedOnFirstLogin() throws IOException, MessagingException {
        // ignore
    }

    @Override
    @Ignore
    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_newUser_emailAsUsername() {
        // ignore
    }

    @Override
    @Ignore
    @Test
    public void testDisabled() {
        // ignore
    }

    @Override
    @Test
    @Ignore
    public void testProviderOnLoginPage() {
        // ignore
    }

    @Override
    @Test
    @Ignore
    public void testAccountManagementLinkIdentity() {
        // ignore
    }

    @Override
    @Test
    @Ignore
    public void testAccountManagementLinkedIdentityAlreadyExists() {
        // ignore
    }

    @Override
    @Test
    @Ignore
    public void testIdentityProviderNotAllowed() {
        // ignore
    }

    @Override
    @Test
    @Ignore
    public void testTokenStorageAndRetrievalByApplication() {
        // ignore
    }

    @Override
    @Test
    @Ignore
    public void testWithLinkedFederationProvider() throws Exception {
        // ignore
    }
}
