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
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.WaitUtils.pause;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.ResourcePermissionsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractServletPolicyEnforcerTest extends AbstractExampleAdapterTest {

    protected static final String REALM_NAME = "servlet-policy-enforcer-authz";
    protected static final String RESOURCE_SERVER_ID = "servlet-policy-enforcer";

    @BeforeClass
    public static void enabled() { ProfileAssume.assumePreview(); }

    @ArquillianResource
    private Deployer deployer;

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(
                loadRealm(new File(TEST_APPS_HOME_DIR + "/servlet-policy-enforcer/servlet-policy-enforcer-authz-realm.json")));
    }

    @Deployment(name = RESOURCE_SERVER_ID, managed = false)
    public static WebArchive deployment() throws IOException {
        return exampleDeployment(RESOURCE_SERVER_ID);
    }

    @Test
    public void testPattern1() throws Exception {
        performTests(() -> {
            login("alice", "alice");

            navigateTo("/resource/a/b");
            assertFalse(wasDenied());

            updatePermissionPolicies("Pattern 1 Permission", "Deny Policy");
            login("alice", "alice");
            navigateTo("/resource/a/b");
            assertTrue(wasDenied());

            updatePermissionPolicies("Pattern 1 Permission", "Default Policy");
            login("alice", "alice");
            navigateTo("/resource/a/b");
            assertFalse(wasDenied());
        });
    }

    @Test
    public void testPattern2() throws Exception {
        performTests(() -> {
            login("alice", "alice");

            navigateTo("/a/resource-a");
            assertFalse(wasDenied());
            navigateTo("/b/resource-a");
            assertFalse(wasDenied());

            updatePermissionPolicies("Pattern 2 Permission", "Deny Policy");
            login("alice", "alice");
            navigateTo("/a/resource-a");
            assertTrue(wasDenied());
            navigateTo("/b/resource-a");
            assertTrue(wasDenied());

            updatePermissionPolicies("Pattern 2 Permission", "Default Policy");
            login("alice", "alice");
            navigateTo("/b/resource-a");
            assertFalse(wasDenied());
        });
    }

    @Test
    public void testPattern3() throws Exception {
        performTests(() -> {
            login("alice", "alice");

            navigateTo("/a/resource-b");
            assertFalse(wasDenied());
            navigateTo("/b/resource-b");
            assertFalse(wasDenied());

            updatePermissionPolicies("Pattern 3 Permission", "Deny Policy");
            login("alice", "alice");
            navigateTo("/a/resource-b");
            assertTrue(wasDenied());
            navigateTo("/b/resource-b");
            assertTrue(wasDenied());

            updatePermissionPolicies("Pattern 3 Permission", "Default Policy");
            login("alice", "alice");
            navigateTo("/b/resource-b");
            assertFalse(wasDenied());

            updatePermissionPolicies("Pattern 2 Permission", "Default Policy");
            login("alice", "alice");
            navigateTo("/b/resource-a");
            assertFalse(wasDenied());

            updatePermissionPolicies("Pattern 3 Permission", "Deny Policy");
            login("alice", "alice");
            navigateTo("/a/resource-b");
            assertTrue(wasDenied());
            navigateTo("/b/resource-a");
            assertFalse(wasDenied());
        });
    }

    @Test
    public void testPattern4() throws Exception {
        performTests(() -> {
            login("alice", "alice");

            navigateTo("/resource-c");
            assertFalse(wasDenied());

            updatePermissionPolicies("Pattern 4 Permission", "Deny Policy");
            login("alice", "alice");
            navigateTo("/resource-c");
            assertTrue(wasDenied());

            updatePermissionPolicies("Pattern 4 Permission", "Default Policy");
            login("alice", "alice");
            navigateTo("/resource-c");
            assertFalse(wasDenied());
        });
    }

    @Test
    public void testPattern5() throws Exception {
        performTests(() -> {
            login("alice", "alice");

            navigateTo("/resource/a/resource-d");
            assertFalse(wasDenied());
            navigateTo("/resource/b/resource-d");
            assertFalse(wasDenied());

            updatePermissionPolicies("Pattern 5 Permission", "Deny Policy");
            login("alice", "alice");
            navigateTo("/resource/a/resource-d");
            assertTrue(wasDenied());
            navigateTo("/resource/b/resource-d");
            assertTrue(wasDenied());

            updatePermissionPolicies("Pattern 5 Permission", "Default Policy");
            login("alice", "alice");
            navigateTo("/resource/b/resource-d");
            assertFalse(wasDenied());
        });
    }

    @Test
    public void testPattern6() throws Exception {
        performTests(() -> {
            login("alice", "alice");

            navigateTo("/resource/a");
            assertFalse(wasDenied());
            navigateTo("/resource/b");
            assertFalse(wasDenied());

            updatePermissionPolicies("Pattern 6 Permission", "Deny Policy");
            login("alice", "alice");
            navigateTo("/resource/a");
            assertTrue(wasDenied());
            navigateTo("/resource/b");
            assertTrue(wasDenied());

            updatePermissionPolicies("Pattern 6 Permission", "Default Policy");
            login("alice", "alice");
            navigateTo("/resource/b");
            assertFalse(wasDenied());
        });
    }

    @Test
    public void testPattern7() throws Exception {
        performTests(() -> {
            login("alice", "alice");

            navigateTo("/resource/a/f/b");
            assertFalse(wasDenied());
            navigateTo("/resource/c/f/d");
            assertFalse(wasDenied());

            updatePermissionPolicies("Pattern 7 Permission", "Deny Policy");
            login("alice", "alice");
            navigateTo("/resource/a/f/b");
            assertTrue(wasDenied());
            navigateTo("/resource/c/f/d");
            assertTrue(wasDenied());

            updatePermissionPolicies("Pattern 7 Permission", "Default Policy");
            login("alice", "alice");
            navigateTo("/resource/c/f/d");
            assertFalse(wasDenied());
        });
    }

    @Test
    public void testPattern8() throws Exception {
        performTests(() -> {
            login("alice", "alice");

            navigateTo("/resource");
            assertFalse(wasDenied());

            updatePermissionPolicies("Pattern 8 Permission", "Deny Policy");
            login("alice", "alice");
            navigateTo("/resource");
            assertTrue(wasDenied());

            updatePermissionPolicies("Pattern 8 Permission", "Default Policy");
            login("alice", "alice");
            navigateTo("/resource");
            assertFalse(wasDenied());
        });
    }

    @Test
    public void testPattern9() throws Exception {
        performTests(() -> {
            login("alice", "alice");

            navigateTo("/file/*.suffix");
            assertFalse(wasDenied());

            updatePermissionPolicies("Pattern 9 Permission", "Deny Policy");
            login("alice", "alice");
            navigateTo("/file/*.suffix");
            assertTrue(wasDenied());

            updatePermissionPolicies("Pattern 9 Permission", "Default Policy");
            login("alice", "alice");
            navigateTo("/file/*.suffix");
            assertFalse(wasDenied());
        });
    }

    @Test
    public void testPattern10() throws Exception {
        performTests(() -> {
            login("alice", "alice");

            navigateTo("/resource/a/i/b/c/d/e");
            navigateTo("/resource/a/i/b/c/");
            assertFalse(wasDenied());

            updatePermissionPolicies("Pattern 10 Permission", "Deny Policy");
            login("alice", "alice");
            navigateTo("/resource/a/i/b/c/d/e");
            navigateTo("/resource/a/i/b/c/d");
            assertTrue(wasDenied());

            updatePermissionPolicies("Pattern 10 Permission", "Default Policy");
            login("alice", "alice");
            navigateTo("/resource/a/i/b/c/d");
            assertFalse(wasDenied());
        });
    }

    @Test
    public void testPattern11UsingResourceInstancePermission() throws Exception {
        performTests(() -> {
            login("alice", "alice");
            navigateTo("/api/v1/resource-a");
            assertFalse(wasDenied());
            navigateTo("/api/v1/resource-b");
            assertFalse(wasDenied());

            ResourceRepresentation resource = new ResourceRepresentation("/api/v1/resource-c");

            resource.setUri(resource.getName());

            getAuthorizationResource().resources().create(resource);

            createResourcePermission(resource.getName() + " permission", resource.getName(), "Default Policy");

            login("alice", "alice");
            navigateTo(resource.getUri());
            assertFalse(wasDenied());

            updatePermissionPolicies(resource.getName() + " permission", "Deny Policy");

            login("alice", "alice");
            navigateTo(resource.getUri());
            assertTrue(wasDenied());

            updatePermissionPolicies(resource.getName() + " permission", "Default Policy");

            login("alice", "alice");
            navigateTo(resource.getUri());
            assertFalse(wasDenied());

            navigateTo("/api/v1");
            assertTrue(wasDenied());
            navigateTo("/api/v1/");
            assertTrue(wasDenied());
            navigateTo("/api");
            assertTrue(wasDenied());
            navigateTo("/api/");
            assertTrue(wasDenied());
        });
    }

    private void navigateTo(String path) {
        this.driver.navigate().to(getResourceServerUrl() + path);
    }

    private void performTests(ExceptionRunnable assertion) {
        performTests(() -> {}, assertion);
    }

    private void performTests(ExceptionRunnable beforeDeploy, ExceptionRunnable assertion) {
        try {
            beforeDeploy.run();
            deployer.deploy(RESOURCE_SERVER_ID);
            assertion.run();
        } catch (FileNotFoundException cause) {
            throw new RuntimeException("Failed to import authorization settings", cause);
        } catch (Exception cause) {
            throw new RuntimeException("Error while executing tests", cause);
        } finally {
            deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    private AuthorizationResource getAuthorizationResource() {
        return getClientResource(RESOURCE_SERVER_ID).authorization();
    }

    private ClientResource getClientResource(String clientId) {
        ClientsResource clients = this.realmsResouce().realm(REALM_NAME).clients();
        ClientRepresentation resourceServer = clients.findByClientId(clientId).get(0);
        return clients.get(resourceServer.getId());
    }

    private void logOut() {
        navigateTo();
        By by = By.xpath("//a[text() = 'Sign Out']");
        WaitUtils.waitUntilElement(by);
        this.driver.findElement(by).click();
        pause(500);
    }

    private  void login(String username, String password) {
        try {
            navigateTo();
            if (this.driver.getCurrentUrl().startsWith(getResourceServerUrl().toString())) {
                logOut();
                navigateTo();
            }
            this.loginPage.form().login(username, password);
            navigateTo();
            assertFalse(wasDenied());
        } catch (Exception cause) {
            throw new RuntimeException("Login failed", cause);
        }
    }

    private void navigateTo() {
        this.driver.navigate().to(getResourceServerUrl());
        WaitUtils.waitUntilElement(By.xpath("//p[text() = 'Welcome']"));
    }

    private boolean wasDenied() {
        return this.driver.getPageSource().contains("You can not access this resource");
    }

    private URL getResourceServerUrl() {
        try {
            return new URL(this.appServerContextRootPage + "/" + RESOURCE_SERVER_ID);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not obtain resource server url.", e);
        }
    }

    private void updatePermissionPolicies(String permissionName, String... policyNames) {
        ResourcePermissionsResource permissions = getAuthorizationResource().permissions().resource();
        ResourcePermissionRepresentation permission = permissions.findByName(permissionName);

        permission.addPolicy(policyNames);

        permissions.findById(permission.getId()).update(permission);
    }

    private void createResourcePermission(String name, String resourceName, String... policyNames) {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(name);
        permission.addResource(resourceName);
        permission.addPolicy(policyNames);

        getAuthorizationResource().permissions().resource().create(permission);
    }

    private interface ExceptionRunnable {
        void run() throws Exception;
    }
}
