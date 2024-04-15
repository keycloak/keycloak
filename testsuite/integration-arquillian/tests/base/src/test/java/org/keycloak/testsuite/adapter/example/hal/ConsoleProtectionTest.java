/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.adapter.example.hal;

import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractAdapterTest;
import org.keycloak.testsuite.arquillian.AppServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.WebDriverUtils;
import org.keycloak.testsuite.util.TestAppHelper;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.pages.AppServerWelcomePage;
import org.keycloak.testsuite.webdriver.JSBrowser;
import org.openqa.selenium.WebDriver;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
public class ConsoleProtectionTest extends AbstractAdapterTest {

    @Page
    protected LoginPage loginPage;

    @Page
    protected AppPage appPage;

    private final JSBrowser jsBrowser = new JSBrowser();

    protected WebDriver jsDriver;

    @Page
    protected AppServerWelcomePage appServerWelcomePage;

    @BeforeClass
    public void setupLocalDriver() {
        this.jsBrowser.startBrowser();
        this.jsDriver = this.jsBrowser.getBrowser();
    }

    @Before
    public void beforeConsoleProtectionTest() throws IOException, OperationException {
        Assume.assumeTrue("This testClass doesn't work with phantomjs", !"phantomjs".equals(System.getProperty("js.browser")));

        try (OnlineManagementClient clientWorkerNodeClient = AppServerTestEnricher.getManagementClient()) {

            Operations operations = new Operations(clientWorkerNodeClient);

            Assume.assumeTrue(operations.exists(Address.subsystem("elytron").and("security-domain", "KeycloakDomain")));

            // Create a realm for both wildfly console and mgmt interface
            clientWorkerNodeClient.execute("/subsystem=keycloak/realm=jboss-infra:add(auth-server-url=" + getAuthServerContextRoot() + "/auth,realm-public-key=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB)");

            // Create a secure-deployment in order to protect mgmt interface
            clientWorkerNodeClient.execute("/subsystem=keycloak/secure-deployment=wildfly-management:add(realm=jboss-infra,resource=wildfly-management,principal-attribute=preferred_username,bearer-only=true,ssl-required=EXTERNAL)");

            // Protect HTTP mgmt interface with Keycloak adapter
            clientWorkerNodeClient.execute("/core-service=management/management-interface=http-interface:undefine-attribute(name=security-realm)");
            clientWorkerNodeClient.execute("/subsystem=elytron/http-authentication-factory=keycloak-mgmt-http-authentication:add(security-domain=KeycloakDomain,http-server-mechanism-factory=wildfly-management,mechanism-configurations=[{mechanism-name=KEYCLOAK,mechanism-realm-configurations=[{realm-name=KeycloakOIDCRealm,realm-mapper=keycloak-oidc-realm-mapper}]}])");
            clientWorkerNodeClient.execute("/core-service=management/management-interface=http-interface:write-attribute(name=http-authentication-factory,value=keycloak-mgmt-http-authentication)");
            clientWorkerNodeClient.execute("/core-service=management/management-interface=http-interface:write-attribute(name=http-upgrade, value={enabled=true, sasl-authentication-factory=management-sasl-authentication})");

            // Enable RBAC where roles are obtained from the identity
            clientWorkerNodeClient.execute("/core-service=management/access=authorization:write-attribute(name=provider,value=rbac)");
            clientWorkerNodeClient.execute("/core-service=management/access=authorization:write-attribute(name=use-identity-roles,value=true)");

            // Create a secure-server in order to publish the wildfly console configuration via mgmt interface
            clientWorkerNodeClient.execute("/subsystem=keycloak/secure-server=wildfly-console:add(realm=jboss-infra,resource=wildfly-console,public-client=true)");

            log.debug("Reloading the server");
            new Administration(clientWorkerNodeClient).reload();
            log.debug("Reloaded");
        } catch (CliException | IOException | InterruptedException | TimeoutException cause) {
            throw new RuntimeException("Failed to configure app server", cause);
        }

        WebDriverUtils.addWebDriver(jsDriver);
        log.debug("Added jsDriver");
    }

    @AfterClass
    public void localDriverCleanup() {
        this.jsBrowser.stopBrowser();
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/wildfly-integration/wildfly-management-realm.json"));
    }

    @Test
    public void testUserCanAccessAccountService() {
        TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);
        testAppHelper.login("admin", "admin");
        appPage.assertCurrent();

        appServerWelcomePage.navigateToAccessControl();
        appServerWelcomePage.navigateManageProfile();
    }
}
