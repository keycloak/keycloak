package org.keycloak.testframework.log;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import io.quarkus.bootstrap.logging.InitialConfigurator;

public class WrappedLogHandler extends Handler {

    private static final WrappedLogHandler INSTANCE = new WrappedLogHandler(InitialConfigurator.DELAYED_HANDLER);

    private final Handler wrappedHandler;

    private Logger managedKeycloakLogger;

    private WrappedLogHandler(Handler wrappedHandler) {
        this.wrappedHandler = wrappedHandler;
    }

    public static WrappedLogHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void publish(LogRecord record) {
        if (record.getLoggerName().equals(LogCategories.TESTINFO)) {
            wrappedHandler.publish(record);
        } else if (!record.getLoggerName().equals(LogCategories.MANAGED_KEYCLOAK) && isEmbeddedKeycloak(Thread.currentThread())){
            Logger managedKeycloakLogger = managedKeycloakLogger();
            if (managedKeycloakLogger.isLoggable(record.getLevel())) {
                String message = "[" + record.getLoggerName() + "] (null) " + record.getMessage();
                managedKeycloakLogger.log(record.getLevel(), message, record.getParameters());
            }
        } else {
            LogQueue logQueue = LogQueue.getInstance();
            logQueue.add(record);
            if (logQueue.shouldPublish()) {
                wrappedHandler.publish(record);
            }
        }
    }

    @Override
    public void flush() {
        wrappedHandler.flush();
    }

    @Override
    public void close() {
        wrappedHandler.close();
    }

    private boolean isEmbeddedKeycloak(Thread thread) {
        return thread.getContextClassLoader() != null && thread.getContextClassLoader().getClass().getSimpleName().equals("QuarkusClassLoader");
    }

    private Logger managedKeycloakLogger() {
        if (managedKeycloakLogger == null) {
            synchronized (this) {
                if (managedKeycloakLogger == null) {
                    managedKeycloakLogger = Logger.getLogger(LogCategories.MANAGED_KEYCLOAK);
                }
            }
        }
        return managedKeycloakLogger;
    }
}
