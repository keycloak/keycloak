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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.console.page.clients.authorization.resource.Resource;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceManagementTest extends AbstractAuthorizationSettingsTest {

    @Before
    @Override
    public void configureTest() {
        super.configureTest();
        for (String scopeName : Arrays.asList("Scope A", "Scope B", "Scope C")) {
            authorizationPage.navigateTo();
            authorizationPage.authorizationTabs().scopes().create(new ScopeRepresentation(scopeName));
        }
        authorizationPage.navigateTo();
    }

    @Test
    public void testUpdate() {
        ResourceRepresentation expected = createResource();
        String previousName = expected.getName();

        expected.setName("changed");
        expected.setDisplayName("changed");
        expected.setType("changed");
        expected.setUri("changed");
        expected.setScopes(Arrays.asList("Scope A", "Scope B", "Scope C").stream().map(name -> new ScopeRepresentation(name)).collect(Collectors.toSet()));

        authorizationPage.navigateTo();
        Resource resource = authorizationPage.authorizationTabs().resources().name(previousName);
        resource.update(expected);
        assertAlertSuccess();
        assertResource(expected);

        expected.setScopes(expected.getScopes().stream().filter(scope -> scope.getName().equals("Scope C")).collect(Collectors.toSet()));

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().resources().update(expected.getName(), expected);
        assertAlertSuccess();
        assertResource(expected);
    }

    @Test
    public void testDeleteFromDetails() {
        ResourceRepresentation expected = createResource();
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().resources().delete(expected.getName());
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().resources().resources().findByName(expected.getName()));
    }

    @Test
    public void testDeleteFromList() {
        ResourceRepresentation expected = createResource();
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().resources().deleteFromList(expected.getName());
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().resources().resources().findByName(expected.getName()));
    }

    private ResourceRepresentation createResource() {
        ResourceRepresentation expected = new ResourceRepresentation();

        expected.setName("Test Resource");
        expected.setDisplayName("Test Display Name");
        expected.setType("Test Type");
        expected.setUri("/test/resource");

        authorizationPage.authorizationTabs().resources().create(expected);
        assertAlertSuccess();

        return expected;
    }

    private void assertResource(ResourceRepresentation expected) {
        authorizationPage.navigateTo();
        ResourceRepresentation actual = authorizationPage.authorizationTabs().resources().resources().findByName(expected.getName());

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getUri(), actual.getUri());
        assertEquals(expected.getIconUri(), actual.getIconUri());

        ResourceRepresentation resource = authorizationPage.authorizationTabs().resources().name(expected.getName()).toRepresentation();

        assertEquals(expected.getDisplayName(), resource.getDisplayName());

        Set<ScopeRepresentation> associatedScopes = resource.getScopes();

        if (expected.getScopes() != null) {
            assertNotNull(associatedScopes);
            assertEquals(expected.getScopes().size(), associatedScopes.size());
            assertEquals(0, resource.getScopes().stream().filter(actualScope -> !expected.getScopes().stream()
                    .filter(expectedScope -> actualScope.getName().equals(expectedScope.getName()))
                    .findFirst().isPresent())
                    .count());
        } else {
            assertTrue(associatedScopes.isEmpty());
        }

    }
}
