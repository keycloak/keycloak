/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.logging;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.config.LoggingOptions;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.logging.LoggingFilter;
import org.infinispan.commons.jdkspecific.ThreadCreator;
import org.jboss.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.FileHandler;
import org.jboss.logmanager.handlers.SyslogHandler;

/**
 * @author Alexander Schwartz
 */
public abstract class KeycloakLogFilter implements Filter {

    private static final Logger logger = Logger.getLogger(KeycloakLogFilter.class);

    // avoid logging ISPN000312 for sessions, offlineSessions, clientSessions and offlineClientSessions caches only.
    private static final Pattern ISPN000312_PATTERN = Pattern.compile(
            "^\\[Context=(" + String.join("|", InfinispanConnectionProvider.USER_SESSION_CACHE_NAME, InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME, InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME, InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME) + ")] ISPN000312: .*");

    // Use this thread pool to asynchronously log from virtual threads, which could otherwise be pinned and lead to deadlocks.
    // A single thread ensures that all log entries appear in the correct order.
    private final ExecutorService executor;
    // Original handler for this these logs
    private Handler handler;

    public KeycloakLogFilter() {
        // The class ThreadCreator needs to be called and initialized here as when we do this in isLoggable() we'll have a recursive logging
        if (ThreadCreator.useVirtualThreads() && isHandlerEnabled() && !isAsyncLoggingEnabled()) {
            executor = Executors.newSingleThreadExecutor();
        } else {
            executor = null;
        }
    }

    protected abstract Class<? extends Handler> getHandlerClass();

    /**
     * Whether the logging is enabled for specific handler
     */
    public abstract boolean isHandlerEnabled();

    /**
     * Whether the async logging is enabled for specific handler
     */
    public abstract boolean isAsyncLoggingEnabled();

    @Override
    public boolean isLoggable(LogRecord record) {
        // The ARJUNA012125 messages are logged and then thrown.
        // As those messages might later be caught and handled, this is an antipattern so we prevent logging them.
        // https://narayana.zulipchat.com/#narrow/channel/323714-users/topic/Message.20.22ARJUNA012125.22.20implements.20log-and-throw.20antipattern
        if (Objects.equals(record.getLevel(), Level.WARNING) && record.getLoggerName().equals("com.arjuna.ats.arjuna") && record.getMessage().startsWith("ARJUNA012125:")) {
            return false;
        }

        if (MultiSiteUtils.isPersistentSessionsEnabled()) {
            // Suppress messages for ISPN000312 as there shouldn't be a warning as this is expected as user and client sessions have only a single owner.
            // https://github.com/keycloak/keycloak/issues/39816
            if (Objects.equals(record.getLevel(), Level.WARNING) && record.getLoggerName().equals("org.infinispan.CLUSTER") && ISPN000312_PATTERN.matcher(record.getMessage()).matches()) {
                return false;
            }
        }

        if (executor != null && ThreadCreator.isVirtual(Thread.currentThread())) {
            executor.submit(new RecordLogger(ExtLogRecord.wrap(record), this));
            return false;
        }

        return true;
    }

    private Handler getHandler() {
        if (handler == null) {
            // This needs a lazy initialization the logging is not yet fully initialized when instantiating the filter if the image is pre-built.
            synchronized (this) {
                if (handler == null) {
                    Class<? extends Handler> handlerClass = getHandlerClass();
                    // Retrieving the original log handler. None might be found during build phase,
                    // but then it should fail with an NPE when virtual threads are involved
                    handler = Arrays.stream(InitialConfigurator.DELAYED_HANDLER.getHandlers()).filter(
                            h -> handlerClass.isAssignableFrom(h.getClass())
                    ).findFirst().orElse(null);
                }
                if (handler == null) {
                    executor.submit(() -> logger.error("Can't find handler for " + getHandlerClass()));
                }
            }
        }
        return handler;
    }

    public record RecordLogger(LogRecord record, KeycloakLogFilter filter) implements Runnable {
        @Override
        public void run() {
            Handler handler = filter.getHandler();
            if (handler != null) {
                handler.publish(record);
            }
        }
    }

    @LoggingFilter(name = "keycloak-filter-console")
    private static final class KeycloakConsoleLogFilter extends KeycloakLogFilter {
        @Override
        protected Class<? extends Handler> getHandlerClass() {
            return ConsoleHandler.class;
        }

        @Override
        public boolean isHandlerEnabled() {
            return Configuration.isTrue(LoggingOptions.LOG_CONSOLE_ENABLED);
        }

        @Override
        public boolean isAsyncLoggingEnabled() {
            return Configuration.isTrue(LoggingOptions.LOG_CONSOLE_ASYNC);
        }
    }

    @LoggingFilter(name = "keycloak-filter-file")
    private static final class KeycloakFileLogFilter extends KeycloakLogFilter {
        @Override
        protected Class<? extends Handler> getHandlerClass() {
            return FileHandler.class;
        }

        @Override
        public boolean isHandlerEnabled() {
            return Configuration.isTrue(LoggingOptions.LOG_FILE_ENABLED);
        }

        @Override
        public boolean isAsyncLoggingEnabled() {
            return Configuration.isTrue(LoggingOptions.LOG_FILE_ASYNC);
        }
    }

    @LoggingFilter(name = "keycloak-filter-syslog")
    private static final class KeycloakSyslogLogFilter extends KeycloakLogFilter {
        protected Class<? extends Handler> getHandlerClass() {
            return SyslogHandler.class;
        }

        @Override
        public boolean isHandlerEnabled() {
            return Configuration.isTrue(LoggingOptions.LOG_SYSLOG_ENABLED);
        }

        @Override
        public boolean isAsyncLoggingEnabled() {
            return Configuration.isTrue(LoggingOptions.LOG_SYSLOG_ASYNC);
        }
    }
}
