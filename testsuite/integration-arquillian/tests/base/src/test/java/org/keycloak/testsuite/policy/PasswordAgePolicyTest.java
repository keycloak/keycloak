/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.policy;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.ApiUtil.getCreatedId;


public class PasswordAgePolicyTest extends AbstractAuthTest {

    @Page
    private LoginPage loginPage;

    @Page
    private RegisterPage registerPage;

    @Page
    private AppPage appPage;

    private UserResource user;

    private void setPasswordAgePolicy(String passwordAge) {
        log.info(String.format("Setting %s", passwordAge));
        RealmRepresentation testRealmRepresentation = testRealmResource().toRepresentation();
        testRealmRepresentation.setPasswordPolicy(passwordAge);
        testRealmResource().update(testRealmRepresentation);
    }

    private void setPasswordHistory(String passwordHistory) {
        log.info(String.format("Setting %s", passwordHistory));
        RealmRepresentation testRealmRepresentation = testRealmResource().toRepresentation();
        testRealmRepresentation.setPasswordPolicy(passwordHistory);
        testRealmResource().update(testRealmRepresentation);
    }

    private void setPasswordAgePolicyValue(String value) {
        setPasswordAgePolicy(String.format("passwordAge(%s)", value));
    }

    private void setPasswordAgePolicyValue(int value) {
        setPasswordAgePolicyValue(String.valueOf(value));
    }

    private void setPasswordHistoryValue(String value) {
        setPasswordHistory(String.format("passwordHistory(%s)", value));
    }

    private void setPasswordHistoryValue(int value) {
        setPasswordHistoryValue(String.valueOf(value));
    }

