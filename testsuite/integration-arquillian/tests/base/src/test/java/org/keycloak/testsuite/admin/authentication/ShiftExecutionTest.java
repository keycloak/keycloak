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
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.testsuite.util.AdminEventPaths;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ShiftExecutionTest extends AbstractAuthenticationTest {

    @Test
    public void testShiftExecution() {

        // copy built-in flow so we get a new editable flow
        HashMap<String, String> params = new HashMap<>();
        params.put("newName", "Copy of browser");
        Response response = authMgmtResource.copy("browser", params);
        assertAdminEvents.assertEvent(testRealmId, OperationType.CREATE, AdminEventPaths.authCopyFlowPath("browser"), params, ResourceType.AUTH_FLOW);
        try {
            Assert.assertEquals("Copy flow", 201, response.getStatus());
        } finally {
            response.close();
        }

        // get executions
        List<AuthenticationExecutionInfoRepresentation> executions = authMgmtResource.getExecutions("Copy of browser");

        AuthenticationExecutionInfoRepresentation last = executions.get(executions.size() - 1);
        AuthenticationExecutionInfoRepresentation oneButLast = executions.get(executions.size() - 2);

        // Not possible to raisePriority of not-existent flow
        try {
            authMgmtResource.raisePriority("not-existent");
            Assert.fail("Not expected to raise priority of not existent flow");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // shift last execution up
        authMgmtResource.raisePriority(last.getId());
        assertAdminEvents.assertEvent(testRealmId, OperationType.UPDATE, AdminEventPaths.authRaiseExecutionPath(last.getId()), ResourceType.AUTH_EXECUTION);

        List<AuthenticationExecutionInfoRepresentation> executions2 = authMgmtResource.getExecutions("Copy of browser");

        AuthenticationExecutionInfoRepresentation last2 = executions2.get(executions.size() - 1);
        AuthenticationExecutionInfoRepresentation oneButLast2 = executions2.get(executions.size() - 2);

        Assert.assertEquals("Execution shifted up - N", last.getId(), oneButLast2.getId());
        Assert.assertEquals("Execution shifted up - N-1", oneButLast.getId(), last2.getId());

        // Not possible to lowerPriority of not-existent flow
        try {
            authMgmtResource.lowerPriority("not-existent");
            Assert.fail("Not expected to raise priority of not existent flow");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // shift one before last down
        authMgmtResource.lowerPriority(oneButLast2.getId());
        assertAdminEvents.assertEvent(testRealmId, OperationType.UPDATE, AdminEventPaths.authLowerExecutionPath(oneButLast2.getId()), ResourceType.AUTH_EXECUTION);

        executions2 = authMgmtResource.getExecutions("Copy of browser");

        last2 = executions2.get(executions.size() - 1);
        oneButLast2 = executions2.get(executions.size() - 2);

        Assert.assertEquals("Execution shifted down - N", last.getId(), last2.getId());
        Assert.assertEquals("Execution shifted down - N-1", oneButLast.getId(), oneButLast2.getId());
    }

    @Test
    public void testBuiltinShiftNotAllowed() {
        List<AuthenticationExecutionInfoRepresentation> executions = authMgmtResource.getExecutions("browser");

        AuthenticationExecutionInfoRepresentation last = executions.get(executions.size() - 1);
        AuthenticationExecutionInfoRepresentation oneButLast = executions.get(executions.size() - 2);

        // Not possible to raise - It's builtin flow
        try {
            authMgmtResource.raisePriority(last.getId());
            Assert.fail("Not expected to raise priority of builtin flow");
        } catch (BadRequestException nfe) {
            // Expected
        }

        // Not possible to lower - It's builtin flow
        try {
            authMgmtResource.lowerPriority(oneButLast.getId());
            Assert.fail("Not expected to lower priority of builtin flow");
        } catch (BadRequestException nfe) {
            // Expected
        }

    }
}
