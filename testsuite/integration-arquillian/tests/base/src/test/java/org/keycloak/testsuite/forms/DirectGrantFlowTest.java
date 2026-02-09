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

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.directgrant.ValidatePassword;
import org.keycloak.authentication.authenticators.directgrant.ValidateUsername;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAuthenticationTest;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Rule;
import org.junit.Test;
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
        AccessTokenResponse response = oauth.doPasswordGrantRequest(login, "password");

        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
        assertEquals("Account is not fully set up", response.getErrorDescription());
    }

    public static void revertFlows(RealmResource realmResource, String flowToDeleteAlias) {
        List<AuthenticationFlowRepresentation> flows = realmResource.flows().getFlows();

        // Set default direct grant flow
        RealmRepresentation realm = realmResource.toRepresentation();
        realm.setDirectGrantFlow(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW);
        realmResource.update(realm);

        AuthenticationFlowRepresentation flowRepresentation = AbstractAuthenticationTest.findFlowByAlias(flowToDeleteAlias, flows);

        // Throw error if flow doesn't exist to ensure we did not accidentally use different alias of non-existing flow when
        // calling this method
        if (flowRepresentation == null) {
            throw new IllegalArgumentException("The flow with alias " + flowToDeleteAlias + " did not exist");
        }

        realmResource.flows().deleteFlow(flowRepresentation.getId());
    }
}