    public UserRepresentation createUserRepresentation(String username) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(username);
        userRepresentation.setEmail(String.format("%s@email.test", userRepresentation.getUsername()));
        userRepresentation.setEmailVerified(true);
        return userRepresentation;
    }

    public UserResource createUser(UserRepresentation user) {
        String createdUserId;
        try (Response response = testRealmResource().users().create(user)) {
            createdUserId = getCreatedId(response);
        }
        return testRealmResource().users().get(createdUserId);
    }

    public void resetUserPassword(UserResource userResource, String newPassword) {
        CredentialRepresentation newCredential = new CredentialRepresentation();
        newCredential.setType(PASSWORD);
        newCredential.setValue(newPassword);
        newCredential.setTemporary(false);
        userResource.resetPassword(newCredential);
    }

    private void expectBadRequestException(Consumer<Void> f) {
        try {
            f.accept(null);
            throw new AssertionError("An expected BadRequestException was not thrown.");
        } catch (BadRequestException bre) {
            log.info("An expected BadRequestException was caught.");
        }
    }

    static private int daysToSeconds(int days) {
        return days * 24 * 60 * 60;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create()
                .name(AuthRealm.TEST)
                .client(ClientBuilder.create()
                        .clientId("test-app")
                        .redirectUris(
                                "http://localhost:8180/auth/realms/master/app/auth/*",
                                "https://localhost:8543/auth/realms/master/app/auth/*",
                                "http://localhost:8180/auth/realms/test/app/auth/*",
                                "https://localhost:8543/auth/realms/test/app/auth/*")
                        .secret(PASSWORD)
                        .baseUrl("http://localhost:8180/auth/realms/master/app/auth")
                        .enabled(Boolean.TRUE)
                        .adminUrl("http://localhost:8180/auth/realms/master/app/admin")
                        .build())
                .build());
    }

    @Before
    public void before() {
        user = createUser(createUserRepresentation("test_user"));
    }

    @After
    public void after() {
        user.remove();
    }

    @Test
    public void testPasswordHistoryRetrySamePassword() {
        setPasswordAgePolicyValue(1);
        //set offset to 12h ago
        setTimeOffset(-12 * 60 * 60);
        resetUserPassword(user, "secret");
        //try to set again same password
        setTimeOffset(0);
        expectBadRequestException(f -> resetUserPassword(user, "secret"));
    }

    @Test
    public void testPasswordHistoryWithTwoPasswordsErrorThrown() {
        setPasswordAgePolicyValue(1);
        //set offset to 12h ago
        setTimeOffset(-12 * 60 * 60);
        resetUserPassword(user, "secret");
        setTimeOffset(-10 * 60 * 60);
        resetUserPassword(user, "secret1");

        //try to set again same password after 12h
        setTimeOffset(0);
        expectBadRequestException(f -> resetUserPassword(user, "secret"));
    }

    @Test
    public void testPasswordHistoryWithTwoPasswords() {
        setPasswordAgePolicyValue(1);
        //set offset to more than a day ago
        setTimeOffset(-24 * 60 * 60 * 2);
        resetUserPassword(user, "secret");
        setTimeOffset(-10 * 60 * 60);
        resetUserPassword(user, "secret1");

        //try to set again same password after 48h
        setTimeOffset(0);
        resetUserPassword(user, "secret");
    }

    @Test
    public void testPasswordHistoryWithMultiplePasswordsErrorThrown() {
        setPasswordAgePolicyValue(30);
        //set offset to 29 days, 23:45:00
        setTimeOffset(-30 * 24 * 60 * 60 + 15 * 60);
        resetUserPassword(user, "secret");
        setTimeOffset(-25 * 24 * 60 * 60);
        resetUserPassword(user, "secret1");
        setTimeOffset(-20 * 24 * 60 * 60);
        resetUserPassword(user, "secret2");
        setTimeOffset(-10 * 24 * 60 * 60);
        resetUserPassword(user, "secret3");

        //try to set again same password after 30 days, should throw error, 15 minutes too early
        setTimeOffset(0);
        expectBadRequestException(f -> resetUserPassword(user, "secret"));
    }

    @Test
    public void testPasswordHistoryWithMultiplePasswords() {
        setPasswordAgePolicyValue(30);
        //set offset to 30 days and 15 minutes
        setTimeOffset(-30 * 24 * 60 * 60 - 5 * 60);
        resetUserPassword(user, "secret");
        setTimeOffset(-25 * 24 * 60 * 60);
        resetUserPassword(user, "secret1");
        setTimeOffset(-20 * 24 * 60 * 60);
        resetUserPassword(user, "secret2");
        setTimeOffset(-10 * 24 * 60 * 60);
        resetUserPassword(user, "secret3");
        //try to set again same password after 30 days and 15 minutes

        setTimeOffset(0);
        resetUserPassword(user, "secret");
    }


    @Test
    public void testPasswordAge0Days() {
        setPasswordAgePolicyValue(0);

        resetUserPassword(user, "secret");
        //can't set the same password
        expectBadRequestException(f -> resetUserPassword(user, "secret"));
        resetUserPassword(user, "secret1");
        resetUserPassword(user, "secret");
    }

    @Test
    public void testPasswordAgeSetToNegative() {
        setPasswordAgePolicyValue(-1);

        resetUserPassword(user, "secret");
        //no check is performed
        setPasswordAgePolicyValue(10);
        resetUserPassword(user, "secret1");
        resetUserPassword(user, "secret2");
        resetUserPassword(user, "secret3");
        setPasswordAgePolicyValue(-2);
        //no check is performed
        resetUserPassword(user, "secret");
        resetUserPassword(user, "secret1");
        setPasswordAgePolicyValue(-3);
    }

    @Test
    public void testPasswordAgeSetToInvalid() {
        expectBadRequestException(f -> setPasswordAgePolicyValue("abc"));
        expectBadRequestException(f -> setPasswordAgePolicyValue("2a"));
        expectBadRequestException(f -> setPasswordAgePolicyValue("asda2"));
        expectBadRequestException(f -> setPasswordAgePolicyValue("-/!"));
    }

    @Test
    public void testBothPasswordHistoryPoliciesPasswordHistoryPolicyTakesOver() {
        //1 day
        setPasswordAgePolicyValue(1);
        //last 3 passwords
        setPasswordHistoryValue(3);
        setTimeOffset(daysToSeconds(-2));
        resetUserPassword(user, "secret");
        resetUserPassword(user, "secret1");
        resetUserPassword(user, "secret2");

        setTimeOffset(daysToSeconds(0));
        //password history takes precedence
        expectBadRequestException(f -> setPasswordAgePolicyValue("secret"));
    }

    @Test
    public void testBothPasswordHistoryPoliciesPasswordAgePolicyTakesOver() {
        //2 days
        setPasswordAgePolicyValue(2);
        //last 10 passwords
        setPasswordHistoryValue(10);
        setTimeOffset(daysToSeconds(-1));
        resetUserPassword(user, "secret");
        resetUserPassword(user, "secret1");
        resetUserPassword(user, "secret2");

        setTimeOffset(daysToSeconds(0));
        //password age takes precedence
        expectBadRequestException(f -> setPasswordAgePolicyValue("secret"));
    }

    @Test
    public void testRegistration() throws IOException {
        try (RealmAttributeUpdater realmUpdater = new RealmAttributeUpdater(testRealmResource())
                .setRegistrationAllowed(Boolean.TRUE)
                .setPasswordPolicy(String.format("passwordAge(%s)", 2)) // 2 days
                .update()) {

            oauth.openLoginForm();
            loginPage.assertCurrent();

            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registration-user@localhost", "registration-user", "password", "password");

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            AuthorizationEndpointResponse response = oauth.parseLoginResponse();
            Assert.assertNull(response.getError());
            Assert.assertNotNull(response.getCode());

            ApiUtil.findUserByUsernameId(testRealmResource(), "registration-user").remove();
        }
    }
}
