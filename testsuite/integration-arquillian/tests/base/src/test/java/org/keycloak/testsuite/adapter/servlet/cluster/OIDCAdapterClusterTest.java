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
package org.keycloak.testsuite.adapter.servlet.cluster;


import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Retry;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractAdapterClusteredTest;
import org.keycloak.testsuite.adapter.page.SessionPortalDistributable;
import org.keycloak.testsuite.adapter.servlet.SessionServlet;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.util.DroneUtils;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_CLUSTER)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_DEPRECATED_CLUSTER)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP_CLUSTER)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6_CLUSTER)
public class OIDCAdapterClusterTest extends AbstractAdapterClusteredTest {

    @TargetsContainer(value = TARGET_CONTAINER_NODE_1)
    @Deployment(name = SessionPortalDistributable.DEPLOYMENT_NAME, managed = false)
    protected static WebArchive sessionPortalNode1() {
        return servletDeployment(SessionPortalDistributable.DEPLOYMENT_NAME, "keycloak.json", SessionServlet.class);
    }

    @TargetsContainer(value = TARGET_CONTAINER_NODE_2)
    @Deployment(name = SessionPortalDistributable.DEPLOYMENT_NAME + "_2", managed = false)
    protected static WebArchive sessionPortalNode2() {
        return servletDeployment(SessionPortalDistributable.DEPLOYMENT_NAME, "keycloak.json", SessionServlet.class);
    }

    @Page
    protected OIDCLogin loginPage;

    @Page
    protected SessionPortalDistributable sessionPortalPage;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        addAdapterTestRealms(testRealms);

        if (!"localhost".equals(ServerURLs.APP_SERVER_HOST)) {
            for (RealmRepresentation realm : testRealms) {
                Optional<ClientRepresentation> clientRepresentation = realm.getClients().stream()
                        .filter(c -> c.getClientId().equals("session-portal-distributable"))
                        .findFirst();

                clientRepresentation.ifPresent(cr -> {
                    cr.setAdminUrl(cr.getAdminUrl().replace("localhost", ServerURLs.APP_SERVER_HOST));
                    cr.setBaseUrl(cr.getBaseUrl().replace("localhost", ServerURLs.APP_SERVER_HOST));
                    cr.setRedirectUris(cr.getRedirectUris()
                            .stream()
                            .map(url -> url.replace("localhost", ServerURLs.APP_SERVER_HOST))
                            .collect(Collectors.toList())
                    );
                });
            }

        }
    }

    @Override
    protected void deploy() {
        deployer.deploy(SessionPortalDistributable.DEPLOYMENT_NAME);
        deployer.deploy(SessionPortalDistributable.DEPLOYMENT_NAME + "_2");
    }

    @Override
    protected void undeploy() {
        deployer.undeploy(SessionPortalDistributable.DEPLOYMENT_NAME);
        deployer.undeploy(SessionPortalDistributable.DEPLOYMENT_NAME + "_2");
    }

    @Before
    public void onBefore() {
        loginPage.setAuthRealm(AuthRealm.DEMO);
    }

    @Test
    public void testSuccessfulLoginAndBackchannelLogout(@ArquillianResource
                                    @OperateOnDeployment(value = SessionPortalDistributable.DEPLOYMENT_NAME) URL appUrl) {
        String proxiedUrl = getProxiedUrl(appUrl);
        driver.navigate().to(proxiedUrl);
        assertCurrentUrlStartsWith(loginPage);
        loginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(proxiedUrl);
        assertSessionCounter(NODE_2_NAME, NODE_2_URI, NODE_1_URI, proxiedUrl, 2);
        assertSessionCounter(NODE_1_NAME, NODE_1_URI, NODE_2_URI, proxiedUrl, 3);
        assertSessionCounter(NODE_2_NAME, NODE_2_URI, NODE_1_URI, proxiedUrl, 4);

        String logoutUri = OIDCLoginProtocolService.logoutUrl(authServerPage.createUriBuilder())
                .queryParam(OAuth2Constants.REDIRECT_URI, proxiedUrl).build(AuthRealm.DEMO).toString();
        driver.navigate().to(logoutUri);
        Retry.execute(() -> {
            driver.navigate().to(proxiedUrl);
            assertCurrentUrlStartsWith(loginPage);
        }, 10, 300);
    }

    @Test
    public void testSuccessfulLoginAndProgrammaticLogout(@ArquillianResource
                                                        @OperateOnDeployment(value = SessionPortalDistributable.DEPLOYMENT_NAME) URL appUrl) {
        String proxiedUrl = getProxiedUrl(appUrl);
        driver.navigate().to(proxiedUrl);
        assertCurrentUrlStartsWith(loginPage);
        loginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlEquals(proxiedUrl);
        assertSessionCounter(NODE_2_NAME, NODE_2_URI, NODE_1_URI, proxiedUrl, 2);
        assertSessionCounter(NODE_1_NAME, NODE_1_URI, NODE_2_URI, proxiedUrl, 3);
        assertSessionCounter(NODE_2_NAME, NODE_2_URI, NODE_1_URI, proxiedUrl, 4);

        String logoutUri = proxiedUrl + "/logout";
        driver.navigate().to(logoutUri);
        Retry.execute(() -> {
            driver.navigate().to(proxiedUrl);
            assertCurrentUrlStartsWith(loginPage);
        }, 10, 300);
    }

    private void waitForCacheReplication(String appUrl, int expectedCount) {
        new WebDriverWait(DroneUtils.getCurrentDriver(), 5) // Check every 500ms of 5 seconds
                .until((driver) -> {
                    driver.navigate().to(appUrl + "/donotincrease");
                    waitForPageToLoad();

                    return driver.getPageSource().contains("Counter=" + expectedCount) && driver.getPageSource().contains("CounterWrapper=" + expectedCount);
                });
    }

    private void assertSessionCounter(String hostToPointToName, URI hostToPointToUri, URI hostToRemove, String appUrl, int expectedCount) {
        updateProxy(hostToPointToName, hostToPointToUri, hostToRemove);

        // Wait for cache replication, this is necessary due to https://access.redhat.com/solutions/20861
        waitForCacheReplication(appUrl, expectedCount - 1); // Not increased yet therefore -1

        driver.navigate().to(appUrl);
        waitForPageToLoad();

        String pageSource = driver.getPageSource();
        assertThat(pageSource, containsString("Counter=" + expectedCount));
        assertThat(pageSource, containsString("CounterWrapper=" + expectedCount));
        assertThat(pageSource, containsString("Node name=" + hostToPointToName));
    }
}
