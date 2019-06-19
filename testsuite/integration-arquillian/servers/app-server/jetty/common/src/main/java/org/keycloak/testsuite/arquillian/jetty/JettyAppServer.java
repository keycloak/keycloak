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

package org.keycloak.testsuite.arquillian.jetty;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.AppLifeCycle;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.ClassAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.keycloak.adapters.jetty.KeycloakJettyAuthenticator;
import org.keycloak.adapters.saml.jetty.KeycloakSamlAuthenticator;
import org.keycloak.testsuite.arquillian.jetty.saml.util.RestSamlApplicationConfig;
import org.keycloak.testsuite.utils.tls.TLSUtils;

import javax.ws.rs.Path;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JettyAppServer implements DeployableContainer<JettyAppServerConfiguration> {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private JettyAppServerConfiguration configuration;
    private JettyAppProvider appProvider;
    private DeploymentManager deployer;
    private Server server;

    Map<String, KeycloakAdapterApp> deployedApps = new ConcurrentHashMap<>();

    @Override
    public Class<JettyAppServerConfiguration> getConfigurationClass() {
        return JettyAppServerConfiguration.class;
    }

    @Override
    public void setup(JettyAppServerConfiguration configuration) {
        this.configuration = configuration;
        appProvider = new JettyAppProvider(this.configuration);
    }

    @Override
    public void start() throws LifecycleException {
        if (configuration == null) {
            log.warn("Starting Jetty with default setup.");
            setup(new JettyAppServerConfiguration());
        }

        long start = System.currentTimeMillis();

        server = new Server(configuration.getBindHttpPort());
        setupSSL();

        ContextHandlerCollection contexts = new ContextHandlerCollection();

        deployer = new DeploymentManager();
        deployer.setContexts(contexts);
        deployer.addAppProvider(appProvider);
        server.addBean(deployer);

        HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(contexts);
        handlers.addHandler(new DefaultHandler());
        server.setHandler(handlers);

        try {
            server.start();
        } catch (Exception e) {
            throw new LifecycleException("Unable to start Jetty", e);
        }
        log.infof("App server started in %dms on http://%s:%d/", (System.currentTimeMillis() - start), configuration.getBindAddress(), configuration.getBindHttpPort());
    }

    private void setupSSL() {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setSslContext(TLSUtils.initializeTLS());
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(configuration.getBindHttpPort());
        HttpConfiguration https = new HttpConfiguration();
        ServerConnector sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(https));
        sslConnector.setPort(configuration.getBindHttpsPort());
        server.setConnectors(new Connector[] { connector, sslConnector });
    }

    @Override
    public void stop() throws LifecycleException {
        try {
            server.stop();
        } catch (Exception e) {
            throw new LifecycleException("Unable to stop Jetty", e);
        }
        log.info("App Server stopped.");
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.1");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        log.info("Deploying archive " + archive.getName());

        if (!(archive instanceof WebArchive)) {
            throw new IllegalArgumentException("JettyContainer only supports WebArchives.");
        }

        WebArchive webArchive = (WebArchive) archive;

        try {
            KeycloakAdapterApp app = appProvider.createApp(webArchive);
            WebAppContext webAppContext = (WebAppContext) app.getContextHandler();


            addAdditionalConfigurations(webAppContext);

            setContextRoot(webArchive, app, webAppContext);

            if (app.usesOIDCAuthenticator()) {
                addOIDCAuthenticator(webAppContext);
            }

            if (app.usesSAMLAuthenticator()) {
                addSAMLAuthenticator(webAppContext);
            }

            if (app.usesJaxrs()) {
                addRestEasyServlet(webArchive, webAppContext);
            }

            setEmbeddedClassloaderForDeployment(webAppContext);

            deployer.addApp(app);
            deployer.requestAppGoal(app, AppLifeCycle.STARTED);

            deployedApps.put(archive.getId(), app);

            HTTPContext httpContext = new HTTPContext(configuration.getBindAddress(), configuration.getBindHttpPort());
            ServletHandler servletHandler = webAppContext.getServletHandler();

            for (ServletHolder servlet : servletHandler.getServlets()) {
                log.debugf("Servlet context mapping: %s => %s", servlet.getName(), servlet.getContextPath());
                httpContext.add(new Servlet(servlet.getName(), servlet.getContextPath()));
            }

            if (log.isInfoEnabled()) {
                for (ServletMapping mapping : server.getChildHandlerByClass(ServletHandler.class).getServletMappings()) {
                    log.debugf("Servlet mapping: %s => %s", mapping.getServletName(), Arrays.toString(mapping.getPathSpecs()));
                }
            }

            return new ProtocolMetaData().addContext(httpContext);
        } catch (Exception e) {
            throw new DeploymentException("Unable to deploy archive", e);
        }
    }

    private void addAdditionalConfigurations(WebAppContext webAppContext) {
        List<String> configurations = new ArrayList<>();
        configurations.add(AnnotationConfiguration.class.getName());
        //due to Jetty incompatibility between 8 and 9, we need to use reflections here
        try {
            Method m = webAppContext.getClass().getDeclaredMethod("getDefaultConfigurationClasses", null);
            configurations.addAll(Arrays.asList((String[]) m.invoke(webAppContext)));
        } catch (Exception e) {
            throw new IllegalStateException("Critical Jetty incompatibility detected", e);
        }

        webAppContext.setConfigurationClasses(configurations.toArray(new String[0]));
    }

    private void setContextRoot(WebArchive archive, KeycloakAdapterApp app, WebAppContext webAppContext) {
        if ("ROOT.war".equals(archive.getName())) {
            webAppContext.setContextPath("/");
        } else {
            webAppContext.setContextPath("/" + app.getApplicationName());
        }
    }

    private void addRestEasyServlet(WebArchive archive, WebAppContext webAppContext) {
        log.debug("Starting Resteasy deployment");
        boolean addServlet = true;
        ServletHolder resteasyServlet = new ServletHolder("javax.ws.rs.core.Application", new HttpServlet30Dispatcher());

        String jaxrsApplication = getJaxRsApplication(archive);
        Set<Class<?>> pathAnnotatedClasses = getPathAnnotatedClasses(archive);

        if (jaxrsApplication != null) {
            log.debug("App has an Application.class: " + jaxrsApplication);
            resteasyServlet.setInitParameter("javax.ws.rs.Application", jaxrsApplication);
        } else if (!pathAnnotatedClasses.isEmpty()) {
            log.debug("App has @Path annotated classes: " + pathAnnotatedClasses);
            ResteasyDeployment deployment = new ResteasyDeployment();
            deployment.setApplication(new RestSamlApplicationConfig(pathAnnotatedClasses));
            webAppContext.setAttribute(ResteasyDeployment.class.getName(), deployment);
        } else {
            log.debug("An application doesn't have Application.class, nor @Path annotated classes. Skipping Resteasy initialization.");
            addServlet = false;
        }

        if (addServlet) {
            // this should be /* in general. However Jetty 9.2 (this is bug specific to this version),
            // can not merge two instances of javax.ws.rs.Application together (one from web.xml
            // and the other one added here). In 9.1 and 9.4 this works fine.
            // Once we stop supporting 9.2, this should replaced with /* and this comment should be removed.
            webAppContext.addServlet(resteasyServlet, "/");
        }
        log.debug("Finished Resteasy deployment");
    }

    private String getJaxRsApplication(WebArchive archive) {
        return archive.getContent(archivePath ->
                archivePath.get().startsWith("/WEB-INF/classes/") && archivePath.get().endsWith("Application.class"))
                .values().stream().findFirst().map(node -> node.getPath().get()).orElse(null);
    }

    private void addSAMLAuthenticator(WebAppContext webAppContext) {
        webAppContext.getSecurityHandler().setAuthenticator(new KeycloakSamlAuthenticator());
    }

    private void addOIDCAuthenticator(WebAppContext webAppContext) {
        webAppContext.getSecurityHandler().setAuthenticator(new KeycloakJettyAuthenticator());
    }

    private void setEmbeddedClassloaderForDeployment(WebAppContext webAppContext) {
        ClassLoader parentCl = Thread.currentThread().getContextClassLoader();
        webAppContext.setClassLoader(parentCl);
    }

    private Set<Class<?>> getPathAnnotatedClasses(WebArchive webArchive) {
        return webArchive.getContent(archivePath ->
                archivePath.get().startsWith("/WEB-INF/classes/") &&
                        archivePath.get().endsWith(".class")
        ).values().stream()
                .filter(node -> node.getAsset() instanceof ClassAsset)
                .map(node -> ((ClassAsset)node.getAsset()).getSource())
                .filter(clazz -> clazz.isAnnotationPresent(Path.class))
                .collect(Collectors.toSet());
    }

    @Override
    public void undeploy(Archive<?> archive) {
        log.info("Undeploying archive " + archive.getName());

        App app = deployedApps.get(archive.getId());
        if (app != null) {
            deployer.requestAppGoal(app, AppLifeCycle.UNDEPLOYED);
        } else {
            log.warnf("Deployment '%s' (name=%s) not found", archive.getId(), archive.getName());
        }
    }

    @Override
    public void deploy(Descriptor descriptor) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(Descriptor descriptor) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getJettyVersion() {
        return Server.getVersion();
    }

    /*
     * This is a non-public method that should not be used. Only for testing.
     */
    protected Server getServer() {
        return server;
    }
}
