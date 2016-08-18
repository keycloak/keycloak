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

package org.keycloak.testsuite.keycloaksaml;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlAdapterTest {

    @ClassRule
    public static SamlKeycloakRule keycloakRule = new SamlKeycloakRule() {
        @Override
        public void initWars() {
             ClassLoader classLoader = SamlAdapterTest.class.getClassLoader();

            initializeSamlSecuredWar("/keycloak-saml/simple-post", "/sales-post",  "post.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/simple-post2", "/sales-post2",  "post.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/simple-post-passive", "/sales-post-passive", "post-passive.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/signed-post", "/sales-post-sig",  "post-sig.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/sales-post-assertion-and-response-sig", "/sales-post-assertion-and-response-sig",  "sales-post-assertion-and-response-sig.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/signed-post-email", "/sales-post-sig-email",  "post-sig-email.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/signed-post-transient", "/sales-post-sig-transient",  "post-sig-transient.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/signed-post-persistent", "/sales-post-sig-persistent",  "post-sig-persistent.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/signed-metadata", "/sales-metadata",  "post-metadata.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/signed-get", "/employee-sig",  "employee-sig.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/mappers", "/employee2",  "employee2.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/signed-front-get", "/employee-sig-front",  "employee-sig-front.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/bad-client-signed-post", "/bad-client-sales-post-sig",  "bad-client-post-sig.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/bad-realm-signed-post", "/bad-realm-sales-post-sig",  "bad-realm-post-sig.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/bad-assertion-signed-post", "/bad-assertion-sales-post-sig",  "bad-assertion-post-sig.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/missing-assertion-sig", "/missing-assertion-sig",  "missing-assertion-sig.war", classLoader);
            initializeSamlSecuredWar("/keycloak-saml/encrypted-post", "/sales-post-enc",  "post-enc.war", classLoader);
            System.setProperty("app.server.base.url", "http://localhost:8081");
            initializeSamlSecuredWar("/keycloak-saml/simple-input", "/input-portal",  "input.war", classLoader, InputServlet.class, "/secured/*");
            SamlAdapterTestStrategy.uploadSP("http://localhost:8081/auth");
            server.getServer().deploy(createDeploymentInfo("employee.war", "/employee", SamlSPFacade.class));



        }

        @Override
        public String getRealmJson() {
            return "/keycloak-saml/testsaml.json";
        }
    };

    @Rule
    public SamlAdapterTestStrategy testStrategy = new SamlAdapterTestStrategy("http://localhost:8081/auth", "http://localhost:8081", keycloakRule);

    //@Test
    public void testIDE() throws Exception {
        Thread.sleep(100000000);
    }

    @Test
    public void testPostBadRealmSignature() {
        testStrategy.testPostBadRealmSignature();
    }

    @Test
    public void testPostBadAssertionSignature() {
        testStrategy.testPostBadAssertionSignature();
    }

    @Test
    public void testMissingAssertionSignature() {
        testStrategy.testMissingAssertionSignature();
    }

    @Test
    public void testPostSimpleUnauthorized() {
        testStrategy.testPostSimpleUnauthorized( new SamlAdapterTestStrategy.CheckAuthError() {
            @Override
            public void check(WebDriver driver) {
                String pageSource = driver.getPageSource();
                Assert.assertTrue(pageSource.contains("Error Page"));
            }
        });
    }


    @Test
    public void testSavedPostRequest() throws Exception {
        testStrategy.testSavedPostRequest();
    }
    @Test
    public void testErrorHandling() throws Exception {
        testStrategy.testErrorHandling();
    }
    @Test
    public void testMetadataPostSignedLoginLogout() throws Exception {
        testStrategy.testMetadataPostSignedLoginLogout();
    }

    @Test
    public void testRedirectSignedLoginLogout() {
        testStrategy.testRedirectSignedLoginLogout();
    }

    @Test
    public void testPostSignedLoginLogoutEmailNameID() {
        testStrategy.testPostSignedLoginLogoutEmailNameID();
    }

    @Test
    public void testPostEncryptedLoginLogout() {
        testStrategy.testPostEncryptedLoginLogout();
    }

    @Test
    public void testRedirectSignedLoginLogoutFrontNoSSO() {
        testStrategy.testRedirectSignedLoginLogoutFrontNoSSO();
    }

    @Test
    public void testPostSimpleLoginLogout() {
        testStrategy.testPostSimpleLoginLogout();
    }

    @Test
    public void testPostPassiveLoginLogout() {
        testStrategy.testPostPassiveLoginLogout(true);
    }

    @Test
    public void testPostSignedLoginLogoutTransientNameID() {
        testStrategy.testPostSignedLoginLogoutTransientNameID();
    }

    @Test
    public void testPostSimpleLoginLogoutIdpInitiated() {
        testStrategy.testPostSimpleLoginLogoutIdpInitiated();
    }

    @Test
    public void testPostSimpleLoginLogoutIdpInitiatedRedirectTo() {
        testStrategy.testPostSimpleLoginLogoutIdpInitiatedRedirectTo();
    }

    @Test
    public void testAttributes() throws Exception {
        testStrategy.testAttributes();
    }

    @Test
    public void testPostSignedLoginLogoutPersistentNameID() {
        testStrategy.testPostSignedLoginLogoutPersistentNameID();
    }

    @Test
    public void testRelayStateEncoding() throws Exception {
        testStrategy.testRelayStateEncoding();
    }

    @Test
    public void testPostBadClientSignature() {
        testStrategy.testPostBadClientSignature();
    }

    @Test
    public void testRedirectSignedLoginLogoutFront() {
        testStrategy.testRedirectSignedLoginLogoutFront();
    }

    @Test
    public void testPostSignedLoginLogout() {
        testStrategy.testPostSignedLoginLogout();
    }

    @Test
    public void testPostSignedResponseAndAssertionLoginLogout() {
        testStrategy.testPostSignedResponseAndAssertionLoginLogout();
    }

    @Test
    public void testIDPDescriptor() throws Exception {
        Client client = ClientBuilder.newClient();
        String text = client.target("http://localhost:8081/auth/realms/master/protocol/saml/descriptor").request().get(String.class);
        client.close();

    }

    /**
     * Test KEYCLOAK-2718
     */
    @Test
    public void testNameIDFormatImport() throws Exception {
        String resourcePath = "/keycloak-saml/sp-metadata-email-nameid.xml";
        Keycloak keycloak = Keycloak.getInstance("http://localhost:8081/auth", "master", "admin", "admin", Constants.ADMIN_CLI_CLIENT_ID, null);
        RealmResource admin = keycloak.realm("demo");

        admin.toRepresentation();

        ClientRepresentation clientRep = admin.convertClientDescription(IOUtils.toString(SamlAdapterTestStrategy.class.getResourceAsStream(resourcePath)));
        assertEquals("email", clientRep.getAttributes().get("saml_name_id_format"));


        keycloak.close();


    }



}
