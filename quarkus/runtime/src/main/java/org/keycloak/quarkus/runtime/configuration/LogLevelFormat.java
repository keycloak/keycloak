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

package org.keycloak.quarkus.runtime.configuration;

import org.jboss.logmanager.LogContext;
import org.keycloak.config.Option;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.utils.StringUtil;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Used for managing log levels specified in 'log-*-level' properties
 * It allows to parse the log level format {@code <rootLevel>,<category1>:<level>,<category2>:<level>}
 * </p>
 * f.e {@code --log-level=info,org.keycloak:debug,org.keycloak.events:trace}
 */
public class LogLevelFormat {
    public static final LogLevelFormat EMPTY = new LogLevelFormat(null, Collections.emptyMap());

    private Level rootLevel;
    private Map<String, Level> categories;

    public LogLevelFormat() {
        this.categories = new ConcurrentHashMap<>();
    }

    public LogLevelFormat(Level level, Map<String, Level> categories) {
        this.rootLevel = level;
        this.categories = categories;
    }

    public Optional<Level> getRootLevel() {
        return Optional.ofNullable(rootLevel);
    }

    public void setRootLevel(Level level) {
        this.rootLevel = level;
    }

    public Map<String, Level> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, Level> categories) {
        this.categories = categories;
    }

    @Override
    public String toString() {
        return String.format("Level: %s. Categories: %s", getRootLevel().map(Level::getName).orElse("unset"), categories);
    }

    public record CategoryLevel(String category, Level level) {
    }

    /**
     * Parse the log-level format based on the Keycloak option
     * It fetches the option from the configuration and then parse its value
     */
    public static LogLevelFormat parseFromProperty(Option<List<String>> logLevelOption) {
        return parseFromProperty(logLevelOption.getKey());
    }

    /**
     * Parse the log-level format based on the Keycloak property starting with 'kc.' prefix
     * It fetches the option from the configuration and then parse its value
     */
    public static LogLevelFormat parseFromProperty(String logLevelProperty) {
        if (logLevelProperty == null) return LogLevelFormat.EMPTY;

        var logLevel = Configuration.getOptionalKcValue(logLevelProperty).filter(StringUtil::isNotBlank);
        if (logLevel.isEmpty()) return LogLevelFormat.EMPTY;

        return parseValue(logLevel.get());
    }

    /**
     * Parse the log-level format of the provided value
     */
    public static LogLevelFormat parseValue(String value) {
        if (StringUtil.isBlank(value)) return EMPTY;

        final LogLevelFormat context = new LogLevelFormat();

        for (String level : value.split(",")) {
            var categoryLevel = validateLogLevel(level);
            if (categoryLevel.category == null) {
                context.setRootLevel(categoryLevel.level);
            } else {
                context.getCategories().put(categoryLevel.category, categoryLevel.level);
            }
        }
        return context;
    }

    /**
     * Validate log-level format
     *
     * @throws PropertyException when the format is invalid
     */
    public static CategoryLevel validateLogLevel(String level) {
        String[] parts = level.split(":");
        String category = null;
        String categoryLevel;

        if (parts.length == 1) {
            categoryLevel = parts[0];
        } else if (parts.length == 2) {
            category = parts[0];
            categoryLevel = parts[1];
        } else {
            throw new PropertyException(Messages.invalidLogCategoryFormat(level));
        }

        try {
            Level levelType = toLevel(categoryLevel);
            return new CategoryLevel(category, levelType);
        } catch (IllegalArgumentException iae) {
            throw new PropertyException(Messages.invalidLogCategoryFormat(level));
        }
    }

    /**
     * Convert level name to java.util.logging.Level object
     */
    public static Level toLevel(String levelName) throws IllegalArgumentException {
        return LogContext.getLogContext().getLevelForName(levelName.toUpperCase(Locale.ROOT));
    }
}
