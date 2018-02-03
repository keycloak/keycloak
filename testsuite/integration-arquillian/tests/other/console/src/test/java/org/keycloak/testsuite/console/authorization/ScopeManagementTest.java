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
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.console.page.clients.authorization.resource.Resource;
import org.keycloak.testsuite.console.page.clients.authorization.scope.Scope;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopeManagementTest extends AbstractAuthorizationSettingsTest {

    @Test
    public void testUpdate() {
        ScopeRepresentation expected = createScope();
        String previousName = expected.getName();

        expected.setName("changed");
        expected.setDisplayName("changed");

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().scopes().update(previousName, expected);

        assertAlertSuccess();
        assertScope(expected);
    }

    @Test
    public void testDelete() {
        ScopeRepresentation expected = createScope();
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().scopes().delete(expected.getName());
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().scopes().scopes().findByName(expected.getName()));
    }

    @Test
    public void testDeleteFromList() {
        ScopeRepresentation expected = createScope();
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().scopes().deleteFromList(expected.getName());
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().scopes().scopes().findByName(expected.getName()));
    }

    private ScopeRepresentation createScope() {
        ScopeRepresentation expected = new ScopeRepresentation();

        expected.setName("Test Scope");
        expected.setDisplayName("Test Scope Display Name");

        authorizationPage.authorizationTabs().scopes().create(expected);
        assertAlertSuccess();
        assertScope(expected);

        return expected;
    }

    private void assertScope(ScopeRepresentation expected) {
        authorizationPage.navigateTo();
        ScopeRepresentation actual = authorizationPage.authorizationTabs().scopes().scopes().findByName(expected.getName());

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getIconUri(), actual.getIconUri());

        ScopeRepresentation scope = authorizationPage.authorizationTabs().scopes().name(expected.getName()).toRepresentation();

        assertEquals(expected.getDisplayName(), scope.getDisplayName());
    }
}
