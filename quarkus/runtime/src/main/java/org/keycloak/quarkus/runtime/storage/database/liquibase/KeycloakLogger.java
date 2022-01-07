/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.storage.database.liquibase;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.logging.LogLevel;
import liquibase.logging.Logger;

public class KeycloakLogger implements Logger {

    private static final org.jboss.logging.Logger logger = org.jboss.logging.Logger.getLogger(KeycloakLogger.class);
    
    @Override
    public void setName(String name) {
    }

    @Override
    public void setLogLevel(String logLevel, String logFile) {
    }

    @Override
    public void severe(String message) {
        logger.error(message);
    }

    @Override
    public void severe(String message, Throwable e) {
        logger.error(message, e);
    }

    @Override
    public void warning(String message) {
        // Ignore this warning as cascaded drops doesn't work anyway with all DBs, which we need to support
        if ("Database does not support drop with cascade".equals(message)) {
            logger.debug(message);
        } else {
            logger.warn(message);
        }
    }

    @Override
    public void warning(String message, Throwable e) {
        logger.warn(message, e);
    }

    @Override
    public void info(String message) {
        logger.debug(message);
    }

    @Override
    public void info(String message, Throwable e) {
        logger.debug(message, e);
    }

    @Override
    public void debug(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(message);
        }
    }

    @Override
    public LogLevel getLogLevel() {
        if (logger.isTraceEnabled()) {
            return LogLevel.DEBUG;
        } else if (logger.isDebugEnabled()) {
            return LogLevel.INFO;
        } else {
            return LogLevel.WARNING;
        }
    }

    @Override
    public void setLogLevel(String level) {
    }

    @Override
    public void setLogLevel(LogLevel level) {
    }

    @Override
    public void debug(String message, Throwable e) {
        logger.trace(message, e);
    }

    @Override
    public void setChangeLog(DatabaseChangeLog databaseChangeLog) {
    }

    @Override
    public void setChangeSet(ChangeSet changeSet) {
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void closeLogFile() {
    }

}
