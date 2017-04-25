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
package org.keycloak.testsuite.adapter.example.authorization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.util.WaitUtils;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractServletAuthzFunctionalAdapterTest extends AbstractServletAuthzAdapterTest {

    @Deployment(name = RESOURCE_SERVER_ID, managed = false)
    public static WebArchive deployment() throws IOException {
        return exampleDeployment(RESOURCE_SERVER_ID);
    }

    @Test
    public void testCanNotAccessWhenEnforcing() throws Exception {
        performTests(() -> {
            importResourceServerSettings();
            ResourcesResource resources = getAuthorizationResource().resources();
            ResourceRepresentation resource = resources.findByName("Protected Resource").get(0);

            resource.setUri("/index.jsp");

            resources.resource(resource.getId()).update(resource);
        }, () -> {
            login("jdoe", "jdoe");
            driver.navigate().to(getResourceServerUrl().toString() + "/enforcing/resource");
            assertTrue(wasDenied());
        });
    }

    @Test
    public void testRegularUserPermissions() throws Exception {
        performTests(() -> {
            login("alice", "alice");
            assertFalse(wasDenied());
            assertTrue(hasLink("User Premium"));
            assertTrue(hasLink("Administration"));
            assertTrue(hasText("urn:servlet-authz:page:main:actionForUser"));
            assertFalse(hasText("urn:servlet-authz:page:main:actionForAdmin"));
            assertFalse(hasText("urn:servlet-authz:page:main:actionForPremiumUser"));

            navigateToDynamicMenuPage();
            assertTrue(hasText("Do user thing"));
            assertFalse(hasText("Do  user premium thing"));
            assertFalse(hasText("Do administration thing"));

            navigateToUserPremiumPage();
            assertTrue(wasDenied());

            navigateToAdminPage();
            assertTrue(wasDenied());
        });
    }

    @Test
    public void testUserPremiumPermissions() throws Exception {
        performTests(() -> {
            login("jdoe", "jdoe");
            assertFalse(wasDenied());
            assertTrue(hasLink("User Premium"));
            assertTrue(hasLink("Administration"));
            assertTrue(hasText("urn:servlet-authz:page:main:actionForUser"));
            assertTrue(hasText("urn:servlet-authz:page:main:actionForPremiumUser"));
            assertFalse(hasText("urn:servlet-authz:page:main:actionForAdmin"));

            navigateToDynamicMenuPage();
            assertTrue(hasText("Do user thing"));
            assertTrue(hasText("Do  user premium thing"));
            assertFalse(hasText("Do administration thing"));

            navigateToUserPremiumPage();
            assertFalse(wasDenied());

            navigateToAdminPage();
            assertTrue(wasDenied());
        });
    }

    @Test
    public void testAdminPermissions() throws Exception {
        performTests(() -> {
            login("admin", "admin");
            assertFalse(wasDenied());
            assertTrue(hasLink("User Premium"));
            assertTrue(hasLink("Administration"));
            assertTrue(hasText("urn:servlet-authz:page:main:actionForUser"));
            assertTrue(hasText("urn:servlet-authz:page:main:actionForAdmin"));
            assertFalse(hasText("urn:servlet-authz:page:main:actionForPremiumUser"));

            navigateToDynamicMenuPage();
            assertTrue(hasText("Do user thing"));
            assertTrue(hasText("Do administration thing"));
            assertFalse(hasText("Do  user premium thing"));

            navigateToUserPremiumPage();
            assertTrue(wasDenied());

            navigateToAdminPage();
            assertFalse(wasDenied());
        });
    }

    @Test
    public void testGrantPremiumAccessToUser() throws Exception {
        performTests(() -> {
            login("alice", "alice");
            assertFalse(wasDenied());

            navigateToUserPremiumPage();
            assertTrue(wasDenied());

            updatePermissionPolicies("Premium Resource Permission", "Any User Policy");

            login("alice", "alice");

            navigateToUserPremiumPage();
            assertFalse(wasDenied());

            updatePermissionPolicies("Premium Resource Permission", "Only Premium User Policy");

            login("alice", "alice");

            navigateToUserPremiumPage();
            assertTrue(wasDenied());

            createUserPolicy("Temporary Premium Access Policy", "alice");

            updatePermissionPolicies("Premium Resource Permission", "Temporary Premium Access Policy");

            login("alice", "alice");

            navigateToUserPremiumPage();
            assertFalse(wasDenied());
        });
    }

    @Test
    public void testGrantAdministrativePermissions() throws Exception {
        performTests(() -> {
            login("jdoe", "jdoe");

            navigateToAdminPage();
            assertTrue(wasDenied());

            RealmResource realmResource = realmsResouce().realm(REALM_NAME);
            UsersResource usersResource = realmResource.users();
            List<UserRepresentation> users = usersResource.search("jdoe", null, null, null, null, null);

            assertFalse(users.isEmpty());

            UserResource userResource = usersResource.get(users.get(0).getId());

            RoleRepresentation adminRole = realmResource.roles().get("admin").toRepresentation();
            userResource.roles().realmLevel().add(Arrays.asList(adminRole));

            login("jdoe", "jdoe");

            navigateToAdminPage();
            assertFalse(wasDenied());
        });
    }
    
    //KEYCLOAK-3830
    @Test
    public void testAccessPublicResource() throws Exception {
        performTests(() -> {
            driver.navigate().to(getResourceServerUrl() + "/public-html.html");
            WaitUtils.waitForPageToLoad(driver);
            assertTrue(hasText("This is public resource that should be accessible without login."));
        });
    }
}
