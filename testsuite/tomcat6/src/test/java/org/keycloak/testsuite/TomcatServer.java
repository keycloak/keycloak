package org.keycloak.testsuite;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Embedded;
import org.keycloak.adapters.tomcat.KeycloakAuthenticatorValve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TomcatServer {
    private Embedded server;
    private int port;
    private boolean isRunning;

    private static final Logger LOG = LoggerFactory.getLogger(TomcatServer.class);
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

    /**
     * Start the tomcat embedded server
     */
    public void start() throws LifecycleException {
        if (isRunning) {
            LOG.warn("Tomcat server is already running @ port={}; ignoring the start", port);
            return;
        }

        Engine engine = server.createEngine();
        engine.setDefaultHost(host.getName());
        engine.setName("TomcatEngine");
        engine.addChild(host);

        server.addEngine(engine);

        Connector connector = server.createConnector(host.getName(), port, false);
        server.addConnector(connector);

        if (isInfo) LOG.info("Starting the Tomcat server @ port={}", port);

        server.setAwait(true);
        server.start();
        isRunning = true;
    }

    /**
     * Stop the tomcat embedded server
     */
    public void stop() throws LifecycleException {
        if (!isRunning) {
            LOG.warn("Tomcat server is not running @ port={}", port);
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