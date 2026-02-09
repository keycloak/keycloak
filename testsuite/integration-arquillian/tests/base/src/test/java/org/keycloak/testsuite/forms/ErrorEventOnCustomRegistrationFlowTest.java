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
package org.keycloak.testsuite.forms;

import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.util.FlowUtil;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class ErrorEventOnCustomRegistrationFlowTest extends AbstractFlowTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {}

    @Before
    public void setup() {
    }

        @Page
    protected LoginPasswordResetPage resetPasswordPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    // 30453
    @Test
    public void resetLinkWithErrorEventDoNotFail() {
        configureResetCredentialsFlow();

        String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials?client_id=test-app";

        driver.navigate().to(resetUri);

        events.expect(EventType.RESET_PASSWORD)
                .error(ErrorEventAuthenticator.ERROR_MESSAGE)
                .user(ErrorEventAuthenticator.FAKE_USERID)
                .assertEvent();

        resetPasswordPage.assertCurrent();

    }

    private void configureResetCredentialsFlow() {
        String newFlowAlias = "reset-credentials-custom";
        testingClient.server("test").run(session -> {
            // Create a copy of the default reset credentials flow with the specified flow alias if it doesn't exist yet
            if(session.getContext().getRealm().getFlowByAlias(newFlowAlias) == null) {
                FlowUtil.inCurrentRealm(session).copyResetCredentialsFlow(newFlowAlias);
            }
        });

        // add the custom the execution(s)
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ErrorEventAuthenticator.PROVIDER_ID, 5)
        );

        // Bind the flow as the reset-credentials one
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .defineAsResetCredentialsFlow()
        );
    }

}
