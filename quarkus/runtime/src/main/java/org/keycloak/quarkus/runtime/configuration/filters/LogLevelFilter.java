/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.configuration.filters;

import org.jboss.logging.Logger;
import org.keycloak.config.LoggingOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.LogLevelFormat;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Abstract Log filter used for discarding log records based on required log levels
 */
public abstract class LogLevelFilter implements Filter {
    protected static final LogLevelFormat PARENT_CONTEXT = LogLevelFormat.parseFromProperty(LoggingOptions.LOG_LEVEL);
    protected static final Level MINIMUM_LOG_LEVEL = Configuration.getOptionalValue("quarkus.log.min-level")
            .map(LogLevelFormat::toLevel)
            .orElse(Level.ALL);

    private static final Logger log = Logger.getLogger(LogLevelFilter.class);

    protected final LogLevelFormat context;
    protected final TreeMap<String, Level> categories; // for efficient categories lookup

    public LogLevelFilter(LoggingOptions.Handler handler) {
        this.context = handleContext(handler);
        this.categories = new TreeMap<>(context.getCategories());
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        var level = findLevelForLogger(record.getLoggerName(), categories);
        if (level == null) {
            final var rootLevel = context.getRootLevel();
            if (rootLevel.isPresent()) {
                level = rootLevel.get();
            }
            if (level == null) {
                level = MINIMUM_LOG_LEVEL;
            }
        }

        // The more fine-grained, the less intValue() - trace=400, error=1000
        return record.getLevel().intValue() >= level.intValue();
    }

    protected static LogLevelFormat handleContext(LoggingOptions.Handler handler) {
        var childContext = LogLevelFormat.parseFromProperty(String.format("log-%s-level", handler.name()));

        log.debugf("LogLevelContext (%s): %s\n", handler.name(), childContext);
        log.debugf("LogLevelContext (%s) - Parent: %s\n", handler.name(), PARENT_CONTEXT);

        var context = new LogLevelFormat();

        // setting the log level: handler root level -> parent root level -> default log level
        context.setRootLevel(childContext.getRootLevel().or(PARENT_CONTEXT::getRootLevel)
                .orElseGet(() -> LogLevelFormat.toLevel(LoggingOptions.DEFAULT_LOG_LEVEL.toString())));

        // set levels for categories - particular log handler configuration has precedence over the parent log handler
        context.setCategories(new HashMap<>(PARENT_CONTEXT.getCategories()));
        context.getCategories().putAll(childContext.getCategories());

        log.debugf("LogLevelContext (%s) - Merged: %s\n", handler.name(), context);
        return context;
    }

    protected static Level findLevelForLogger(String loggerName, TreeMap<String, Level> categories) {
        if (loggerName == null) return null;

        var found = categories.get(loggerName);
        if (found != null) return found;

        // find the longest match
        var entry = categories.floorEntry(loggerName);
        if (entry != null && loggerName.startsWith(entry.getKey())) { // needs to verify the `foo.bar` is not parent for `foo.barza`
            return entry.getValue();
        }
        return null;
    }
}
