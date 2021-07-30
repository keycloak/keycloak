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

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientPoliciesResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.admin.client.resource.RolePoliciesResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractServletAuthzAdapterTest extends AbstractBaseServletAuthzAdapterTest {

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
            assertWasDenied();
        });
    }

    @Test
    public void testRegularUserPermissions() throws Exception {
        performTests(() -> {
            login("alice", "alice");
            assertWasNotDenied();
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
            assertWasDenied();

            navigateToAdminPage();
            assertWasDenied();
        });
    }

    @Test
    public void testUserPremiumPermissions() throws Exception {
        performTests(() -> {
            login("jdoe", "jdoe");
            assertWasNotDenied();
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
            assertWasNotDenied();

            navigateToAdminPage();
            assertWasDenied();
        });
    }

    @Test
    public void testAdminPermissions() throws Exception {
        performTests(() -> {
            login("admin", "admin");
            assertWasNotDenied();
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
            assertWasDenied();

            navigateToAdminPage();
            assertWasNotDenied();
        });
    }

    @Test
    public void testGrantPremiumAccessToUser() throws Exception {
        performTests(() -> {
            login("alice", "alice");
            assertWasNotDenied();

            navigateToUserPremiumPage();
            assertWasDenied();

            updatePermissionPolicies("Premium Resource Permission", "Any User Policy");

            login("alice", "alice");

            navigateToUserPremiumPage();
            assertWasNotDenied();

            updatePermissionPolicies("Premium Resource Permission", "Only Premium User Policy");

            login("alice", "alice");

            navigateToUserPremiumPage();
            assertWasDenied();

            createUserPolicy("Temporary Premium Access Policy", "alice");

            updatePermissionPolicies("Premium Resource Permission", "Temporary Premium Access Policy");

            login("alice", "alice");

            navigateToUserPremiumPage();
            assertWasNotDenied();
        });
    }

    @Test
    public void testGrantAdministrativePermissions() throws Exception {
        performTests(() -> {
            login("jdoe", "jdoe");

            navigateToAdminPage();
            assertWasDenied();

            RealmResource realmResource = realmsResouce().realm(REALM_NAME);
            UsersResource usersResource = realmResource.users();
            List<UserRepresentation> users = usersResource.search("jdoe", null, null, null, null, null);

            assertFalse(users.isEmpty());

            UserResource userResource = usersResource.get(users.get(0).getId());

            RoleRepresentation adminRole = realmResource.roles().get("admin").toRepresentation();
            userResource.roles().realmLevel().add(Arrays.asList(adminRole));

            login("jdoe", "jdoe");

            navigateToAdminPage();
            assertWasNotDenied();
        });
    }
    
    //KEYCLOAK-3830
    @Test
    @Ignore // Ignored because: KEYCLOAK-7941
    public void testAccessPublicResource() throws Exception {
        performTests(() -> {
            driver.navigate().to(getResourceServerUrl() + "/public-html.html");
            WaitUtils.waitForPageToLoad();
            assertTrue(hasText("This is public resource that should be accessible without login."));
        });
    }

    @Test
    public void testRequiredRole() throws Exception {
        performTests(() -> {
            login("jdoe", "jdoe");
            navigateToUserPremiumPage();
            assertWasNotDenied();

            RolesResource rolesResource = getClientResource(RESOURCE_SERVER_ID).roles();

            rolesResource.create(new RoleRepresentation("required-role", "", false));

            RolePolicyRepresentation policy = new RolePolicyRepresentation();

            policy.setName("Required Role Policy");
            policy.addRole("user_premium", false);
            policy.addRole(RESOURCE_SERVER_ID + "/required-role", false);

            RolePoliciesResource rolePolicy = getAuthorizationResource().policies().role();

            rolePolicy.create(policy);
            policy = rolePolicy.findByName(policy.getName());

            updatePermissionPolicies("Premium Resource Permission", policy.getName());

            login("jdoe", "jdoe");
            navigateToUserPremiumPage();
            assertWasNotDenied();

            policy.getRoles().clear();
            policy.addRole("user_premium", false);
            policy.addRole(RESOURCE_SERVER_ID + "/required-role", true);

            rolePolicy.findById(policy.getId()).update(policy);

            login("jdoe", "jdoe");
            navigateToUserPremiumPage();
            assertWasDenied();

            UsersResource users = realmsResouce().realm(REALM_NAME).users();
            UserRepresentation user = users.search("jdoe").get(0);

            RoleScopeResource roleScopeResource = users.get(user.getId()).roles().clientLevel(getClientResource(RESOURCE_SERVER_ID).toRepresentation().getId());
            RoleRepresentation requiredRole = rolesResource.get("required-role").toRepresentation();
            roleScopeResource.add(Arrays.asList(requiredRole));

            login("jdoe", "jdoe");
            navigateToUserPremiumPage();
            assertWasNotDenied();

            policy.getRoles().clear();
            policy.addRole("user_premium", false);
            policy.addRole(RESOURCE_SERVER_ID + "/required-role", false);

            rolePolicy.findById(policy.getId()).update(policy);

            login("jdoe", "jdoe");
            navigateToUserPremiumPage();
            assertWasNotDenied();

            roleScopeResource.remove(Arrays.asList(requiredRole));

            login("jdoe", "jdoe");
            navigateToUserPremiumPage();
            assertWasNotDenied();
        });
    }

    @Test
    public void testOnlySpecificClient() throws Exception {
        performTests(() -> {
            login("jdoe", "jdoe");
            assertWasNotDenied();

            ClientPolicyRepresentation policy = new ClientPolicyRepresentation();

            policy.setName("Only Client Policy");
            policy.addClient("admin-cli");

            ClientPoliciesResource policyResource = getAuthorizationResource().policies().client();
            Response response = policyResource.create(policy);
            response.close();
            policy = policyResource.findByName(policy.getName());

            updatePermissionPolicies("Protected Resource Permission", policy.getName());

            login("jdoe", "jdoe");
            assertWasDenied();

            policy.addClient("servlet-authz-app");
            policyResource.findById(policy.getId()).update(policy);

            login("jdoe", "jdoe");
            assertWasNotDenied();
        });
    }

    @Test
    public void testAccessResourceWithAnyScope() throws Exception {
        performTests(() -> {
            login("jdoe", "jdoe");
            driver.navigate().to(getResourceServerUrl() + "/protected/scopes.jsp");
            WaitUtils.waitForPageToLoad();
            assertTrue(hasText("Granted"));
        });
    }

    @Test
    public void testMultipleURLsForResourceRealmConfig() throws Exception {
        performTests(() -> {
            login("jdoe", "jdoe");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource1/index1.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("sub-resource1 index1.jsp");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource1/index2.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("sub-resource1 index2.jsp");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource2/pattern1/page.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("sub-resource2/pattern1");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource2/pattern2/page.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("sub-resource2/pattern2");

            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/test.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("keycloak-7269/test");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource2/test.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("keycloak-7269/sub-resource2/test");

            updatePermissionPolicies("Permission for multiple url resource", "Deny Policy");
            login("jdoe", "jdoe");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource1/index1.jsp");
            waitUntilElement(By.tagName("h2")).text().not().contains("sub-resource1 index1.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("You can not access this resource.");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource1/index2.jsp");
            waitUntilElement(By.tagName("h2")).text().not().contains("sub-resource1 index2.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("You can not access this resource.");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource2/pattern1/page.jsp");
            waitUntilElement(By.tagName("h2")).text().not().contains("sub-resource2/pattern1");
            waitUntilElement(By.tagName("h2")).text().contains("You can not access this resource.");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource2/pattern2/page.jsp");
            waitUntilElement(By.tagName("h2")).text().not().contains("sub-resource2/pattern2");
            waitUntilElement(By.tagName("h2")).text().contains("You can not access this resource.");

            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/test.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("keycloak-7269/test");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource2/test.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("keycloak-7269/sub-resource2/test");

            updatePermissionPolicies("Permission for multiple url resource", "All Users Policy");
            login("jdoe", "jdoe");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource1/index1.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("sub-resource1 index1.jsp");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource1/index2.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("sub-resource1 index2.jsp");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource2/pattern1/page.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("sub-resource2/pattern1");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource2/pattern2/page.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("sub-resource2/pattern2");

            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/test.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("keycloak-7269/test");
            driver.navigate().to(getResourceServerUrl() + "/keycloak-7269/sub-resource2/test.jsp");
            waitUntilElement(By.tagName("h2")).text().contains("keycloak-7269/sub-resource2/test");
        });
    }
}
