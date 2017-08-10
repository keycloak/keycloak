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

import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.console.page.clients.authorization.policy.UserPolicy;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UserPolicyManagementTest extends AbstractAuthorizationSettingsTest {

    @Before
    public void configureTest() {
        super.configureTest();
        UsersResource users = testRealmResource().users();
        users.create(UserBuilder.create().username("user a").build());
        users.create(UserBuilder.create().username("user b").build());
        users.create(UserBuilder.create().username("user c").build());
    }

    @Test
    public void testUpdate() throws InterruptedException {
        authorizationPage.navigateTo();
        UserPolicyRepresentation expected = new UserPolicyRepresentation();

        expected.setName("Test User Policy");
        expected.setDescription("description");
        expected.addUser("user a");
        expected.addUser("user b");
        expected.addUser("user c");

        expected = createPolicy(expected);

        String previousName = expected.getName();

        expected.setName("Changed Test User Policy");
        expected.setDescription("Changed description");
        expected.setLogic(Logic.NEGATIVE);

        expected.setUsers(expected.getUsers().stream().filter(user -> !user.equals("user b")).collect(Collectors.toSet()));

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(previousName, expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        UserPolicy actual = authorizationPage.authorizationTabs().policies().name(expected.getName());

        assertPolicy(expected, actual);
    }

    @Test
    public void testDelete() throws InterruptedException {
        authorizationPage.navigateTo();
        UserPolicyRepresentation expected = new UserPolicyRepresentation();

        expected.setName("Test User Policy");
        expected.setDescription("description");
        expected.addUser("user c");

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
        UserPolicyRepresentation expected = new UserPolicyRepresentation();

        expected.setName("Test User Policy");
        expected.setDescription("description");
        expected.addUser("user c");

        expected = createPolicy(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().deleteFromList(expected.getName());
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().policies().policies().findByName(expected.getName()));
    }

    private UserPolicyRepresentation createPolicy(UserPolicyRepresentation expected) {
        UserPolicy policy = authorizationPage.authorizationTabs().policies().create(expected);
        assertAlertSuccess();
        return assertPolicy(expected, policy);
    }

    private UserPolicyRepresentation assertPolicy(UserPolicyRepresentation expected, UserPolicy policy) {
        UserPolicyRepresentation actual = policy.toRepresentation();

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getLogic(), actual.getLogic());

        assertNotNull(actual.getUsers());
        assertEquals(expected.getUsers().size(), actual.getUsers().size());
        assertEquals(0, actual.getUsers().stream().filter(actualUser -> !expected.getUsers().stream()
                .filter(expectedUser -> actualUser.equals(expectedUser))
                .findFirst().isPresent())
                .count());
        return actual;
    }
}
