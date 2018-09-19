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

package org.keycloak.testsuite.arquillian.undertow;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletException;

import org.apache.commons.io.FileUtils;
import org.arquillian.undertow.UndertowContainerConfiguration;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.undertow.api.UndertowWebArchive;
import org.keycloak.common.util.reflections.Reflections;
import org.keycloak.testsuite.utils.undertow.UndertowDeployerHelper;
import org.keycloak.testsuite.utils.undertow.UndertowWarClassLoader;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlasta Ramik</a>
 */
public class UndertowAppServer implements DeployableContainer<UndertowAppServerConfiguration> {

    private static final Logger log = Logger.getLogger(UndertowAppServer.class);

    private UndertowContainerConfiguration configuration;
    private UndertowJaxrsServer undertow;
    Map<String, String> deployedArchivesToContextPath = new ConcurrentHashMap<>();

    @Override
    public Class<UndertowAppServerConfiguration> getConfigurationClass() {
        return UndertowAppServerConfiguration.class;
    }

    @Override
    public void setup(UndertowAppServerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws LifecycleException {
        long start = System.currentTimeMillis();

        undertow = new UndertowJaxrsServer();
        undertow.start(Undertow.builder()
            .addHttpListener(configuration.getBindHttpPort(), configuration.getBindAddress()));
        log.infof("App server started in %dms on http://%s:%d/", (System.currentTimeMillis() - start), configuration.getBindAddress(), configuration.getBindHttpPort());
    }

    @Override
    public void stop() throws LifecycleException {
        undertow.stop();
        log.info("App Server stopped.");
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.1");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        log.info("Deploying archive " + archive.getName());

        // Remove jsps
        String ioTMPDir = System.getProperty("java.io.tmpdir", ""); // My Intellij and Terminal stores tmp directory in this property
        if (!ioTMPDir.isEmpty()) {
            ioTMPDir = ioTMPDir.endsWith("/") ? ioTMPDir : ioTMPDir + "/";
            File tmpUndertowJSPDirectory = new File(ioTMPDir + "org/apache/jsp");
            if (tmpUndertowJSPDirectory.exists()) {
                try {
                    FileUtils.deleteDirectory(tmpUndertowJSPDirectory);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        DeploymentInfo di;
        if (archive instanceof UndertowWebArchive) {
            di = ((UndertowWebArchive) archive).getDeploymentInfo();
        } else if (archive instanceof WebArchive) {
            di = new UndertowDeployerHelper().getDeploymentInfo(configuration, (WebArchive)archive);
        } else {
            throw new IllegalArgumentException("UndertowContainer only supports UndertowWebArchive or WebArchive.");
        }

        ClassLoader parentCl = Thread.currentThread().getContextClassLoader();
        UndertowWarClassLoader classLoader = new UndertowWarClassLoader(parentCl, archive);
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            undertow.deploy(di);
        } finally {
            Thread.currentThread().setContextClassLoader(parentCl);
        }

        deployedArchivesToContextPath.put(archive.getName(), di.getContextPath());

        return new ProtocolMetaData().addContext(
                createHttpContextForDeploymentInfo(di));
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        log.info("Undeploying archive " + archive.getName());
        Field containerField = Reflections.findDeclaredField(UndertowJaxrsServer.class, "container");
        Reflections.setAccessible(containerField);
        ServletContainer container = (ServletContainer) Reflections.getFieldValue(containerField, undertow);

        DeploymentManager deploymentMgr = container.getDeployment(archive.getName());
        if (deploymentMgr != null) {
            DeploymentInfo deployment = deploymentMgr.getDeployment().getDeploymentInfo();

            try {
                deploymentMgr.stop();
            } catch (ServletException se) {
                throw new DeploymentException(se.getMessage(), se);
            }

            deploymentMgr.undeploy();

            Field rootField = Reflections.findDeclaredField(UndertowJaxrsServer.class, "root");
            Reflections.setAccessible(rootField);
            PathHandler root = (PathHandler) Reflections.getFieldValue(rootField, undertow);

            String path = deployedArchivesToContextPath.get(archive.getName());
            root.removePrefixPath(path);

            container.removeDeployment(deployment);
        } else {
            log.warnf("Deployment '%s' not found", archive.getName());
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    private HTTPContext createHttpContextForDeploymentInfo(DeploymentInfo deploymentInfo) {
        HTTPContext httpContext = new HTTPContext(configuration.getBindAddress(), configuration.getBindHttpPort());
        final Map<String, ServletInfo> servlets = deploymentInfo.getServlets();
        final Collection<ServletInfo> servletsInfo = servlets.values();
        for (ServletInfo servletInfo : servletsInfo) {
            httpContext.add(new Servlet(servletInfo.getName(), deploymentInfo.getContextPath()));
        }
        return httpContext;
    }
}
