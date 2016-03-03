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

package org.keycloak.testsuite;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Embedded;
import org.jboss.logging.Logger;
import org.keycloak.adapters.saml.tomcat.SamlAuthenticatorValve;
import org.keycloak.adapters.tomcat.KeycloakAuthenticatorValve;

public class TomcatServer {
    private Embedded server;
    private int port;
    private boolean isRunning;

    private static final Logger LOG = Logger.getLogger(TomcatServer.class);
    private static final boolean isInfo = LOG.isInfoEnabled();
    private final Host host;


    /**
     * Create a new Tomcat embedded server instance. Setup looks like:
     * <pre><Server>
     *    <Service>
     *        <Connector />
     *        <Engine&gt
     *            <Host>
     *                <Context />
     *            </Host>
     *        </Engine>
     *    </Service>
     * </Server></pre>
     * <Server> & <Service> will be created automcatically. We need to hook the remaining to an {@link Embedded} instnace
     *
     * @param port         Port number to be used for the embedded Tomcat server
     * @param appBase      Path to the Application files (for Maven based web apps, in general: <code>/src/main/</code>)
     * @throws Exception
     */
    public TomcatServer(int port, String appBase) {

        this.port = port;
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        server = new Embedded();
        server.setName("TomcatEmbeddedServer");
        server.setCatalinaBase(TomcatTest.getBaseDirectory());

        host = server.createHost("localhost", appBase);
        host.setAutoDeploy(false);

      }

    public void deploy(String contextPath, String appDir) {
        if (contextPath == null) {
            throw new IllegalArgumentException("Context path or appbase should not be null");
        }
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        StandardContext rootContext = (StandardContext) server.createContext(contextPath, appDir);
        KeycloakAuthenticatorValve valve = new KeycloakAuthenticatorValve();
        rootContext.addValve(valve);
        //rootContext.addLifecycleListener(valve);
        rootContext.setDefaultWebXml("web.xml");
        host.addChild(rootContext);
    }
    public void deploySaml(String contextPath, String appDir) {
        if (contextPath == null) {
            throw new IllegalArgumentException("Context path or appbase should not be null");
        }
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        StandardContext rootContext = (StandardContext) server.createContext(contextPath, appDir);
        SamlAuthenticatorValve valve = new SamlAuthenticatorValve();
        rootContext.addValve(valve);
        //rootContext.addLifecycleListener(valve);
        rootContext.setDefaultWebXml("web.xml");
        host.addChild(rootContext);
    }

    /**
     * Start the tomcat embedded server
     */
    public void start() throws LifecycleException {
        if (isRunning) {
            LOG.warnv("Tomcat server is already running @ port={}; ignoring the start", port);
            return;
        }

        Engine engine = server.createEngine();
        engine.setDefaultHost(host.getName());
        engine.setName("TomcatEngine");
        engine.addChild(host);

        server.addEngine(engine);

        Connector connector = server.createConnector(host.getName(), port, false);
        server.addConnector(connector);

        if (isInfo) LOG.infov("Starting the Tomcat server @ port={}", port);

        server.setAwait(true);
        server.start();
        isRunning = true;
    }

    /**
     * Stop the tomcat embedded server
     */
    public void stop() throws LifecycleException {
        if (!isRunning) {
            LOG.warnv("Tomcat server is not running @ port={}", port);
            return;
        }

        if (isInfo) LOG.info("Stopping the Tomcat server");

        server.stop();
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

}