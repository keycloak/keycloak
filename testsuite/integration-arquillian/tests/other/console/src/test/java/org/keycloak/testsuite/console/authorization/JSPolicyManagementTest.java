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
package org.keycloak.testsuite.console.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.testsuite.console.page.clients.authorization.policy.JSPolicy;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JSPolicyManagementTest extends AbstractAuthorizationSettingsTest {

    @Test
    public void testUpdate() throws InterruptedException {
        authorizationPage.navigateTo();
        JSPolicyRepresentation expected = new JSPolicyRepresentation();

        expected.setName("Deny Policy");
        expected.setType("script-scripts/always-deny-policy.js");
        expected.setDescription("description");

        expected = createPolicy(expected);

        String previousName = expected.getName();

        expected.setName("Changed Test JS Policy");
        expected.setDescription("Changed description");
        expected.setLogic(Logic.NEGATIVE);

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(previousName, expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        JSPolicy actual = authorizationPage.authorizationTabs().policies().name(expected.getName());

        assertPolicy(expected, actual);
    }

    @Test
    public void testDelete() throws InterruptedException {
        authorizationPage.navigateTo();
        JSPolicyRepresentation expected = new JSPolicyRepresentation();

        expected.setName("Deny Policy");
        expected.setType("script-scripts/always-deny-policy.js");
        expected.setDescription("description");

        expected = createPolicy(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().delete(expected.getName());
        assertAlertSuccess();
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().policies().policies().findByName(expected.getName()));
    }

    @Test
    public void testDeleteFromList() throws InterruptedException {
        authorizationPage.navigateTo();
        JSPolicyRepresentation expected = new JSPolicyRepresentation();

        expected.setName("Deny Policy");
        expected.setType("script-scripts/always-deny-policy.js");
        expected.setDescription("description");

        expected = createPolicy(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().deleteFromList(expected.getName());
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().policies().policies().findByName(expected.getName()));
    }

    private JSPolicyRepresentation createPolicy(JSPolicyRepresentation expected) {
        authorizationPage.authorizationTabs().policies().create(expected);
        assertAlertSuccess();
        return expected;
    }

    private JSPolicyRepresentation assertPolicy(JSPolicyRepresentation expected, JSPolicy policy) {
        JSPolicyRepresentation actual = policy.toRepresentation();

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getLogic(), actual.getLogic());

        if (actual.getCode() != null) {
            assertEquals("$evaluation.deny();", actual.getCode());
        }

        return actual;
    }
}
