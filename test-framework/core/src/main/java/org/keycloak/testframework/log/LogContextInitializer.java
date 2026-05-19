package org.keycloak.testframework.log;

import java.util.logging.Handler;
import java.util.logging.Level;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.runtime.logging.LogCleanupFilter;

/**
 * Quarkus installs {@link LogCleanupFilter} during bootstrap overriding installed filters; as a work-around this uses
 * a wrapped Handler instead.
 */
public class LogContextInitializer  implements org.jboss.logmanager.LogContextInitializer {

    private static final InitialConfigurator INITIAL_CONFIGURATOR = new InitialConfigurator();

    @Override
    public Level getMinimumLevel(final String loggerName) {
        return INITIAL_CONFIGURATOR.getMinimumLevel(loggerName);
    }

    @Override
    public Level getInitialLevel(final String loggerName) {
        return INITIAL_CONFIGURATOR.getInitialLevel(loggerName);
    }

    @Override
    public Handler[] getInitialHandlers(final String loggerName) {
        if (loggerName.isEmpty()) {
            return new Handler[] { WrappedLogHandler.getInstance() };
        } else {
            return NO_HANDLERS;
        }
    }

    @Override
    public boolean useStrongReferences() {
        return INITIAL_CONFIGURATOR.useStrongReferences();
    }

}
