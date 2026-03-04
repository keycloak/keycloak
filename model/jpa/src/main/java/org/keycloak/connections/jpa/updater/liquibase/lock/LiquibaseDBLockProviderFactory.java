/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.jpa.updater.liquibase.lock;

import org.keycloak.Config;
import org.keycloak.config.TransactionOptions;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.dblock.DBLockProviderFactory;

import io.quarkus.runtime.configuration.DurationConverter;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LiquibaseDBLockProviderFactory implements DBLockProviderFactory {

    private static final Logger logger = Logger.getLogger(LiquibaseDBLockProviderFactory.class);
    public static final int PROVIDER_PRIORITY = 1;

    private long lockWaitTimeoutMillis;

    public long getLockWaitTimeoutMillis() {
        return lockWaitTimeoutMillis;
    }

    @Override
    public void init(Config.Scope config) {
        var lockWaitTimeout = config.get("lockWaitTimeout", TransactionOptions.MIGRATION_TRANSACTION_TIMEOUT);
        this.lockWaitTimeoutMillis = DurationConverter.parseDuration(lockWaitTimeout).toSeconds();
        logger.debugf("Liquibase lock provider configured with lockWaitTime: %d seconds", lockWaitTimeout);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public LiquibaseDBLockProvider create(KeycloakSession session) {
        return new LiquibaseDBLockProvider(this, session);
    }

    @Override
    public void setTimeouts(long lockRecheckTimeMillis, long lockWaitTimeoutMillis) {
        this.lockWaitTimeoutMillis = lockWaitTimeoutMillis;
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "jpa";
    }

    @Override
    public int order() {
        return PROVIDER_PRIORITY;
    }
}
