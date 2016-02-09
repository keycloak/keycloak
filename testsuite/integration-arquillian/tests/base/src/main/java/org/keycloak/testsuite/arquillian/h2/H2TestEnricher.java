package org.keycloak.testsuite.arquillian.h2;

import java.sql.SQLException;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.jboss.logging.Logger;
import org.h2.tools.Server;

/**
 * Starts H2 before suite and stops it after.
 *
 * @author tkyjovsk
 */
public class H2TestEnricher {

    protected final Logger log = Logger.getLogger(this.getClass());

    boolean runH2 = Boolean.parseBoolean(System.getProperty("run.h2", "false"));

    private Server server = null;

    public void startH2(@Observes(precedence = 2) BeforeSuite event) throws SQLException {
        if (runH2) {
            log.info("Starting H2 database.");
            server = Server.createTcpServer();
            server.start();
            log.info(String.format("URL: %s", server.getURL()));
        }
    }

    public void stopH2(@Observes(precedence = -2) AfterSuite event) {
        if (runH2 && server.isRunning(false)) {
            log.info("Stopping H2 database.");
            server.stop();
            assert !server.isRunning(false);
        }
    }

}