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
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;

import javax.ws.rs.core.GenericType;
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
        try {
            Assert.assertEquals("Copy flow", 201, response.getStatus());
        } finally {
            response.close();
        }

        // get executions
        response = authMgmtResource.getExecutions("Copy of browser");
        List<AuthenticationExecutionInfoRepresentation> executions = response.readEntity(new GenericType<List<AuthenticationExecutionInfoRepresentation>>() {
        });

        AuthenticationExecutionInfoRepresentation last = executions.get(executions.size() - 1);
        AuthenticationExecutionInfoRepresentation oneButLast = executions.get(executions.size() - 2);

        // shift last execution up
        authMgmtResource.raisePriority(last.getId());

        response = authMgmtResource.getExecutions("Copy of browser");
        List<AuthenticationExecutionInfoRepresentation> executions2 = response.readEntity(new GenericType<List<AuthenticationExecutionInfoRepresentation>>() {
        });

        AuthenticationExecutionInfoRepresentation last2 = executions2.get(executions.size() - 1);
        AuthenticationExecutionInfoRepresentation oneButLast2 = executions2.get(executions.size() - 2);

        Assert.assertEquals("Execution shifted up - N", last.getId(), oneButLast2.getId());
        Assert.assertEquals("Execution shifted up - N-1", oneButLast.getId(), last2.getId());

        // shift one before last down
        authMgmtResource.lowerPriority(oneButLast2.getId());

        response = authMgmtResource.getExecutions("Copy of browser");
        executions2 = response.readEntity(new GenericType<List<AuthenticationExecutionInfoRepresentation>>() {
        });

        last2 = executions2.get(executions.size() - 1);
        oneButLast2 = executions2.get(executions.size() - 2);

        Assert.assertEquals("Execution shifted down - N", last.getId(), last2.getId());
        Assert.assertEquals("Execution shifted down - N-1", oneButLast.getId(), oneButLast2.getId());
    }
}
