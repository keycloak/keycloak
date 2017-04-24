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
import static org.keycloak.testsuite.util.IOUtil.loadJson;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.WaitUtils.pause;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.BeforeClass;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractServletAuthzAdapterTest extends AbstractExampleAdapterTest {

    protected static final String REALM_NAME = "servlet-authz";
    protected static final String RESOURCE_SERVER_ID = "servlet-authz-app";

    @BeforeClass
    public static void enabled() { ProfileAssume.assumePreview(); }

    @ArquillianResource
    private Deployer deployer;

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(
                loadRealm(new File(TEST_APPS_HOME_DIR + "/servlet-authz-app/servlet-authz-realm.json")));
    }

    protected void performTests(ExceptionRunnable assertion) {
        performTests(() -> importResourceServerSettings(), assertion);
    }

    protected void performTests(ExceptionRunnable beforeDeploy, ExceptionRunnable assertion) {
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

    protected boolean hasLink(String text) {
        return getLink(text) != null;
    }

    protected boolean hasText(String text) {
        return this.driver.getPageSource().contains(text);
    }

    private WebElement getLink(String text) {
        return this.driver.findElement(By.xpath("//a[text() = '" + text + "']"));
    }

    protected void importResourceServerSettings() throws FileNotFoundException {
        getAuthorizationResource().importSettings(loadJson(new FileInputStream(new File(TEST_APPS_HOME_DIR + "/servlet-authz-app/servlet-authz-app-authz-service.json")), ResourceServerRepresentation.class));
    }

    protected AuthorizationResource getAuthorizationResource() {
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

    protected void login(String username, String password) {
        try {
            navigateTo();
            Thread.sleep(2000);
            if (this.driver.getCurrentUrl().startsWith(getResourceServerUrl().toString())) {
                Thread.sleep(2000);
                logOut();
                navigateTo();
            }

            Thread.sleep(2000);

            this.loginPage.form().login(username, password);
        } catch (Exception cause) {
            throw new RuntimeException("Login failed", cause);
        }
    }

    private void navigateTo() {
        this.driver.navigate().to(getResourceServerUrl());
        WaitUtils.waitUntilElement(By.xpath("//a[text() = 'Dynamic Menu']"));
    }

    protected boolean wasDenied() {
        return this.driver.getPageSource().contains("You can not access this resource.");
    }

    protected URL getResourceServerUrl() {
        try {
            return new URL(this.appServerContextRootPage + "/" + RESOURCE_SERVER_ID);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not obtain resource server url.", e);
        }
    }

    protected void navigateToDynamicMenuPage() {
        navigateTo();
        getLink("Dynamic Menu").click();
    }

    protected void navigateToUserPremiumPage() {
        navigateTo();
        getLink("User Premium").click();
    }

    protected void navigateToAdminPage() {
        navigateTo();
        getLink("Administration").click();
    }

    protected void updatePermissionPolicies(String permissionName, String... policyNames) {
        for (PolicyRepresentation policy : getAuthorizationResource().policies().policies()) {
            if (permissionName.equalsIgnoreCase(policy.getName())) {
                StringBuilder policies = new StringBuilder("[");

                for (String policyName : policyNames) {
                    if (policies.length() > 1) {
                        policies.append(",");
                    }
                    policies.append("\"").append(policyName).append("\"");

                }

                policies.append("]");

                policy.getConfig().put("applyPolicies", policies.toString());
                getAuthorizationResource().policies().policy(policy.getId()).update(policy);
            }
        }
    }

    protected void createUserPolicy(String name, String... userNames) {
        UserPolicyRepresentation policy = new UserPolicyRepresentation();

        policy.setName(name);

        for (String userName : userNames) {
            policy.addUser(userName);
        }

        assertFalse(policy.getUsers().isEmpty());

        getAuthorizationResource().policies().users().create(policy);
    }

    protected interface ExceptionRunnable {
        void run() throws Exception;
    }
}
