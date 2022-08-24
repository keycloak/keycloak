/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
import java.util.function.Consumer;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.directgrant.ValidatePassword;
import org.keycloak.authentication.authenticators.directgrant.ValidateUsername;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.WebDriver;

import static org.junit.Assert.assertEquals;

/**
 * Test of custom configurations of DirectGrant flow (Resource Owner Password Credentials Grant)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DirectGrantFlowTest extends AbstractTestRealmKeycloakTest {

    @ArquillianResource
    protected OAuthClient oauth;

    @Drone
    protected WebDriver driver;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }


    private void configureDirectGrantFlowWithOTPForm(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyDirectGrantFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .clear()
                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ValidateUsername.PROVIDER_ID)
                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ValidatePassword.PROVIDER_ID)
                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID)
                .defineAsDirectGrantFlow()
        );
    }

    // KEYCLOAK-18949
    @Test
    public void testDirectGrantLoginWithOTPFormShouldFail() throws Exception {
        configureDirectGrantFlowWithOTPForm("new-direct-grant");

        String clientId = "direct-grant";
        String login = "test-user@localhost";

        // User should not be able to login as there was required action added to authenticationSession by OTPFormAuthenticator
        oauth.clientId(clientId);
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", login, "password", null);

        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
        assertEquals("Account is not fully set up", response.getErrorDescription());
    }


}
