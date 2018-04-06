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

package org.keycloak.testsuite.admin.authentication;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.testsuite.util.AdminEventPaths;

import javax.ws.rs.BadRequestException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RegistrationFlowTest extends AbstractAuthenticationTest {

    @Test
    public void testAddExecution() {
        // Add registration flow 2
        AuthenticationFlowRepresentation flowRep = newFlow("registration2", "RegistrationFlow2", "basic-flow", true, false);
        createFlow(flowRep);

        // add registration execution form flow
        Map<String, String> data = new HashMap<>();
        data.put("alias", "registrationForm2");
        data.put("type", "form-flow");
        data.put("description", "registrationForm2 flow");
        data.put("provider", "registration-page-form");
        authMgmtResource.addExecutionFlow("registration2", data);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authAddExecutionFlowPath("registration2"), data, ResourceType.AUTH_EXECUTION_FLOW);

        // Should fail to add execution under top level flow
        Map<String, String> data2 = new HashMap<>();
        data2.put("provider", "registration-profile-action");
        try {
            authMgmtResource.addExecution("registration2", data2);
            Assert.fail("Not expected to add execution of type 'registration-profile-action' under top flow");
        } catch (BadRequestException bre) {
        }

        // Should success to add execution under form flow
        authMgmtResource.addExecution("registrationForm2", data2);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AdminEventPaths.authAddExecutionPath("registrationForm2"), data2, ResourceType.AUTH_EXECUTION);
    }

    // TODO: More type-safety instead of passing generic maps

}
