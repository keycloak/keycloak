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
package org.keycloak.testsuite.adapter.example.authorization;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
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
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.keycloak.common.Profile.Feature.AUTHORIZATION;
import static org.keycloak.common.Profile.Feature.UPLOAD_SCRIPTS;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.utils.io.IOUtil.loadJson;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@EnableFeature(value = UPLOAD_SCRIPTS, skipRestart = true)
public abstract class AbstractBaseServletAuthzAdapterTest extends AbstractExampleAdapterTest {

    protected static final String REALM_NAME = "servlet-authz";
    protected static final String RESOURCE_SERVER_ID = "servlet-authz-app";

    @ArquillianResource
    private Deployer deployer;

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

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

    protected WebElement getLink(String text) {
        return this.driver.findElement(By.xpath("//a[text() = '" + text + "']"));
    }

    protected void importResourceServerSettings() throws FileNotFoundException {
        getAuthorizationResource().importSettings(loadJson(new FileInputStream(new File(TEST_APPS_HOME_DIR + "/servlet-authz-app/servlet-authz-app-authz-service.json")), ResourceServerRepresentation.class));
    }

    protected AuthorizationResource getAuthorizationResource() {
        return getClientResource(RESOURCE_SERVER_ID).authorization();
    }

    protected ClientResource getClientResource(String clientId) {
        ClientsResource clients = this.realmsResouce().realm(REALM_NAME).clients();
        ClientRepresentation resourceServer = clients.findByClientId(clientId).get(0);
        return clients.get(resourceServer.getId());
    }

    private void logOut() {
        navigateTo();
        UIUtils.clickLink(driver.findElement(By.xpath("//a[text() = 'Sign Out']")));
    }


    protected void login(String username, String password) {
        try {
            navigateTo();
            if (this.driver.getCurrentUrl().startsWith(getResourceServerUrl().toString())) {
                logOut();
                navigateTo();
            }

            this.loginPage.form().login(username, password);
        } catch (Exception cause) {
            throw new RuntimeException("Login failed", cause);
        }
    }

    protected void navigateTo() {
        this.driver.navigate().to(getResourceServerUrl() + "/");
        waitForPageToLoad();
    }

    protected void assertWasDenied() {
        waitUntilElement(By.tagName("body")).text().contains("You can not access this resource.");
    }

    protected void assertWasNotDenied() {
        waitUntilElement(By.tagName("body")).text().not().contains("You can not access this resource.");
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
        UIUtils.clickLink(getLink("Dynamic Menu"));
    }

    protected void navigateToUserPremiumPage() {
        navigateTo();
        UIUtils.clickLink(getLink("User Premium"));
    }

    protected void navigateToAdminPage() {
        navigateTo();
        UIUtils.clickLink(getLink("Administration"));
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

        Response response = getAuthorizationResource().policies().user().create(policy);
        response.close();
    }

    protected interface ExceptionRunnable {
        void run() throws Exception;
    }
}
