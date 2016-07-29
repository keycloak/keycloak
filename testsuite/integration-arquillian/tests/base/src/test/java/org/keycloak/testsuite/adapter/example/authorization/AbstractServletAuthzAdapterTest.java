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

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.IOUtil.loadJson;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractServletAuthzAdapterTest extends AbstractExampleAdapterTest {

    private static final String REALM_NAME = "servlet-authz";
    private static final String RESOURCE_SERVER_ID = "servlet-authz-app";

    @ArquillianResource
    private Deployer deployer;

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(
                loadRealm(new File(TEST_APPS_HOME_DIR + "/servlet-authz-app/servlet-authz-realm.json")));
    }

    @Deployment(name = RESOURCE_SERVER_ID, managed = false)
    public static WebArchive deployment() throws IOException {
        return exampleDeployment(RESOURCE_SERVER_ID);
    }

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
        importResourceServerSettings();
    }

    @Test
    public void testRegularUserPermissions() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

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
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testUserPremiumPermissions() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

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
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testAdminPermissions() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

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
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testGrantPremiumAccessToUser() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

            login("alice", "alice");
            assertFalse(wasDenied());

            navigateToUserPremiumPage();
            assertTrue(wasDenied());

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Premium Resource Permission".equals(policy.getName())) {
                    policy.getConfig().put("applyPolicies", "[\"Any User Policy\"]");
                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
            }

            login("alice", "alice");

            navigateToUserPremiumPage();
            assertFalse(wasDenied());

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Premium Resource Permission".equals(policy.getName())) {
                    policy.getConfig().put("applyPolicies", "[\"Only Premium User Policy\"]");
                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
            }

            login("alice", "alice");

            navigateToUserPremiumPage();
            assertTrue(wasDenied());

            PolicyRepresentation onlyAlicePolicy = new PolicyRepresentation();

            onlyAlicePolicy.setName("Temporary Premium Access Policy");
            onlyAlicePolicy.setType("user");
            HashMap<String, String> config = new HashMap<>();
            UsersResource usersResource = realmsResouce().realm(REALM_NAME).users();
            List<UserRepresentation> users = usersResource.search("alice", null, null, null, null, null);

            assertFalse(users.isEmpty());

            config.put("users", JsonSerialization.writeValueAsString(Arrays.asList(users.get(0).getId())));

            onlyAlicePolicy.setConfig(config);
            getAuthorizationResource().policies().create(onlyAlicePolicy);

            for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
                if ("Premium Resource Permission".equals(policy.getName())) {
                    policy.getConfig().put("applyPolicies", "[\"Temporary Premium Access Policy\"]");
                    getAuthorizationResource().policies().policy(policy.getId()).update(policy);
                }
            }

            login("alice", "alice");

            navigateToUserPremiumPage();
            assertFalse(wasDenied());
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    @Test
    public void testGrantAdministrativePermissions() throws Exception {
        try {
            this.deployer.deploy(RESOURCE_SERVER_ID);

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
        } finally {
            this.deployer.undeploy(RESOURCE_SERVER_ID);
        }
    }

    private boolean hasLink(String text) {
        return getLink(text) != null;
    }

    private boolean hasText(String text) {
        return this.driver.getPageSource().contains(text);
    }

    private WebElement getLink(String text) {
        return this.driver.findElement(By.xpath("//a[text() = '" + text + "']"));
    }

    private void importResourceServerSettings() throws FileNotFoundException {
        getAuthorizationResource().importSettings(loadJson(new FileInputStream(new File(TEST_APPS_HOME_DIR + "/servlet-authz-app/servlet-authz-app-authz-service.json")), ResourceServerRepresentation.class));
    }

    private AuthorizationResource getAuthorizationResource() throws FileNotFoundException {
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

    private void login(String username, String password) throws InterruptedException {
        navigateTo();
        Thread.sleep(2000);
        if (this.driver.getCurrentUrl().startsWith(getResourceServerUrl().toString())) {
            Thread.sleep(2000);
            logOut();
            navigateTo();
        }

        Thread.sleep(2000);

        this.loginPage.form().login(username, password);
    }

    private void navigateTo() {
        this.driver.navigate().to(getResourceServerUrl());
        WaitUtils.waitUntilElement(By.xpath("//a[text() = 'Dynamic Menu']"));
    }

    private  boolean wasDenied() {
        return this.driver.getPageSource().contains("You can not access this resource.");
    }

    private URL getResourceServerUrl() {
        try {
            return new URL(this.appServerContextRootPage + "/" + RESOURCE_SERVER_ID);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not obtain resource server url.", e);
        }
    }

    private void navigateToDynamicMenuPage() {
        navigateTo();
        getLink("Dynamic Menu").click();
    }

    private void navigateToUserPremiumPage() {
        navigateTo();
        getLink("User Premium").click();
    }

    private void navigateToAdminPage() {
        navigateTo();
        getLink("Administration").click();
    }
}
