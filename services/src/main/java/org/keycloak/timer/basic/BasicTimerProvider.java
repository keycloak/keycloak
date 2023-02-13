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

package org.keycloak.timer.basic;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class BasicTimerProvider implements TimerProvider {

    private static final Logger logger = Logger.getLogger(BasicTimerProvider.class);

    private final KeycloakSession session;
    private final Timer timer;
    private final int transactionTimeout;
    private final BasicTimerProviderFactory factory;

    public BasicTimerProvider(KeycloakSession session, Timer timer, int transactionTimeout, BasicTimerProviderFactory factory) {
        this.session = session;
        this.timer = timer;
        this.transactionTimeout = transactionTimeout;
        this.factory = factory;
    }

    @Override
    public void schedule(final Runnable runnable, final long intervalMillis, String taskName) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        };

        TimerTaskContextImpl taskContext = new TimerTaskContextImpl(runnable, task, intervalMillis);
        TimerTaskContextImpl existingTask = factory.putTask(taskName, taskContext);
        if (existingTask != null) {
            logger.debugf("Existing timer task '%s' found. Cancelling it", taskName);
            existingTask.timerTask.cancel();
        }

        logger.debugf("Starting task '%s' with interval '%d'", taskName, intervalMillis);
        timer.schedule(task, intervalMillis, intervalMillis);
    }

    @Override
    public void scheduleTask(ScheduledTask scheduledTask, long intervalMillis, String taskName) {
        ScheduledTaskRunner scheduledTaskRunner = new ScheduledTaskRunner(session.getKeycloakSessionFactory(), scheduledTask, transactionTimeout);
        this.schedule(scheduledTaskRunner, intervalMillis, taskName);
    }

    @Override
    public TimerTaskContext cancelTask(String taskName) {
        TimerTaskContextImpl existingTask = factory.removeTask(taskName);
        if (existingTask != null) {
            logger.debugf("Cancelling task '%s'", taskName);
            existingTask.timerTask.cancel();
        }

        return existingTask;
    }

    @Override
    public void close() {
        // do nothing
    }

}
