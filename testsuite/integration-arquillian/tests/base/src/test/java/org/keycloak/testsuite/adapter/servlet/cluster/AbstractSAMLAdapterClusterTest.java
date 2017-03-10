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
package org.keycloak.testsuite.adapter.servlet.cluster;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.*;
import org.keycloak.testsuite.Retry;
import org.keycloak.testsuite.adapter.page.EmployeeServletDistributable;
import org.keycloak.testsuite.adapter.page.SAMLServlet;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.auth.page.login.*;
import org.keycloak.testsuite.page.AbstractPage;
import org.keycloak.testsuite.util.WaitUtils;

import io.undertow.Undertow;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.*;

import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.AbstractAuthTest.createUserRepresentation;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.getNearestSuperclassWithAnnotation;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractSAMLAdapterClusterTest extends AbstractServletsAdapterTest {

    protected static final String NODE_1_NAME = "ha-node-1";
    protected static final String NODE_2_NAME = "ha-node-2";

    protected final String NODE_1_SERVER_NAME = getAppServerId() + "-" + NODE_1_NAME;
    protected final String NODE_2_SERVER_NAME = getAppServerId() + "-" + NODE_2_NAME;

    protected static final int PORT_OFFSET_NODE_REVPROXY = NumberUtils.toInt(System.getProperty("app.server.reverse-proxy.port.offset"), -1);
    protected static final int HTTP_PORT_NODE_REVPROXY = 8080 + PORT_OFFSET_NODE_REVPROXY;
    protected static final int PORT_OFFSET_NODE_1 = NumberUtils.toInt(System.getProperty("app.server.1.port.offset"), -1);
    protected static final int HTTP_PORT_NODE_1 = 8080 + PORT_OFFSET_NODE_1;
    protected static final int PORT_OFFSET_NODE_2 = NumberUtils.toInt(System.getProperty("app.server.2.port.offset"), -1);
    protected static final int HTTP_PORT_NODE_2 = 8080 + PORT_OFFSET_NODE_2;
    protected static final URI NODE_1_URI = URI.create("http://localhost:" + HTTP_PORT_NODE_1);
    protected static final URI NODE_2_URI = URI.create("http://localhost:" + HTTP_PORT_NODE_2);

    @BeforeClass
    public static void checkPropertiesSet() {
        Assume.assumeThat(PORT_OFFSET_NODE_1, not(is(-1)));
        Assume.assumeThat(PORT_OFFSET_NODE_2, not(is(-1)));
        Assume.assumeThat(PORT_OFFSET_NODE_REVPROXY, not(is(-1)));
    }

    protected static void prepareServerDirectory(String targetSubdirectory) throws IOException {
        Path path = Paths.get(System.getProperty("app.server.home"), targetSubdirectory);
        File targetSubdirFile = path.toFile();
        FileUtils.deleteDirectory(targetSubdirFile);
        FileUtils.forceMkdir(targetSubdirFile);
        FileUtils.copyDirectoryToDirectory(Paths.get(System.getProperty("app.server.home"), "standalone", "deployments").toFile(), targetSubdirFile);
    }
    protected LoadBalancingProxyClient loadBalancerToNodes;
    protected Undertow reverseProxyToNodes;

    @ArquillianResource
    protected ContainerController controller;

    @ArquillianResource
    protected Deployer deployer;

    @Page
    LoginActions loginActionsPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/keycloak-saml/testsaml-behind-lb.json"));
    }

    @Before
    public void prepareReverseProxy() throws Exception {
        loadBalancerToNodes = new LoadBalancingProxyClient().addHost(NODE_1_URI, NODE_1_NAME).setConnectionsPerThread(10);
        reverseProxyToNodes = Undertow.builder().addHttpListener(HTTP_PORT_NODE_REVPROXY, "localhost").setIoThreads(2).setHandler(new ProxyHandler(loadBalancerToNodes, 5000, ResponseCodeHandler.HANDLE_404)).build();
        reverseProxyToNodes.start();
    }

    @After
    public void stopReverseProxy() {
        reverseProxyToNodes.stop();
    }

    @Before
    public void startServer() throws Exception {
        prepareServerDirectory("standalone-" + NODE_1_NAME);
        controller.start(NODE_1_SERVER_NAME);
        prepareWorkerNode(Integer.valueOf(System.getProperty("app.server.1.management.port")));
        prepareServerDirectory("standalone-" + NODE_2_NAME);
        controller.start(NODE_2_SERVER_NAME);
        prepareWorkerNode(Integer.valueOf(System.getProperty("app.server.2.management.port")));
        deployer.deploy(EmployeeServletDistributable.DEPLOYMENT_NAME);
        deployer.deploy(EmployeeServletDistributable.DEPLOYMENT_NAME + "_2");
    }

    protected abstract void prepareWorkerNode(Integer managementPort) throws Exception;

    @After
    public void stopServer() {
        controller.stop(NODE_1_SERVER_NAME);
        controller.stop(NODE_2_SERVER_NAME);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();

        testRealmSAMLPostLoginPage.setAuthRealm(DEMO);
        loginPage.setAuthRealm(DEMO);
        loginActionsPage.setAuthRealm(DEMO);
    }

    protected void testLogoutViaSessionIndex(URL employeeUrl, Consumer<EmployeeServletDistributable> logoutFunction) {
        EmployeeServletDistributable page = PageFactory.initElements(driver, EmployeeServletDistributable.class);
        page.setUrl(employeeUrl);
        page.getUriBuilder().port(HTTP_PORT_NODE_REVPROXY);

        UserRepresentation bburkeUser = createUserRepresentation("bburke", "bburke@redhat.com", "Bill", "Burke", true);
        setPasswordFor(bburkeUser, CredentialRepresentation.PASSWORD);

        assertSuccessfulLogin(page, bburkeUser, testRealmSAMLPostLoginPage, "principal=bburke");

        updateProxy(NODE_2_NAME, NODE_2_URI, NODE_1_URI);
        logoutFunction.accept(page);
        delayedCheckLoggedOut(page, loginActionsPage);

        updateProxy(NODE_1_NAME, NODE_1_URI, NODE_2_URI);
        delayedCheckLoggedOut(page, loginActionsPage);
    }

    @Test
    public void testBackchannelLogout(@ArquillianResource
      @OperateOnDeployment(value = EmployeeServletDistributable.DEPLOYMENT_NAME) URL employeeUrl) throws Exception {
        testLogoutViaSessionIndex(employeeUrl, (EmployeeServletDistributable page) -> {
            RealmResource demoRealm = adminClient.realm(DEMO);
            String bburkeId = ApiUtil.findUserByUsername(demoRealm, "bburke").getId();
            demoRealm.users().get(bburkeId).logout();
            log.infov("Logged out via admin console");
        });
    }

    @Test
    public void testFrontchannelLogout(@ArquillianResource
      @OperateOnDeployment(value = EmployeeServletDistributable.DEPLOYMENT_NAME) URL employeeUrl) throws Exception {
        testLogoutViaSessionIndex(employeeUrl, (EmployeeServletDistributable page) -> {
            page.logout();
            log.infov("Logged out via application");
        });
    }

    protected void updateProxy(String hostToPointToName, URI hostToPointToUri, URI hostToRemove) {
        loadBalancerToNodes.removeHost(hostToRemove);
        loadBalancerToNodes.addHost(hostToPointToUri, hostToPointToName);
        log.infov("Reverse proxy will direct requests to {0}", hostToPointToUri);
    }

    protected void assertSuccessfulLogin(SAMLServlet page, UserRepresentation user, Login loginPage, String expectedString) {
        page.navigateTo();
        assertCurrentUrlStartsWith(loginPage);
        loginPage.form().login(user);
        WebDriverWait wait = new WebDriverWait(driver, WaitUtils.PAGELOAD_TIMEOUT_MILLIS / 1000);
        wait.until((WebDriver d) -> d.getPageSource().contains(expectedString));
    }

    protected void delayedCheckLoggedOut(AbstractPage page, AuthRealm loginPage) {
        Retry.execute(() -> {
            try {
                checkLoggedOut(page, loginPage);
            } catch (AssertionError | TimeoutException ex) {
                driver.navigate().refresh();
                log.debug("[Retriable] Timed out waiting for login page");
                throw new RuntimeException(ex);
            }
        }, 10, 100);
    }

    protected void checkLoggedOut(AbstractPage page, AuthRealm loginPage) {
        page.navigateTo();
        WaitUtils.waitForPageToLoad(driver);
        assertCurrentUrlStartsWith(loginPage);
    }

    private String getAppServerId() {
        Class<?> annotatedClass = getNearestSuperclassWithAnnotation(this.getClass(), AppServerContainer.class);

        return (annotatedClass == null ? "<cannot-find-@AppServerContainer>"
                : annotatedClass.getAnnotation(AppServerContainer.class).value());
    }

}
