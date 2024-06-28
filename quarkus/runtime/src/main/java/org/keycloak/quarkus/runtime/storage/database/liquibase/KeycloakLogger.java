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

import java.util.logging.Level;

import liquibase.logging.core.AbstractLogger;
import org.jboss.logging.Logger;

/**
 * A {@link liquibase.logging.Logger} implementation that delegates to a JBoss {@link Logger}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class KeycloakLogger extends AbstractLogger {

    private final Logger delegate;

    public KeycloakLogger(final Class clazz) {
        super();
        this.delegate = Logger.getLogger(clazz);
    }

    @Override
    public void severe(String message) {
        this.delegate.error(message);
    }

    @Override
    public void severe(String message, Throwable e) {
        this.delegate.error(message, e);
    }

    @Override
    public void warning(String message) {
        // Ignore this warning as cascaded drops doesn't work anyway with all DBs, which we need to support
        if ("Database does not support drop with cascade".equals(message)) {
            this.delegate.debug(message);
        } else {
            this.delegate.warn(message);
        }
    }

    @Override
    public void warning(String message, Throwable e) {
        this.delegate.warn(message, e);
    }

    @Override
    public void info(String message) {
        this.delegate.debug(message);
    }

    @Override
    public void info(String message, Throwable e) {
        this.delegate.debug(message, e);
    }

    @Override
    public void debug(String message) {
        if (this.delegate.isTraceEnabled()) {
            this.delegate.trace(message);
        }
    }

    @Override
    public void debug(String message, Throwable e) {
        this.delegate.trace(message, e);
    }

    @Override
    public void log(Level level, String message, Throwable e) {
        if (level.equals(Level.OFF)) {
            return;
        } else if (level.equals(Level.SEVERE)) {
            this.delegate.error(message, e);
        } else if (level.equals(Level.WARNING)) {
            this.delegate.warn(message, e);
        } else if (level.equals(Level.INFO)) {
            this.delegate.debug(message, e);
        } else if (level.equals(Level.FINE) | level.equals(Level.FINER) | level.equals(Level.FINEST)) {
            if (this.delegate.isTraceEnabled())
                this.delegate.trace(message, e);
        }
    }
}
