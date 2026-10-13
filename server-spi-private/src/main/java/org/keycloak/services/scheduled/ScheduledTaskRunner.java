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

package org.keycloak.services.scheduled;

import org.keycloak.logging.MappedDiagnosticContextUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TaskRunner;
import org.keycloak.tracing.TracingProvider;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ScheduledTaskRunner implements TaskRunner {

    private static final Logger logger = Logger.getLogger(ScheduledTaskRunner.class);

    protected final KeycloakSessionFactory sessionFactory;

    protected final ScheduledTask task;

    protected final int transactionLimit;

    public ScheduledTaskRunner(KeycloakSessionFactory sessionFactory, ScheduledTask task) {
        this(sessionFactory, task, 0);
    }

    public ScheduledTaskRunner(KeycloakSessionFactory sessionFactory, ScheduledTask task, int transactionLimit) {
        this.sessionFactory = sessionFactory;
        this.task = task;
        this.transactionLimit = transactionLimit;
    }

    @Override
    public void run() {
        // trace a tracing provider directly to avoid creating a transaction that is unnecessary and would a surplus JTA transaction element to it
        TracingProvider tracing = sessionFactory.getProviderFactory(TracingProvider.class).create(null);
        try {
            tracing.trace("ScheduledTaskRunner", task.getTaskName() + ".run", span -> {
                KeycloakModelUtils.runJobInTransaction(sessionFactory, new NamedSessionTask("Scheduled task: " + task.getTaskName()) {

                    @Override
                    public void run(KeycloakSession session) {
                        try {
                            if (transactionLimit != 0) {
                                KeycloakModelUtils.setTransactionLimit(sessionFactory, transactionLimit);
                            }

                            runTask(session);
                        } finally {
                            if (transactionLimit != 0) {
                                KeycloakModelUtils.setTransactionLimit(sessionFactory, 0);
                            }
                        }
                    }
                });
            });
        } catch (Throwable t) {
            logger.errorf(t, "Failed to run scheduled task %s", task.getTaskName());
        } finally {
            tracing.close();
            MappedDiagnosticContextUtil.clearMdc();
        }
    }

    protected void runTask(KeycloakSession session) {
        task.run(session);

        logger.debugf("Executed scheduled task %s", task.getTaskName());
    }

    @Override
    public ScheduledTask getTask() {
        return task;
    }
}
