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
package org.keycloak.testsuite.oauth;

import java.util.List;

import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:slawomir@dabek.name">Slawomir Dabek</a>
 */
public class AccessTokenDuplicateEmailsTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);


    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Before
    public void clientConfiguration() {
        oauth.clientId("test-app");
        oauth.realm("test-duplicate-emails");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm-duplicate-emails.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }
    
    @Test
    public void loginFormUsernameLabel() throws Exception {
        oauth.openLoginForm();
        oauth.redirectUri(getAuthServerContextRoot() + "/does/not/matter/");
        
        assertEquals("Username", driver.findElement(By.xpath("//label[@for='username']")).getText());
    }

    @Test
    public void loginWithNonDuplicateEmailUser() throws Exception {
        oauth.doLogin("non-duplicate-email-user", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        assertEquals(200, response.getStatusCode());
        
        AccessToken token = oauth.verifyToken(response.getAccessToken());
        
        assertEquals(findUserByUsername(adminClient.realm("test-duplicate-emails"), "non-duplicate-email-user").getId(), token.getSubject());
        assertEquals("non-duplicate-email-user@localhost", token.getEmail());
    }

    @Test
    public void loginWithFirstDuplicateEmailUser() throws Exception {
        oauth.doLogin("duplicate-email-user1", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        assertEquals(200, response.getStatusCode());
        
        AccessToken token = oauth.verifyToken(response.getAccessToken());
        
        assertEquals(findUserByUsername(adminClient.realm("test-duplicate-emails"), "duplicate-email-user1").getId(), token.getSubject());
        assertEquals("duplicate-email-user@localhost", token.getEmail());
    }
    
    @Test
    public void loginWithSecondDuplicateEmailUser() throws Exception {
        oauth.doLogin("duplicate-email-user2", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        assertEquals(200, response.getStatusCode());
        
        AccessToken token = oauth.verifyToken(response.getAccessToken());
        
        assertEquals(findUserByUsername(adminClient.realm("test-duplicate-emails"), "duplicate-email-user2").getId(), token.getSubject());
        assertEquals("duplicate-email-user@localhost", token.getEmail());
    }

    @Test
    public void loginWithNonDuplicateEmail() throws Exception {
        oauth.doLogin("non-duplicate-email-user@localhost", "password");

        assertEquals("Invalid username or password.", driver.findElement(By.className("kc-feedback-text")).getText());
    }
    
    @Test
    public void loginWithDuplicateEmail() throws Exception {
        oauth.doLogin("duplicate-email-user@localhost", "password");

        assertEquals("Invalid username or password.", driver.findElement(By.className("kc-feedback-text")).getText());
    }
}
