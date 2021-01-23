package org.keycloak.testsuite.arquillian.h2;

import org.h2.tools.Server;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.jboss.logging.Logger;

import java.sql.SQLException;

/**
 * Starts H2 before suite and stops it after.
 *
 * @author tkyjovsk
 */
public class H2TestEnricher {

    protected final Logger log = Logger.getLogger(this.getClass());

    boolean runH2 = Boolean.parseBoolean(System.getProperty("run.h2", "false"));
    boolean dockerDatabaseSkip = Boolean.parseBoolean(System.getProperty("docker.database.skip", "true"));

    private Server server = null;

    public void startH2(@Observes(precedence = 3) BeforeSuite event) throws SQLException {
        if (runH2 && dockerDatabaseSkip) {
            log.info("Starting H2 database.");
            server = Server.createTcpServer();
            server.start();
            log.info(String.format("URL: %s", server.getURL()));
        }
    }

    public void stopH2(@Observes(precedence = -2) AfterSuite event) {
        if (runH2 && dockerDatabaseSkip && server.isRunning(false)) {
            log.info("Stopping H2 database.");
            server.stop();
            assert !server.isRunning(false);
        }
    }

    // Ability to run H2 database available via TCP in separate process. Useful for dev purposes
    public static void main(String[] args) {
        System.setProperty("run.h2", "true");

        final H2TestEnricher h2Enricher = new H2TestEnricher();

        try {
            h2Enricher.startH2(null);
        } catch (Exception se) {
            h2Enricher.stopH2(null);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                h2Enricher.stopH2(null);
            }

        });
    }

}