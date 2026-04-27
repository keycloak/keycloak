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
package org.keycloak.testsuite.adapter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.auth.page.login.LoginActions;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.ServerURLs;

import io.undertow.Undertow;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.keycloak.testsuite.arquillian.DeploymentTargetModifier.APP_SERVER_CURRENT;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractAdapterClusteredTest extends AbstractServletsAdapterTest {

    protected static final String NODE_1_NAME = "ha-node-1";
    protected static final String NODE_2_NAME = "ha-node-2";

    // target containers will be replaced in runtime in DeploymentTargetModifier by real app-server
    public static final String TARGET_CONTAINER_NODE_1 = APP_SERVER_CURRENT + NODE_1_NAME;
    public static final String TARGET_CONTAINER_NODE_2 = APP_SERVER_CURRENT + NODE_2_NAME;

    protected static final int PORT_OFFSET_NODE_REVPROXY = NumberUtils.toInt(System.getProperty("app.server.reverse-proxy.port.offset"), -1);
    protected static final int HTTP_PORT_NODE_REVPROXY = 8080 + PORT_OFFSET_NODE_REVPROXY;
    protected static final int PORT_OFFSET_NODE_1 = NumberUtils.toInt(System.getProperty("app.server.1.port.offset"), -1);
    protected static final int HTTP_PORT_NODE_1 = 8080 + PORT_OFFSET_NODE_1;
    protected static final int PORT_OFFSET_NODE_2 = NumberUtils.toInt(System.getProperty("app.server.2.port.offset"), -1);
    protected static final int HTTP_PORT_NODE_2 = 8080 + PORT_OFFSET_NODE_2;
    protected static final URI NODE_1_URI = URI.create("http://" + ServerURLs.APP_SERVER_HOST + ":" + HTTP_PORT_NODE_1);
    protected static final URI NODE_2_URI = URI.create("http://" + ServerURLs.APP_SERVER_HOST + ":" + HTTP_PORT_NODE_2);

    protected LoadBalancingProxyClient loadBalancerToNodes;
    protected Undertow reverseProxyToNodes;

    @ArquillianResource
    protected ContainerController controller;

    @ArquillianResource
    protected Deployer deployer;

    @Page
    LoginActions loginActionsPage;

    @BeforeClass
    public static void checkPropertiesSet() {
        // Remove once https://github.com/keycloak/keycloak/issues/19834 is resolved
        ContainerAssume.assumeNotAuthServerQuarkusCluster();

        Assume.assumeThat(PORT_OFFSET_NODE_1, not(is(-1)));
        Assume.assumeThat(PORT_OFFSET_NODE_2, not(is(-1)));
        Assume.assumeThat(PORT_OFFSET_NODE_REVPROXY, not(is(-1)));
        ContainerAssume.assumeNotAppServerSSL();
    }

    @Before
    public void prepareReverseProxy() throws Exception {
        loadBalancerToNodes = new LoadBalancingProxyClient().addHost(NODE_1_URI, NODE_1_NAME).setConnectionsPerThread(10);
        int maxTime = 3600000; // 1 hour for proxy request timeout, so we can debug the backend keycloak servers
        reverseProxyToNodes = Undertow.builder()
          .addHttpListener(HTTP_PORT_NODE_REVPROXY, ServerURLs.APP_SERVER_HOST)
          .setIoThreads(2)
          .setHandler(new ProxyHandler(loadBalancerToNodes, maxTime, ResponseCodeHandler.HANDLE_404)).build();
        reverseProxyToNodes.start();
    }

    @Before
    public void startServers() throws Exception {
        prepareServerDirectories();
        
        for (ContainerInfo containerInfo : testContext.getAppServerBackendsInfo()) {
            controller.start(containerInfo.getQualifier());
        }
        deploy();
    }

    protected abstract void deploy();

    protected void prepareServerDirectories() throws Exception {
        prepareServerDirectory("standalone-cluster", "standalone-" + NODE_1_NAME);
        prepareServerDirectory("standalone-cluster", "standalone-" + NODE_2_NAME);
    }

    protected void prepareServerDirectory(String baseDir, String targetSubdirectory) throws IOException {
        Path path = Paths.get(System.getProperty("app.server.home"), targetSubdirectory);
        File targetSubdirFile = path.toFile();
        FileUtils.deleteDirectory(targetSubdirFile);
        FileUtils.forceMkdir(targetSubdirFile);
        //workaround for WFARQ-44
        FileUtils.copyDirectory(Paths.get(System.getProperty("app.server.home"), baseDir, "deployments").toFile(), new File(targetSubdirFile, "deployments"));
        FileUtils.copyDirectory(Paths.get(System.getProperty("app.server.home"), baseDir, "configuration").toFile(), new File(targetSubdirFile, "configuration"));
    }

    @After
    public void stopReverseProxy() {
        reverseProxyToNodes.stop();
    }

    @After
    public void stopServers() {
        undeploy();
        for (ContainerInfo containerInfo : testContext.getAppServerBackendsInfo()) {
            controller.stop(containerInfo.getQualifier());
        }
    }

    protected abstract void undeploy();

    protected void updateProxy(String hostToPointToName, URI hostToPointToUri, URI hostToRemove) {
        loadBalancerToNodes.removeHost(hostToRemove);
        loadBalancerToNodes.addHost(hostToPointToUri, hostToPointToName);
        log.infov("Reverse proxy will direct requests to {0}", hostToPointToUri);
    }

    protected String getProxiedUrl(URL url) {
        try {
            return new URL(url.getProtocol(), url.getHost(), HTTP_PORT_NODE_REVPROXY, url.getFile()).toString();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
