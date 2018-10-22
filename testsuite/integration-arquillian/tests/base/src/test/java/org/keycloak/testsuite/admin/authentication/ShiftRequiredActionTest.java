/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import java.util.List;

import javax.ws.rs.NotFoundException;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testsuite.util.AdminEventPaths;

/**
 * @author <a href="mailto:wadahiro@gmail.com">Hiroyuki Wada</a>
 */
public class ShiftRequiredActionTest extends AbstractAuthenticationTest {

    @Test
    public void testShiftRequiredAction() {

        // get action
        List<RequiredActionProviderRepresentation> actions = authMgmtResource.getRequiredActions();

        RequiredActionProviderRepresentation last = actions.get(actions.size() - 1);
        RequiredActionProviderRepresentation oneButLast = actions.get(actions.size() - 2);

        // Not possible to raisePriority of not-existent required action
        try {
            authMgmtResource.raisePriority("not-existent");
            Assert.fail("Not expected to raise priority of not existent required action");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // shift last required action up
        authMgmtResource.raiseRequiredActionPriority(last.getAlias());
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authRaiseRequiredActionPath(last.getAlias()), ResourceType.REQUIRED_ACTION);

        List<RequiredActionProviderRepresentation> actions2 = authMgmtResource.getRequiredActions();

        RequiredActionProviderRepresentation last2 = actions2.get(actions.size() - 1);
        RequiredActionProviderRepresentation oneButLast2 = actions2.get(actions.size() - 2);

        Assert.assertEquals("Required action shifted up - N", last.getAlias(), oneButLast2.getAlias());
        Assert.assertEquals("Required action up - N-1", oneButLast.getAlias(), last2.getAlias());

        // Not possible to lowerPriority of not-existent required action
        try {
            authMgmtResource.lowerRequiredActionPriority("not-existent");
            Assert.fail("Not expected to raise priority of not existent required action");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // shift one before last down
        authMgmtResource.lowerRequiredActionPriority(oneButLast2.getAlias());
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authLowerRequiredActionPath(oneButLast2.getAlias()), ResourceType.REQUIRED_ACTION);

        actions2 = authMgmtResource.getRequiredActions();

        last2 = actions2.get(actions.size() - 1);
        oneButLast2 = actions2.get(actions.size() - 2);

        Assert.assertEquals("Required action shifted down - N", last.getAlias(), last2.getAlias());
        Assert.assertEquals("Required action shifted down - N-1", oneButLast.getAlias(), oneButLast2.getAlias());
    }
}
