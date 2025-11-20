package org.keycloak.testframework;

import java.util.logging.Filter;
import java.util.logging.Handler;

import org.keycloak.testframework.config.Config;

import io.quarkus.runtime.logging.LoggingSetupRecorder;
import io.smallrye.config.SmallRyeConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;
import org.jboss.logmanager.LogManager;
import org.junit.jupiter.api.extension.ExtensionContext;

public class LogHandler {

    private static final Logger LOGGER = Logger.getLogger("testinfo");
    private final boolean logFilterEnabled;

    public LogHandler() {
        logFilterEnabled = Config.get("kc.test.log.filter", false, Boolean.class);

        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        System.setProperty("junit.quarkus.enable-basic-logging", "false");
        System.setProperty("log4j2.disable.jmx", "true");

        initializeQuarkusLogging();
    }

    private static void initializeQuarkusLogging() {
        // We do not care about Config that was created by Quarkus' TestConfigProviderResolver.
        // Alternatively, a Customizer could be used so we could keep the Config created by Quarkus but this is not Quarkus tests,
        // relying on Quarkus' Config is not necessary and might be fragile.
        SmallRyeConfigProviderResolver configProviderResolver = (SmallRyeConfigProviderResolver) ConfigProviderResolver.instance();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        configProviderResolver.releaseConfig(cl);
        ConfigProviderResolver.instance().registerConfig(Config.getConfig(), cl);
        LoggingSetupRecorder.handleFailedStart();
    }

    public void beforeAll(ExtensionContext context) {
        logDivider(Logger.Level.INFO);
        logTestClassStatus(context, Status.RUNNING, Logger.Level.INFO);
    }

    public void beforeEachStarting(ExtensionContext context) {
        logTestMethodStatus(context, Status.INIT, Logger.Level.DEBUG);
    }

    public void beforeEachCompleted(ExtensionContext context) {
        logTestMethodStatus(context, Status.RUNNING, Logger.Level.DEBUG);
        initLogFilter();
    }

    public void afterAll(ExtensionContext context) {
        logTestClassStatus(context, Status.FINISHED, Logger.Level.DEBUG);
    }

    public void afterEachStarting(ExtensionContext context) {
        logTestMethodStatus(context, Status.CLEANUP, Logger.Level.DEBUG);
    }

    public void afterEachCompleted(ExtensionContext context) {
    }

    public void testSuccessful(ExtensionContext context) {
        clearLogFilter(false);
        logTestMethodStatus(context, Status.SUCCESS, Logger.Level.DEBUG);
    }

    public void testFailed(ExtensionContext context) {
        clearLogFilter(true);
        logTestMethodStatus(context, Status.FAILED, Logger.Level.ERROR);
    }

    public void testAborted(ExtensionContext context) {
        clearLogFilter(true);
        logTestMethodStatus(context, Status.ABORTED, Logger.Level.ERROR);
    }

    public void testDisabled(ExtensionContext context) {
        clearLogFilter(false);
        logTestMethodStatus(context, Status.DISABLED, Logger.Level.DEBUG);
    }

    private void logDivider(Logger.Level level) {
        LOGGER.log(level, "----------------------------------------------------------------");
    }

    private void logTestClassStatus(ExtensionContext context, Status status, Logger.Level level) {
        LOGGER.logv(level, "{0} - {1}", status.getLogString(), context.getRequiredTestClass().getName());
    }

    private void logTestMethodStatus(ExtensionContext context, Status status, Logger.Level level) {
        LOGGER.logv(level, "{0} - {1} / {2}", status.getLogString(), context.getRequiredTestClass().getName(), context.getRequiredTestMethod().getName());
    }

    private void initLogFilter() {
        if (!logFilterEnabled) {
            return;
        }

        for (Handler handler : LogManager.getLogManager().getLogger("").getHandlers()) {
            handler.setFilter(new LogFilter());
        }
    }

    private void clearLogFilter(boolean forwardLogs) {
        if (!logFilterEnabled) {
            return;
        }

        for (Handler handler : LogManager.getLogManager().getLogger("").getHandlers()) {
            Filter filter = handler.getFilter();
            handler.setFilter(null);
            if (filter instanceof LogFilter) {
                ((LogFilter) filter).clear(forwardLogs);
            }
        }
    }

    private enum Status {
        INIT,
        CLEANUP,
        RUNNING,
        FINISHED,
        SUCCESS,
        ABORTED,
        DISABLED,
        FAILED;

        private final String logString;

        Status() {
            this.logString = String.format("%1$10s", this);
        }

        private String getLogString() {
            return logString;
        }
    }

}
