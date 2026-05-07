/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.forms;

import java.util.List;

import org.keycloak.authentication.authenticators.resetcred.ResetCredentialChooseUser;
import org.keycloak.authentication.authenticators.resetcred.ResetCredentialEmail;
import org.keycloak.authentication.authenticators.resetcred.ResetPassword;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.MailServer;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests setting up alternative reset credentials sub flow to prevent signing in after clicking "forgot password"
 *
 * @author <a href="mailto:drichtar@redhat.com">Denis Richtárik</a>
 */
public class AltSubflowForCredentialResetTest extends AbstractTestRealmKeycloakTest {

    private String userID;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public MailServer mailRule = new MailServer();

    @Page
    LoginPage loginPage;

    @Page
    protected LoginUsernameOnlyPage loginUsernameOnlyPage;

    @Page
    LoginPasswordResetPage loginPasswordResetPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    private RealmRepresentation loadTestRealm() {
        RealmRepresentation res = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        res.setResetCredentialsFlow(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW);
        return res;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        log.debug("Adding test realm for import from testrealm.json");
        testRealms.add(loadTestRealm());
    }

    @Before
    public void setup() {
        log.info("Adding login-test user");
        UserRepresentation testUser = UserBuilder.create().username("login-test").email("login@test.com").enabled(true).build();

        userID = AdminApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), testUser, "password");
        getCleanup().addUserId(userID);
    }

    private void configureAlternativeResetCredentialsFlow() {
        configureAlternativeResetCredentialsFlow(testingClient);
    }

    static void configureAlternativeResetCredentialsFlow(KeycloakTestingClient testingClient) {
        final String newFlowAlias = DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW + " - alternative";
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyResetCredentialsFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).selectFlow(newFlowAlias)
                .clear()
                .addSubFlowExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, altSubFlow -> altSubFlow
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ResetCredentialChooseUser.PROVIDER_ID)
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ResetCredentialEmail.PROVIDER_ID)
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ResetPassword.PROVIDER_ID))
                .defineAsResetCredentialsFlow());
    }

    @Test
    public void alternativeSubflowStaySignedOutTest() {
        configureAlternativeResetCredentialsFlow();
        try {
            oauth.openLoginForm();
            loginPage.resetPassword();
            Assertions.assertTrue(loginPasswordResetPage.isCurrent());
            loginPasswordResetPage.changePassword("login@test.com.com");
            Assertions.assertTrue(loginPage.isCurrent());
            assertEquals("You should receive an email shortly with further instructions.", loginUsernameOnlyPage.getSuccessMessage());
            oauth.openLoginForm();
            Assertions.assertTrue(loginPage.isCurrent());
        } finally {
            managedRealm.admin().flows().getFlows().clear();
            RealmRepresentation realm = managedRealm.admin().toRepresentation();
            realm.setResetCredentialsFlow(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW);
            managedRealm.admin().update(realm);
        }
    }
}
